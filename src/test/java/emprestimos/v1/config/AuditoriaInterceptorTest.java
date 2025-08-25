package emprestimos.v1.config;

import emprestimos.v1.service.AuditoriaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import jakarta.interceptor.InvocationContext;
import java.lang.reflect.Method;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuditoriaInterceptorTest {

    @Mock
    private AuditoriaService auditoriaService;

    @Mock
    private InvocationContext invocationContext;

    @Mock
    private Method method;

    private AuditoriaInterceptor interceptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        interceptor = new AuditoriaInterceptor();
        // Inject the mock service manually for testing
        interceptor.auditoriaService = auditoriaService;
    }

    @Test
    void testAuditInterceptorRegistraTempoExecucaoEIpCorretamente() throws Exception {
        // Arrange
        Auditado auditado = mock(Auditado.class);
        when(auditado.acao()).thenReturn("TESTE");
        when(auditado.recurso()).thenReturn("RECURSO_TESTE");
        when(auditado.capturarDadosAnteriores()).thenReturn(false);
        when(auditado.capturarDadosNovos()).thenReturn(false);

        when(method.getAnnotation(Auditado.class)).thenReturn(auditado);
        when(method.getName()).thenReturn("metodoTeste");

        when(invocationContext.getMethod()).thenReturn(method);
        when(invocationContext.getTarget()).thenReturn(new Object());
        when(invocationContext.getParameters()).thenReturn(new Object[0]);
        when(invocationContext.proceed()).thenReturn("resultado");

        // Act
        Object resultado = interceptor.audit(invocationContext);

        // Assert
        verify(auditoriaService).registrarSucesso(
            eq("sistema"),                    // usuario
            eq("TESTE"),                      // acao
            eq("RECURSO_TESTE"),             // recurso
            startsWith("127.0.0.1"),         // ipOrigem (não deve ser null)
            eq("Quarkus-Application/1.0"),   // userAgent (não deve ser null)
            anyString(),                     // detalhes
            isNull(),                        // dadosAnteriores
            isNull(),                        // dadosNovos
            longThat(tempo -> tempo >= 0),   // tempoExecucao (não deve ser null e >= 0)
            anyString()                      // sessaoId (não deve ser null)
        );

        assertEquals("resultado", resultado);
    }

    private static void assertEquals(String expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected: " + expected + ", but was: " + actual);
        }
    }
}
