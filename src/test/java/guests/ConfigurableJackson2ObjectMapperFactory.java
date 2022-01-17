package guests;

import com.fasterxml.jackson.databind.ObjectMapper;
import guests.config.JacksonConfiguration;
import io.restassured.path.json.mapper.factory.DefaultJackson2ObjectMapperFactory;

import java.lang.reflect.Type;

public class ConfigurableJackson2ObjectMapperFactory extends DefaultJackson2ObjectMapperFactory {

    private final static ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();

    @Override
    public ObjectMapper create(Type cls, String charset) {
        return ConfigurableJackson2ObjectMapperFactory.objectMapper;
    }
}
