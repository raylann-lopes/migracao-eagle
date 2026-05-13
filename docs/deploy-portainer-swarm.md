# Deploy no Portainer com Docker Swarm

## DNS

Crie um registro `A` no DNS:

```text
Nome: eagle
Valor: 195.200.4.197
```

O dominio usado pela stack e:

```text
eagle.wizeflowsolutions.cloud
```

## Arquivos no servidor

Antes de subir a stack, prepare a pasta usada pelo Firebird:

```bash
mkdir -p /opt/migracao-eagle/firebird/work

cat > /opt/migracao-eagle/firebird/aliases.conf <<'EOF'
EAGLEERP = /firebird/data/work/EAGLEERP.FDB
EOF
```

Envie o banco migrador para:

```text
/opt/migracao-eagle/firebird/MIGRADOR.FDB
```

Ajuste permissoes:

```bash
chmod 666 /opt/migracao-eagle/firebird/MIGRADOR.FDB
chmod 777 /opt/migracao-eagle/firebird/work
```

## GitHub Container Registry

As imagens sao publicadas pelo GitHub Actions:

```text
ghcr.io/raylann-lopes/migracao-eagle-backend:latest
ghcr.io/raylann-lopes/migracao-eagle-frontend:latest
```

Se o pacote estiver privado no GHCR, cadastre no Portainer um registry com usuario GitHub e token com permissao `read:packages`.

## Stack no Portainer

No Portainer:

1. Acesse `Stacks`.
2. Crie uma stack usando Git Repository.
3. Informe o repositorio `raylann-lopes/migracao-eagle`.
4. Use o arquivo:

```text
docker-stack.portainer.yml
```

5. Configure as variaveis do `.env.portainer.example` no Portainer.

Nao exponha publicamente as portas `3050`, `5432` ou `8080`. A stack publica somente `80` e `443` pelo Caddy.
