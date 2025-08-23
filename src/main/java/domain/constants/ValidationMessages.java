package domain.constants;

/**
 * Constantes para mensagens de validação padronizadas.
 */
public final class ValidationMessages {
    
    // Mensagens de validação para simulação
    public static final String VALOR_OBRIGATORIO = "Valor desejado é obrigatório";
    public static final String VALOR_POSITIVO = "Valor deve ser maior que zero";
    public static final String PRAZO_OBRIGATORIO = "Prazo é obrigatório";
    public static final String PRAZO_POSITIVO = "Prazo deve ser maior que zero";
    
    // Mensagens de validação para parâmetros de consulta
    public static final String DATA_INVALIDA = "Data inválida: deve estar no formato YYYY-MM-DD";
    public static final String PRODUTO_ID_POSITIVO = "produtoId deve ser um número positivo";
    public static final String PAGINA_MINIMA = "Página deve ser maior ou igual a 1";
    public static final String QTD_REGISTROS_MINIMA = "Quantidade de registros deve ser maior que zero";
    public static final String QTD_REGISTROS_MAXIMA = "Quantidade máxima de registros é 100";
    
    // Mensagens de erro de negócio
    public static final String PRODUTO_NAO_ENCONTRADO = "Produto não encontrado";
    public static final String SIMULACAO_NAO_ENCONTRADA = "Simulação não encontrada";
    public static final String FORMATO_DATA_INVALIDO = "Formato de data inválido";
    
    private ValidationMessages() {
        // Classe utilitária - construtor privado
    }
}
