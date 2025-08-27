# Simulação de Empréstimos - Sistema Backend

## 1. Índice (Sumário)

1. [Índice (Sumário)](#1-índice-sumário)
2. [O que é o projeto](#2-o-que-é-o-projeto)
3. [Como rodar](#3-como-rodar)
   - 3.1. [Docker Compose](#31-docker-compose)
   - 3.2. [Localmente (modo dev)](#32-localmente-modo-dev)
4. [Índice de endpoints](#4-índice-de-endpoints)
5. [Recursos avançados](#5-recursos-avançados)
   - 5.1. [Hypermedia (HATEOAS)](#51-hypermedia-hateoas)
   - 5.2. [Content Negotiation (JSON/XML)](#52-content-negotiation-jsonxml)
   - 5.3. [Filtro de campos](#53-filtro-de-campos)
   - 5.4. [Paginação](#54-paginação)
   - 5.5. [Rate Limiting](#55-rate-limiting)
   - 5.6. [Telemetria](#56-telemetria)
   - 5.7. [Logging e Rastreabilidade](#57-logging-e-rastreabilidade)
   - 5.8. [Auditoria](#58-auditoria)
   - 5.9. [Tratamento de Erros](#59-tratamento-de-erros)
6. [Exemplos práticos](#6-exemplos-práticos)
7. [Configurações e perfis](#7-configurações-e-perfis)

## 2. O que é o projeto

Sistema backend desenvolvido em **Java 17** com **Quarkus** para simulação de empréstimos bancários.

### Funcionalidades principais:
- **Simulação de empréstimos** com cálculos SAC e PRICE
- **Consultas detalhadas** de simulações e parcelas específicas
- **Listagem paginada** de simulações
- **Estatísticas agregadas** por produto e data
- **API REST** com suporte a JSON e XML
- **Links de navegação** (HATEOAS) entre recursos
- **Filtro de campos** para otimizar payloads
- **Rate limiting** para controle de acesso
- **Telemetria** e **auditoria** completas
- **Logging estruturado** com rastreabilidade
- **Dados guardados em cache** 


### Tecnologias:
- Java 17 + Quarkus
- H2 Database (desenvolvimento e produção)
- SQL Server (produtos - via Azure)
- Cache Caffeine
- Azure Event Hubs
- Docker + Docker Compose

## 3. Como rodar

### 3.1 Docker Compose

**Modo mais rápido para rodar em produção:**

```bash
# 1. Preparar pastas (Linux)
mkdir -p data logs
sudo chown -R 185:185 data logs

# 2. Subir o serviço
docker compose up -d --build

# 3. Verificar saúde
curl -f http://localhost:8080/emprestimos/q/health

# 4. Testar API
curl -X POST http://localhost:8080/emprestimos/v1/simulacoes \
  -H 'Content-Type: application/json' \
  -d '{"valorDesejado": 10000.00, "prazo": 36}'

# 5. Parar
docker compose down
```

📖 **Detalhes completos**: `README-DOCKER.md`

### 3.2 Localmente (modo dev)

**Para desenvolvimento com hot reload:**

```bash
# Pré-requisitos: Java 17+ e Maven 3.8+

# Build
./mvnw clean package

# Rodar em modo dev
./mvnw quarkus:dev

# Executar testes
./mvnw test
```

**Base URL**: http://localhost:8080/emprestimos

## 4. Índice de endpoints

**Prefixo global**: `/emprestimos`

### Árvore de recursos:
```
PREFIXO GLOBAL:/emprestimos
├─ /v1/simulacoes
│  ├─ POST   /                                          criar simulação
│  ├─ GET    /                                          listar (paginado)
│  ├─ GET    /por-produto-dia                           agregado por produto/data
│  ├─ GET    /{id}                                      detalhes da simulação
│  ├─ GET    /{id}/{tipoAmortizacao}                    parcelas por tipo (SAC/PRICE)
│  └─ GET    /{id}/{tipoAmortizacao}/{parcelaId}        parcela específica
├─ /v1/telemetria
│  ├─ GET    /detalhes                                  estatísticas por endpoint
│  └─ GET    /simulacoes                                estatísticas agregadas
└─ /auditoria
   ├─ GET    /periodo                                   logs por período
   ├─ GET    /erros                                     logs de erro
   └─ DELETE /limpeza/{diasRetencao}                    limpar logs antigos
```

### Tabela resumo:

| Método | Endpoint | Descrição | Rate Limit |
|--------|----------|-----------|------------|
| POST | `/v1/simulacoes` | Criar simulação | 10/min |
| GET | `/v1/simulacoes` | Listar (paginado) | 50/min |
| GET | `/v1/simulacoes/por-produto-dia` | Agregado por produto/data | 30/min |
| GET | `/v1/simulacoes/{id}` | Detalhes da simulação | 100/min |
| GET | `/v1/simulacoes/{id}/{tipo}` | Parcelas por tipo | 80/min |
| GET | `/v1/simulacoes/{id}/{tipo}/{parcelaId}` | Parcela específica | 120/min |
| GET | `/v1/telemetria/detalhes` | Stats por endpoint | - |
| GET | `/v1/telemetria/simulacoes` | Stats agregadas | - |
| GET | `/auditoria/periodo` | Logs por período | - |
| GET | `/auditoria/erros` | Logs de erro | - |
| DELETE | `/auditoria/limpeza/{dias}` | Limpar logs | - |

## 5. Recursos avançados

### 5.1 Hypermedia (HATEOAS)

Todas as respostas incluem links de navegação no campo `links` (JSON) ou `Links` (XML).

**Relações suportadas:**
- `self` → próprio recurso
- `listarSimulacoes` → lista de simulações
- `proximaPagina`/`paginaAnterior` → navegação de páginas
- `parcelas-sac`/`parcelas-price` → parcelas por tipo
- `simulacao` → detalhes da simulação
- `detalhe` → detalhes da parcela

**Exemplo:**
```json
{
  "id": 101,
  "valorDesejado": 10000.0,
  "links": {
    "self": "http://localhost:8080/emprestimos/v1/simulacoes/101",
    "parcelas-sac": "http://localhost:8080/emprestimos/v1/simulacoes/101/SAC",
    "parcelas-price": "http://localhost:8080/emprestimos/v1/simulacoes/101/PRICE"
  }
}
```

### 5.2 Content Negotiation (JSON/XML)

Suporte a múltiplos formatos via headers `Accept` e `Content-Type`:

```bash
# JSON (padrão)
curl -H 'Accept: application/json' ...

# XML
curl -H 'Accept: application/xml' ...
```

### 5.3 Filtro de campos

Reduza o payload selecionando apenas os campos necessários com o parâmetro `campos`:

```bash
# Campos simples
GET /v1/simulacoes?campos=pagina,qtdRegistros

# Campos aninhados
GET /v1/simulacoes/123?campos=id,valorDesejado,resultadosSimulacao.tipo

# Campos de arrays
GET /v1/simulacoes/123/SAC?campos=quantidadeParcelas,parcelas.valorPrestacao
```

### 5.4 Paginação

**Parâmetros:**
- `pagina` (mínimo: 1, padrão: 1)
- `qtdRegistrosPagina` (1-100, padrão: 10)

**Resposta (status 206):**
```json
{
  "pagina": 1,
  "qtdRegistros": 150,
  "qtdRegistrosPagina": 10,
  "registros": [...],
  "links": {
    "proximaPagina": "...",
    "paginaAnterior": "..."
  }
}
```

### 5.5 Rate Limiting

**Headers de resposta:**
- `X-RateLimit-Limit` → limite por janela
- `X-RateLimit-Remaining` → requisições restantes
- `X-RateLimit-Reset` → timestamp do reset

**Erro 429 (limite excedido):**
```json
{
  "codigo": "LIMITE_REQUISICOES_EXCEDIDO",
  "mensagem": "Limite de requisições excedido",
  "status": 429,
  "retryAfter": 30
}
```

### 5.6 Telemetria

Monitore o desempenho da API:

- `GET /v1/telemetria/detalhes` → métricas por endpoint
- `GET /v1/telemetria/simulacoes` → métricas agregadas

### 5.7 Logging e Rastreabilidade

- **Header**: `X-Request-ID` (gerado automaticamente se não fornecido)
- **Logs**: SLF4J com rotação em `logs/application.log`
- **Formato**: inclui requestId, timestamp, nível e contexto

### 5.8 Auditoria

Registro automático de operações críticas:

- **Período**: `GET /auditoria/periodo?dataInicio=2025-08-01&dataFim=2025-08-25`
- **Erros**: `GET /auditoria/erros`
- **Limpeza**: `DELETE /auditoria/limpeza/30` (remove registros > 30 dias)

### 5.9 Tratamento de Erros

**Formato padrão (ErrorResponseDTO):**
```json
{
  "codigo": "VALIDACAO",
  "mensagem": "Parâmetros inválidos",
  "detalhe": "prazo deve ser >= 1",
  "status": 400,
  "path": "/v1/simulacoes",
  "timestamp": "2025-08-27T10:22:00",
  "erros": [
    {
      "campo": "prazo",
      "mensagem": "mínimo 1",
      "valorRejeitado": 0
    }
  ]
}
```

## 6. Exemplos práticos

### Criar simulação
```bash
curl -X POST http://localhost:8080/emprestimos/v1/simulacoes \
  -H 'Content-Type: application/json' \
  -d '{"valorDesejado": 900.00, "prazo": 5}'
```

**Resposta:**
```json
{
  "idSimulacao": 101,
  "codigoProduto": 456,
  "descricaoProduto": "Crédito Imobiliário",
  "taxaJuros": 0.08,
  "resultadoSimulacao": [
    {
      "tipo": "SAC",
      "parcelas": [
        { "numero": 1, "valorAmortizacao": 277.78, "valorJuros": 66.67, "valorPrestacao": 344.45 }
      ]
    },
    {
      "tipo": "PRICE",
      "parcelas": [
        { "numero": 1, "valorAmortizacao": 220.00, "valorJuros": 120.00, "valorPrestacao": 340.00 }
      ]
    }
  ]
}
```

### Listar simulações (paginado)
```bash
curl "http://localhost:8080/emprestimos/v1/simulacoes?pagina=1&qtdRegistrosPagina=5"
```

### Buscar por produto e data
```bash
curl "http://localhost:8080/emprestimos/v1/simulacoes/por-produto-dia?dataSimulacao=2025-08-21&produtoId=1"
```

**Resposta:**
```json
{
  "dataReferencia": "2025-08-21",
  "simulacoes": [
    {
      "codigoProduto": 123,
      "descricaoProduto": "Crédito Imobiliário",
      "taxaMediaJuro": 0.08,
      "valorMedioPrestacao": 845.32,
      "valorTotalDesejado": 20000.00,
      "valorTotalCredito": 23500.00
    }
  ]
}
```

### Obter detalhes de uma simulação
```bash
curl "http://localhost:8080/emprestimos/v1/simulacoes/101"
```

### Listar parcelas SAC
```bash
curl "http://localhost:8080/emprestimos/v1/simulacoes/101/SAC"
```

### Detalhes de parcela específica
```bash
curl "http://localhost:8080/emprestimos/v1/simulacoes/101/SAC/1"
```

### Telemetria
```bash
# Por endpoint
curl "http://localhost:8080/emprestimos/v1/telemetria/detalhes"

# Agregada
curl "http://localhost:8080/emprestimos/v1/telemetria/simulacoes"
```

**Resposta da telemetria:**
```json
{
  "dataReferencia": "2025-08-27",
  "listaEndpoints": [
    {
      "metodo": "GET",
      "path": "/v1/simulacoes",
      "qtdRequisicoes": 120,
      "tempoMedio": 12,
      "tempoMinimo": 4,
      "tempoMaximo": 35,
      "percentualSucesso": 0.99
    }
  ]
}
```

## 7. Configurações e perfis

### Perfis disponíveis:
- **`dev`** → desenvolvimento (H2 em memória + SQL Server via DevServices)
- **`prod`** → produção (H2 arquivo + SQL Server Azure)
- **`test`** → testes automatizados

### Executar com perfil específico:
```bash
# Via script
./run-profile.sh dev

# Via Maven
./mvnw quarkus:dev -Dquarkus.profile=dev
```

### Documentação detalhada:
- **Perfis e configurações**: `PERFIS-QUARKUS.md`
- **Docker Compose**: `README-DOCKER.md`

---

## 📚 Recursos adicionais

- **OpenAPI/Swagger**: http://localhost:8080/emprestimos/q/swagger-ui
- **Health Check**: http://localhost:8080/emprestimos/q/health
- **Métricas**: http://localhost:8080/emprestimos/q/metrics

---

**Desenvolvido com ❤️ usando Quarkus**
