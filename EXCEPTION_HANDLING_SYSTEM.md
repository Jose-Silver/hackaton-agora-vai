# Sistema de Tratamento de Exceções - Clean Code

Este documento descreve o sistema completo de tratamento de exceções implementado seguindo as melhores práticas de Clean Code.

## 🎯 Arquitetura do Sistema

### Estrutura Hierárquica de Exceções

```
Exception
└── RuntimeException
    └── BusinessException (base)
        ├── SimulacaoException (específica)
        ├── ProdutoException (específica)
        └── ParametroInvalidoException (específica)
```

## 📋 Componentes Implementados

### 1. **Enum de Mensagens de Erro**
`MensagemErro.java` - Centraliza todas as mensagens de erro do sistema:

```java
// Exemplos de uso
MensagemErro.ERRO_INTERNO          // Erro interno do servidor (500)
MensagemErro.PRODUTO_NAO_ENCONTRADO // Produto não encontrado (404)  
MensagemErro.PARAMETROS_INVALIDOS   // Parâmetros inválidos (400)
```

**Benefícios:**
- Mensagens padronizadas
- Códigos HTTP consistentes
- Facilita internacionalização
- Reduz duplicação

### 2. **Exceções Personalizadas**

#### **BusinessException** (Classe Base)
- Exceção genérica para regras de negócio
- Contém `MensagemErro`, detalhe e causa
- Base para todas as exceções específicas

#### **SimulacaoException**
Específica para erros relacionados a simulações:
```java
// Factory methods para facilitar uso
SimulacaoException.simulacaoInvalida("Detalhe do erro");
SimulacaoException.valorInvalido(1000.0);
SimulacaoException.prazoInvalido(0);
SimulacaoException.tipoAmortizacaoInvalido("INVALID");
```

#### **ProdutoException**
Específica para erros relacionados a produtos:
```java
ProdutoException.produtosNaoDisponiveis();
ProdutoException.produtoNaoEncontrado(123);
ProdutoException.produtosNaoElegiveis(10000.0, 12);
```

#### **ParametroInvalidoException**
Específica para erros de validação de parâmetros:
```java
ParametroInvalidoException.parametrosInvalidos("Detalhe");
ParametroInvalidoException.parametroObrigatorio("campo");
ParametroInvalidoException.valorNegativo("valor", -100.0);
```

### 3. **DTO de Resposta de Erro**

`ErrorResponseDTO.java` - Padroniza respostas de erro seguindo RFC 7807:

```json
{
  "codigo": "PRODUCT_NOT_FOUND",
  "mensagem": "Produto não encontrado para o ID informado",
  "detalhe": "ID: 123",
  "status": 404,
  "path": "/simulacoes",
  "timestamp": "2025-08-21T10:30:00",
  "erros": [
    {
      "campo": "valorDesejado",
      "mensagem": "não pode ser nulo",
      "valorRejeitado": null
    }
  ]
}
```

### 4. **ExceptionMappers (JAX-RS)**

#### **BusinessExceptionMapper**
- Mapper genérico para `BusinessException` e subclasses
- Converte exceções em respostas HTTP padronizadas
- Log automático para auditoria

#### **SimulacaoExceptionMapper** 
- Mapper específico para `SimulacaoException`
- Log especializado para simulações

#### **ProdutoExceptionMapper**
- Mapper específico para `ProdutoException` 
- Log especializado para produtos

#### **ParametroInvalidoExceptionMapper**
- Mapper específico para `ParametroInvalidoException`
- Log especializado para validações

#### **ValidationExceptionMapper**
- Trata `ConstraintViolationException` (Bean Validation)
- Converte violações em lista de erros por campo

#### **GenericExceptionMapper**
- Fallback para exceções não tratadas
- Previne vazamento de informações sensíveis
- Log completo para investigação

## 🔧 Como Usar

### No Código de Negócio

```java
@Service
public class MeuService {
    
    public void validarProduto(Integer produtoId) {
        Produto produto = repository.findById(produtoId);
        if (produto == null) {
            throw ProdutoException.produtoNaoEncontrado(produtoId);
        }
    }
    
    public void validarSimulacao(SimulacaoDTO dto) {
        if (dto.getValorDesejado() <= 0) {
            throw SimulacaoException.valorInvalido(dto.getValorDesejado());
        }
    }
}
```

### Nos Controllers

```java
@Path("/simulacoes")
public class SimulacaoResource {
    
    @POST
    public Response criarSimulacao(@Valid SimulacaoCreateDTO dto) {
        // Não precisa de try/catch!
        // Os ExceptionMappers cuidam de tudo automaticamente
        SimulacaoResponseDTO resposta = service.simularEmprestimo(dto);
        return Response.ok(resposta).build();
    }
}
```

## 🚀 Benefícios Alcançados

### 1. **Separação de Responsabilidades**
- Controllers focam apenas na lógica HTTP
- Services focam apenas na lógica de negócio
- ExceptionMappers cuidam da conversão para HTTP

### 2. **Consistência**
- Todas as respostas de erro seguem o mesmo padrão
- Códigos HTTP apropriados automaticamente
- Logs padronizados para auditoria

### 3. **Manutenibilidade**
- Mensagens centralizadas no enum
- Factory methods facilitam criação
- Hierarquia clara de exceções

### 4. **Testabilidade**
- Exceções específicas podem ser testadas individualmente
- Mock de ExceptionMappers é simples
- Validação automática de contratos

### 5. **Observabilidade**
- Logs estruturados e categorizados
- Rastreamento por tipo de erro
- Métricas automáticas por ExceptionMapper

## 📊 Mapeamento de Erros por HTTP Status

| Status | Tipo de Erro | Exemplos |
|--------|--------------|----------|
| **400** | Bad Request | Parâmetros inválidos, Validação |
| **404** | Not Found | Produto/Simulação não encontrada |
| **500** | Internal Error | Erros de sistema, Banco de dados |

## 🧪 Testes Implementados

O sistema inclui testes completos que validam:
- Conversão correta de exceções para HTTP
- Estrutura do JSON de erro
- Factory methods das exceções
- Comportamento dos ExceptionMappers
- Hierarquia de exceções

## 🔄 Migração do Código Existente

### Antes (Código Antigo)
```java
// Tratamento manual e inconsistente
try {
    // lógica
} catch (Exception ex) {
    ex.printStackTrace(); // ❌ Ruim
    return Response.status(500).entity("Erro").build();
}
```

### Depois (Código Limpo)
```java
// Automático e padronizado  
if (produto == null) {
    throw ProdutoException.produtoNaoEncontrado(id); // ✅ Bom
}
// ExceptionMapper cuida do resto automaticamente!
```

## 🎯 Resultado Final

✅ **Sistema robusto** de tratamento de exceções  
✅ **Respostas padronizadas** seguindo RFC 7807  
✅ **Logs estruturados** para observabilidade  
✅ **Código limpo** nos controllers e services  
✅ **Testes completos** garantindo qualidade  
✅ **Fácil manutenção** com mensagens centralizadas  

O sistema agora trata exceções de forma profissional, seguindo as melhores práticas da indústria!
