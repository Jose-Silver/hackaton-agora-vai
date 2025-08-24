package domain.dto.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO padronizado para respostas de erro da API.
 * Segue o padrão RFC 7807 (Problem Details for HTTP APIs).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "DTO padronizado para respostas de erro da API.")
public class ErrorResponseDTO {
    
    private String codigo;
    private String mensagem;
    private String detalhe;
    private int status;
    private String path;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    private List<CampoErroDTO> erros;

    public ErrorResponseDTO() {
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponseDTO(String codigo, String mensagem, int status) {
        this();
        this.codigo = codigo;
        this.mensagem = mensagem;
        this.status = status;
    }

    public ErrorResponseDTO(String codigo, String mensagem, String detalhe, int status) {
        this(codigo, mensagem, status);
        this.detalhe = detalhe;
    }

    public ErrorResponseDTO(String codigo, String mensagem, String detalhe, int status, String path) {
        this(codigo, mensagem, detalhe, status);
        this.path = path;
    }

    // Getters e Setters
    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getDetalhe() {
        return detalhe;
    }

    public void setDetalhe(String detalhe) {
        this.detalhe = detalhe;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public List<CampoErroDTO> getErros() {
        return erros;
    }

    public void setErros(List<CampoErroDTO> erros) {
        this.erros = erros;
    }

    /**
     * DTO para representar erros específicos em campos.
     */
    public static class CampoErroDTO {
        private String campo;
        private String mensagem;
        private Object valorRejeitado;

        public CampoErroDTO() {}

        public CampoErroDTO(String campo, String mensagem) {
            this.campo = campo;
            this.mensagem = mensagem;
        }

        public CampoErroDTO(String campo, String mensagem, Object valorRejeitado) {
            this.campo = campo;
            this.mensagem = mensagem;
            this.valorRejeitado = valorRejeitado;
        }

        // Getters e Setters
        public String getCampo() {
            return campo;
        }

        public void setCampo(String campo) {
            this.campo = campo;
        }

        public String getMensagem() {
            return mensagem;
        }

        public void setMensagem(String mensagem) {
            this.mensagem = mensagem;
        }

        public Object getValorRejeitado() {
            return valorRejeitado;
        }

        public void setValorRejeitado(Object valorRejeitado) {
            this.valorRejeitado = valorRejeitado;
        }
    }
}
