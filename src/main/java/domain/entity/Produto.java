package domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "produto")
public class Produto {
    @Id
    @Column(name = "co_produto")
    private Integer coProduto;

    @Column(name = "no_produto", nullable = false, length = 200)
    private String noProduto;

    @Column(name = "pc_taxa_juros", nullable = false, precision = 10, scale = 9)
    private BigDecimal pcTaxaJuros;

    @Column(name = "nu_minimo_meses", nullable = false)
    private Short nuMinimoMeses;

    @Column(name = "nu_maximo_meses")
    private Short nuMaximoMeses;

    @Column(name = "vr_minimo", nullable = false, precision = 18, scale = 2)
    private BigDecimal vrMinimo;

    @Column(name = "vr_maximo", precision = 18, scale = 2)
    private BigDecimal vrMaximo;

}