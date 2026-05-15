# Migracao Eagle

Sistema interno para controlar, validar e executar migracoes de bases Eagle Gestao. A aplicacao organiza o processo por cliente, valida planilhas de entrada, importa dados validos para o banco `MIGRADOR.FDB`, executa procedures no Firebird e disponibiliza o banco final migrado para download.

## Funcionalidades

- Criacao de processo de migracao por cliente, CNPJ e versao limpa do Eagle Gestao.
- Selecao de banco limpo por versao, atualmente `2025.001`, `2025.002` e `2026.001`.
- Upload e validacao de planilhas por modulo:
  - `CLIENTES`
  - `FORNECEDORES`
  - `PRODUTOS`
  - `ARECEBER`
  - `APAGAR`
- Validacao de colunas obrigatorias, tipos de dados, tamanhos, campos numericos, datas e regras de layout.
- Geracao de relatorio CSV com os erros encontrados por planilha.
- Importacao das linhas validas para as tabelas do banco `MIGRADOR`.
- Execucao completa da migracao, com atualizacao de status a cada procedure.
- Execucao individual da proxima procedure pendente ou de uma procedure especifica.
- Download do banco final migrado.
- Armazenamento dos bancos limpos e bancos finais em S3.
- Interface web para acompanhar processos, planilhas, validacoes, progresso e resultado final.

## Arquitetura

O projeto e dividido em backend, frontend e infraestrutura Docker.

```text
Backend   Spring Boot 4, Java 21, JPA, Flyway, PostgreSQL, Firebird Jaybird, AWS S3
Frontend  Vue 3, TypeScript, Vite, lucide-vue-next
Banco     PostgreSQL para controle da migracao
Firebird  MIGRADOR.FDB e banco de trabalho EAGLEERP.FDB
Deploy    Docker Swarm, Portainer, Traefik e GHCR
```

O backend expoe a API em `/api`. O frontend e servido por Nginx e acessa o backend pelo mesmo dominio usando proxy interno.

## Fluxo da migracao

1. O usuario cria um processo informando cliente, CNPJ, versao limpa e configuracoes padrao.
2. O sistema registra as procedures esperadas para o processo.
3. O usuario envia as planilhas dos modulos desejados.
4. Cada planilha e lida, normalizada e validada conforme o layout do modulo.
5. Se houver erros, o sistema mostra as falhas e permite baixar um CSV de validacao.
6. As linhas validas sao importadas para as tabelas do banco `MIGRADOR`.
7. O sistema baixa ou reutiliza o banco limpo da versao escolhida.
8. O banco limpo e copiado para o diretorio de trabalho como `EAGLEERP.FDB`.
9. O backend executa as procedures de migracao no Firebird.
10. A barra de progresso e atualizada conforme cada procedure muda de status.
11. O banco final e enviado ao S3 e fica disponivel para download.

## Estrutura principal

```text
src/main/java/com/example/demo/migration
  config/       Configuracoes da migracao
  controller/   Endpoints REST e DTOs
  domain/       Entidades JPA e enums
  integration/  Cliente Firebird
  repository/   Repositorios Spring Data
  service/      Regras de validacao, importacao e execucao

src/main/resources/db/migration
  Scripts Flyway do banco PostgreSQL de controle

frontend/src
  Interface Vue, cliente da API e estilos

docs
  Documentacoes operacionais de deploy, Firebird e S3
```

## Endpoints principais

```text
POST /api/migration-processes
GET  /api/migration-processes
GET  /api/migration-processes/{processId}

POST /api/migration-processes/{processId}/sheets/{module}
GET  /api/migration-processes/{processId}/sheets/{module}
GET  /api/migration-processes/{processId}/sheets/{module}/errors.csv

POST /api/migration-processes/{processId}/import-migrador
POST /api/migration-processes/{processId}/run-complete
POST /api/migration-processes/{processId}/procedures/execute-next
POST /api/migration-processes/{processId}/procedures/{procedureName}/execute

GET  /api/migration-processes/{processId}/final-database
GET  /api/migration-layouts
GET  /api/clean-database-templates
```

## Requisitos locais

- Java 21
- Maven Wrapper incluso no projeto
- Node.js compativel com Vite
- Docker, se quiser subir dependencias localmente

Para rodar os testes do backend no Windows, garanta que o `JAVA_HOME` aponte para o JDK 21:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.7.6-hotspot'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw test
```

Para rodar o frontend:

```powershell
cd frontend
npm install
npm run dev
```

Para validar o build do frontend:

```powershell
cd frontend
npm run build
```

## Configuracoes do backend

As configuracoes ficam em `src/main/resources/application.properties` e podem ser sobrescritas por variaveis de ambiente.

Principais variaveis:

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD

AWS_REGION
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY

MIGRATION_CLEAN_DB_S3_BUCKET
MIGRATION_CLEAN_DB_2025_001_S3_KEY
MIGRATION_CLEAN_DB_2025_002_S3_KEY
MIGRATION_CLEAN_DB_2026_001_S3_KEY
MIGRATION_FINAL_DB_S3_BUCKET
MIGRATION_FINAL_DB_S3_PREFIX

APP_MIGRATION_WORK_DIR
APP_MIGRATION_FIREBIRD_ENABLED
APP_MIGRATION_FIREBIRD_JDBC_URL
APP_MIGRATION_FIREBIRD_USERNAME
APP_MIGRATION_FIREBIRD_PASSWORD
APP_MIGRATION_FIREBIRD_MIGRATOR_DATABASE
APP_MIGRATION_FIREBIRD_EAGLE_ALIAS_NAME
```

Exemplo de valores para producao esta em `.env.prod.example`.

## Firebird

O container Firebird precisa acessar:

```text
/firebird/data/MIGRADOR.FDB
/firebird/data/work/EAGLEERP.FDB
```

O alias `EAGLEERP` deve apontar para:

```text
/firebird/data/work/EAGLEERP.FDB
```

No Docker Swarm, o volume do host usado pela stack e:

```text
/opt/migracao-eagle/firebird
```

Consulte o passo a passo completo em `docs/firebird-linux-setup.md`.

## S3

O S3 e usado para:

- armazenar bancos limpos por versao;
- salvar bancos finais migrados;
- permitir que somente o servidor da aplicacao acesse o bucket.

Os buckets devem permanecer privados. Nao use ACL publica. Para limitar acesso ao IP da VPS e configurar IAM, veja `docs/s3-server-only-access.md`.

## Deploy no Portainer

O deploy de producao usa Docker Swarm e o arquivo:

```text
docker-stack.portainer.yml
```

Imagens esperadas no GHCR:

```text
ghcr.io/raylann-lopes/migracao-eagle-backend:latest
ghcr.io/raylann-lopes/migracao-eagle-frontend:latest
```

Para publicar uma imagem especifica em um servico ja criado:

```bash
docker service update --with-registry-auth --image ghcr.io/raylann-lopes/migracao-eagle-backend:<tag> migracao-eagle_backend
docker service update --with-registry-auth --image ghcr.io/raylann-lopes/migracao-eagle-frontend:<tag> migracao-eagle_frontend
```

O acesso publico deve entrar pelo Traefik. Nao exponha diretamente as portas `3050`, `5432` ou `8080`.

Documentacao complementar:

- `docs/deploy-portainer-swarm.md`
- `docs/firebird-linux-setup.md`
- `docs/s3-server-only-access.md`

## Banco de controle e Flyway

O PostgreSQL guarda apenas o controle do processo de migracao: processos, planilhas, linhas validadas e status das procedures. As alteracoes de schema sao controladas pelo Flyway em:

```text
src/main/resources/db/migration
```

Atencao: a migracao `V2__recreate_control_schema_with_identity_ids.sql` recria as tabelas de controle para usar IDs numericos com identity. Ela afeta o historico de controle da aplicacao, mas nao altera os bancos Firebird `MIGRADOR.FDB` ou `EAGLEERP.FDB`.

## Seguranca operacional

- Mantenha as credenciais AWS somente no Portainer ou em secret manager.
- Use uma policy S3 restrita ao IP publico da VPS.
- Mantenha Traefik com certificado Let's Encrypt valido.
- Nao publique Firebird, PostgreSQL ou backend diretamente na internet.
- Revise o acesso ao GHCR se as imagens estiverem privadas.
- Altere senhas padrao antes de subir a stack em producao.

## Comandos uteis no servidor

Ver servicos:

```bash
docker service ls
```

Ver logs do backend:

```bash
docker service logs --since 5m migracao-eagle_backend
```

Testar API pelo dominio:

```bash
curl -i https://eagle.wizeflowsolutions.cloud/api/migration-processes
```

Testar certificado:

```bash
curl -vI https://eagle.wizeflowsolutions.cloud
```

## Observacoes de desenvolvimento

- O backend usa Lombok para reduzir codigo repetitivo em entidades e classes de configuracao.
- Os DTOs ficam separados entre `request` e `response`.
- Os IDs das entidades de controle usam `Long` com `GenerationType.IDENTITY`.
- A barra de progresso depende do polling do frontend em cima do endpoint de detalhe do processo.
- As planilhas aceitas devem seguir os layouts registrados em `MigrationLayoutRegistry`.
