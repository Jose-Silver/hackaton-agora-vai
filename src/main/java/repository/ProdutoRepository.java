package repository;

import domain.entity.remote.Produto;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped

public class ProdutoRepository implements PanacheRepository<Produto> {
}

