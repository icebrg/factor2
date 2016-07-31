package net.gilstraps.brian.factor2;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

@Configuration
@Import(AnimalsController.class)
@EnableWebMvc
public class AnimalsConfiguration extends WebMvcConfigurationSupport {
    public AnimalsConfiguration() {
        super();
    }

    @Bean
    public RequestMappingHandlerAdapter requestMappingHandlerAdapter() {
        RequestMappingHandlerAdapter handlerAdapter = super.requestMappingHandlerAdapter();

        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        jsonConverter.setPrettyPrint(true);
        jsonConverter.getObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        List<MediaType> jsonTypes = new ArrayList<MediaType>(jsonConverter.getSupportedMediaTypes());
        jsonTypes.add(MediaType.TEXT_PLAIN);
        jsonTypes.add(MediaType.TEXT_HTML);
        jsonTypes.add(MediaType.APPLICATION_JSON);
        jsonConverter.setSupportedMediaTypes(jsonTypes);
        handlerAdapter.getMessageConverters().add(0,jsonConverter);

        return handlerAdapter;
    }

}