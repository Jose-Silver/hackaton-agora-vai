package emprestimos.v1.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.ws.rs.core.HttpHeaders;

/**
 * Configuração personalizada para JAXB que exclui interfaces JAX-RS do processamento.
 * Esta classe resolve o problema de serialização XML quando interfaces como HttpHeaders
 * são encontradas no classpath.
 */
@ApplicationScoped
public class JaxbConfiguration {

    /**
     * Interface marcada como transiente para evitar processamento pelo JAXB.
     * Esta é uma workaround para o problema onde o JAXB tenta processar 
     * interfaces JAX-RS que não devem ser serializadas.
     */
    @XmlTransient
    public interface ExcludedJaxRSInterfaces extends HttpHeaders {
        // Interface vazia usada apenas para aplicar @XmlTransient ao HttpHeaders
    }
    
    /**
     * Produz um JAXBContext customizado que exclui interfaces problemáticas.
     * 
     * @return JAXBContext configurado
     * @throws JAXBException se houver erro na criação do contexto
     */
    @Produces
    @ApplicationScoped
    public JAXBContext createJAXBContext() throws JAXBException {
        // Lista das classes que devem ser processadas pelo JAXB
        // Baseada no arquivo jaxb.index
        Class<?>[] classes = {
            emprestimos.v1.domain.dto.common.ErrorResponseDTO.class,
            emprestimos.v1.domain.dto.simulacao.create.response.SimulacaoResponseDTO.class,
            emprestimos.v1.domain.dto.simulacao.create.response.PaginaSimulacaoSimplificadaDTO.class,
            emprestimos.v1.domain.dto.simulacao.list.response.SimulacaoResumoSimplificadoDTO.class,
            emprestimos.v1.domain.dto.simulacao.create.response.ParcelaDTO.class,
            emprestimos.v1.domain.dto.simulacao.create.response.ResultadoSimulacaoDTO.class,
            emprestimos.v1.domain.dto.simulacao.buscar.response.SimulacaoDetalhesDTO.class,
            emprestimos.v1.domain.dto.simulacao.parcelas.response.ParcelasSimulacaoDTO.class,
            emprestimos.v1.domain.dto.simulacao.parcela.response.ParcelaEspecificaDTO.class,
            emprestimos.v1.domain.dto.simulacao.por_produto_dia.response.SimulacaoPorProdutoDiaDTO.class,
            emprestimos.v1.domain.dto.simulacao.por_produto_dia.response.SimulacaoPorProdutoDiaResponseDTO.class
        };
        
        return JAXBContext.newInstance(classes);
    }
}
