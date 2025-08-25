package emprestimos.v1.domain.exception;

import emprestimos.v1.domain.enums.MensagemErro;

/**
 * Exceção genérica do sistema para erros de validação e regras de negócio.
 */
public class BusinessException extends RuntimeException {
    
    private final MensagemErro mensagemErro;
    private final String detalhe;

    public BusinessException(MensagemErro mensagemErro) {
        super(mensagemErro.getMensagem());
        this.mensagemErro = mensagemErro;
        this.detalhe = null;
    }

    public BusinessException(MensagemErro mensagemErro, String detalhe) {
        super(mensagemErro.getMensagem() + (detalhe != null ? " - " + detalhe : ""));
        this.mensagemErro = mensagemErro;
        this.detalhe = detalhe;
    }

    public BusinessException(MensagemErro mensagemErro, Throwable cause) {
        super(mensagemErro.getMensagem(), cause);
        this.mensagemErro = mensagemErro;
        this.detalhe = null;
    }

    public BusinessException(MensagemErro mensagemErro, String detalhe, Throwable cause) {
        super(mensagemErro.getMensagem() + (detalhe != null ? " - " + detalhe : ""), cause);
        this.mensagemErro = mensagemErro;
        this.detalhe = detalhe;
    }

    public MensagemErro getMensagemErro() {
        return mensagemErro;
    }

    public String getDetalhe() {
        return detalhe;
    }

    public String getCodigo() {
        return mensagemErro.getCodigo();
    }

    public int getHttpStatus() {
        return mensagemErro.getHttpStatus();
    }
}
