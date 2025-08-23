package repository;

import domain.entity.remote.Produto;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.cache.CacheResult;

import java.util.List;

@ApplicationScoped
@PersistenceUnit("produtos")
public class ProdutoRepository implements PanacheRepository<Produto> {
    @CacheResult(cacheName = "produtos")
    public List<Produto> getAllProdutos() {
        return listAll();
    }
}
