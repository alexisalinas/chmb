package com.banamex.cops.update_offer.config;

import com.banamex.cops.app.repository.CopOfferFulfillmentRepository;
import com.banamex.cops.app.repository.CopSuppressionRepository;
import com.banamex.cops.app.service.impl.CopOfferFulfillmentServiceImpl;
import com.banamex.cops.app.service.impl.CopSuppressionServiceImpl;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Conecta el micro con las consultas de la librería {@code chnn-cop-lib-operation-db}: los datos de cumplimiento
 * (fulfillment) de Kafka y la lista de entidades suprimidas ({@code COP_SUPP_CHNL} y {@code COP_SUPP_OPR}). Usan la
 * misma base de datos del {@code spring.datasource} (el {@code NamedParameterJdbcTemplate} que crea Spring Boot).
 * <p>
 * El micro solo escanea {@code com.banamex.cops.update_offer}, así que las clases de la librería no entran al contexto
 * por sí solas. Se registran con {@link Import}, igual que lo hace la autoconfiguración de la librería
 * ({@code CopsDbAutoConfiguration}). <b>Eso evita duplicados:</b> si la librería también importa las mismas clases
 * (como ya pasa con las de cumplimiento), Spring registra cada clase una sola vez, con el nombre completo de la clase.
 * Registrarlas con métodos {@code @Bean} las duplicaba ("expected single matching bean but found 2").
 * <p>
 * No incluye el lookup de zona horaria ({@code CopTimezoneLookupRepository} / {@code CopTimezoneLookupService}): lo
 * registra la propia librería.
 */
@Configuration
@Import({
        CopOfferFulfillmentRepository.class,
        CopOfferFulfillmentServiceImpl.class,
        CopSuppressionRepository.class,
        CopSuppressionServiceImpl.class
})
public class OfferFulfillmentConfig {
}
