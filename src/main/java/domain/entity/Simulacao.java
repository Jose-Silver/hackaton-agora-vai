package domain.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Entity
@Table(name = "simulacao")
public class Simulacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal valorDesejado;

    @Column(nullable = false)
    private Long prazo;

    // Produto não é mais entidade JPA, apenas um objeto de valor
    @Transient
    private Produto produto;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "simulacao_id")
    private List<ResultadoSimulacao> resultadosSimulacao;

    @Column(precision = 10, scale = 4)
    private BigDecimal taxaMediaJuros;
    @Column(precision = 18, scale = 2)
    private BigDecimal valorMedioPrestacao;
    @Column(precision = 18, scale = 2)
    private BigDecimal valorTotalDesejado;
    @Column(precision = 18, scale = 2)
    private BigDecimal valorTotalCredito;

    @Column(nullable = false)
    private java.time.LocalDateTime dataSimulacao;
}
