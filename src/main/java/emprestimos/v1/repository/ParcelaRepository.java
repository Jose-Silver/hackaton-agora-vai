package emprestimos.v1.repository;

import emprestimos.v1.domain.entity.local.Parcela;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ParcelaRepository implements PanacheRepository<Parcela> {
}
