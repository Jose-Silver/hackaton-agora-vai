package emprestimos.v1.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Utilitário para filtrar campos de objetos de resposta baseado no parâmetro "campos"
 */
@ApplicationScoped
public class FieldFilterUtil {

    @Inject
    ObjectMapper objectMapper;

    private ObjectMapper customMapper;

    @PostConstruct
    public void init() {
        // Cria um ObjectMapper customizado que não usa notação científica
        customMapper = objectMapper.copy();

        // Serializer global para BigDecimal -> sempre plain string (sem notação científica)
        SimpleModule bigDecimalModule = new SimpleModule();
        bigDecimalModule.addSerializer(java.math.BigDecimal.class, new JsonSerializer<java.math.BigDecimal>() {
            @Override
            public void serialize(java.math.BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeNumber(value.toPlainString());
            }
        });
        customMapper.registerModule(bigDecimalModule);
    }

    /**
     * Filtra os campos do objeto baseado na lista de campos solicitados
     *
     * @param object O objeto a ser filtrado
     * @param camposParam String com campos separados por vírgula (ex: "id,nome,valor")
     * @return Objeto filtrado contendo apenas os campos solicitados
     */
    public Object filterFields(Object object, String camposParam) {
        if (camposParam == null || camposParam.trim().isEmpty()) {
            return object; // Retorna objeto completo se não há filtro
        }

        try {
            // Converte o objeto para JsonNode usando o mapper customizado
            JsonNode jsonNode = customMapper.valueToTree(object);

            // Parse dos campos solicitados
            Set<String> camposSolicitados = new HashSet<>(
                    Arrays.asList(camposParam.split(","))
            );

            // Remove espaços em branco dos campos
            camposSolicitados = camposSolicitados.stream()
                    .map(String::trim)
                    .collect(java.util.stream.Collectors.toSet());

            if (jsonNode.isArray()) {
                return filterArrayFields((ArrayNode) jsonNode, camposSolicitados);
            } else {
                return filterObjectFields((ObjectNode) jsonNode, camposSolicitados);
            }

        } catch (Exception e) {
            // Em caso de erro, retorna o objeto original
            return object;
        }
    }

    /**
     * Filtra campos de um array de objetos
     */
    private ArrayNode filterArrayFields(ArrayNode arrayNode, Set<String> camposSolicitados) {
        ArrayNode filteredArray = customMapper.createArrayNode();

        for (JsonNode item : arrayNode) {
            if (item.isObject()) {
                ObjectNode filteredObject = filterObjectFields((ObjectNode) item, camposSolicitados);
                filteredArray.add(filteredObject);
            } else {
                filteredArray.add(item);
            }
        }

        return filteredArray;
    }

    /**
     * Filtra campos de um objeto
     */
    private ObjectNode filterObjectFields(ObjectNode objectNode, Set<String> camposSolicitados) {
        ObjectNode filteredObject = customMapper.createObjectNode();

        // Itera sobre os campos solicitados
        for (String campo : camposSolicitados) {
            if (campo.contains(".")) {
                // Suporta campos aninhados (ex: "produto.nome")
                handleNestedField(objectNode, filteredObject, campo);
            } else if (objectNode.has(campo)) {
                filteredObject.set(campo, objectNode.get(campo));
            }
        }

        return filteredObject;
    }

    /**
     * Manipula campos aninhados (dot notation)
     */
    private void handleNestedField(ObjectNode source, ObjectNode target, String nestedField) {
        String[] parts = nestedField.split("\\.", 2);
        String currentField = parts[0];
        String remainingPath = parts[1];

        if (source.has(currentField)) {
            JsonNode currentNode = source.get(currentField);

            if (currentNode.isObject()) {
                ObjectNode nestedObject = target.has(currentField)
                        ? (ObjectNode) target.get(currentField)
                        : target.putObject(currentField);

                if (remainingPath.contains(".")) {
                    handleNestedField((ObjectNode) currentNode, nestedObject, remainingPath);
                } else if (currentNode.has(remainingPath)) {
                    nestedObject.set(remainingPath, currentNode.get(remainingPath));
                }
            } else if (currentNode.isArray() && parts.length == 2) {
                // Para arrays, aplica o filtro em cada item
                ArrayNode filteredArray = target.putArray(currentField);
                for (JsonNode item : currentNode) {
                    if (item.isObject() && item.has(remainingPath)) {
                        ObjectNode itemObject = filteredArray.addObject();
                        itemObject.set(remainingPath, item.get(remainingPath));
                    }
                }
            }
        }
    }
}
