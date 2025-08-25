package emprestimos.v1.domain.exception;

import emprestimos.v1.domain.enums.MensagemErro;

/**
 * Exceção específica para erros relacionados a produtos financeiros.
 */
public class ProdutoException extends BusinessException {

    public ProdutoException(MensagemErro mensagemErro) {
        super(mensagemErro);
    }

    public ProdutoException(MensagemErro mensagemErro, String detalhe) {
        super(mensagemErro, detalhe);
    }

    public ProdutoException(MensagemErro mensagemErro, Throwable cause) {
        super(mensagemErro, cause);
    }

    public ProdutoException(MensagemErro mensagemErro, String detalhe, Throwable cause) {
        super(mensagemErro, detalhe, cause);
    }

    /**
     * Factory method para criar exceção de produtos não disponíveis.
     */
    public static ProdutoException produtosNaoDisponiveis() {
        return new ProdutoException(MensagemErro.PRODUTOS_NAO_ELEGIVEIS);
    }

    /**
     * Factory method para criar exceção de produto não encontrado.
     */
    public static ProdutoException produtoNaoEncontrado(Integer produtoId) {
        return new ProdutoException(MensagemErro.PRODUTO_NAO_ENCONTRADO, "ID: " + produtoId);
    }

    /**
     * Factory method para criar exceção de produtos não elegíveis.
     */
    public static ProdutoException produtosNaoElegiveis(double valor, int prazo) {
        String detalhe = String.format("Valor: R$ %.2f, Prazo: %d meses", valor, prazo);
        return new ProdutoException(MensagemErro.PRODUTOS_NAO_ELEGIVEIS, detalhe);
    }
}
