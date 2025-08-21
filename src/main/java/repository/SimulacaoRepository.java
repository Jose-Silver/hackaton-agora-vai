package repository;

import domain.entity.local.Simulacao;
import io.quarkus.agroal.DataSource;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
//@PersistenceUnit("h2")
@DataSource("h2")
public class SimulacaoRepository implements PanacheRepository<Simulacao> {
}
