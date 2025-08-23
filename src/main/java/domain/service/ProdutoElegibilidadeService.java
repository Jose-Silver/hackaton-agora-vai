package domain.service;

import domain.constants.FinanceiroConstants;
import domain.entity.remote.Produto;
import domain.exception.ProdutoException;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Serviço responsável por determinar a elegibilidade de produtos
 * para simulações de empréstimo.
 */
@ApplicationScoped
public class ProdutoElegibilidadeService {

    /**
     * Filtra produtos elegíveis baseado no valor desejado e prazo.
     */
    public List<Produto> filtrarProdutosElegiveis(List<Produto> produtos, BigDecimal valorDesejado, int prazo) {
        return produtos.stream()
            .filter(produto -> isProdutoElegivel(produto, valorDesejado, prazo))
            .toList();
    }

    /**
     * Encontra o melhor produto baseado na menor taxa de juros.
     * Retorna Optional.empty() se não encontrar produtos elegíveis.
     */
    public Optional<Produto> encontrarMelhorProdutoOptional(List<Produto> produtos, BigDecimal valorDesejado, int prazo) {
        if (produtos == null || produtos.isEmpty()) {
            return Optional.empty();
        }

        List<Produto> produtosElegiveis = filtrarProdutosElegiveis(produtos, valorDesejado, prazo);

        return produtosElegiveis.stream()
            .min(Comparator.comparing(Produto::getPcTaxaJuros));
    }

    /**
     * Encontra o melhor produto baseado na menor taxa de juros.
     * @deprecated Use encontrarMelhorProdutoOptional para melhor tratamento de casos sem produtos elegíveis
     */
    @Deprecated
    public Produto encontrarMelhorProduto(List<Produto> produtos, BigDecimal valorDesejado, int prazo) {
        if (produtos == null || produtos.isEmpty()) {
            throw ProdutoException.produtosNaoDisponiveis();
        }

        List<Produto> produtosElegiveis = filtrarProdutosElegiveis(produtos, valorDesejado, prazo);
        
        return produtosElegiveis.stream()
            .min(Comparator.comparing(Produto::getPcTaxaJuros))
            .orElseThrow(() -> ProdutoException.produtosNaoElegiveis(valorDesejado.doubleValue(), prazo));
    }

    /**
     * Verifica se um produto é elegível para o valor e prazo especificados.
     */
    public boolean isProdutoElegivel(Produto produto, BigDecimal valorDesejado, int prazo) {
        return isValorDentroDoLimite(produto, valorDesejado) && 
               isPrazoDentroDoLimite(produto, prazo);
    }

    /**
     * Encontra o produto associado a uma simulação baseado nos critérios de elegibilidade.
     */
    public Optional<Produto> encontrarProdutoPorSimulacao(List<Produto> produtos, BigDecimal valorDesejado, int prazo) {
        return produtos.stream()
            .filter(produto -> isProdutoElegivel(produto, valorDesejado, prazo))
            .min(Comparator.comparing(Produto::getPcTaxaJuros));
    }

    private boolean isValorDentroDoLimite(Produto produto, BigDecimal valorDesejado) {
        return produto.getVrMinimo().compareTo(valorDesejado) <= 0 &&
               produto.getVrMaximo().compareTo(valorDesejado) >= 0;
    }

    private boolean isPrazoDentroDoLimite(Produto produto, int prazo) {
        return produto.getNuMinimoMeses() <= prazo &&
               produto.getNuMaximoMeses() >= prazo;
    }
}
