package emprestimos.v1.domain.enums;

/**
 * Enum para constantes financeiras e de cálculo.
 */
public enum FinanceiroConstant {
    // Escalas de precisão
    DECIMAL_SCALE(2),
    TAXA_SCALE(4),
    MESES_POR_ANO(12);

    private final int valor;

    FinanceiroConstant(int valor) {
        this.valor = valor;
    }

    public int getValor() {
        return valor;
    }
}
