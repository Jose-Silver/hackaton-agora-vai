package emprestimos.v1.repository;

import emprestimos.v1.domain.entity.local.ResultadoSimulacao;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ResultadoSimulacaoRepository implements PanacheRepository<ResultadoSimulacao> {
}
