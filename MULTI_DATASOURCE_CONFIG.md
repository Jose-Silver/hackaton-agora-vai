# Configuração Multi-Datasource do Projeto Quarkus

Este projeto está configurado com dois perfis (desenvolvimento e produção) utilizando múltiplas fontes de dados.

## Perfis Configurados

### Perfil de Desenvolvimento (`dev`)
- **SQL Server**: Utiliza Dev Services do Quarkus (container automaticamente iniciado)
  - Para consultar produtos da tabela `dbo.Produto`
  - Dados inicializados automaticamente via `import-produtos.sql`
- **H2**: Base de dados em arquivo para persistir simulações
  - Console H2 habilitado em `/h2`
  - Dados persistidos em `./data/testdb`

### Perfil de Produção (`prod`)
- **SQL Server**: Conecta ao banco Azure
  - URL: `dbhackathon.database.windows.net:1433`
  - Database: `hack`
  - Usuário: `hack`
  - Senha: `Password23`
  - Tabela: `dbo.Produto`
- **H2**: Base de dados em arquivo para persistir simulações
  - Console H2 desabilitado
  - Dados persistidos em `./data/prod-simulacoes`

## Como Usar

### Executar em Desenvolvimento
```bash
# Usando perfil padrão (dev)
mvn quarkus:dev

# Ou especificando explicitamente
mvn quarkus:dev -Dquarkus.profile=dev
```

### Executar em Produção
```bash
# Compilar para produção
mvn clean package -Dquarkus.profile=prod

# Executar em produção
java -Dquarkus.profile=prod -jar target/quarkus-app/quarkus-run.jar
```

### Acessar Console H2 (apenas desenvolvimento)
- URL: http://localhost:8080/h2
- JDBC URL: `jdbc:h2:file:./data/testdb`
- Usuário: `sa`
- Senha: `sa`

## Estrutura das Datasources

### Datasource Padrão (H2)
- **Entidades**: `domain.entity.local.*`
  - `Simulacao`
  - `ResultadoSimulacao`
  - `Parcela`
- **Repository**: `SimulacaoRepository`

### Datasource "produtos" (SQL Server)
- **Entidades**: `domain.entity.remote.*`
  - `Produto`
- **Repository**: `ProdutoRepository`
- **Cache**: Configurado com TTL de 10min (dev) / 30min (prod)

## Configurações de Cache

Os produtos são automaticamente cacheados usando Caffeine:
- **Desenvolvimento**: 10 minutos, máximo 1000 itens
- **Produção**: 30 minutos, máximo 5000 itens

## Logs

### Desenvolvimento
- Nível: INFO
- SQL queries visíveis (DEBUG)
- Bind parameters visíveis (TRACE)

### Produção
- Nível: WARN
- SQL queries não visíveis
- Logs otimizados para performance

## Troubleshooting

### Problemas com Dev Services
Se o SQL Server Dev Services não iniciar corretamente:
```bash
# Verificar se Docker está rodando
docker ps

# Limpar containers órfãos
docker system prune -f
```

### Problemas de Conexão em Produção
Verificar se:
1. As credenciais do Azure SQL estão corretas
2. O firewall do Azure permite conexões
3. A rede local permite conexões na porta 1433
