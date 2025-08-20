package domain.entity.local;

import domain.entity.Tipo;
import jakarta.persistence.*;
import lombok.Data;

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
    private Tipo tipo;

    @OneToMany(mappedBy = "resultadoSimulacao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Parcela> parcelas;

    private Long taxaMediaJuros;
    private Long valorMedioPrestacao;
    private Long valorTotalDesejado;
    private Long valorTotalCredito;
}
