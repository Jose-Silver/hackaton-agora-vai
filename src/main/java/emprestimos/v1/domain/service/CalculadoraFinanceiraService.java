package emprestimos.v1.domain.service;

import emprestimos.v1.domain.dto.simulacao.create.response.ParcelaDTO;
import emprestimos.v1.domain.dto.simulacao.create.response.ResultadoSimulacaoDTO;
import emprestimos.v1.domain.dto.simulacao.create.request.SimulacaoCreateDTO;
import emprestimos.v1.domain.entity.remote.Produto;
import emprestimos.v1.domain.enums.TipoAmortizacao;
import emprestimos.v1.domain.enums.FinanceiroConstant;
import emprestimos.v1.domain.exception.SimulacaoException;
import emprestimos.v1.domain.service.strategy.CalculadoraParcelasStrategy;
import emprestimos.v1.domain.qualifier.Price;
import emprestimos.v1.domain.qualifier.Sac;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * Serviço responsável pelos cálculos financeiros das simulações de empréstimo.
 *
 * Este serviço centraliza toda a lógica de cálculos financeiros, incluindo:
 * - Cálculo de parcelas para diferentes tipos de amortização (SAC e PRICE)
 * - Conversão de taxas anuais para mensais
 * - Cálculo de valores totais e médios das prestações
 *
 * @author Sistema de Empréstimos
 * @since 1.0
 */
@Slf4j
@ApplicationScoped
public class CalculadoraFinanceiraService {

    private static final String ERRO_SIMULACAO_NULA = "Simulação não pode ser nula";
    private static final String ERRO_PRODUTO_NULO = "Produto não pode ser nulo";
    private static final String ERRO_TIPO_AMORTIZACAO_NULO = "Tipo de amortização não pode ser nula";
    private static final String ERRO_PARCELAS_NULAS = "Lista de parcelas não pode ser nula";

    private final CalculadoraParcelasStrategy estrategiaSac;
    private final CalculadoraParcelasStrategy estrategiaPrice;

    @Inject
    public CalculadoraFinanceiraService(
            @Sac CalculadoraParcelasStrategy estrategiaSac,
            @Price CalculadoraParcelasStrategy estrategiaPrice) {
        this.estrategiaSac = estrategiaSac;
        this.estrategiaPrice = estrategiaPrice;
    }

    /**
     * Calcula o resultado completo da simulação de empréstimo para um tipo específico de amortização.
     *
     * @param simulacao Dados da simulação contendo valor desejado e prazo
     * @param produto Produto financeiro com informações de taxa de juros
     * @param tipoAmortizacao Código do tipo de amortização (SAC ou PRICE)
     * @return Resultado completo da simulação com todas as parcelas calculadas
     * @throws SimulacaoException Quando há erro na validação dos parâmetros ou tipo de amortização inválido
     */
    public ResultadoSimulacaoDTO calcularResultadoSimulacao(
            SimulacaoCreateDTO simulacao,
            Produto produto,
            String tipoAmortizacao) {

        log.info("Iniciando cálculo da simulação para tipo de amortização: {}", tipoAmortizacao);

        validarParametrosEntrada(simulacao, produto, tipoAmortizacao);

        final BigDecimal valorFinanciado = simulacao.getValorDesejado();
        final int prazoEmMeses = simulacao.getPrazo();
        // A taxa já vem como mensal do produto (ex: 0.0179 = 1,79% ao mês)
        final BigDecimal taxaMensalJuros = produto.getPcTaxaJuros()
            .setScale(FinanceiroConstant.TAXA_SCALE.getValor(), RoundingMode.HALF_UP);

        final TipoAmortizacao tipoAmortizacaoEnum = obterTipoAmortizacao(tipoAmortizacao);
        final List<ParcelaDTO> parcelasCalculadas = calcularParcelasPorTipo(
                valorFinanciado,
                taxaMensalJuros,
                prazoEmMeses,
                tipoAmortizacaoEnum
        );

        log.info("Simulação calculada com sucesso. Total de parcelas: {}", parcelasCalculadas.size());

        return construirResultadoSimulacao(tipoAmortizacaoEnum.getCodigo(), parcelasCalculadas);
    }

    /**
     * Calcula o valor total de todas as prestações do empréstimo.
     *
     * @param parcelas Lista das parcelas do empréstimo
     * @return Valor total das prestações com escala correta
     * @throws IllegalArgumentException Quando a lista de parcelas é nula
     */
    public BigDecimal calcularValorTotalPrestacoes(List<ParcelaDTO> parcelas) {
        validarListaParcelas(parcelas);

        final BigDecimal valorTotal = parcelas.stream()
                .map(ParcelaDTO::getValorPrestacao)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return arredondarValorFinanceiro(valorTotal);
    }

    /**
     * Calcula o valor médio das prestações do empréstimo.
     *
     * @param parcelas Lista das parcelas do empréstimo
     * @return Valor médio das prestações com escala correta
     * @throws IllegalArgumentException Quando a lista de parcelas é nula
     */
    public BigDecimal calcularValorMedioPrestacoes(List<ParcelaDTO> parcelas) {
        validarListaParcelas(parcelas);

        if (parcelas.isEmpty()) {
            log.warn("Lista de parcelas vazia, retornando valor zero");
            return BigDecimal.ZERO;
        }

        final BigDecimal valorTotal = calcularValorTotalPrestacoes(parcelas);
        final BigDecimal quantidadeParcelas = BigDecimal.valueOf(parcelas.size());

        return valorTotal.divide(
                quantidadeParcelas,
                FinanceiroConstant.DECIMAL_SCALE.getValor(),
                RoundingMode.HALF_UP
        );
    }

    /**
     * Calcula as parcelas usando a estratégia apropriada para o tipo de amortização.
     */
    private List<ParcelaDTO> calcularParcelasPorTipo(
            BigDecimal valorFinanciado,
            BigDecimal taxaMensal,
            int prazoMeses,
            TipoAmortizacao tipo) {

        return switch (tipo) {
            case SAC -> {
                log.debug("Utilizando estratégia SAC para cálculo das parcelas");
                yield estrategiaSac.calcularParcelas(valorFinanciado, taxaMensal, prazoMeses);
            }
            case PRICE -> {
                log.debug("Utilizando estratégia PRICE para cálculo das parcelas");
                yield estrategiaPrice.calcularParcelas(valorFinanciado, taxaMensal, prazoMeses);
            }
        };
    }

    /**
     * Constrói o objeto de resultado da simulação com os dados calculados.
     */
    private ResultadoSimulacaoDTO construirResultadoSimulacao(String tipoAmortizacao, List<ParcelaDTO> parcelas) {
        final ResultadoSimulacaoDTO resultado = new ResultadoSimulacaoDTO();
        resultado.setTipo(tipoAmortizacao);
        resultado.setParcelas(parcelas);
        return resultado;
    }

    /**
     * Obtém o enum de tipo de amortização a partir do código string.
     */
    private TipoAmortizacao obterTipoAmortizacao(String tipoAmortizacao) {
        try {
            return TipoAmortizacao.porCodigo(tipoAmortizacao);
        } catch (IllegalArgumentException e) {
            log.error("Tipo de amortização inválido: {}", tipoAmortizacao, e);
            throw SimulacaoException.tipoAmortizacaoInvalido(tipoAmortizacao);
        }
    }

    /**
     * Arredonda um valor financeiro usando a escala padrão do sistema.
     */
    private BigDecimal arredondarValorFinanceiro(BigDecimal valor) {
        return valor.setScale(FinanceiroConstant.DECIMAL_SCALE.getValor(), RoundingMode.HALF_UP);
    }

    /**
     * Valida os parâmetros de entrada do método principal.
     */
    private void validarParametrosEntrada(SimulacaoCreateDTO simulacao, Produto produto, String tipoAmortizacao) {
        if (simulacao == null) {
            throw new IllegalArgumentException(ERRO_SIMULACAO_NULA);
        }
        if (produto == null) {
            throw new IllegalArgumentException(ERRO_PRODUTO_NULO);
        }
        if (tipoAmortizacao == null || tipoAmortizacao.trim().isEmpty()) {
            throw new IllegalArgumentException(ERRO_TIPO_AMORTIZACAO_NULO);
        }
    }

    /**
     * Valida a lista de parcelas.
     */
    private void validarListaParcelas(List<ParcelaDTO> parcelas) {
        if (parcelas == null) {
            throw new IllegalArgumentException(ERRO_PARCELAS_NULAS);
        }
    }

    /**
     * Método de compatibilidade para manter a API existente.
     * Delega para o método refatorado calcularResultadoSimulacao.
     *
     * @deprecated Use calcularResultadoSimulacao em seu lugar
     */
    @Deprecated(forRemoval = true)
    public ResultadoSimulacaoDTO calcularResultado(
            SimulacaoCreateDTO simulacao,
            Produto produto,
            String tipoAmortizacao) {
        return calcularResultadoSimulacao(simulacao, produto, tipoAmortizacao);
    }

    /**
     * Método de compatibilidade para manter a API existente.
     * Delega para o método refatorado calcularValorTotalPrestacoes.
     *
     * @deprecated Use calcularValorTotalPrestacoes em seu lugar
     */
    @Deprecated(forRemoval = true)
    public BigDecimal calcularValorTotalParcelas(List<ParcelaDTO> parcelas) {
        return calcularValorTotalPrestacoes(parcelas);
    }

    /**
     * Método de compatibilidade para manter a API existente.
     * Delega para o método refatorado calcularValorMedioPrestacoes.
     *
     * @deprecated Use calcularValorMedioPrestacoes em seu lugar
     */
    @Deprecated(forRemoval = true)
    public BigDecimal calcularValorMedioPrestacao(List<ParcelaDTO> parcelas) {
        return calcularValorMedioPrestacoes(parcelas);
    }
}
