package domain.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
@Table(name = "simulacao")
public class Simulacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long valorDesejado;

    @Column(nullable = false)
    private Long prazo;

    // Produto não é mais entidade JPA, apenas um objeto de valor
    @Transient
    private Produto produto;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "simulacao_id")
    private List<ResultadoSimulacao> resultadosSimulacao;

    private Long taxaMediaJuros;
    private Long valorMedioPrestacao;
    private Long valorTotalDesejado;
    private Long valorTotalCredito;
}
