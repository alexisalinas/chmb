package com.banamex.cops.update_offer.config;

import com.banamex.cops.app.repository.CopOfferFulfillmentRepository;
import com.banamex.cops.app.service.CopOfferFulfillmentService;
import com.banamex.cops.app.service.impl.CopOfferFulfillmentServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Conecta el micro con la consulta de los datos de cumplimiento (fulfillment) que está en la librería
 * {@code chnn-cop-lib-operation-db}.
 * <p>
 * El micro solo escanea {@code com.banamex.cops.update_offer}, así que las clases de la librería no entran
 * al contexto por sí solas. Esta configuración las registra, con la misma conexión de Oracle del yml
 * ({@code spring.datasource}), mediante el {@link NamedParameterJdbcTemplate} que crea Spring Boot.
 * <p>
 * Si algún día la librería registra estos beans en su propia autoconfiguración, el
 * {@link ConditionalOnMissingBean} evita duplicarlos en el micro.
 */
@Configuration
public class OfferFulfillmentConfig {

    /**
     * Repositorio que ejecuta la consulta de la librería.
     *
     * @param jdbc plantilla JDBC creada con el {@code spring.datasource} del yml
     * @return el repositorio de la consulta
     */
    @Bean
    @ConditionalOnMissingBean
    public CopOfferFulfillmentRepository copOfferFulfillmentRepository(NamedParameterJdbcTemplate jdbc) {
        return new CopOfferFulfillmentRepository(jdbc);
    }

    /**
     * Servicio que usa {@code OfferDataClient} para traer los datos de la oferta.
     *
     * @param repository repositorio de la consulta
     * @return el servicio de la consulta
     */
    @Bean
    @ConditionalOnMissingBean
    public CopOfferFulfillmentService copOfferFulfillmentService(CopOfferFulfillmentRepository repository) {
        return new CopOfferFulfillmentServiceImpl(repository);
    }
}
