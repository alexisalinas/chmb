# Cambios en `CopsDbAutoConfiguration` (com.banamex.cops.app.autoconfigure)

La clase registra los beans con `@Import({...})`. Agrega **dos clases** a esa lista (junto a
`CopTimezoneLookupRepository.class` y `CopTimezoneLookupServiceImpl.class`):

```java
import com.banamex.cops.app.repository.CopOfferKafkaRepository;
import com.banamex.cops.app.service.impl.CopOfferKafkaServiceImpl;

@AutoConfiguration
@EntityScan(basePackages = "com.banamex.cops.app.entity")
@EnableJpaRepositories(basePackages = "com.banamex.cops.app.repository")
@Import({
    CopOfferQuery.class,
    ...
    CopTimezoneLookupRepository.class,
    CopOfferKafkaRepository.class,      // <-- NUEVO
    ...
    CopTimezoneLookupServiceImpl.class,
    CopOfferKafkaServiceImpl.class,     // <-- NUEVO
    ...
})
```

Notas:
- `CopOfferKafkaRepository` NO es un repositorio de Spring Data (no extiende `JpaRepository`), igual que
  `CopTimezoneLookupRepository`; por eso `@EnableJpaRepositories` no lo toca y hay que importarlo explícitamente.
- Después hay que **recompilar y volver a publicar/instalar la librería** (`mvn clean install`) y refrescar la
  dependencia en el microservicio (Maven > Update Project, con Force Update).
