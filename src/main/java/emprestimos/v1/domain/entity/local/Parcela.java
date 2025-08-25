package emprestimos.v1.domain.entity.local;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "parcela")
public class Parcela {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long numero;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal valorAmortizacao;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal valorJuros;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal valorPrestacao;

    @ManyToOne
    @JoinColumn(name = "resultado_simulacao_id")
    private ResultadoSimulacao resultadoSimulacao;
}
