package domain.entity.remote;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "PRODUTO", schema = "dbo")
public class Produto {
    @Id
    @Column(name = "CO_PRODUTO")
    private Integer coProduto;

    @Column(name = "NO_PRODUTO", nullable = false, length = 200)
    private String noProduto;

    @Column(name = "PC_TAXA_JUROS", nullable = false, precision = 10, scale = 9)
    private BigDecimal pcTaxaJuros;

    @Column(name = "NU_MINIMO_MESES", nullable = false)
    private Short nuMinimoMeses;

    @Column(name = "NU_MAXIMO_MESES")
    private Short nuMaximoMeses;

    @Column(name = "VR_MINIMO", nullable = false, precision = 18, scale = 2)
    private BigDecimal vrMinimo;

    @Column(name = "VR_MAXIMO", precision = 18, scale = 2)
    private BigDecimal vrMaximo;

}