package emprestimos.v1.domain.dto.common;

import jakarta.ws.rs.QueryParam;
import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Classe base para parâmetros de consulta que inclui o filtro de campos
 */
@Data
public class BaseQueryParams {
    
    @QueryParam("campos")
    @Schema(description = "Lista de campos separados por vírgula para filtrar a resposta. " +
            "Suporta campos aninhados usando notação de ponto (ex: 'id,nome,produto.descricao'). " +
            "Se vazio, retorna todos os campos.")
    private String campos;
}
