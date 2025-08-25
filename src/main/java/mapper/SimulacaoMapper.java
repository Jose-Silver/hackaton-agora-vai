package mapper;

import domain.dto.simulacao.buscar.response.SimulacaoDetalhesDTO;
import domain.dto.simulacao.create.response.PaginaSimulacaoSimplificadaDTO;
import domain.dto.simulacao.create.response.SimulacaoResponseDTO;
import domain.dto.simulacao.list.response.SimulacaoResumoSimplificadoDTO;
import domain.dto.simulacao.parcelas.response.ParcelasSimulacaoDTO;
import domain.dto.simulacao.parcela.response.ParcelaEspecificaDTO;
import domain.dto.simulacao.por_produto_dia.response.SimulacaoPorProdutoDiaDTO;
import domain.dto.simulacao.create.response.ResultadoSimulacaoDTO;
import domain.dto.simulacao.create.response.ParcelaDTO;
import domain.entity.local.Simulacao;
import domain.entity.remote.Produto;
import domain.enums.FinanceiroConstant;
import domain.enums.TipoAmortizacao;
import domain.service.CalculadoraFinanceiraService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.RoundingMode;
import java.util.List;

@ApplicationScoped
public class SimulacaoMapper {

    @Inject
    CalculadoraFinanceiraService calculadoraFinanceira;

    /**
     * Converte Simulacao para SimulacaoDetalhesDTO
     */
    public SimulacaoDetalhesDTO toSimulacaoDetalhesDTO(Simulacao simulacao) {
        var dto = new SimulacaoDetalhesDTO();
        dto.setId(simulacao.getId());
        dto.setValorDesejado(simulacao.getValorDesejado());
        dto.setPrazo(simulacao.getPrazo().intValue());
        dto.setTaxaJuros(simulacao.getTaxaMediaJuros());
        dto.setValorMedioPrestacao(simulacao.getValorMedioPrestacao());
        dto.setValorTotalCredito(simulacao.getValorTotalCredito());
        dto.setDataSimulacao(simulacao.getDataSimulacao() != null ? simulacao.getDataSimulacao().toString() : null);
        return dto;
    }

    /**
     * Enriquece SimulacaoDetalhesDTO com informações do produto e parcelas
     */
    public void enriqueceSimulacaoDetalhesDTO(SimulacaoDetalhesDTO dto, Produto produto, List<ResultadoSimulacaoDTO> resultados) {
        dto.setCodigoProduto(produto.getCoProduto());
        dto.setDescricaoProduto(produto.getNoProduto());
        dto.setResultadosSimulacao(resultados);
    }

    /**
     * Converte Simulacao e Produto para ParcelasSimulacaoDTO
     */
    public ParcelasSimulacaoDTO toParcelasSimulacaoDTO(Simulacao simulacao, Produto produto, TipoAmortizacao tipo, ResultadoSimulacaoDTO resultado) {
        var dto = new ParcelasSimulacaoDTO();
        dto.setIdSimulacao(simulacao.getId());
        dto.setTipoAmortizacao(tipo.getCodigo());
        dto.setDescricaoTipoAmortizacao(tipo.getDescricao());
        dto.setCodigoProduto(produto.getCoProduto());
        dto.setDescricaoProduto(produto.getNoProduto());
        dto.setValorDesejado(simulacao.getValorDesejado());
        dto.setPrazo(simulacao.getPrazo().intValue());
        dto.setTaxaJuros(produto.getPcTaxaJuros());
        dto.setParcelas(resultado.getParcelas());
        dto.setQuantidadeParcelas(resultado.getParcelas().size());

        var valorTotal = calculadoraFinanceira.calcularValorTotalParcelas(resultado.getParcelas());
        dto.setValorTotalParcelas(valorTotal);

        return dto;
    }

    /**
     * Converte Simulacao, Produto e ParcelaDTO para ParcelaEspecificaDTO
     */
    public ParcelaEspecificaDTO toParcelaEspecificaDTO(Simulacao simulacao, Produto produto, TipoAmortizacao tipo,
                                                      ParcelaDTO parcelaEspecifica, ResultadoSimulacaoDTO resultado) {
        var dto = new ParcelaEspecificaDTO();

        // Informações básicas
        dto.setIdSimulacao(simulacao.getId());
        dto.setTipoAmortizacao(tipo.getCodigo());
        dto.setDescricaoTipoAmortizacao(tipo.getDescricao());
        dto.setCodigoProduto(produto.getCoProduto());
        dto.setDescricaoProduto(produto.getNoProduto());
        dto.setValorDesejado(simulacao.getValorDesejado());
        dto.setPrazo(simulacao.getPrazo().intValue());
        dto.setTaxaJuros(produto.getPcTaxaJuros());

        // Dados da parcela específica
        dto.setNumeroParcela(parcelaEspecifica.getNumero());
        dto.setValorAmortizacao(parcelaEspecifica.getValorAmortizacao());
        dto.setValorJuros(parcelaEspecifica.getValorJuros());
        dto.setValorPrestacao(parcelaEspecifica.getValorPrestacao());

        // Dados do financiamento
        var valorTotalFinanciamento = calculadoraFinanceira.calcularValorTotalParcelas(resultado.getParcelas());
        dto.setValorTotalFinanciamento(valorTotalFinanciamento);
        dto.setQuantidadeTotalParcelas(resultado.getParcelas().size());

        var saldoDevedor = FinanceiroMapper.calcularSaldoDevedor(resultado.getParcelas(), parcelaEspecifica.getNumero().intValue());
        dto.setSaldoDevedor(saldoDevedor);

        // Percentuais
        dto.setPercentualAmortizacao(FinanceiroMapper.calcularPercentual(parcelaEspecifica.getValorAmortizacao(), parcelaEspecifica.getValorPrestacao()));
        dto.setPercentualJuros(FinanceiroMapper.calcularPercentual(parcelaEspecifica.getValorJuros(), parcelaEspecifica.getValorPrestacao()));

        return dto;
    }

    /**
     * Converte Simulacao e Produto para SimulacaoPorProdutoDiaDTO
     */
    public SimulacaoPorProdutoDiaDTO toSimulacaoPorProdutoDiaDTO(Simulacao simulacao, Produto produto) {
        var dto = new SimulacaoPorProdutoDiaDTO();
        dto.setCodigoProduto(produto.getCoProduto());
        dto.setDescricaoProduto(produto.getNoProduto());

        // Dados individuais da simulação (não agregados)
        dto.setTaxaMediaJuro(simulacao.getTaxaMediaJuros() != null ?
            simulacao.getTaxaMediaJuros().doubleValue() : null);
        dto.setValorMedioPrestacao(simulacao.getValorMedioPrestacao() != null ?
            simulacao.getValorMedioPrestacao().doubleValue() : null);
        dto.setValorTotalDesejado(simulacao.getValorTotalDesejado() != null ?
            simulacao.getValorTotalDesejado().doubleValue() : null);
        dto.setValorTotalCredito(simulacao.getValorTotalCredito() != null ?
            simulacao.getValorTotalCredito().doubleValue() : null);

        return dto;
    }

    /**
     * Constrói SimulacaoResponseDTO
     */
    public SimulacaoResponseDTO toSimulacaoResponseDTO(Simulacao simulacao, Produto produto, List<ResultadoSimulacaoDTO> resultados) {
        var resposta = new SimulacaoResponseDTO();
        resposta.setIdSimulacao(simulacao.getId());
        resposta.setCodigoProduto(produto.getCoProduto());
        resposta.setDescricaoProduto(produto.getNoProduto());
        resposta.setTaxaJuros(produto.getPcTaxaJuros().setScale(FinanceiroConstant.TAXA_SCALE.getValor(), RoundingMode.HALF_UP));
        resposta.setResultadoSimulacao(resultados);
        return resposta;
    }

    /**
     * Constrói PaginaSimulacaoSimplificadaDTO
     */
    public PaginaSimulacaoSimplificadaDTO toPaginaSimulacaoSimplificadaDTO(int numeroPagina, int quantidadePorPagina,
                                                                          long totalRegistros, List<SimulacaoResumoSimplificadoDTO> resumos) {
        var paginaDTO = new PaginaSimulacaoSimplificadaDTO();
        paginaDTO.setPagina(numeroPagina);
        paginaDTO.setQtdRegistros(totalRegistros);
        paginaDTO.setQtdRegistrosPagina(quantidadePorPagina);
        paginaDTO.setRegistros(resumos);
        return paginaDTO;
    }
}
