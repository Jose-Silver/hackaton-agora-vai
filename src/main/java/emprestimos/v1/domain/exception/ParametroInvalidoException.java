package emprestimos.v1.domain.exception;

import emprestimos.v1.domain.enums.MensagemErro;

/**
 * Exceção específica para erros relacionados a parâmetros de entrada inválidos.
 */
public class ParametroInvalidoException extends BusinessException {

    public ParametroInvalidoException(MensagemErro mensagemErro, String detalhe) {
        super(mensagemErro, detalhe);
    }

    /**
     * Factory method para criar exceção de parâmetros inválidos genérica.
     */
    public static ParametroInvalidoException parametrosInvalidos(String detalhe) {
        return new ParametroInvalidoException(MensagemErro.PARAMETROS_INVALIDOS, detalhe);
    }

    /**
     * Factory method para criar exceção de parâmetro obrigatório.
     */
    public static ParametroInvalidoException parametroObrigatorio(String nomeParametro) {
        return new ParametroInvalidoException(MensagemErro.PARAMETRO_OBRIGATORIO, "Parâmetro: " + nomeParametro);
    }

    /**
     * Factory method para criar exceção de formato de data inválido.
     */
    public static ParametroInvalidoException formatoDataInvalido(String data) {
        return new ParametroInvalidoException(MensagemErro.FORMATO_DATA_INVALIDO, "Data: " + data);
    }

    /**
     * Factory method para criar exceção de valor negativo.
     */
    public static ParametroInvalidoException valorNegativo(String campo, double valor) {
        return new ParametroInvalidoException(MensagemErro.VALOR_NEGATIVO, 
            String.format("Campo: %s, Valor: %.2f", campo, valor));
    }

    /**
     * Factory method para criar exceção de prazo zero ou negativo.
     */
    public static ParametroInvalidoException prazoZeroOuNegativo(int prazo) {
        return new ParametroInvalidoException(MensagemErro.PRAZO_ZERO_NEGATIVO, "Prazo: " + prazo);
    }
}
