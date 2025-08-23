package domain.enums;

/**
 * Enum contendo as mensagens de erro padronizadas do sistema.
 * Centraliza todas as mensagens para facilitar manutenção e internacionalização.
 */
public enum MensagemErro {
    
    // Erros internos do servidor
    ERRO_INTERNO("INTERNAL_ERROR", "Erro interno do servidor. Tente novamente mais tarde.", 500),
    ERRO_PROCESSAMENTO("PROCESSING_ERROR", "Erro no processamento da solicitação.", 500),
    ERRO_CONEXAO_BANCO("DATABASE_CONNECTION_ERROR", "Erro de conexão com o banco de dados.", 500),
    ERRO_EVENT_HUB("EVENT_HUB_ERROR", "Erro no envio de mensagem para Event Hub.", 500),
    
    // Erros de produtos
    PRODUTO_NAO_ENCONTRADO("PRODUCT_NOT_FOUND", "Produto não encontrado para o ID informado.", 404),
    PRODUTOS_NAO_ELEGIVEIS("PRODUCTS_NOT_ELIGIBLE", "Nenhum produto elegível para o valor e prazo informados.", 400),
    
    // Erros de simulação
    SIMULACAO_INVALIDA("INVALID_SIMULATION", "Dados da simulação são inválidos.", 400),
    SIMULACAO_NAO_ENCONTRADA("SIMULATION_NOT_FOUND", "Simulação não encontrada.", 404),
    VALOR_INVALIDO("INVALID_VALUE", "Valor informado é inválido ou está fora dos limites permitidos.", 400),
    PRAZO_INVALIDO("INVALID_TERM", "Prazo informado é inválido ou está fora dos limites permitidos.", 400),
    TIPO_AMORTIZACAO_INVALIDO("INVALID_AMORTIZATION_TYPE", "Tipo de amortização informado é inválido.", 400),
    
    // Erros de parâmetros de entrada
    PARAMETROS_INVALIDOS("INVALID_PARAMETERS", "Um ou mais parâmetros informados são inválidos.", 400),
    PARAMETRO_OBRIGATORIO("REQUIRED_PARAMETER", "Parâmetro obrigatório não informado.", 400),
    FORMATO_DATA_INVALIDO("INVALID_DATE_FORMAT", "Formato de data inválido. Use o formato YYYY-MM-DD.", 400),
    VALOR_NEGATIVO("NEGATIVE_VALUE", "Valor não pode ser negativo.", 400),
    PRAZO_ZERO_NEGATIVO("INVALID_TERM_RANGE", "Prazo deve ser maior que zero.", 400),
    
    // Erros de validação genérica
    VALIDACAO_GENERICA("VALIDATION_ERROR", "Erro de validação nos dados informados.", 400),
    DADOS_INCONSISTENTES("INCONSISTENT_DATA", "Dados informados são inconsistentes.", 400),
    LIMITE_EXCEDIDO("LIMIT_EXCEEDED", "Limite máximo permitido foi excedido.", 400);
    
    private final String codigo;
    private final String mensagem;
    private final int httpStatus;

    MensagemErro(String codigo, String mensagem, int httpStatus) {
        this.codigo = codigo;
        this.mensagem = mensagem;
        this.httpStatus = httpStatus;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getMensagem() {
        return mensagem;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    /**
     * Retorna uma representação completa do erro para logging.
     */
    public String getDescricaoCompleta() {
        return String.format("[%s] %s (HTTP %d)", codigo, mensagem, httpStatus);
    }
}
