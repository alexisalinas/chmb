package com.banamex.cops.update_offer.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Si una clase de la librería quedó registrada más de una vez, deja solo el registro de la librería. */
@Slf4j
@Component
public class LibraryBeanDeduplicator implements BeanFactoryPostProcessor, Ordered {

    private final String libraryPackage;

    /** Revisa las clases de {@code com.banamex.cops.app}. */
    public LibraryBeanDeduplicator() {
        this("com.banamex.cops.app.");
    }

    LibraryBeanDeduplicator(String libraryPackage) {
        this.libraryPackage = libraryPackage;
    }

    /** Se ejecuta al final, cuando ya están todos los registros. */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    /** Quita los registros duplicados y conserva el de la librería (el que se llama como la clase). */
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        if (!(beanFactory instanceof BeanDefinitionRegistry registry)) {
            return;
        }
        Map<String, Class<?>> library = new LinkedHashMap<>();
        for (String name : beanFactory.getBeanDefinitionNames()) {
            Class<?> type = typeOf(beanFactory, name);
            if (type != null && type.getName().startsWith(libraryPackage)) {
                library.put(name, type);
            }
        }
        for (Map.Entry<String, Class<?>> keeper : library.entrySet()) {
            String keeperName = keeper.getKey();
            Class<?> keeperType = keeper.getValue();
            if (!keeperName.equals(keeperType.getName()) || !registry.containsBeanDefinition(keeperName)) {
                continue;
            }
            List<String> extra = new ArrayList<>();
            library.forEach((name, type) -> {
                if (!name.equals(keeperName) && registry.containsBeanDefinition(name) && type.isAssignableFrom(keeperType)) {
                    extra.add(name);
                }
            });
            for (String name : extra) {
                log.warn("[BEANS] {} estaba registrado dos veces: se quita '{}' ({}) y se conserva el de la librería",
                        keeperType.getSimpleName(), name, origin(registry.getBeanDefinition(name)));
                registry.removeBeanDefinition(name);
            }
        }
    }

    private static Class<?> typeOf(ConfigurableListableBeanFactory beanFactory, String name) {
        try {
            return beanFactory.getType(name, false);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String origin(BeanDefinition definition) {
        if (definition.getFactoryMethodName() != null) {
            String factory = definition.getFactoryBeanName() == null ? "?" : definition.getFactoryBeanName();
            return "@Bean " + definition.getFactoryMethodName() + "() en " + factory.substring(factory.lastIndexOf('.') + 1);
        }
        return definition.getResourceDescription() == null ? "@Import" : "escaneo de componentes";
    }
}
