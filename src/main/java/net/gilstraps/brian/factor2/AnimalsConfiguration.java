package net.gilstraps.brian.factor2;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.zafarkhaja.semver.Version;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.mvc.condition.MediaTypeExpression;
import org.springframework.web.servlet.mvc.condition.ProducesRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

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
        handlerAdapter.getMessageConverters().add(0, jsonConverter);

        return handlerAdapter;
    }

    @Bean
    public RequestMappingHandlerMapping requestMappingHandlerMapping() {
        return new RequestMappingHandlerMapping() {
            @Override
            protected Comparator<RequestMappingInfo> getMappingComparator(HttpServletRequest request) {
                return getComparator(request, super.getMappingComparator(request));
            }

            // Filter out all mappings that have a different major version number or which are less than the accepted
            // version
            @Override
            protected RequestMappingInfo getMatchingMapping(RequestMappingInfo mapping, HttpServletRequest request) {
                ProducesRequestCondition producesRequestCondition = mapping.getProducesCondition();
                Set<MediaTypeExpression> expressions = producesRequestCondition.getExpressions();
                // TODO - handle more than one expression
                if (expressions.size() > 1) {
                    throw new UnsupportedOperationException("Need to support multiple media type expressions!");
                }
                else if ( expressions.size() == 0 ) {
                    return mapping;
                }
                MediaTypeExpression expression = expressions.iterator().next();
                // TODO - handle negated
                if (expression.isNegated()) {
                    throw new UnsupportedOperationException("What to do with negated expressions?!?");
                }
                MediaType mappingMediaType = expression.getMediaType();
                String mappingVersionString = mappingMediaType.getParameter(VERSION_PARAMETER_NAME);
                if ( mappingVersionString == null ) {
                    // No version - leave it in - later we'll match against the highest version
                    return mapping;
                }
                Version mappingVersion = Version.valueOf(mappingVersionString);

                Set<MediaType> mediaTypesWithVersions = parseTypesWithVersioning(request);
                // TODO - what if they pass both 'version=1.0.0' and 'version=2.0.0'?
                // I think I probably need a whole new approach
                for (MediaType mediaType : mediaTypesWithVersions) {
                    if (mediaType.isCompatibleWith(mappingMediaType)) {
                        final String acceptedVersionString = mediaType.getParameter(VERSION_PARAMETER_NAME);
                        final Version acceptedVersion = Version.valueOf(acceptedVersionString);
                        if (!acceptedVersion.lessThanOrEqualTo(mappingVersion) || acceptedVersion.getMajorVersion() != mappingVersion.getMajorVersion()) {
                            return null;
                        }
                    }
                }
                return mapping;
            }
        };
    }

    private Comparator<RequestMappingInfo> getComparator(final HttpServletRequest request, final Comparator<RequestMappingInfo> baseComparator) {
        return new VersionCheckingRequestMappingComparator(request, baseComparator);
    }

    public static final String VERSION_PARAMETER_NAME = "version";

    // TODO - make non-static
    // TODO - there's probably an existing spring or other open source way to do this
    public static Set<MediaType> parseTypesWithVersioning(final HttpServletRequest request) {
        Set<MediaType> results = new HashSet<>();
        final String acceptHeader = request.getHeader(HttpHeaders.ACCEPT);
        if (acceptHeader != null) {
            final String[] mediaTypeStrings = acceptHeader.split(",");
            for ( String mediaType : mediaTypeStrings ) {
                MediaType parsed = MediaType.parseMediaType(mediaType);
                if ( parsed.getParameter(VERSION_PARAMETER_NAME) != null ) {
                    results.add(parsed);
                }
            }
        }
        return results;
    }

    private static class VersionCheckingRequestMappingComparator implements Comparator<RequestMappingInfo> {
        private final HttpServletRequest request;
        private final Comparator<RequestMappingInfo> baseComparator;
        private final Set<MediaType> acceptTypesWithVersioning;

        public VersionCheckingRequestMappingComparator(final HttpServletRequest request, final Comparator<RequestMappingInfo> baseComparator) {
            this.request = request;
            this.baseComparator = baseComparator;
            this.acceptTypesWithVersioning = parseTypesWithVersioning(request);
        }

        @Override
        public int compare(final RequestMappingInfo o1, final RequestMappingInfo o2) {
            if ( acceptTypesWithVersioning.size() == 0 ) {
                return baseComparator.compare(o1,o2);
            }

            // Skip the produces critera. If the same then check it specifically
            RequestMappingInfo checker = new RequestMappingInfo(o1.getName(), o1.getPatternsCondition(), o1.getMethodsCondition(), o1.getParamsCondition(),
                                                                o1.getHeadersCondition(), o1.getConsumesCondition(), o2.getProducesCondition(),
                                                                o1.getCustomCondition());
            int checked = baseComparator.compare(checker, o2);
            if (checked == 0) {
                ProducesRequestCondition p1 = o1.getProducesCondition();
                ProducesRequestCondition p2 = o2.getProducesCondition();
                Set<MediaTypeExpression> p1Expressions = p1.getExpressions();
                Set<MediaTypeExpression> p2Expressions = p2.getExpressions();
                // TODO - handle more than one expression
                if ( p1Expressions.size() > 1 || p2Expressions.size() > 1 ) {
                    throw new UnsupportedOperationException("Need to support multiple media type expressions!");
                }

                MediaTypeExpression m1 = p1Expressions.iterator().next();
                MediaTypeExpression m2 = p2Expressions.iterator().next();
                // TODO - handle negated
                if ( m1.isNegated() || m2.isNegated() ) {
                    throw new UnsupportedOperationException("What to do with negated expressions?!?");
                }
                MediaType media1 = m1.getMediaType();
                MediaType media2 = m2.getMediaType();
                String version1String = media1.getParameter(VERSION_PARAMETER_NAME);
                String version2String = media2.getParameter(VERSION_PARAMETER_NAME);
                Version version1 = Version.valueOf(version1String);
                Version version2 = Version.valueOf(version2String);
                for ( MediaType mediaType : acceptTypesWithVersioning) {
                    if ( mediaType.isCompatibleWith(media1)) {
                        final String acceptedVersionString = mediaType.getParameter(VERSION_PARAMETER_NAME);
                        final Version acceptedVersion = Version.valueOf(acceptedVersionString);
                        if ( version1.greaterThanOrEqualTo(version2) ) {
                            if ( acceptedVersion.lessThanOrEqualTo(version1) &&
                                    acceptedVersion.getMajorVersion() == version1.getMajorVersion() ) {
                                // Version 1 wins, so it's 'less' because 'less' matches better
                                checked = -1;
                            }
                        }
                        if ( checked == 0 ) {
                            if ( acceptedVersion.lessThanOrEqualTo(version2) &&
                                    acceptedVersion.getMajorVersion() == version2.getMajorVersion() ) {
                                // Version 2 wins, so it's 'less' because 'less' matches better
                                checked = 1;
                            }
                        }
                    }
                    if ( checked != 0 ) {
                        break;
                    }
                }
            }
            return checked;
        }
    }

}