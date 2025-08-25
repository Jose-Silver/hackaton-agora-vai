//package config;
//
//import domain.enums.SystemConstant;
//import jakarta.servlet.*;
//import jakarta.servlet.annotation.WebFilter;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.jboss.logging.Logger;
//
//import java.io.IOException;
//import java.util.UUID;
//
//@WebFilter("/*")
//public class RequestIdFilter implements Filter {
//    private static final Logger LOG = Logger.getLogger(RequestIdFilter.class);
//
//    @Override
//    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
//            throws IOException, ServletException {
//
//        HttpServletRequest httpRequest = (HttpServletRequest) request;
//        HttpServletResponse httpResponse = (HttpServletResponse) response;
//
//        String requestId = httpRequest.getHeader(SystemConstant.REQUEST_ID_HEADER.getStringValue());
//
//        if (requestId == null || requestId.isEmpty()) {
//            requestId = UUID.randomUUID().toString();
//        }
//
//        httpResponse.setHeader(SystemConstant.REQUEST_ID_HEADER.getStringValue(), requestId);
//
//        LOG.debugf("RequestId: %s for %s %s", requestId,
//                  httpRequest.getMethod(),
//                  httpRequest.getRequestURI());
//
//        chain.doFilter(request, response);
//    }
//}
