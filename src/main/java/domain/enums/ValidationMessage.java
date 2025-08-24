package domain.enums;

/**
 * Enum para mensagens de validação padronizadas.
 */
public enum ValidationMessage {
    // Mensagens de validação para simulação
    VALOR_OBRIGATORIO("Valor desejado é obrigatório"),
    VALOR_POSITIVO("Valor deve ser maior que zero"),
    PRAZO_OBRIGATORIO("Prazo é obrigatório"),
    PRAZO_POSITIVO("Prazo deve ser maior que zero"),
    
    // Mensagens de validação para parâmetros de consulta
    DATA_INVALIDA("Data inválida: deve estar no formato YYYY-MM-DD"),
    PRODUTO_ID_POSITIVO("produtoId deve ser um número positivo"),
    PAGINA_MINIMA("Página deve ser maior ou igual a 1"),
    QTD_REGISTROS_MINIMA("Quantidade de registros deve ser maior que zero"),
    QTD_REGISTROS_MAXIMA("Quantidade máxima de registros é 100"),
    
    // Mensagens de erro de negócio
    PRODUTO_NAO_ENCONTRADO("Produto não encontrado"),
    SIMULACAO_NAO_ENCONTRADA("Simulação não encontrada"),
    FORMATO_DATA_INVALIDO("Formato de data inválido");

    private final String mensagem;

    ValidationMessage(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getMensagem() {
        return mensagem;
    }

    @Override
    public String toString() {
        return mensagem;
    }
}
