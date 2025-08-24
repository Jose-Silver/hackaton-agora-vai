package domain.enums;

/**
 * Enum para mensagens de erro de negócio padronizadas.
 */
public enum BusinessErrorMessage {
    PRODUTO_NAO_ENCONTRADO("Produto não encontrado para o id informado."),
    NENHUM_PRODUTO_ELEGIVEL("Nenhum produto elegível para o valor e prazo informados."),
    TIPO_AMORTIZACAO_INVALIDO("Tipo de amortização inválido: "),
    PRODUTOS_NAO_DISPONIVEIS("Não há produtos disponíveis no momento."),
    ERRO_CALCULO_PARCELAS("Erro interno no cálculo das parcelas."),
    SIMULACAO_NAO_PROCESSADA("Simulação não pôde ser processada.");

    private final String mensagem;

    BusinessErrorMessage(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getMensagem() {
        return mensagem;
    }

    public String getMensagemComDetalhe(String detalhe) {
        return mensagem + detalhe;
    }

    @Override
    public String toString() {
        return mensagem;
    }
}
