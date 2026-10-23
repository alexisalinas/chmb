package com.banamex.cops.update_offer.config;

import com.banamex.cops.update_offer.dto.error.ErrorResponse;
import com.banamex.cops.update_offer.dto.error.ErrorType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Valida que lleguen los headers marcados con @HeaderValidation.Required (en la clase o en el método).
 * Si falta alguno responde 400 con un ErrorResponse y no llega al controller.
 *
 * Uso:  @HeaderValidation.Required({"uuid", "ChannelId", ...})
 */
@Configuration
public class HeaderValidation implements WebMvcConfigurer, HandlerInterceptor {

    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Required {
        String[] value();
    }

    private final JsonMapper objectMapper;

    public HeaderValidation(JsonMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this);
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws IOException {

        if (handler instanceof HandlerMethod hm) {
            List<String> missing = Stream.of(
                            hm.getBeanType().getAnnotation(Required.class),
                            hm.getMethodAnnotation(Required.class))
                    .filter(Objects::nonNull)
                    .flatMap(a -> Arrays.stream(a.value()))
                    .distinct()
                    .filter(h -> !StringUtils.hasText(request.getHeader(h)))
                    .toList();

            if (!missing.isEmpty()) {
                ErrorResponse error = invalidResponse(
                        "Missing required headers: " + String.join(" ", missing),
                        Optional.ofNullable(request.getHeader("uuid")).orElse(UUID.randomUUID().toString()));

                response.setStatus(HttpStatus.BAD_REQUEST.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                objectMapper.writeValue(response.getWriter(), error);
                return false;
            }
        }
        return true;
    }

    public static ErrorResponse invalidResponse(String moreInfo, String uuid) {
        return ErrorResponse.builder()
                .type(ErrorType.ERROR)
                .details("Missing or invalid Parameters")
                .code("InvalidRequest")
                .location("header")
                .moreInfo(moreInfo)
                .uuid(uuid)
                .timestamp(OffsetDateTime.now().toString())
                .build();
    }
}
