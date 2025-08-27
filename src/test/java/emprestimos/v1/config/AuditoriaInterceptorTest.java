package emprestimos.v1.config;

import emprestimos.v1.service.AuditoriaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import jakarta.interceptor.InvocationContext;
import java.lang.reflect.Method;

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
        MockitoAnnotations.initMocks(this);
        interceptor = new AuditoriaInterceptor();
        // Inject the mock service manually for testing
        interceptor.auditoriaService = auditoriaService;
    }


    private static void assertEquals(String expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected: " + expected + ", but was: " + actual);
        }
    }
}
