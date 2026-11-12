package com.banamex.cops.update_offer.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

/**
 * Escribe al arrancar lo que el micro va a usar para conectarse a la base de datos, sin mostrar la contraseña:
 * <ul>
 *   <li>{@code [DB] Configuración}: URL, usuario y descripción de la contraseña de {@code spring.datasource}, y
 *       <b>de qué fuente sale cada uno</b> (el yml, una variable de entorno, otro archivo...).</li>
 *   <li>{@code [DB] Conexión real}: URL, usuario y descripción de la contraseña del {@code DataSource} que se
 *       crea de verdad (el que usan JPA y las consultas de la librería).</li>
 * </ul>
 * Sirve para encontrar rápido un {@code ORA-01017}: una variable de entorno que le gana al yml, un archivo viejo en
 * {@code target}, una contraseña con espacios, comillas o coma, o una variable sin definir. No abre ninguna conexión.
 */
@Slf4j
@Lazy(false)
@Component
@RequiredArgsConstructor
public class DataSourceDiagnostic {

    private static final String URL = "spring.datasource.url";
    private static final String USER = "spring.datasource.username";
    private static final String PASSWORD = "spring.datasource.password";

    private final Environment environment;
    private final ObjectProvider<DataSource> dataSources;

    /**
     * Escribe las líneas {@code [DB]} cuando la aplicación terminó de arrancar.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void logConnection() {
        String password = passwordDescription();
        boolean sinResolver = password.startsWith("NO DEFINIDA") || password.startsWith("SIN RESOLVER");
        String linea = "[DB] Configuración: url={} (fuente: {}) username={} (fuente: {}) password={} (fuente: {})";
        Object[] datos = {property(URL), origin(URL), property(USER), origin(USER), password, origin(PASSWORD)};
        if (sinResolver) {
            log.error(linea + ". Oracle recibirá una contraseña incorrecta (ORA-01017)", datos);
        } else {
            log.info(linea, datos);
        }
        for (String fuente : List.of(USER, PASSWORD)) {
            if (origin(fuente).toLowerCase().contains("environment")) {
                log.warn("[DB] {} viene de una variable de entorno, que le gana al yml: si no es la correcta, corrígela o bórrala", fuente);
            }
        }
        log.info("[DB] Conexión real: {}", realDataSources());
    }

    private String property(String name) {
        try {
            return environment.getProperty(name);
        } catch (IllegalArgumentException e) {
            return "(no se pudo resolver: " + e.getMessage() + ")";
        }
    }

    private String passwordDescription() {
        try {
            return describe(environment.getProperty(PASSWORD));
        } catch (IllegalArgumentException e) {
            return "NO DEFINIDA (falta la variable DB_PASSWORD)";
        }
    }

    /** Nombre de la primera fuente de propiedades que define la propiedad (la que gana). */
    private String origin(String name) {
        if (environment instanceof ConfigurableEnvironment configurable) {
            for (PropertySource<?> source : configurable.getPropertySources()) {
                if ("configurationProperties".equals(source.getName())) {
                    continue;
                }
                if (source.containsProperty(name)) {
                    return source.getName();
                }
            }
        }
        return "no definida";
    }

    private String realDataSources() {
        List<String> out = new ArrayList<>();
        try {
            dataSources.orderedStream().forEach(ds -> out.add(describeDataSource(ds)));
        } catch (RuntimeException e) {
            return "no se pudo crear el DataSource: " + e.getMessage();
        }
        return out.isEmpty() ? "(no hay ningún DataSource)" : String.join(" | ", out);
    }

    /** URL, usuario y descripción de la contraseña de un DataSource (por reflexión, sin depender de Hikari). */
    static String describeDataSource(Object ds) {
        String url = firstNonNull(call(ds, "getJdbcUrl"), call(ds, "getUrl"));
        String user = firstNonNull(call(ds, "getUsername"), call(ds, "getUser"));
        return ds.getClass().getSimpleName() + " url=" + url + " username=" + user + " password=" + describe(call(ds, "getPassword"));
    }

    private static String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }

    private static String call(Object target, String method) {
        try {
            Object value = target.getClass().getMethod(method).invoke(target);
            return value == null ? null : value.toString();
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /**
     * Describe una contraseña sin mostrarla: su longitud y los defectos más comunes.
     *
     * @param password contraseña (puede ser nula)
     * @return texto con la longitud y los avisos, sin el valor de la contraseña
     */
    static String describe(String password) {
        if (password == null) {
            return "NO DEFINIDA";
        }
        if (password.isEmpty()) {
            return "VACÍA";
        }
        if (password.contains("${")) {
            // Spring Boot no falla con una variable sin definir en spring.datasource: manda el texto "${DB_PASSWORD}" como contraseña
            return "SIN RESOLVER (la variable no está definida y se enviaría el texto literal " + password + ")";
        }
        StringBuilder sb = new StringBuilder("******** (longitud ").append(password.length()).append(')');
        if (!password.equals(password.strip())) {
            sb.append(" ¡tiene espacios al inicio o al final!");
        }
        char first = password.charAt(0);
        char last = password.charAt(password.length() - 1);
        if (first == '"' || first == '\'' || last == '"' || last == '\'') {
            sb.append(" ¡tiene comillas en los extremos!");
        }
        if (last == ',' || last == ';') {
            sb.append(" ¡termina en coma o punto y coma!");
        }
        return sb.toString();
    }
}
