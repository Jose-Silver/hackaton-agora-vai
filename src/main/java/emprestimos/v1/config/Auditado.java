package emprestimos.v1.config;

import jakarta.interceptor.InterceptorBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação para marcar métodos ou classes que devem ter suas operações auditadas
 */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Auditado {

    /**
     * Ação a ser registrada na auditoria
     */
    String acao() default "";

    /**
     * Recurso sendo auditado
     */
    String recurso() default "";

    /**
     * Se deve capturar os dados antes da operação
     */
    boolean capturarDadosAnteriores() default false;

    /**
     * Se deve capturar os dados após a operação
     */
    boolean capturarDadosNovos() default true;
}
