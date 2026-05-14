# Preparacao do Firebird no servidor Linux

Este guia lista os comandos necessarios no servidor Linux para permitir que a stack Docker Swarm acesse o Firebird, receba o banco `MIGRADOR.FDB` e use o alias `EAGLEERP` durante a migracao.

Os comandos abaixo assumem que a stack usa o arquivo `docker-stack.portainer.yml` e que os volumes bind ficam em:

```text
/opt/migracao-eagle/firebird
```

## 1. Criar estrutura de pastas

```bash
mkdir -p /opt/migracao-eagle/firebird/data/work
mkdir -p /opt/migracao-eagle/firebird/etc
```

Uso das pastas:

```text
/opt/migracao-eagle/firebird/data/MIGRADOR.FDB      Banco migrador original
/opt/migracao-eagle/firebird/data/work/EAGLEERP.FDB Banco limpo copiado pela aplicacao
/opt/migracao-eagle/firebird/etc/aliases.conf       Alias do Firebird
```

## 2. Enviar o banco MIGRADOR.FDB

No seu computador, envie o arquivo para o servidor:

```bash
scp MIGRADOR.FDB root@SEU_SERVIDOR:/opt/migracao-eagle/firebird/data/MIGRADOR.FDB
```

Ou envie pelo SFTP/Portainer/File Browser, desde que o arquivo fique exatamente em:

```text
/opt/migracao-eagle/firebird/data/MIGRADOR.FDB
```

## 3. Criar o alias EAGLEERP

O backend conecta no Firebird usando o alias `EAGLEERP`. Esse alias precisa apontar para o banco limpo que a aplicacao cria em `data/work`.

```bash
cat > /opt/migracao-eagle/firebird/etc/aliases.conf <<'EOF'
EAGLEERP = /firebird/data/work/EAGLEERP.FDB
EOF
```

## 4. Liberar permissoes

Use estas permissoes para evitar bloqueio de leitura/escrita entre o host Linux, o container Firebird e o container backend:

```bash
chmod 777 /opt/migracao-eagle/firebird
chmod 777 /opt/migracao-eagle/firebird/data
chmod 777 /opt/migracao-eagle/firebird/data/work
chmod 777 /opt/migracao-eagle/firebird/etc
chmod 666 /opt/migracao-eagle/firebird/etc/aliases.conf
chmod 666 /opt/migracao-eagle/firebird/data/MIGRADOR.FDB
```

Se o arquivo `EAGLEERP.FDB` ja existir no work dir, libere permissao nele tambem:

```bash
chmod 666 /opt/migracao-eagle/firebird/data/work/EAGLEERP.FDB
```

## 5. Reiniciar os servicos da stack

Depois de criar o alias ou trocar o banco `MIGRADOR.FDB`, reinicie o Firebird:

```bash
docker service update --force migracao-eagle_firebird
```

Depois reinicie o backend:

```bash
docker service update --force migracao-eagle_backend
```

Acompanhe os servicos:

```bash
docker service ps migracao-eagle_firebird
docker service ps migracao-eagle_backend
docker service logs --since 2m migracao-eagle_firebird
docker service logs --since 2m migracao-eagle_backend
```

## 6. Validar o banco MIGRADOR.FDB

Verifique se o Firebird consegue abrir o banco migrador:

```bash
CID=$(docker ps -q --filter name=migracao-eagle_firebird)

printf 'show tables;\nquit;\n' | docker exec -i "$CID" /bin/bash -lc \
  '/usr/local/firebird/bin/isql -user SYSDBA -password "$ISC_PASSWORD" /firebird/data/MIGRADOR.FDB'
```

Se quiser conferir uma tabela especifica:

```bash
CID=$(docker ps -q --filter name=migracao-eagle_firebird)

printf 'show table PRODUTOS;\nquit;\n' | docker exec -i "$CID" /bin/bash -lc \
  '/usr/local/firebird/bin/isql -user SYSDBA -password "$ISC_PASSWORD" /firebird/data/MIGRADOR.FDB'
```

## 7. Validar o alias EAGLEERP

O arquivo `/opt/migracao-eagle/firebird/data/work/EAGLEERP.FDB` normalmente e criado pela aplicacao quando a migracao completa e iniciada. Depois que ele existir, valide o alias:

```bash
CID=$(docker ps -q --filter name=migracao-eagle_firebird)

printf 'show tables;\nquit;\n' | docker exec -i "$CID" /bin/bash -lc \
  '/usr/local/firebird/bin/isql -user SYSDBA -password "$ISC_PASSWORD" EAGLEERP'
```

Se aparecer erro como:

```text
I/O error during "open" operation for file "EAGLEERP"
```

confirme estes pontos:

- `/opt/migracao-eagle/firebird/etc/aliases.conf` existe.
- O conteudo do alias e `EAGLEERP = /firebird/data/work/EAGLEERP.FDB`.
- O servico `migracao-eagle_firebird` foi reiniciado depois da criacao do alias.
- O arquivo `/opt/migracao-eagle/firebird/data/work/EAGLEERP.FDB` existe.
- O arquivo `EAGLEERP.FDB` tem permissao de leitura/escrita.

## 8. Limpar inicializacao quebrada do Firebird

Se o Firebird ja subiu antes com volume incompleto e ficou com arquivos internos inconsistentes, remova apenas os arquivos internos gerados pela imagem, preservando a pasta `data`:

```bash
rm -rf /opt/migracao-eagle/firebird/etc
rm -rf /opt/migracao-eagle/firebird/system
rm -rf /opt/migracao-eagle/firebird/log

mkdir -p /opt/migracao-eagle/firebird/etc
cat > /opt/migracao-eagle/firebird/etc/aliases.conf <<'EOF'
EAGLEERP = /firebird/data/work/EAGLEERP.FDB
EOF

chmod 777 /opt/migracao-eagle/firebird/etc
chmod 666 /opt/migracao-eagle/firebird/etc/aliases.conf

docker service update --force migracao-eagle_firebird
```

Nao apague `/opt/migracao-eagle/firebird/data`, pois ali ficam o `MIGRADOR.FDB` e os bancos de trabalho.

