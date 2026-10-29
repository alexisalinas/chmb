# UpdateOffer: de TIBCO BW a Spring Boot

## 1. Flujo completo (BW → Java)

| Paso en BW (`OpUpdateOffer`) | Clase Java | Método |
|---|---|---|
| Start (recibe SOAP/REST) | `UpdateOfferController` | `updateOffer(...)` |
| Validación de headers (el ESB los exigía) | `HeaderValidation` (tuya) | `preHandle(...)` |
| GetContext (arma el `RqHeader`) | `RqHeaderFactory` | `create(headers)` |
| Log service request | `AuditLogService` + `UpdateOfferMapper` | `logRequest(...)` + `toLogRequestXml(...)` |
| RequestTimeZoneValidation | `TimeZoneValidationService` | `validate(...)` |
| PerformSuppressionCheck | `PerformSuppressionCheckService` | `perform(...)` |
| JMS Queue Requestor (Input) | `OfferEventMapper` + `UpdateOfferRequestXmlMapper` | `map(...)` + `toEmsXml(...)` |
| JMS Queue Requestor (envío / espera de respuesta) | `EmsSender` | `send(xml)` |
| Parse JSON / RsUpdateOffer | `UpdateOfferResponseXmlMapper` | `fromReply(...)` |
| Log service response | `AuditLogService` + `UpdateOfferMapper` | `logResponse(...)` + `toLogResponseXml(...)` |
| End / respuesta | `UpdateOfferServiceImpl` | `procesarOferta(...)` |

## 2. Decisiones que tomé para que se comporte como BW

1. **XML al EMS = Envelope SOAP** (formato de tu ejemplo `UpdateConfigRq`): `Header/RqHeader` con prefijo `head:` y en el `Body` el evento `UpdateOffer` cuyos hijos son los `OtherProperties` del JMS Queue Requestor. `_nm_` y `_ns_` no son campos: son el nombre y el namespace del evento (`update-offer.ems.body-element` / `body-namespace`).
2. **XML del log = formato `<root>` de tu archivo `bb70f4`**: `RqHeader` (`ns0:`) + `UpdateOfferRq` con campos `ns1:` y los nombres antiguos de BW. Se genera a partir del request REST nuevo.
3. **Campos obligatorios de BW (ABC sin `?`) siempre se escriben**, vacíos si no hay dato (`AccountNO`, `CardNO`, `ApptStartTime`, `ApptEndTime`, `ReasonCode`, `Comments`, `UserField1..20`, `UserField29`, etc.). Los opcionales (`ABC?`) solo si traen valor.
4. **Lógica oculta de las capturas**: `ApptDate`, `ApptStartTime`, `UpdateTime` y `CustomerNO` se calculan igual que los XPath de BW (detalle en la sección 3).
5. **`ApptEndTime`**: no se ve en las capturas; lo traté igual que `ApptStartTime`.
6. **UserFields**: ya no vienen sueltos; se leen del JSON de `userText`.
7. **ControlField / LocationIndicator**: `contextType[0]` y `contextType[1]` (antes `ControlField[1]` y `[2]`).
8. **Zona horaria solo si `translate-time-zone=true`** y el `ResponseCode` termina en `'0'` (misma condición `and` que usa BW en cada campo).
9. **Respuesta del EMS**: acepta XML o JSON (en BW había un "Parse JSON"). Los nombres de campo son supuestos y se prueban los nuevos y los antiguos.
10. **Log de respuesta** también cuando la respuesta fue de supresión, de zona horaria o de error (en BW el Log service response se ejecuta en todas las ramas).
11. **`TransactionContext`**: ya no genera nada; usa el uuid del header y el esbUuid de `RqHeaderFactory` (`yyyyMMddHHmmss`; tu versión tenía `yyyyMMddmmss`, sin la hora).
12. **Rama del Null**: siempre va a la rama "XSD Format" del JMS Queue Requestor; `keyProcess` no decide ramas.
13. **Cola**: `citi.gsg.la.cop_163621.MX_card.offer.update`.

## 3. Clase por clase

### `controller/UpdateOfferController`
| Método | Qué hace | Relación con TIBCO |
|---|---|---|
| `updateOffer(headers…, requests)` | Recibe el POST, junta los headers HTTP en `UpdateOfferHeaders` y llama al service. Devuelve 200 con la lista de respuestas. | Evento **Start** del proceso y su operación `OpUpdateOffer`. |
| `@HeaderValidation.Required(...)` | Exige `uuid`, `ChannelId`, `sid`, `countryCode`, `businessCode`, `host`, `Accept`, `Content-Type`. | El ESB/servicio SOAP rechazaba mensajes sin `RqHeader` válido. |

### `dto/UpdateOfferHeaders`
Contenedor de los headers HTTP (`uuid`, `host`, `channelId`, `countryCode`, `businessCode`, `sid`, `acceptLanguage`). En BW esa información venía dentro del `RqHeader` del mensaje.

### `service/RqHeaderFactory`
| Método | Qué hace | TIBCO |
|---|---|---|
| `create(headers)` | Arma el `RqHeaderDTO`: `UUID` del header (NO se genera; si falta lanza error), `ESBUUID = UUID + yyyyMMddHHmmss` (lo único que se genera), fecha actual, versión y `ClientDetails` (canal y país del header; el resto de `application.yml`). | Actividad **GetContext** / nodo `RqHeader` que BW recibía y reutilizaba en todo el proceso. |
| `language(...)` | Convierte `Accept-Language` (`es-MX,es;q=0.9`) en `ES` para `SessionLanguageCode`. | `ClientDetails/SessionLanguageCode`. |

### `service/impl/UpdateOfferServiceImpl`
| Método | Qué hace | TIBCO |
|---|---|---|
| `updateOffer(requests, headers)` | Procesa cada request de la lista. | Una ejecución del proceso por mensaje. |
| `procesarOferta(...)` | Crea el `RqHeader` y el `TransactionContext`, escribe el Log service request, ejecuta el flujo y escribe el Log service response aunque falle. | Secuencia `GetContext → Log service request → … → Log service response → End`. |
| `ejecutar(...)` | Zona horaria → supresión → arma XML → EMS → convierte respuesta. | Transiciones del diagrama (`lower-case(TranslateTimeZone)="true"`, `Suppressed channel`, `Otherwise`). |
| `dates(...)`, `times(...)` | Arman las listas `Start/root/Date` y `Time` para la validación de zona horaria. | `Start/root/Date[]`, `Time[]` de `TimeZoneValidation`. |
| `checkSuppression(...)` | Llama a `PerformSuppressionCheckService`; si hay un detalle con `indicator=true` responde con su código y texto sin llamar al EMS. | Subproceso **PerformSuppressionCheck** y la rama `Suppressed channel`. |

### `mapper/UpdateOfferMapper` (fachada)
Solo delega: `toEmsXml`, `toLogRequestXml`, `toRestResponse`, `toLogResponseXml`. Reemplaza al antiguo `updateofferMapper.toSoapRequest/toRestResponse`.

### `mapper/OfferEventMapper` (el corazón: pestaña **Input** del JMS Queue Requestor)
| Parte | Qué hace | XPath de BW |
|---|---|---|
| `map(...)` | Devuelve los campos del evento en el mismo orden que el mapper de BW. | `ActivityInput/OtherProperties` |
| `AccountNO`, `CardNO` | Valor o `""` | `if(ns4:AccountNo != "") then … else ""` |
| `ApptDate` | Solo si hay fecha. Con zona horaria: fecha convertida `Date[DateName="AppointmentDate"]`; si no, la original. | `if(ResponseCode termina en '0' and TranslateTimeZone="true") then …/Date[…]/DateVal else ns4:AppointmentDate` |
| `ApptStartTime` / `ApptEndTime` | Con zona horaria: hora convertida; si no, quita los segundos cuando viene `HH:mm:ss`. | `Choose`: `…/Time[1]/TimeVal` u `otherwise` con `count(tib:tokenize(x,':'))=3 → tib:substring-before-last(x,':')` |
| `customerNo(...)` | Con padding rellena a la izquierda; sin padding deja el valor; vacío da `""`. | `Choose` con `tib:pad-front(CustomerNo, COP/Padding/CustomerNo, COP/Padding/PadCharacter)` |
| `UpdateTime` | Con zona horaria: ahora en la zona ECRM; si no, la fecha del `RqHeader`. | `tib:translate-timezone(current-dateTime(), ECRMTimeZone)` o `RqHeader/DateAndTimeStamp` |
| `UpdatedBy` | `UserID` del `ClientDetails`. | `$GetContext/…/ClientDetails/UserID` |
| `ControlField`, `LocationIndicator` | `contextType[0]` y `[1]`. | `ns4:ControlField[1]`, `[2]` |
| `UserField1..100` | Salen de `userText`; 1–20 y 29 siempre. | `ns4:UserFieldN` |
| `tzTime`, `tzDate`, `withoutSeconds`, `tzOk` | Auxiliares de los XPath anteriores. | `substring(ResponseCode, string-length(ResponseCode), 1) = '0'` |

### `mapper/UserTextParser`
`parse(userText)` convierte el JSON de `userText` en `UserField1..100`. En BW esos campos eran elementos sueltos del request; en el contrato nuevo viajan dentro de `userText`.

### `mapper/RqHeaderXmlMapper`
| Método | Qué hace | TIBCO |
|---|---|---|
| `toEnvelopeHeader(h, indent)` | `RqHeader` con prefijo `head:` para el Envelope SOAP; omite lo que viene `null`. | Serialización del `RqHeader` en el mensaje al EMS. |
| `toLogHeader(h, indent)` | `RqHeader` con prefijo `ns0:`, fecha con offset y opcionales vacíos como `<ns0:X/>`. | Lo que escribe `tib:render-xml` en el Log. |

### `mapper/UpdateOfferRequestXmlMapper`
| Método | Qué hace | TIBCO |
|---|---|---|
| `toEmsXml(...)` | Envelope SOAP: `Header/RqHeader` + `Body/<evento UpdateOffer>` con los campos de `OfferEventMapper`. | Mensaje que construye el **JMS Queue Requestor**. |
| `toLogXml(rq, header)` | `<root>` con `RqHeader` y `UpdateOfferRq` (campos `ns1:` con los nombres antiguos de BW, sin transformaciones). | `Payload = concat("&lf;", tib:render-xml($Start/RqLog/RqBody/*, …))` del **Map Data** de `LogRequest`. |

### `mapper/UpdateOfferResponseXmlMapper`
| Método | Qué hace | TIBCO |
|---|---|---|
| `fromReply(reply)` | Detecta si la respuesta es JSON o XML y la convierte a `UpdateOfferResponse`. | Actividad **Parse JSON** + mapper **RsUpdateOffer**. |
| `toLogXml(rs)` | XML de la respuesta REST para el log (nombres antiguos `ReferenceContacto`, `ModeCmp`, `ControlField`). | **LogResponse**. |

### `mapper/XmlUtil`
`tag`, `tagOrEmpty`, `nsTag`, `esc`: escriben XML a mano y escapan caracteres. Reemplazan a JAXB (ya no existen los DTOs SOAP). Equivale a `tib:render-xml`.

### `sender/EmsSender`
| Método | Qué hace | TIBCO |
|---|---|---|
| `send(xml)` | Crea una cola temporal de respuesta, manda el `TextMessage` (no persistente, con `JMSReplyTo`), espera la respuesta (`timeout-seconds`) y la devuelve como `String`. Enmascara la tarjeta en el log de debug. | **JMS Queue Requestor**: `destinationQueue`, `replyToQueue`, `JMSExpiration` (`time-to-live-ms`) y `requestTimeout` (`timeout-seconds`). |

### `audit/TransactionContext`
| Método | Qué hace | TIBCO |
|---|---|---|
| `from(RqHeaderDTO)` | Reutiliza uuid / esbUuid / fecha del header. | Datos de `GetContext`. |
| `transactionId()` | `" ESBUUID: … UUID: … "` | `TransactionID = concat(" ESBUUID: ", ESBUUID, " UUID: ", UUID, " ")` en `LogRequest`. |

### `audit/AuditLogService`
| Método | Qué hace | TIBCO |
|---|---|---|
| `logRequest` / `logResponse` | Si `audit.enabled`, arma el header y escribe título + header XML + payload XML. Enmascara `CardNumber` y `CardNO` (últimos 4 dígitos). | `/BW/Logging/LogRequest` y `LogResponse`. |
| `enabled` | Corta el log. | Transición `Check Audit Enabled` vs `Bypass Audit Log`. |
| `buildHeader` | Llena `ApplicationID`, `ComponentName`, `Timestamp`, `TransactionID`, `CountryCode`, `ServiceName`, `ServiceVersion`. | Mapper **Map Data** de `LogRequest`. |
| `headerXml`, `write` | Serializa y escribe. Nunca rompe el flujo. | Actividad **Write To Log**. |

### `config/OfferJmsProperties`
`customerPadding`, `customerPaddingLength`, `customerPaddingChar` ← `GlobalVariables/COP/Padding/CustomerPadding`, `CustomerNo`, `PadCharacter`.

### `service/timeZoneValidation/*`
| Clase | Qué hace | TIBCO |
|---|---|---|
| `TimeZoneValidationService.validate` | Caché → `InitTZ` → consulta BD → `TimeZoneMapRequest`; devuelve `ResponseCode`, `ResponseText`, formatos y grupos Date/Time. | Proceso `/BW/Common/TimeZoneValidation`. |
| `InitTZ.execute` | Busca sistema + modo + tipo en la caché. | Assign **InitTZ**. |
| `CopTimezoneLookupClient.find` | Consulta `COP_TIMEZONE_LOOKUP` con tu librería `cops-bd`. | Actividad JDBC **LookupTZ**. |
| `TimeZoneMapRequest.map` | Valida y convierte fechas/horas. | Mapper **MapRequest**. |

### Flujo Kafka (`sender/FulFillkafkaSender` y paquete `kafka/`)
Se dispara al final de `OpUpdateOffer`, después de la respuesta del EMS, y **nunca afecta la respuesta**.

| Clase | Qué hace | TIBCO |
|---|---|---|
| `KafkaSenderDashboard.build` | Paso 0: arma el evento de entrada (uuid, sid = ESBUUID, canal, RefID, datos del request y `ResponseCode`/`ResponseText` del EMS). | Actividad **KafkaSenderDashboard** en `OpUpdateOffer`. |
| `FulFillkafkaSender.sendAsync` | Encola el trabajo en un ejecutor con cola acotada; si está llena lo descarta y lo cuenta. | Llamada en segundo plano al subproceso. |
| `FulFillkafkaSender.process` | Pasos 1–5: espera `delay-ms`, consulta la BD, ensambla, aplica reglas y publica; reintenta solo fallas de infraestructura. | `/BW/Kafka/FulFillKafkaSender`. |
| `FulFillkafkaSender.assemble` | Arma `{event:{header,body}}`; `CustomerNo` a 9 dígitos después de la consulta, `AppointmentMinDur` solo si > 0, `timeStamp` siempre actual, UserFields de la BD. | Mapper + `Render JSON`. |
| `OfferKeyBuilder` | Claves `off:mx:…` (RefID y OFFPAL.EXTID$) y `wvch:…` (WAVCH.EXTID$). | Concatenaciones de `DataFetch`. |
| `OfferDataRepository.find` | Un SELECT con parámetros preparados (timeout 10 s, 100 filas, usa la primera). | `/BW/Kafka/DataFetch` (JDBC Query). |
| `CitiDataStore` / `CitiDataStoreConfig` | Conexión dedicada opcional (`offer.kafka.datasource.*`); si no, usa `spring.datasource`. | Conexión `CitiDataStore`. |
| `OfferPublisher` → `DryRunOfferPublisher`, `JmsOfferPublisher` (y `KafkaOfferPublisher` opcional) | Paso 5: publica `{RefId, jsonString}`. `dry-run` solo escribe en el log. | **JMS Queue Sender `KafkaMsgSender`** (cola que lee el bridge). |
| `KafkaMetrics` | Contadores: enviados, sin filas, inválidos, fallidos, rechazados. | — |
| `OfferKafkaProps` | Propiedades `offer.kafka.*`. | Globales `COP/Kafka/...`. |

Códigos del log: `KAFKA-SENT`, `KAFKA-NO-ROWS`, `KAFKA-INVALID`, `KAFKA-RETRY`, `KAFKA-ERROR`, `KAFKA-REJECTED`, `KAFKA-MULTI-ROWS`.

## 4. Equivalencia de nombres

| BW (`ns4` / log) | Contrato REST | Campo del evento (OtherProperties) |
|---|---|---|
| RequestingChannel | channelCode | RequestingChannel |
| CampaignID | campaignId | CampaignID |
| WaveID | waveId | WaveID |
| CustomerNo | customerId | CustomerNO |
| AccountNo | accountNumber | AccountNO |
| CardNumber | cardNumber | CardNO |
| AssignedTo | assignedTo | AgentID |
| IsConfirmed | offeredNumber | ApptConfirmation |
| ContactNumber | contactIndicatorNumber | ContactNumber / ContactInfo |
| ContactReference | customerReferenceNumber | ContactReference |
| Sysversion | schemaVersion | SysVersion |
| OfferAction | offerActionCode | OfferAction |
| GiftConfirmationNo | giftConfirmationNumber | GiftConfirmationNO |
| FulfillmentRefNo | fulfillmentTrackerFlag | FulfillmentReferencedNO |
| Amount | amount | ProbabilityAmount |
| DepartmentID / BranchID | departmentId / branchId | Department / Branch |
| OfferPriority | offerPriority | Priority |
| Workgroup | workgroupId | Workgroup |
| LVCode | levelCode | LVCode |
| DivisionID | divisionCode | DivisionID |
| RegionID | branchRegionIdentifier | RegionID |
| AreaID | areaIdentifier | AreaID |
| Agentname | agentName | AgentName |
| ChannelResponse | responseChannel | ChannelResponse |
| FulfillmentScript | fulfillmentCode | FulfillmentScript |
| UserField1..N | userText (JSON) | UserField1..N |
| ControlField[n] | contextType[n] | ControlField / LocationIndicator |
| processVersion | keyProcess | (sin uso todavía) |
| ReferenceContacto *(respuesta)* | customerReferenceNumber | — |
| ModeCmp *(respuesta)* | surveyModeCode | — |

## 5. Pendientes / supuestos
- Cuerpo exacto del mensaje al EMS (nombre y namespace del evento) y nombres de campo de la respuesta.
- `ApptEndTime` y las conversiones de Time / DateTime en el request de zona horaria.
- Código de error de fecha/hora inválida (`COP_TZCOD_101` es inventado).
- `ServiceTimeout` del `RqHeader` (aparece en el log de BW) no existe en `RqHeaderDTO`.
