package emprestimos.v1.config;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.UUID;

/**
 * Filtro para capturar informações da requisição HTTP para auditoria
 */
@Provider
@Slf4j
public class AuditoriaRequestFilter implements ContainerRequestFilter {

    @Inject
    AuditoriaContext auditoriaContext;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        try {
            // Captura IP de origem
            String ipOrigem = obterIpOrigem(requestContext);
            auditoriaContext.setIpOrigem(ipOrigem);

            // Captura User-Agent
            String userAgent = requestContext.getHeaderString("User-Agent");
            auditoriaContext.setUserAgent(userAgent);

            // Captura sessão ID
            String sessaoId = obterSessaoId(requestContext);
            auditoriaContext.setSessaoId(sessaoId);

            // Captura usuário se disponível
            String usuario = obterUsuario(requestContext);
            auditoriaContext.setUsuario(usuario);

            // Inicia contagem de tempo
            auditoriaContext.iniciarTempo();

            log.debug("Informações de auditoria capturadas - IP: {}, UserAgent: {}, Sessão: {}", 
                     ipOrigem, userAgent, sessaoId);

        } catch (Exception e) {
            log.warn("Erro ao capturar informações para auditoria: ", e);
        }
    }

    private String obterIpOrigem(ContainerRequestContext requestContext) {
        try {
            // Verifica headers de proxy primeiro
            String xForwardedFor = requestContext.getHeaderString("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.trim().isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }

            String xRealIp = requestContext.getHeaderString("X-Real-IP");
            if (xRealIp != null && !xRealIp.trim().isEmpty()) {
                return xRealIp;
            }

            String xOriginalForwarded = requestContext.getHeaderString("X-Original-Forwarded-For");
            if (xOriginalForwarded != null && !xOriginalForwarded.trim().isEmpty()) {
                return xOriginalForwarded.split(",")[0].trim();
            }

            // Para desenvolvimento local, retorna localhost
            return "127.0.0.1";
        } catch (Exception e) {
            log.warn("Erro ao obter IP de origem: ", e);
            return "desconhecido";
        }
    }

    private String obterSessaoId(ContainerRequestContext requestContext) {
        try {
            // Verifica header customizado de sessão
            String sessionHeader = requestContext.getHeaderString("X-Session-ID");
            if (sessionHeader != null && !sessionHeader.trim().isEmpty()) {
                return sessionHeader;
            }

            // Verifica cookie de sessão
            String cookieHeader = requestContext.getHeaderString("Cookie");
            if (cookieHeader != null) {
                // Procura por JSESSIONID ou similar
                String[] cookies = cookieHeader.split(";");
                for (String cookie : cookies) {
                    if (cookie.trim().startsWith("JSESSIONID=")) {
                        return cookie.trim().substring(11);
                    }
                }
            }

            // Se não houver sessão, gera um ID único para a requisição
            return UUID.randomUUID().toString();
        } catch (Exception e) {
            log.warn("Erro ao obter sessão: ", e);
            return UUID.randomUUID().toString();
        }
    }

    private String obterUsuario(ContainerRequestContext requestContext) {
        try {
            if (requestContext.getSecurityContext() != null 
                && requestContext.getSecurityContext().getUserPrincipal() != null) {
                return requestContext.getSecurityContext().getUserPrincipal().getName();
            }

            // Verifica header de autorização personalizado
            String authHeader = requestContext.getHeaderString("X-User-ID");
            if (authHeader != null && !authHeader.trim().isEmpty()) {
                return authHeader;
            }

            return "sistema"; // usuário padrão
        } catch (Exception e) {
            log.warn("Erro ao obter usuário: ", e);
            return "desconhecido";
        }
    }
}
