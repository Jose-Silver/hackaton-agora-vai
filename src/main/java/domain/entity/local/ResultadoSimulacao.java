package domain.entity.local;

import domain.enums.TipoAmortizacao;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Entity
@Table(name = "resultado_simulacao")
public class ResultadoSimulacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoAmortizacao tipo;

    @OneToMany(mappedBy = "resultadoSimulacao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Parcela> parcelas;

    @Column(precision = 10, scale = 4)
    private BigDecimal taxaMediaJuros;

    @Column(precision = 18, scale = 2)
    private BigDecimal valorMedioPrestacao;

    @Column(precision = 18, scale = 2)
    private BigDecimal valorTotalDesejado;

    @Column(precision = 18, scale = 2)
    private BigDecimal valorTotalCredito;
}
