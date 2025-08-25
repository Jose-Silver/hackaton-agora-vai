package emprestimos.v1.domain.enums;

/**
 * Enum para tipos de amortização suportados pelo sistema.
 */
public enum TipoAmortizacao {
    SAC("SAC", "Sistema de Amortização Constante"),
    PRICE("PRICE", "Sistema Francês de Amortização");

    private final String codigo;
    private final String descricao;

    TipoAmortizacao(String codigo, String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    /**
     * Encontra o tipo de amortização pelo código.
     */
    public static TipoAmortizacao porCodigo(String codigo) {
        for (TipoAmortizacao tipo : values()) {
            if (tipo.codigo.equalsIgnoreCase(codigo)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Tipo de amortização inválido: " + codigo);
    }
}
