package com.banamex.cops.update_offer.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

/**
 * Registra las implementaciones personalizadas de los repositorios JPA de la librería (por ejemplo
 * {@code DOfferpaletteRepositoryCustomImpl}), que Spring Data busca por nombre. Solo esas: el resto de la librería
 * no se escanea, para no duplicar beans.
 */
@Configuration
@ComponentScan(basePackages = "com.banamex.cops.app", useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*Repository(Custom)?Impl"))
public class LibraryRepositoryFragmentsConfig {
}
