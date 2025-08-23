package domain.exception;

import domain.enums.MensagemErro;

/**
 * Exceção específica para erros relacionados a simulações de empréstimo.
 */
public class SimulacaoException extends BusinessException {

    public SimulacaoException(MensagemErro mensagemErro) {
        super(mensagemErro);
    }

    public SimulacaoException(MensagemErro mensagemErro, String detalhe) {
        super(mensagemErro, detalhe);
    }

    public SimulacaoException(MensagemErro mensagemErro, Throwable cause) {
        super(mensagemErro, cause);
    }

    public SimulacaoException(MensagemErro mensagemErro, String detalhe, Throwable cause) {
        super(mensagemErro, detalhe, cause);
    }

    /**
     * Factory method para criar exceção de simulação inválida.
     */
    public static SimulacaoException simulacaoInvalida(String detalhe) {
        return new SimulacaoException(MensagemErro.SIMULACAO_INVALIDA, detalhe);
    }

    /**
     * Factory method para criar exceção de valor inválido.
     */
    public static SimulacaoException valorInvalido(double valor) {
        return new SimulacaoException(MensagemErro.VALOR_INVALIDO, "Valor: " + valor);
    }

    /**
     * Factory method para criar exceção de prazo inválido.
     */
    public static SimulacaoException prazoInvalido(int prazo) {
        return new SimulacaoException(MensagemErro.PRAZO_INVALIDO, "Prazo: " + prazo);
    }

    /**
     * Factory method para criar exceção de tipo de amortização inválido.
     */
    public static SimulacaoException tipoAmortizacaoInvalido(String tipo) {
        return new SimulacaoException(MensagemErro.TIPO_AMORTIZACAO_INVALIDO, "Tipo: " + tipo);
    }
}
