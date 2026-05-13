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
mkdir -p /opt/migracao-eagle/firebird/data/work
```

Envie o banco migrador para:

```text
/opt/migracao-eagle/firebird/data/MIGRADOR.FDB
```

Ajuste permissoes:

```bash
chmod 777 /opt/migracao-eagle/firebird
chmod 777 /opt/migracao-eagle/firebird/data
chmod 777 /opt/migracao-eagle/firebird/data/work
chmod 666 /opt/migracao-eagle/firebird/data/MIGRADOR.FDB
```

Se o Firebird ja tentou subir antes e ficou com inicializacao quebrada, limpe apenas os arquivos internos gerados pela imagem, preservando `/opt/migracao-eagle/firebird/data`:

```bash
rm -rf /opt/migracao-eagle/firebird/etc
rm -rf /opt/migracao-eagle/firebird/system
rm -rf /opt/migracao-eagle/firebird/log
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

5. Configure as variaveis do `.env.prod.example` no Portainer.

Como a VPS ja usa Traefik, a stack nao publica `80` e `443` diretamente. Configure `TRAEFIK_NETWORK` com o nome da rede externa usada pelo Traefik. No servidor atual, a rede encontrada e `wize-net`.

Para descobrir o nome no servidor:

```bash
docker network ls
```

Nao exponha publicamente as portas `3050`, `5432` ou `8080`. O acesso publico entra pelo Traefik.
