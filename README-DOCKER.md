# Executando em Produção com Docker Compose

Este guia mostra como subir o projeto em modo de produção usando Docker Compose.

## Visão geral
- Perfil do Quarkus: `prod`
- Porta (container): `8080` (mapeada para `8080` no host no compose)
- Base path HTTP: `/emprestimos`
- H2 (prod): arquivo persistente em `./data/prod-simulacoes` (volume `./data`)
- Logs: `./logs/application.log` (volume `./logs`)
- Variáveis de ambiente: definidas diretamente em `docker-compose.yml`

## Pré-requisitos
- Docker 20.10+ e Docker Compose v2
- Conexão com a internet
## Passo a passo
1) (Opcional Linux) Evite problemas de permissão nas pastas montadas:
   
   mkdir -p data logs
   sudo chown -R 185:185 data logs

2) Build e subida dos serviços:
   
   docker compose up -d --build

3) Ver logs e saúde:
   
   docker compose logs -f simulacao-emprestimo
   curl -f http://localhost:8080/emprestimos/q/health

4) Acesso à API (exemplo):
   - Base: http://localhost:8080/emprestimos

## Onde configurar variáveis
- Todas as variáveis (SQL Server, Event Hubs, H2, log level) estão definidas em `docker-compose.yml` na seção `environment` do serviço `simulacao-emprestimo`.
- Ajuste valores diretamente no compose conforme seu ambiente (ex.: usuário/senha/URL JDBC do SQL Server, connection string do Event Hubs, etc.).

## Persistência e logs
- Dados H2: `./data` -> `/deployments/data`
- Logs: `./logs` -> `/deployments/logs`
- Para limpar, pare os serviços e remova `data/` e `logs/` (isso apaga dados).

## Troubleshooting
- Porta em uso: edite o mapeamento de portas em `docker-compose.yml`.
- Permissões (Linux): se não conseguir gravar em `data/` ou `logs/`, rode `sudo chown -R 185:185 data logs`.
- SQL Server: ajuste `QUARKUS_DATASOURCE_PRODUTOS_*` no compose e valide conectividade.
- Event Hubs: ajuste `AZURE_EVENTHUBS_CONNECTION_STRING` (e opcional `AZURE_EVENTHUBS_HUB_NAME`).

## Notas de segurança
- O compose contém segredos em texto claro (credenciais/connection strings). Mantenha o repositório privado e limite o acesso. Em produção, prefira injeção de segredos por vault/secret manager.

## Detalhes técnicos
- Build multi-stage: `src/main/docker/Dockerfile.jvm-multi` compila via Maven (stage builder) e produz a imagem runtime (OpenJDK 17 UBI).
