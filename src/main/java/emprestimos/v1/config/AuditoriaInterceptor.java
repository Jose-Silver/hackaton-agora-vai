package emprestimos.v1.config;

import emprestimos.v1.service.AuditoriaService;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

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

    @AroundInvoke
    public Object audit(InvocationContext context) throws Exception {
        Auditado auditado = getAuditadoAnnotation(context);
        
        if (auditado == null) {
            return context.proceed();
        }

        // Coleta informações do contexto
        String usuario = obterUsuario();
        String acao = obterAcao(auditado, context);
        String recurso = obterRecurso(auditado, context);
        String ipOrigem = obterIpOrigem();
        String detalhes = construirDetalhes(context);

        Object resultado;

        try {
            resultado = context.proceed();
            
            // Registra sucesso
            String dadosNovos = auditado.capturarDadosNovos() ?
                auditoriaService.objetoParaString(resultado) : null;
                
            auditoriaService.registrarSucesso(
                usuario, acao, recurso, ipOrigem, detalhes, dadosNovos
            );
            
            log.debug("Auditoria registrada - IP: {}, Usuário: {}", ipOrigem, usuario);

            return resultado;
            
        } catch (Exception e) {
            // Registra erro
            auditoriaService.registrarErro(
                usuario, acao, recurso, ipOrigem, detalhes, e.getMessage()
            );
            
            log.debug("Erro auditado - IP: {}, Usuário: {}", ipOrigem, usuario);

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
            // Implementação simplificada que sempre retorna valor válido
            return "sistema";
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
            // Implementação para obter IP real em desenvolvimento
            // Em produção, isso seria obtido dos headers HTTP do load balancer
            String ip = java.net.InetAddress.getLocalHost().getHostAddress();
            return ip != null ? ip : "127.0.0.1";
        } catch (Exception e) {
            log.warn("Erro ao obter IP de origem: ", e);
            return "127.0.0.1"; // Fallback garantido
        }
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
}
