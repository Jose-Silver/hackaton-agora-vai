# Simulação de Empréstimos - Sistema Backend

## Visão Geral

Este projeto é um sistema backend para simulação de empréstimos, desenvolvido em Java utilizando Quarkus. Ele expõe uma API REST para simular empréstimos, consultar simulações e obter estatísticas agregadas por produto e data. O sistema é preparado para produção, com logging estruturado, auditoria e rastreabilidade de requisições via requestId.

## Principais Funcionalidades

- [Simulação e parcelas (SAC/PRICE)](#feature-simulacao)
- [Consulta por produto e data](#feature-por-produto)
- [Respostas agregadas ou individuais](#feature-agregacao)
- [Paginação](#feature-paginacao)
- [Logging estruturado e rastreabilidade](#feature-logging)
- [Envio de eventos com retry](#feature-retry)
- [Validação com cache](#feature-cache)
- [Trilha de auditoria em banco](#feature-auditoria)
- [Retorno em XML (content negotiation)](#feature-xml)
- [Filtro de campos (campos)](#feature-campos)
- [Telemetria por endpoint e agregada](#feature-telemetria)
- [Tratamento padronizado de erros](#feature-erros)
- [Rate limiting (limite de requisições)](#feature-rate-limit)

## Estrutura de Diretórios

```
src/
  main/
    java/
      resource/         # Controllers REST (SimulacaoResource, TelemetriaResource, AuditoriaResource)
      service/          # Lógica de negócio (SimulacaoService, TelemetriaService, etc.)
      repository/       # Repositórios de acesso a dados
      domain/           # Entidades, DTOs, exceções, enums
    resources/          # Configurações, scripts SQL
  test/
    java/               # Testes unitários e de integração
    resources/          # Configurações de teste
```

## Como Rodar Localmente

1. Pré-requisitos:
   - Java 17+
   - Maven 3.8+

2. Build:
   ```sh
   ./mvnw clean package
   ```

3. Rodar em modo dev:
   ```sh
   ./mvnw quarkus:dev
   ```

4. Base URL da API:
   - http://localhost:8080/emprestimos

Observação: todos os endpoints abaixo assumem o prefixo global /emprestimos (quarkus.http.root-path).

## Rodar com Docker Compose

Siga estes passos para subir tudo em modo produção rapidamente com Docker Compose:

```bash
# 1) Preparar pastas (Linux) para evitar problemas de permissão
mkdir -p data logs
sudo chown -R 185:185 data logs

# 2) Build e subir o serviço
docker compose up -d --build

# 3) Verificar saúde
curl -f http://localhost:8080/emprestimos/q/health

# 4) Exercitar a API
curl -X POST http://localhost:8080/emprestimos/v1/simulacoes \
  -H 'Content-Type: application/json' -H 'Accept: application/json' \
  -d '{"valorDesejado": 900.00, "prazo": 5}'

# 5) Encerrar
docker compose down
```

Mais detalhes em README-DOCKER.md (variáveis, volumes e dicas de troubleshooting).

## Como Executar os Testes

```sh
./mvnw test
```

Os relatórios de teste ficam em `target/surefire-reports/`.

---

## Árvore de Recursos (Rotas)

Prefixo global: /emprestimos
```
/v1
├─ simulacoes
│  ├─ POST  /v1/simulacoes                               criar simulação
│  ├─ GET   /v1/simulacoes                               listar (paginado)
│  ├─ GET   /v1/simulacoes/por-produto-dia               por produto/data
│  ├─ GET   /v1/simulacoes/{id}                          detalhes
│  ├─ GET   /v1/simulacoes/{id}/{tipoAmortizacao}        parcelas por tipo
│  └─ GET   /v1/simulacoes/{id}/{tipoAmortizacao}/{parcelaId}  parcela específica
├─ telemetria
│  ├─ GET   /v1/telemetria/detalhes                      stats por endpoint
│  └─ GET   /v1/telemetria/simulacoes                    stats agregadas (Simulações)
└─ auditoria
   ├─ GET    /auditoria/periodo                          ?dataInicio&dataFim
   ├─ GET    /auditoria/erros
   └─ DELETE /auditoria/limpeza/{diasRetencao}
```

Dica: detalhes de filtros, paginação e formatos em JSON/XML estão nas seções abaixo.

## Endpoints da Aplicação

| Método | Caminho                                                  | Descrição                      |
|:------:|----------------------------------------------------------|--------------------------------|
| POST   | /v1/simulacoes                                           | Criar simulação                |
| GET    | /v1/simulacoes                                           | Listar simulações (paginado)   |
| GET    | /v1/simulacoes/por-produto-dia                           | Agregado por produto/data      |
| GET    | /v1/simulacoes/{id}                                      | Detalhar simulação             |
| GET    | /v1/simulacoes/{id}/{tipoAmortizacao}                    | Parcelas por tipo              |
| GET    | /v1/simulacoes/{id}/{tipoAmortizacao}/{parcelaId}        | Parcela específica             |
| GET    | /v1/telemetria/detalhes                                  | Telemetria por endpoint        |
| GET    | /v1/telemetria/simulacoes                                | Telemetria agregada            |
| GET    | /auditoria/periodo                                       | Auditoria por período          |
| GET    | /auditoria/erros                                         | Auditoria de erros             |
| DELETE | /auditoria/limpeza/{diasRetencao}                         | Limpar registros antigos       |

Observação: os caminhos acima devem ser precedidos de /emprestimos.

## Simulação e Parcelas (SAC/PRICE) <a name="feature-simulacao"></a>

- O POST /v1/simulacoes calcula cenários de amortização SAC e PRICE.
- Consulte parcelas por tipo em GET /v1/simulacoes/{id}/{tipoAmortizacao} e parcela específica em GET /v1/simulacoes/{id}/{tipoAmortizacao}/{parcelaId}.
- Veja exemplos práticos em "Como Usar a API": itens 1, 5 e 6.

## Consulta por Produto e Data <a name="feature-por-produto"></a>

- Use GET /v1/simulacoes/por-produto-dia com `dataSimulacao` e/ou `produtoId`.
- Retorna métricas por produto e data; combine com `campos` para reduzir o payload.
- Exemplo em "Como Usar a API": item 3.

## Respostas agregadas ou individuais <a name="feature-agregacao"></a>

- O endpoint por-produto-dia responde uma lista de simulações com estatísticas por produto.
- Para respostas mais enxutas, filtre campos (ex.: `campos=codigoProduto,valorMedioPrestacao`).
- Para detalhes completos por simulação, use GET /v1/simulacoes/{id}.

## Paginação <a name="feature-paginacao"></a>

- Onde: `GET /v1/simulacoes`.
- Parâmetros:
  - `pagina` (inteiro, mínimo 1; padrão 1)
  - `qtdRegistrosPagina` (inteiro, 1 a 100; padrão 10)
- Resposta (206 Partial Content):
  - `pagina`: página atual
  - `qtdRegistros`: total de registros
  - `qtdRegistrosPagina`: tamanho da página
  - `registros`: lista de itens da página (SimulacaoResumoSimplificadoDTO)
- Exemplo: `GET /v1/simulacoes?pagina=2&qtdRegistrosPagina=20`

## Suporte a JSON e XML (Content Negotiation) <a name="feature-xml"></a>

- Envie o header `Accept` com `application/json` ou `application/xml`.
- Para POST com XML, envie `Content-Type: application/xml`.

## Parâmetro "campos" (Field Filtering) <a name="feature-campos"></a>

- Objetivo: reduzir o payload retornado selecionando apenas os atributos desejados.
- Como usar: informe `campos` com nomes separados por vírgula. Suporta campos aninhados com notação de ponto.
- Regras:
  - Se não informado, a resposta retorna todos os campos do DTO.
  - Campos inexistentes são ignorados silenciosamente.
  - Funciona para objetos e para cada item de arrays/listas.
- Exemplos:
  - `GET /v1/simulacoes?campos=pagina,qtdRegistros`
  - `GET /v1/simulacoes/123?campos=idSimulacao,descricaoProduto,taxaJuros`
  - `GET /v1/simulacoes/123/SAC?campos=quantidadeParcelas,parcelas.valorPrestacao`

## Hypermedia (HATEOAS)

As respostas retornam um mapa de links para facilitar a navegação entre recursos relacionados.
- Propriedade de saída: `links` (JSON) e `Links` (XML).
- Relações (rel) utilizadas pela API:
  - `self`: o próprio recurso
  - `detalhe`: detalhe de uma parcela específica
  - `listarSimulacoes`: raiz da coleção de simulações
  - `proximaPagina` e `paginaAnterior`: paginação
  - `parcelas-sac` e `parcelas-price`: coleção de parcelas por tipo de amortização da mesma simulação
  - `todasParcelas`: volta para a lista de parcelas do tipo atual
  - `simulacao`: volta para os detalhes da simulação
  - `parcelas-{sac|price}`: atalho para o outro tipo de amortização

Exemplos (JSON):

- Detalhes da simulação — GET /emprestimos/v1/simulacoes/{id}
```json
{
  "id": 101,
  "valorDesejado": 10000.0,
  "prazo": 36,
  "resultadosSimulacao": [ /* ... */ ],
  "links": {
    "self": "http://localhost:8080/emprestimos/v1/simulacoes/101",
    "parcelas-sac": "http://localhost:8080/emprestimos/v1/simulacoes/101/SAC",
    "parcelas-price": "http://localhost:8080/emprestimos/v1/simulacoes/101/PRICE",
    "listarSimulacoes": "http://localhost:8080/emprestimos/v1/simulacoes"
  }
}
```

- Parcelas por tipo — GET /emprestimos/v1/simulacoes/{id}/{tipo}
```json
{
  "idSimulacao": 101,
  "tipoAmortizacao": "SAC",
  "parcelas": [
    {
      "numero": 1,
      "valorPrestacao": 344.45,
      "links": {
        "detalhe": "http://localhost:8080/emprestimos/v1/simulacoes/101/SAC/1"
      }
    }
  ],
  "links": {
    "self": "http://localhost:8080/emprestimos/v1/simulacoes/101/SAC",
    "simulacao": "http://localhost:8080/emprestimos/v1/simulacoes/101",
    "parcelas-price": "http://localhost:8080/emprestimos/v1/simulacoes/101/PRICE",
    "listarSimulacoes": "http://localhost:8080/emprestimos/v1/simulacoes"
  }
}
```

- Página de listagem — GET /emprestimos/v1/simulacoes
```json
{
  "pagina": 1,
  "qtdRegistros": 150,
  "qtdRegistrosPagina": 10,
  "registros": [ /* ... */ ],
  "links": {
    "proximaPagina": "http://localhost:8080/emprestimos/v1/simulacoes?pagina=2",
    "paginaAnterior": "http://localhost:8080/emprestimos/v1/simulacoes?pagina=1"
  }
}
```

## Limite de Requisições (Rate Limiting) e Retry-After <a name="feature-rate-limit"></a>

- Os limites variam por endpoint; a resposta inclui headers `X-RateLimit-*` e `Retry-After` quando aplicável.
- Ao exceder o limite, a API responde com 429 Muitas Requisições.

## Telemetria <a name="feature-telemetria"></a>

- Métricas de uso por endpoint: `GET /v1/telemetria/detalhes`.
- Estatísticas agregadas por API: `GET /v1/telemetria/simulacoes`.

## Logging (SLF4J) e Rastreabilidade <a name="feature-logging"></a>

- Logs utilizam SLF4J e configuração do Quarkus.
- Rastreabilidade por `X-Request-ID`:
  - Se presente no request, é utilizado; caso contrário, é gerado um UUID.
  - O `requestId` aparece nos logs das operações da API.
- Arquivos de log locais em `logs/` com rotação.

## Envio de eventos com retry <a name="feature-retry"></a>

- Após criar uma simulação, o sistema envia um evento (ex.: para Event Hubs) com política de retry.

## Validação com cache <a name="feature-cache"></a>

- Cache em memória (Caffeine) otimiza consultas de elegibilidade e agregações.

## Trilha de Auditoria em Banco <a name="feature-auditoria"></a>

- A auditoria registra operações relevantes em tabela específica.
- Consultas por período e erros estão disponíveis.

## Tratamento de Erros <a name="feature-erros"></a>

- Respostas padronizadas no formato ErrorResponseDTO (inclui código, mensagem, detalhe, status, timestamp e erros de campo).

## Como Usar a API

### 1) Criar Simulação — POST /v1/simulacoes

Request (JSON):
```bash
curl -X POST http://localhost:8080/emprestimos/v1/simulacoes \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json' \
  -d '{"valorDesejado": 10000.00, "prazo": 36}'
```

Resposta 200 (JSON):
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

Request/Resposta (XML):
```bash
curl -X POST http://localhost:8080/emprestimos/v1/simulacoes \
  -H 'Content-Type: application/xml' \
  -H 'Accept: application/xml' \
  -d '<simulacaoCreateDTO><valorDesejado>10000.00</valorDesejado><prazo>36</prazo></simulacaoCreateDTO>'
```
```xml
<simulacao>
  <idSimulacao>101</idSimulacao>
  <codigoProduto>456</codigoProduto>
  <descricaoProduto>Crédito Imobiliário</descricaoProduto>
  <taxaJuros>0.08</taxaJuros>
  <resultadoSimulacao>
    <resultado>
      <tipo>SAC</tipo>
      <parcelas>
        <parcela>
          <numero>1</numero>
          <valorAmortizacao>277.78</valorAmortizacao>
          <valorJuros>66.67</valorJuros>
          <valorPrestacao>344.45</valorPrestacao>
        </parcela>
      </parcelas>
    </resultado>
  </resultadoSimulacao>
</simulacao>
```

### 2) Listar Simulações (paginado) — GET /v1/simulacoes

Request:
```bash
curl "http://localhost:8080/emprestimos/v1/simulacoes?pagina=1&qtdRegistrosPagina=2&campos=pagina,qtdRegistros,registros"
```

Resposta 206 (JSON):
```json
{
  "pagina": 1,
  "qtdRegistros": 150,
  "qtdRegistrosPagina": 2,
  "registros": [
    { "idSimulacao": 101, "valorDesejado": 10000.00, "prazo": 36, "valorTotalParcelas": 12000.00 },
    { "idSimulacao": 102, "valorDesejado": 20000.00, "prazo": 48, "valorTotalParcelas": 26000.50 }
  ]
}
```

### 3) Simulações por Produto/Data — GET /v1/simulacoes/por-produto-dia

Request:
```bash
curl "http://localhost:8080/emprestimos/v1/simulacoes/por-produto-dia?dataSimulacao=2025-08-21&produtoId=123"
```

Resposta 200 (JSON):
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

### 4) Detalhar Simulação — GET /v1/simulacoes/{id}

Request:
```bash
curl "http://localhost:8080/emprestimos/v1/simulacoes/101?campos=id,valorDesejado,prazo,codigoProduto,descricaoProduto,taxaJuros,resultadosSimulacao"
```

Resposta 200 (JSON):
```json
{
  "id": 101,
  "valorDesejado": 10000.00,
  "prazo": 36,
  "codigoProduto": 456,
  "descricaoProduto": "Crédito Imobiliário",
  "taxaJuros": 0.08,
  "resultadosSimulacao": [
    { "tipo": "SAC", "parcelas": [ { "numero": 1, "valorPrestacao": 344.45 } ] }
  ]
}
```

### 5) Parcelas por Tipo — GET /v1/simulacoes/{id}/{tipoAmortizacao}

Request:
```bash
curl "http://localhost:8080/emprestimos/v1/simulacoes/101/SAC?campos=idSimulacao,tipoAmortizacao,quantidadeParcelas,parcelas.valorPrestacao"
```

Resposta 200 (JSON):
```json
{
  "idSimulacao": 101,
  "tipoAmortizacao": "SAC",
  "quantidadeParcelas": 36,
  "parcelas": [
    { "valorPrestacao": 344.45 },
    { "valorPrestacao": 343.12 }
  ]
}
```

### 6) Parcela Específica — GET /v1/simulacoes/{id}/{tipoAmortizacao}/{parcelaId}

Request:
```bash
curl "http://localhost:8080/emprestimos/v1/simulacoes/101/SAC/1?campos=idSimulacao,tipoAmortizacao,numeroParcela,valorPrestacao,saldoDevedor"
```

Resposta 200 (JSON):
```json
{
  "idSimulacao": 101,
  "tipoAmortizacao": "SAC",
  "numeroParcela": 1,
  "valorPrestacao": 344.45,
  "saldoDevedor": 9722.22
}
```

### 7) Telemetria — GET /v1/telemetria

Detalhes por endpoint:
```bash
curl "http://localhost:8080/emprestimos/v1/telemetria/detalhes"
```
Resposta 200 (JSON):
```json
{
  "dataReferencia": "2025-08-25",
  "listaEndpoints": [
    { "metodo": "GET", "path": "/v1/simulacoes", "qtdRequisicoes": 120, "tempoMedio": 12, "tempoMinimo": 4, "tempoMaximo": 35, "percentualSucesso": 0.99 }
  ]
}
```

Agregado da API "Simulacoes":
```bash
curl "http://localhost:8080/emprestimos/v1/telemetria/simulacoes"
```
Resposta 200 (JSON):
```json
{
  "dataReferencia": "2025-08-25",
  "listaEndpoints": { "nomeApi": "Simulacoes", "qtdRequisicoes": 300, "tempoMedio": 10, "tempoMinimo": 3, "tempoMaximo": 40, "percentualSucesso": 0.98 }
}
```

### 8) Auditoria — /auditoria

Por período:
```bash
curl "http://localhost:8080/emprestimos/auditoria/periodo?dataInicio=2025-08-01&dataFim=2025-08-25"
```

Somente erros:
```bash
curl "http://localhost:8080/emprestimos/auditoria/erros"
```

Limpeza:
```bash
curl -X DELETE "http://localhost:8080/emprestimos/auditoria/limpeza/30"
```

### Erros Padrão (400/404/500)

Formato (ErrorResponseDTO):
```json
{
  "codigo": "VALIDACAO",
  "mensagem": "Parâmetros inválidos",
  "detalhe": "prazo deve ser >= 1",
  "status": 400,
  "path": "/v1/simulacoes",
  "timestamp": "2025-08-25T10:22:00",
  "erros": [ { "campo": "prazo", "mensagem": "mínimo 1", "valorRejeitado": 0 } ]
}
```

### Rate Limit Excedido (429)

Quando o limite é excedido, a API retorna 429 com headers e corpo simplificado:

Headers:
```
Retry-After: 30
X-RateLimit-Limit: 50
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1724570400
```

Corpo (JSON):
```json
{
  "codigo": "LIMITE_REQUISICOES_EXCEDIDO",
  "mensagem": "Limite de requisições excedido",
  "detalhe": "Muitas requisições. Tente novamente mais tarde.",
  "status": 429,
  "retryAfter": 30
}
```

---

## Convenções e Boas Práticas

- RequestId: sempre propague o header `X-Request-ID`.
- Tratamento de Erros: respostas padronizadas via ExceptionMappers.
- Validação: Bean Validation nos DTOs.
- Extensibilidade: novos endpoints devem seguir padrão de logging, auditoria e rate limiting.

---

Para dúvidas ou contribuições, consulte os arquivos de código-fonte e a documentação inline.
