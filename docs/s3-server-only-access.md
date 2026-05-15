# S3 restrito ao servidor da aplicacao

Este guia configura o S3 para aceitar acesso somente a partir do servidor Linux onde a stack roda.

O objetivo e:

- manter o bucket privado;
- permitir leitura dos bancos limpos;
- permitir gravacao/leitura dos bancos finais;
- bloquear qualquer chamada ao bucket que nao venha do IP publico do servidor.

## 1. Confirmar o IP publico de saida do servidor

No servidor Linux, rode:

```bash
curl -4 https://checkip.amazonaws.com
```

No ambiente atual, o dominio aponta para:

```text
195.200.4.197
```

Mesmo assim, use o comando acima para confirmar o IP de saida real. A policy do S3 deve usar esse IP com `/32`.

Exemplo:

```text
195.200.4.197/32
```

## 2. Criar buckets

Voce pode usar um bucket unico:

```text
eagle-migracao-templates
```

Com esta organizacao:

```text
templates/2025.001/EAGLEERP.FDB
templates/2025.002/EAGLEERP.FDB
templates/2026.001/EAGLEERP.FDB
bancos-migrados/
```

Ou pode separar:

```text
eagle-migracao-templates
eagle-migracao-final
```

O projeto ja aceita ambos via variaveis:

```env
MIGRATION_CLEAN_DB_S3_BUCKET=eagle-migracao-templates
MIGRATION_FINAL_DB_S3_BUCKET=eagle-migracao-templates
MIGRATION_FINAL_DB_S3_PREFIX=bancos-migrados
```

## 3. Bloquear acesso publico do bucket

No bucket S3, mantenha habilitado:

```text
Block all public access: ON
```

Nao crie ACL publica.

## 4. Criar usuario IAM para a aplicacao

Crie um usuario IAM, por exemplo:

```text
migracao-eagle-s3
```

Gere uma access key e configure no Portainer:

```env
AWS_REGION=sa-east-1
AWS_ACCESS_KEY_ID=AKIA...
AWS_SECRET_ACCESS_KEY=...
```

Use a regiao real do bucket. Se o bucket estiver em `us-east-1`, mantenha `us-east-1`.

## 5. Policy IAM minima para o usuario

Anexe esta policy ao usuario IAM, ajustando o nome do bucket:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "ListBucketOnlyNeededPrefixes",
      "Effect": "Allow",
      "Action": "s3:ListBucket",
      "Resource": "arn:aws:s3:::eagle-migracao-templates",
      "Condition": {
        "StringLike": {
          "s3:prefix": [
            "templates/*",
            "bancos-migrados/*"
          ]
        }
      }
    },
    {
      "Sid": "ReadCleanTemplates",
      "Effect": "Allow",
      "Action": [
        "s3:GetObject"
      ],
      "Resource": "arn:aws:s3:::eagle-migracao-templates/templates/*"
    },
    {
      "Sid": "ReadWriteFinalDatabases",
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject"
      ],
      "Resource": "arn:aws:s3:::eagle-migracao-templates/bancos-migrados/*"
    }
  ]
}
```

Se usar buckets separados, troque os ARNs conforme o bucket de template e o bucket de banco final.

## 6. Bucket policy para aceitar somente o IP do servidor

No bucket S3, adicione uma bucket policy com `Deny` para qualquer origem fora do IP do servidor.

Troque `195.200.4.197/32` pelo IP confirmado no passo 1.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "DenyRequestsOutsideApplicationServer",
      "Effect": "Deny",
      "Principal": "*",
      "Action": "s3:*",
      "Resource": [
        "arn:aws:s3:::eagle-migracao-templates",
        "arn:aws:s3:::eagle-migracao-templates/*"
      ],
      "Condition": {
        "NotIpAddress": {
          "aws:SourceIp": "195.200.4.197/32"
        }
      }
    }
  ]
}
```

Essa policy e propositalmente forte: mesmo com uma access key valida, a AWS negara o acesso se a chamada nao sair do IP do servidor.

## 7. Opcional: restringir tambem ao usuario IAM da aplicacao

Para ficar ainda mais fechado, adicione outro `Deny` bloqueando qualquer principal diferente do usuario IAM da aplicacao.

Troque:

```text
123456789012
```

pelo ID da sua conta AWS.

```json
{
  "Sid": "DenyOtherIamPrincipals",
  "Effect": "Deny",
  "Principal": "*",
  "Action": "s3:*",
  "Resource": [
    "arn:aws:s3:::eagle-migracao-templates",
    "arn:aws:s3:::eagle-migracao-templates/*"
  ],
  "Condition": {
    "StringNotEquals": {
      "aws:PrincipalArn": "arn:aws:iam::123456789012:user/migracao-eagle-s3"
    }
  }
}
```

Se adicionar este bloco, a policy completa do bucket tera dois `Statement`: um por IP e outro por usuario.

## 8. Variaveis no Portainer

Configure no Portainer:

```env
AWS_REGION=sa-east-1
AWS_ACCESS_KEY_ID=AKIA...
AWS_SECRET_ACCESS_KEY=...

MIGRATION_CLEAN_DB_S3_BUCKET=eagle-migracao-templates
MIGRATION_CLEAN_DB_2025_001_S3_KEY=templates/2025.001/EAGLEERP.FDB
MIGRATION_CLEAN_DB_2025_002_S3_KEY=templates/2025.002/EAGLEERP.FDB
MIGRATION_CLEAN_DB_2026_001_S3_KEY=templates/2026.001/EAGLEERP.FDB

MIGRATION_FINAL_DB_S3_BUCKET=eagle-migracao-templates
MIGRATION_FINAL_DB_S3_PREFIX=bancos-migrados
```

Depois atualize o backend:

```bash
docker service update --force migracao-eagle_backend
```

## 9. Testar do servidor

Instale ou use a AWS CLI no servidor.

Teste leitura:

```bash
aws s3 ls s3://eagle-migracao-templates/templates/
```

Teste upload no prefixo permitido:

```bash
echo teste > /tmp/s3-test.txt
aws s3 cp /tmp/s3-test.txt s3://eagle-migracao-templates/bancos-migrados/s3-test.txt
aws s3 rm s3://eagle-migracao-templates/bancos-migrados/s3-test.txt
```

Se esse teste funciona no servidor e falha fora dele, a restricao por IP esta correta.

## 10. Pontos importantes

- Se o IP publico do servidor mudar, o S3 vai bloquear a aplicacao ate a bucket policy ser atualizada.
- Nao use link publico do S3 para download. O sistema baixa pelo backend, usando a credencial do servidor.
- Nao exponha `AWS_ACCESS_KEY_ID` e `AWS_SECRET_ACCESS_KEY` no frontend.
- Se a aplicacao rodar em outro servidor ou outro node Swarm, adicione o IP publico desse node na policy.

Para multiplos IPs:

```json
"aws:SourceIp": [
  "195.200.4.197/32",
  "OUTRO_IP/32"
]
```
