package emprestimos.v1.domain.service;

import emprestimos.v1.domain.entity.remote.Produto;
import emprestimos.v1.domain.exception.ProdutoException;
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
            throw ProdutoException.produtosNaoDisponiveis();
        }

        List<Produto> produtosElegiveis = filtrarProdutosElegiveis(produtos, valorDesejado, prazo);

        if (produtosElegiveis.isEmpty()) {
            throw ProdutoException.produtosNaoElegiveis(valorDesejado.doubleValue(), prazo);
        }

        return produtosElegiveis.stream()
            .min(Comparator.comparing(Produto::getPcTaxaJuros));
    }

    /**
     * Encontra produto por simulação - retorna Optional para melhor tratamento de casos sem produtos elegíveis
     */
    public Optional<Produto> encontrarProdutoPorSimulacao(List<Produto> produtos, BigDecimal valorDesejado, int prazo) {
        if (produtos == null || produtos.isEmpty()) {
            return Optional.empty();
        }

        List<Produto> produtosElegiveis = filtrarProdutosElegiveis(produtos, valorDesejado, prazo);

        return produtosElegiveis.stream()
            .min(Comparator.comparing(Produto::getPcTaxaJuros));
    }

    /**
     * Verifica se um produto é elegível para o valor e prazo especificados.
     */
    public boolean isProdutoElegivel(Produto produto, BigDecimal valorDesejado, int prazo) {
        return isValorDentroDoLimite(produto, valorDesejado) && 
               isPrazoDentroDoLimite(produto, prazo);
    }

    private boolean isValorDentroDoLimite(Produto produto, BigDecimal valorDesejado) {
        BigDecimal vrMinimo = produto.getVrMinimo();
        BigDecimal vrMaximo = produto.getVrMaximo();
        boolean minOk = (vrMinimo == null) || (vrMinimo.compareTo(valorDesejado) <= 0);
        boolean maxOk = (vrMaximo == null) || (vrMaximo.compareTo(valorDesejado) >= 0);
        return minOk && maxOk;
    }

    private boolean isPrazoDentroDoLimite(Produto produto, int prazo) {
        Short minMeses = produto.getNuMinimoMeses();
        Short maxMeses = produto.getNuMaximoMeses();
        boolean minOk = (minMeses == null) || (minMeses <= prazo);
        boolean maxOk = (maxMeses == null) || (maxMeses >= prazo);
        return minOk && maxOk;
    }
}
