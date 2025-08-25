package emprestimos.v1.config;

import emprestimos.v1.service.AuditoriaService;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;

/**
 * Interceptor que captura automaticamente operações auditadas
 */
@Interceptor
@Auditado
@Priority(2000)
@Slf4j
public class AuditoriaInterceptor {

    @Inject
    AuditoriaService auditoriaService;

    @Context
    ContainerRequestContext requestContext;

    @AroundInvoke
    public Object audit(InvocationContext context) throws Exception {
        long startTime = System.currentTimeMillis();
        Auditado auditado = getAuditadoAnnotation(context);
        
        if (auditado == null) {
            return context.proceed();
        }

        // Coleta informações do contexto
        String usuario = obterUsuario();
        String acao = obterAcao(auditado, context);
        String recurso = obterRecurso(auditado, context);
        String ipOrigem = obterIpOrigem();
        String userAgent = obterUserAgent();
        String sessaoId = obterSessaoId();
        String detalhes = construirDetalhes(context);
        
        // Captura dados anteriores se solicitado
        String dadosAnteriores = null;
        if (auditado.capturarDadosAnteriores()) {
            dadosAnteriores = capturarParametros(context);
        }

        Object resultado;

        try {
            resultado = context.proceed();
            
            // Registra sucesso
            long tempoExecucao = System.currentTimeMillis() - startTime;
            String dadosNovos = auditado.capturarDadosNovos() ? 
                auditoriaService.objetoParaString(resultado) : null;
                
            auditoriaService.registrarSucesso(
                usuario, acao, recurso, ipOrigem, userAgent, 
                detalhes, dadosAnteriores, dadosNovos, 
                tempoExecucao, sessaoId
            );
            
            return resultado;
            
        } catch (Exception e) {
            // Registra erro
            long tempoExecucao = System.currentTimeMillis() - startTime;
            auditoriaService.registrarErro(
                usuario, acao, recurso, ipOrigem, userAgent,
                detalhes, e.getMessage(), tempoExecucao, sessaoId
            );
            
            throw e;
        }
    }

    private Auditado getAuditadoAnnotation(InvocationContext context) {
        // Primeiro verifica no método
        Method method = context.getMethod();
        Auditado methodAnnotation = method.getAnnotation(Auditado.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        
        // Se não encontrar no método, verifica na classe
        return context.getTarget().getClass().getAnnotation(Auditado.class);
    }

    private String obterUsuario() {
        try {
            // Tentar obter usuário do contexto de segurança
            // Por enquanto, retorna um usuário padrão
            if (requestContext != null && requestContext.getSecurityContext() != null
                && requestContext.getSecurityContext().getUserPrincipal() != null) {
                return requestContext.getSecurityContext().getUserPrincipal().getName();
            }
            return "sistema"; // usuário padrão para operações automáticas
        } catch (Exception e) {
            log.warn("Erro ao obter usuário: ", e);
            return "desconhecido";
        }
    }

    private String obterAcao(Auditado auditado, InvocationContext context) {
        if (!auditado.acao().isEmpty()) {
            return auditado.acao();
        }
        
        // Usa o nome do método como ação padrão
        return context.getMethod().getName();
    }

    private String obterRecurso(Auditado auditado, InvocationContext context) {
        if (!auditado.recurso().isEmpty()) {
            return auditado.recurso();
        }
        
        // Usa o nome da classe como recurso padrão
        return context.getTarget().getClass().getSimpleName();
    }

    private String obterIpOrigem() {
        try {
            if (requestContext != null) {
                // Verifica headers de proxy primeiro
                String xForwardedFor = requestContext.getHeaderString("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                
                String xRealIp = requestContext.getHeaderString("X-Real-IP");
                if (xRealIp != null && !xRealIp.isEmpty()) {
                    return xRealIp;
                }
                
                // Como último recurso, tenta obter do contexto
                return "cliente";
            }
        } catch (Exception e) {
            log.warn("Erro ao obter IP de origem: ", e);
        }
        return "desconhecido";
    }

    private String obterUserAgent() {
        try {
            if (requestContext != null) {
                return requestContext.getHeaderString("User-Agent");
            }
        } catch (Exception e) {
            log.warn("Erro ao obter User-Agent: ", e);
        }
        return null;
    }

    private String obterSessaoId() {
        try {
            // Para JAX-RS, usaremos um ID único baseado no request
            if (requestContext != null) {
                String sessionHeader = requestContext.getHeaderString("X-Session-ID");
                if (sessionHeader != null && !sessionHeader.isEmpty()) {
                    return sessionHeader;
                }
            }
        } catch (Exception e) {
            log.warn("Erro ao obter sessão: ", e);
        }
        return UUID.randomUUID().toString(); // ID único se não houver sessão
    }

    private String construirDetalhes(InvocationContext context) {
        StringBuilder detalhes = new StringBuilder();
        detalhes.append("Classe: ").append(context.getTarget().getClass().getSimpleName());
        detalhes.append(", Método: ").append(context.getMethod().getName());
        
        if (context.getParameters() != null && context.getParameters().length > 0) {
            detalhes.append(", Parâmetros: ").append(context.getParameters().length);
        }
        
        return detalhes.toString();
    }

    private String capturarParametros(InvocationContext context) {
        try {
            if (context.getParameters() != null && context.getParameters().length > 0) {
                return Arrays.toString(context.getParameters());
            }
        } catch (Exception e) {
            log.warn("Erro ao capturar parâmetros: ", e);
        }
        return null;
    }
}
