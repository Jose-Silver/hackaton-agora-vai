# Configuração de Perfis Quarkus - Simulação de Empréstimos

Este documento explica a configuração dos perfis Quarkus para desenvolvimento e produção da aplicação de simulação de empréstimos.

## Perfis Configurados

### 1. Perfil de Desenvolvimento (`dev`)

**Características:**
- Banco H2 em memória para desenvolvimento rápido
- SQL Server via Dev Services (container automático)
- Hot reload habilitado
- Logs detalhados (incluindo SQL)
- Dev UI habilitado
- Cache com TTL menor para facilitar testes

**Como executar:**
```bash
# Opção 1: Usar o script
./run-profile.sh dev

# Opção 2: Maven direto
./mvnw quarkus:dev -Dquarkus.profile=dev
```

**Datasources:**
- **H2**: Base em memória (`jdbc:h2:mem:testdb`)
- **SQL Server**: Container automático via Dev Services

### 2. Perfil de Produção (`prod`)

**Características:**
- Banco H2 persistente em arquivo
- SQL Server Azure com configurações reais
- Pool de conexões otimizado
- Logs mínimos (WARN level)
- Cache agressivo para melhor performance
- Dev UI desabilitado
- CORS desabilitado
- Swagger UI desabilitado

**Como executar:**
```bash
# Opção 1: Usar o script
./run-profile.sh prod

# Opção 2: Maven direto
./mvnw clean package -Dquarkus.profile=prod
java -Dquarkus.profile=prod -jar target/quarkus-app/quarkus-run.jar
```

**Datasources:**
- **H2**: Arquivo persistente (`./data/prod-simulacoes`)
- **SQL Server**: Azure Database (`dbhackathon.database.windows.net`)

### 3. Perfil de Teste (`test`)

**Características:**
- Configurações isoladas para testes
- Bancos em memória
- Dados de teste carregados automaticamente

**Como executar:**
```bash
# Opção 1: Usar o script
./run-profile.sh test

# Opção 2: Maven direto
./mvnw test -Dquarkus.profile=test
```

## Estrutura de Arquivos

```
src/main/resources/
├── application.properties          # Configurações comuns
├── application-dev.properties      # Específicas para desenvolvimento
└── application-prod.properties     # Específicas para produção

src/test/resources/
└── application.properties          # Configurações para testes
```

## Principais Configurações por Perfil

### Desenvolvimento
- `quarkus.hibernate-orm.database.generation=drop-and-create`
- `quarkus.log.category."org.hibernate.SQL".level=DEBUG`
- `quarkus.live.reload.enabled=true`
- `quarkus.datasource.devservices=true`

### Produção
- `quarkus.hibernate-orm.database.generation=validate`
- `quarkus.log.level=WARN`
- `quarkus.live.reload.enabled=false`
- Pool de conexões configurado (min: 5, max: 20)

## Cache

### Desenvolvimento
- TTL menor (5 minutos)
- Tamanho reduzido (100 itens)

### Produção
- TTL maior (30 minutos)
- Tamanho maior (5000 itens)

## Auditoria

### Desenvolvimento
- Auditoria desabilitada para Azure Event Hubs
- Logs detalhados para debug

### Produção
- Auditoria completa habilitada
- Integração com Azure Event Hubs
- Retenção de 365 dias
- Limpeza automática

## Scripts Utilitários

- `run-profile.sh`: Script principal para executar com diferentes perfis
- `simular.sh`: Script existente para simulações (mantido)

## Troubleshooting

### Problema: Erro de conexão com SQL Server
**Solução**: Verificar se as credenciais do Azure estão corretas no perfil prod

### Problema: Dev Services não inicia
**Solução**: Verificar se o Docker está rodando e acessível

### Problema: Cache não funciona
**Solução**: Verificar se a extensão quarkus-cache está no pom.xml

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
export QUARKUS_DATASOURCE_PRODUTOS_PASSWORD=sua_senha_segura
export AZURE_EVENTHUBS_CONNECTION_STRING=sua_connection_string
```
