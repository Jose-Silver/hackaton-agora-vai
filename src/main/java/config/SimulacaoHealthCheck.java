package config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import repository.ProdutoRepository;
import repository.SimulacaoRepository;

/**
 * Health Check personalizado para verificar a saúde do serviço de simulação.
 * Verifica conectividade com o banco de dados e disponibilidade dos repositórios.
 */
@ApplicationScoped
@Readiness
public class SimulacaoHealthCheck implements HealthCheck {

    @Inject
    @io.quarkus.hibernate.orm.PersistenceUnit("produtos")
    ProdutoRepository produtoRepository;

    @Inject
    SimulacaoRepository simulacaoRepository;

    @Override
    public HealthCheckResponse call() {
        try {
            // Testa conectividade com o banco através dos repositórios
            long countProdutos = produtoRepository.count();
            long countSimulacoes = simulacaoRepository.count();

            return HealthCheckResponse.named("simulacao-service")
                .status(true)
                .withData("produtos-disponíveis", countProdutos)
                .withData("simulações-registradas", countSimulacoes)
                .withData("status", "Serviço operacional")
                .build();

        } catch (Exception e) {
            return HealthCheckResponse.named("simulacao-service")
                .status(false)
                .withData("erro", e.getMessage())
                .withData("status", "Falha na conectividade com banco de dados")
                .build();
        }
    }
}
