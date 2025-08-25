package emprestimos.v1.domain.entity.local;

import emprestimos.v1.domain.entity.remote.Produto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "simulacao")
public class Simulacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal valorDesejado;

    @Column(nullable = false)
    private Long prazo;

    @Transient
    private Produto produto;

    @OneToMany(mappedBy = "simulacao", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
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
    private LocalDateTime dataSimulacao;
}
