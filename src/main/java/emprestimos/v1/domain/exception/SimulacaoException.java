package emprestimos.v1.domain.exception;

/**
 * Exceção específica para operações relacionadas a simulações.
 */
public class SimulacaoException extends RuntimeException {

    private final String titulo;
    private final String detalhe;

    public SimulacaoException(String titulo, String detalhe) {
        super(detalhe);
        this.titulo = titulo;
        this.detalhe = detalhe;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDetalhe() {
        return detalhe;
    }

    /**
     * Método factory para criar exceção de simulação não encontrada
     */
    public static SimulacaoException simulacaoNaoEncontrada(Long id) {
        return new SimulacaoException(
            "Simulação não encontrada",
            String.format("Não foi encontrada simulação com ID: %d", id)
        );
    }

    /**
     * Método factory para criar exceção de tipo de amortização inválido
     */
    public static SimulacaoException tipoAmortizacaoInvalido(String tipo) {
        return new SimulacaoException(
            "Tipo de amortização inválido",
            String.format("Tipo de amortização '%s' não é suportado. Use 'SAC' ou 'PRICE'", tipo)
        );
    }
}
