package domain.constants;

/**
 * Constantes relacionadas a cálculos financeiros.
 */
public final class FinanceiroConstants {
    
    private FinanceiroConstants() {
        // Classe utilitária - não deve ser instanciada
    }
    
    public static final int DECIMAL_SCALE = 2;
    public static final int TAXA_SCALE = 4;
    public static final int MESES_POR_ANO = 12;
    
    // Tipos de amortização
    public static final String TIPO_SAC = "SAC";
    public static final String TIPO_PRICE = "PRICE";
    
    // Mensagens de erro
    public static final String PRODUTO_NAO_ENCONTRADO_MESSAGE = "Produto não encontrado para o id informado.";
    public static final String NENHUM_PRODUTO_ELEGIVEL_MESSAGE = "Nenhum produto elegível para o valor e prazo informados.";
    public static final String TIPO_AMORTIZACAO_INVALIDO_MESSAGE = "Tipo de amortização inválido: ";
    
    // Valores padrão
    public static final int PAGINA_PADRAO = 1;
    public static final int QUANTIDADE_POR_PAGINA_PADRAO = 10;
}
