# Configuração de Perfis Quarkus - Simulação de Empréstimos

Este documento explica a configuração dos perfis Quarkus para desenvolvimento e produção da aplicação de simulação de empréstimos.

## Perfis Configurados

### 1. Perfil de Desenvolvimento (`dev`)

Características principais:
- Base path HTTP global: `/emprestimos` (quarkus.http.root-path)
- Banco H2 em memória (datasource padrão) para rapidez
- SQL Server (datasource `produtos`) via Dev Services com imagem `mcr.microsoft.com/mssql/server:2022-latest`
- Hot reload habilitado (`quarkus.live.reload.enabled=true`)
- Logs com nível `INFO` e SQL detalhado (categorias Hibernate em DEBUG/TRACE)
- Health UI habilitada
- Geração de schema: default H2 `drop-and-create`; `produtos` `drop-and-create` + carga `import.sql`

Como executar:
```bash
# Opção 1: Usar o script
./run-profile.sh dev

# Opção 2: Maven direto
./mvnw quarkus:dev -Dquarkus.profile=dev
```

Datasources:
- H2 (default): `jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`
- SQL Server (produtos): Dev Services (container automático)

### 2. Perfil de Produção (`prod`)

Características principais:
- Base path HTTP global: `/emprestimos`
- H2 (default) persistente em arquivo: `/deployments/data/prod-simulacoes` (mapeado pelo volume `./data` no Docker)
- SQL Server Azure (datasource `produtos`) configurado por variáveis de ambiente
- Pool de conexões ajustado (produtos: min 5, max 20; H2: min 3, max 10)
- Geração de schema: `update` para default e `produtos`
- Logs em `INFO` (Hibernate SQL em `WARN`), live reload desabilitado
- Cache específico para `produtos` mais agressivo (30m; tamanho 5000)

Como executar:
```bash
# Opção 1: Usar o script
./run-profile.sh prod

# Opção 2: Maven direto
./mvnw clean package -Dquarkus.profile=prod
java -Dquarkus.profile=prod -jar target/quarkus-app/quarkus-run.jar
```

Datasources (variáveis de ambiente):
- SQL Server (produtos):
  - `QUARKUS_DATASOURCE_PRODUTOS_USERNAME`
  - `QUARKUS_DATASOURCE_PRODUTOS_PASSWORD`
  - `QUARKUS_DATASOURCE_PRODUTOS_JDBC_URL`
- H2 (default): persiste em `/deployments/data/prod-simulacoes`

### 3. Perfil de Teste (`test`)

- Configurações isoladas para testes
- Bancos em memória
- Dados de teste carregados automaticamente

Como executar:
```bash
# Opção 1: Usar o script
./run-profile.sh test

# Opção 2: Maven direto
./mvnw test -Dquarkus.profile=test
```

## Estrutura de Arquivos

```
src/main/resources/
├── application.properties          # Configurações comuns (root-path, caches, auditoria, OpenAPI)
├── application-dev.properties      # Dev Services SQL Server, H2 em memória, logs detalhados
└── application-prod.properties     # Datasources reais, pool de conexões, geração update, caches

src/test/resources/
└── application.properties          # Configurações para testes
```

## Principais Configurações por Perfil

Desenvolvimento (chaves relevantes):
- `quarkus.http.root-path=/emprestimos`
- `quarkus.hibernate-orm.database.generation=drop-and-create`
- `quarkus.hibernate-orm.produtos.database.generation=drop-and-create`
- `quarkus.hibernate-orm.produtos.sql-load-script=import.sql`
- `quarkus.datasource.produtos.devservices.enabled=true`
- `quarkus.log.category."org.hibernate.SQL".level=DEBUG`
- `quarkus.log.category."org.hibernate.type.descriptor.sql.BasicBinder".level=TRACE`
- `quarkus.live.reload.enabled=true`

Produção (chaves relevantes):
- `quarkus.http.root-path=/emprestimos`
- `quarkus.hibernate-orm.database.generation=update`
- `quarkus.hibernate-orm.produtos.database.generation=update`
- `quarkus.log.level=INFO`
- `quarkus.log.category."org.hibernate.SQL".level=WARN`
- `quarkus.live.reload.enabled=false`
- Pool JDBC (produtos): `min-size=5`, `max-size=20`, `acquisition-timeout=30s`
- Pool JDBC (H2): `min-size=3`, `max-size=10`, `acquisition-timeout=30s`

## Cache

Chaves e políticas padrão:
- `simulacoes-por-produto`: initial-capacity=100, maximum-size=1000, expire-after-write=30m
- `simulacoes-agregadas-por-produto`: initial-capacity=100, maximum-size=1000, expire-after-write=30m
- Global: `quarkus.cache.caffeine.expire-after-write=10m`

Produção (adicional):
- `produtos`: expire-after-write=30m, maximum-size=5000

## Auditoria

- Habilitada por padrão: `auditoria.habilitada=true`
- Retenção: `auditoria.retencao.dias=90`
- Limpeza automática: `auditoria.limpeza.automatica=true`

## OpenAPI e Health

- OpenAPI configurado (título/versão/servidores) para documentação
- Health UI habilitada: `quarkus.smallrye-health.ui.enable=true`

## Integração Azure Event Hubs

Variáveis:
- `AZURE_EVENTHUBS_CONNECTION_STRING`
- `AZURE_EVENTHUBS_HUB_NAME` (padrão: `simulacoes`)

## Scripts Utilitários

- `run-profile.sh`: Script principal para executar com diferentes perfis
- `simular.sh`: Script existente para simulações (mantido)

## Troubleshooting

- Erro de conexão com SQL Server: valide as credenciais/URL nas variáveis `QUARKUS_DATASOURCE_PRODUTOS_*`
- Dev Services não inicia: verifique se o Docker está rodando e acessível
- Cache não funciona: verifique se a extensão quarkus-cache está no pom.xml

## Comandos Úteis

```bash
# Verificar qual perfil está ativo
./mvnw quarkus:info

# Executar testes específicos
./mvnw test -Dtest=SimulacaoResourceTest

# Build para produção
./mvnw clean package -Dquarkus.profile=prod

# Executar em modo nativo (produção)
./mvnw package -Dnative -Dquarkus.profile=prod
```

## Variáveis de Ambiente (Produção)

Para produção, considere usar variáveis de ambiente para dados sensíveis:

```bash
export QUARKUS_DATASOURCE_PRODUTOS_USERNAME=usuario
export QUARKUS_DATASOURCE_PRODUTOS_PASSWORD=sua_senha_segura
export QUARKUS_DATASOURCE_PRODUTOS_JDBC_URL="jdbc:sqlserver://dbhackathon.database.windows.net:1433;databaseName=hack;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;"
export AZURE_EVENTHUBS_CONNECTION_STRING=sua_connection_string
export AZURE_EVENTHUBS_HUB_NAME=simulacoes
```
