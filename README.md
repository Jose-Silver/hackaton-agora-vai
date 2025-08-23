# Simulação de Empréstimos - Sistema Backend

## Visão Geral

Este projeto é um sistema backend para simulação de empréstimos, desenvolvido em Java utilizando Quarkus. Ele expõe uma API REST para simular empréstimos, consultar simulações, e obter estatísticas agregadas por produto e data. O sistema é preparado para produção, com logging estruturado e rastreabilidade de requisições via requestId.

## Principais Funcionalidades

- Simulação de empréstimos com cálculo de parcelas (SAC e PRICE)
- Consulta de simulações com filtros por produto e data
- Respostas agrupadas ou separadas por produto
- Paginação de resultados
- Logging estruturado com requestId para rastreabilidade
- Envio de eventos para Event Hub com mecanismo de retry
- Validação otimizada de produtos com cache

## Estrutura de Diretórios

```
src/
  main/
    java/
      resource/         # Controllers REST (SimulacaoResource)
      service/          # Lógica de negócio (SimulacaoService, ErrorHandlingService, etc)
      repository/       # Repositórios de acesso a dados
      domain/           # Entidades, DTOs, exceções, enums
    resources/          # Configurações, scripts SQL
  test/
    java/              # Testes unitários e de integração
    resources/          # Configurações de teste
```

## Como Rodar Localmente

1. **Pré-requisitos:**
   - Java 17+
   - Maven 3.8+

2. **Build:**
   ```sh
   ./mvnw clean package
   ```

3. **Rodar em modo dev:**
   ```sh
   ./mvnw quarkus:dev
   ```

4. **Acessar a API:**
   - Base URL: `http://localhost:8080/simulacoes`

## Como Executar os Testes

```sh
./mvnw test
```

Os relatórios de teste ficam em `target/surefire-reports/`.

## Logging Estruturado e Rastreabilidade

- Todas as requisições REST geram um `requestId` (UUID) ou utilizam o header `X-Request-ID` se fornecido.
- Todos os logs relevantes incluem o `requestId` para rastreabilidade ponta-a-ponta.
- Exemplo de log:
  ```
  [requestId=123e4567-e89b-12d3-a456-426614174000] Mensagem enviada ao Event Hub com sucesso na tentativa 1
  ```

## Endpoints Principais

### Criar Simulação
- **POST** `/simulacoes`
- Corpo: `SimulacaoCreateDTO`
- Resposta: `SimulacaoResponseDTO`

### Listar Simulações (Paginado)
- **GET** `/simulacoes?pagina=1&qtdRegistrosPagina=10`
- Resposta: `PaginaSimulacaoDTO`

### Buscar Simulações Separadas por Produto/Data
- **GET** `/simulacoes/por-produto-dia?data=YYYY-MM-DD&produtoId=123`
- Resposta: `SimulacoesPorProdutoResponseDTO`

## Convenções e Boas Práticas

- **RequestId:** Sempre propague o header `X-Request-ID` em integrações entre serviços.
- **Tratamento de Erros:** Utilize os ExceptionMappers para respostas padronizadas.
- **Extensibilidade:** Novos endpoints devem seguir o padrão de logging e rastreabilidade.
- **Validação:** Utilize DTOs e Bean Validation para validar entradas da API.

## Pontos de Extensão

- Integração com outros sistemas de mensageria/eventos
- Novos tipos de simulação ou produtos financeiros
- Autenticação/autorização de usuários

---

Para dúvidas ou contribuições, consulte os arquivos de código-fonte e a documentação inline.

