package domain.enums;

/**
 * Enum para constantes financeiras e de cálculo.
 */
public enum FinanceiroConstant {
    // Escalas de precisão
    DECIMAL_SCALE(2),
    TAXA_SCALE(4),
    MESES_POR_ANO(12),
    
    // Valores padrão de paginação
    PAGINA_PADRAO(1),
    QUANTIDADE_POR_PAGINA_PADRAO(10),
    QUANTIDADE_MAXIMA_POR_PAGINA(100);

    // Tipos de amortização como constantes de string
    public static final String TIPO_SAC = "SAC";
    public static final String TIPO_PRICE = "PRICE";

    private final int valor;

    FinanceiroConstant(int valor) {
        this.valor = valor;
    }

    public int getValor() {
        return valor;
    }
}
