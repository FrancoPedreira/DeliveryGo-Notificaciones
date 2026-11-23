# Módulo Notificaciones — TPO DeliveryGo

Módulo de **Notificaciones** del ecosistema DeliveryGo. Expone un **contrato común de notificaciones** para
todos los demás módulos: cualquier módulo puede pedir que se le envíe una notificación a un usuario (por
email, push o bandeja interna), consultar su estado, reintentarla si falló y consultar el historial de un
usuario. El módulo también administra las plantillas de mensajes y los canales de envío.

## Integrantes y responsabilidades

| Integrante | Responsabilidades |
|---|---|
| Pedreira Franco | TODO: completar |
| Peier Ramiro German | TODO: completar |
| Siri De Biase Alejo | TODO: completar |
| Suárez Mateo Emanuel | TODO: completar |

## Tecnologías y requisitos

- **Java 21** (LTS). El `pom.xml` compila con `release 21`.
- **Maven**: no hace falta instalarlo, el repo incluye el Maven wrapper (`mvnw` / `mvnw.cmd`).
- Spring Boot 4.1.1 (Spring MVC, Spring Data JPA, Spring Security), base H2 en memoria, springdoc-openapi
  (Swagger UI) y jjwt para validar los JWT.

## Instalación y ejecución

```bash
git clone https://github.com/FrancoPedreira/DeliveryGo-Notificaciones.git
cd DeliveryGo-Notificaciones
./mvnw spring-boot:run        # en Windows: mvnw.cmd spring-boot:run
```

La aplicación queda escuchando en **http://localhost:8089**. Al arrancar, el `DataLoader` carga los canales y
las plantillas de prueba (ver [Datos de prueba](#datos-de-prueba)).

| Recurso | URL |
|---|---|
| Swagger UI | http://localhost:8089/swagger-ui.html |
| Contrato OpenAPI (JSON) | http://localhost:8089/v3/api-docs |
| Consola H2 | http://localhost:8089/h2-console (JDBC URL `jdbc:h2:mem:notificaciones`, usuario `sa`, sin contraseña) |

## Configuración

Todas las propiedades están en `src/main/resources/application.properties` y se pueden pisar por variable de
entorno o argumento (`--notificaciones.max-intentos=5`).

| Propiedad | Valor por defecto | Descripción |
|---|---|---|
| `server.port` | `8089` | Puerto HTTP. |
| `notificaciones.max-intentos` | `3` | Cantidad máxima de intentos de envío por notificación (RN-04). Al alcanzarla, no se puede reintentar. |
| `notificaciones.emisor.tasa-fallo` | `0.3` | Probabilidad (0 a 1) de que el envío simulado por EMAIL o PUSH falle. IN_APP nunca falla. |
| `notificaciones.usuarios.simular-caida` | `false` | Si es `true`, el cliente simulado de Usuarios lanza `USERS_SERVICE_UNAVAILABLE` (503). Sirve para probar la caída de Usuarios. |
| `notificaciones.jwt.secret` | clave de desarrollo | Clave con la que se valida la firma de los JWT (HS256, mínimo 32 caracteres). Debe ser la misma que usa Usuarios. Se define con la variable de entorno `JWT_SECRET`; el valor por defecto es solo para desarrollo. |
| `notificaciones.cors.origenes-permitidos` | `http://localhost:5173` | Orígenes (separados por coma) desde los que el Portal Web puede llamar a la API. |
| `notificaciones.usuarios.modo` | `simulado` | Implementación del cliente de Usuarios (INT-01): `simulado` (usuarios fijos en memoria) o `http` (llama al módulo Usuarios real). |
| `notificaciones.usuarios.base-url` | `http://localhost:8080` | URL base del módulo Usuarios. Solo se usa con `modo=http`; el valor por defecto es un placeholder. |
| `notificaciones.usuarios.timeout-ms` | `2000` | Timeout en milisegundos (conexión y lectura) de las llamadas a Usuarios. Solo con `modo=http`. |
| `notificaciones.usuarios.api-key` | variable `USUARIOS_API_KEY` (vacía) | Opcional. Si se define, se envía en `X-Api-Key`; hoy Usuarios no pide credencial entre módulos. |

## Seguridad (JWT y CORS)

Las APIs requieren el header `Authorization: Bearer <token>` con un JWT, salvo las dos que los otros módulos
llaman sin credencial (ver la tabla de abajo). Por acuerdo del ecosistema, entre módulos no se usa JWT ni clave:
Usuarios llama a `GET /api/v1/templates` y `POST /api/v1/notifications` solo con `X-Correlation-Id`.

**Notificaciones no emite tokens ni tiene login**: los emite el módulo Usuarios cuando el usuario inicia sesión.
Según el contrato, `sub` contiene el ID numérico del usuario (Long, representado como texto) y `role` contiene
uno de estos valores sin prefijo: `CLIENTE`, `COMERCIO`, `REPARTIDOR` o `ADMIN`. Notificaciones valida
esos datos y agrega internamente el prefijo `ROLE_` para Spring Security.

**Autorización sobre el recurso:** estar autenticado no alcanza. Cada persona solo puede ver y modificar sus
propias notificaciones: el `sub` del token tiene que ser el destinatario. `ADMIN` puede ver todas. Lo resuelve
`ControlDeAcceso`, que usan los controllers antes de llamar al service.

La firma actual es HS256. La variable `JWT_SECRET` debe tener el mismo valor que la clave usada por Usuarios;
el valor por defecto de desarrollo sirve solo para pruebas locales.

| Ruta | Quién puede |
|---|---|
| `/status`, `/swagger-ui.html`, `/v3/api-docs`, `/h2-console` | Público |
| `GET /api/v1/templates` (listar), `POST /api/v1/notifications` (crear) | Público: lo llaman otros módulos sin credencial |
| `POST` / `PUT /api/v1/templates`, `PATCH /api/v1/channels/{tipo}` | Solo rol `ADMIN` |
| `GET /api/v1/users/{id}/notifications` (historial) | El usuario `{id}` (su propio `sub`) o `ADMIN` |
| `GET /api/v1/notifications/{id}`, `/attempts`, `POST /retry`, `PATCH /read` | El destinatario de la notificación o `ADMIN` |
| El resto (`GET /api/v1/templates/{id}`, `GET /api/v1/channels`) | Cualquier token válido |

Sin token, o con un token inválido o vencido, la respuesta es `401 UNAUTHORIZED`; con un token válido pero sin
el rol necesario o sobre una notificación de otra persona, `403 FORBIDDEN`. Los dos usan el formato de error
común. Una notificación que no existe sigue dando `404`.

Las clases están en el paquete `security/`: `JwtService` (valida el token), `JwtAuthFilter` (lo lee del
header en cada solicitud), `SecurityConfig` (reglas por ruta y CORS) y `ControlDeAcceso` (dueño del recurso).

**CORS:** el Portal Web puede llamar a esta API desde el navegador. Los orígenes permitidos se configuran con
`notificaciones.cors.origenes-permitidos` (por defecto `http://localhost:5173`).

Para probar la API, usá un JWT válido emitido por el módulo Usuarios. En Swagger, pegá el token en
**Authorize**. El token debe incluir el ID numérico del usuario en `sub` y uno de los roles acordados en
`role`: `CLIENTE`, `COMERCIO`, `REPARTIDOR` o `ADMIN`. No guardes claves ni tokens reales en el repositorio.

## Endpoints

Base: `/api/v1`. Todos devuelven JSON. Los errores usan el formato común DTO-03
(`timestamp`, `status`, `code`, `message`, `path`, `correlationId`).

| API | Método | Ruta | Caso de uso |
|---|---|---|---|
| API-01 | `POST` | `/api/v1/notifications` | CU-01 Crear/enviar una notificación |
| API-02 | `GET` | `/api/v1/notifications/{id}` | CU-04 Consultar el estado de una notificación |
| API-03 | `POST` | `/api/v1/notifications/{id}/retry` | CU-05 Reintentar una notificación fallida |
| API-04 | `GET` | `/api/v1/users/{id}/notifications` | CU-06 Historial de un usuario (filtros `estado`, `canal`, `leida`; paginado `page`, `size`, `sort`) |
| API-05 | `POST` | `/api/v1/templates` | CU-07 Crear plantilla |
| API-06 | `PUT` | `/api/v1/templates/{id}` | CU-07 Modificar plantilla |
| API-07 | `GET` | `/api/v1/templates/{id}` | CU-07 Consultar plantilla |
| API-08 | `GET` | `/api/v1/templates` | CU-07 Listar plantillas |
| API-09 | `GET` | `/api/v1/channels` | CU-08 Listar canales |
| API-10 | `PATCH` | `/api/v1/channels/{tipo}` | CU-08 Habilitar/deshabilitar un canal (`EMAIL`, `PUSH`, `IN_APP`) |
| API-11 | `GET` | `/api/v1/notifications/{id}/attempts` | CU-09 Consultar los intentos de envío |
| API-12 | `PATCH` | `/api/v1/notifications/{id}/read` | CU-10 Marcar como leída una notificación interna (IN_APP) |

**X-Correlation-Id:** todas las respuestas (exitosas y de error) devuelven el header `X-Correlation-Id`. Si el
llamador lo envía se reutiliza; si no, se genera un UUID. En los errores, el `correlationId` del body es el
mismo valor, y también aparece en los logs. Los módulos que nos llaman deberían enviarlo para poder seguir una
solicitud entre módulos.

El contrato completo (esquemas, ejemplos y códigos de error) está en Swagger y en `doc/openapi.yaml`.

## Integraciones

### INT-01 — Notificaciones consulta a Usuarios

Para crear una notificación, Notificaciones consulta el perfil del destinatario mediante
`GET /api/v1/users/{id}`. Por acuerdo del ecosistema la llamada no lleva credencial (la `X-Api-Key` solo se
envía si está configurada) y propaga el header `X-Correlation-Id`.

Usuarios responde `id`, `nombre`, `apellido`, `email`, `telefono`, `rol`, `estado` y `fechaAlta`.
Notificaciones utiliza `id`, `nombre` y `email`; convierte `estado == "ACTIVO"` en su indicador `activo`.
El contrato no incluye `pushToken`.

El cliente HTTP (`UsuariosClientHttp`) se activa con `notificaciones.usuarios.modo=http`. Por defecto,
el modo es `simulado`, con usuarios fijos para las pruebas. Para usar el módulo real hay que configurar
`NOTIFICACIONES_USUARIOS_MODO=http` y `USUARIOS_BASE_URL` con la URL y el puerto donde esté levantado Usuarios.
Al arrancar, el log indica qué modo se usa (`INT-01 REAL` o un `WARN INT-01 SIMULADA`) y `GET /status` lo informa
en `integracionUsuarios`.

| Respuesta de Usuarios | Resultado |
|---|---|
| `200` | Se mapea el perfil y se valida que el usuario esté activo. |
| `404` | Se considera que el destinatario no existe (`422 RECIPIENT_NOT_FOUND` en nuestra API). |
| Timeout, error de conexión, `5xx` u otra respuesta inesperada | `503 USERS_SERVICE_UNAVAILABLE`. |

El cliente HTTP tiene tests propios y además se probó contra el módulo Usuarios real el 23/11/2026, en las dos
direcciones (Usuarios pide la bienvenida y la recuperación; Notificaciones consulta el perfil). La evidencia, el
contrato acordado y los pasos para repetirlo están en [`doc/INTEGRACION.md`](doc/INTEGRACION.md).

### INT-02 — Los demás módulos llaman a Notificaciones

Los módulos del ecosistema (pedidos, pagos, envíos, etc.) consumen este módulo a través de las APIs de
arriba, principalmente `POST /api/v1/notifications`. Es el contrato común: cada módulo indica el
destinatario, la plantilla y los parámetros, y Notificaciones resuelve el canal, el contacto y el envío.

## Datos de prueba

El `DataLoader` carga al arrancar (los ids son los que asigna la base, en este orden):

**Canales:** `EMAIL`, `PUSH` e `IN_APP`, los tres habilitados.

**Plantillas**

| Id | Nombre | Canal | Variables |
|---|---|---|---|
| 1 | `PEDIDO_CONFIRMADO_EMAIL` | EMAIL | `{nombre}`, `{pedidoId}` |
| 2 | `PAGO_APROBADO_PUSH` | PUSH | `{monto}` |
| 3 | `ENVIO_EN_CAMINO_IN_APP` | IN_APP | `{codigo}` |
| 4 | `USUARIO_BIENVENIDA_EMAIL` | EMAIL | `{nombre}` |
| 5 | `RECUPERACION_PASSWORD_EMAIL` | EMAIL | `{nombre}`, `{token}` |

**Usuarios simulados** (`UsuariosClientSimulado`)

| Id | Nombre | Email | Push token | Activo |
|---|---|---|---|---|
| 10 | Ana | `ana@mail.com` | `push-token-ana` | sí |
| 11 | Bruno | `bruno@mail.com` | — (no tiene) | sí |
| 12 | Carla | `carla@mail.com` | `push-token-carla` | **no** |


Casos útiles: el usuario 11 con la plantilla 2 (PUSH) da `422 RECIPIENT_CONTACT_MISSING`; el usuario 12 (inactivo) y
cualquier otro id dan `422 RECIPIENT_NOT_FOUND`.

**Envío simulado:** los emisores de EMAIL y PUSH **fallan a propósito ~30 % de las veces**
(`notificaciones.emisor.tasa-fallo`) para poder probar el reintento. Una notificación fallida queda
`FALLIDA` y se puede reintentar con API-03. IN_APP nunca falla. Para una demo determinista, usar
`--notificaciones.emisor.tasa-fallo=0` (siempre envía) o `=1` (siempre falla).

## Ejemplo de request

```bash
curl -X POST http://localhost:8089/api/v1/notifications \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
        "destinatarioId": 10,
        "plantillaId": 1,
        "parametros": { "nombre": "Ana", "pedidoId": "55" }
      }'
```

`canal` es opcional: si se omite se usa el de la plantilla; si se indica debe coincidir con el de la plantilla.
El resultado es una notificación en estado `ENVIADA` o `FALLIDA` (por la tasa de fallo simulada).
El mensaje final (la plantilla con los parámetros ya reemplazados) no puede superar los 1000 caracteres;
si los supera, la respuesta es `400 VALIDATION_ERROR`.

## Colección de requests para la demo

`doc/notificaciones.http` (formato REST Client de VS Code / cliente HTTP de IntelliJ) cubre las 12 APIs con los
datos de prueba del DataLoader: caminos felices y los errores principales (400 falta `plantillaId`,
422 `TEMPLATE_PARAMS_MISSING`, 422 `RECIPIENT_CONTACT_MISSING` con el usuario 11 y PUSH, 401, 403, 404, 409 al reintentar
una notificación ENVIADA y 422 al marcar como leída una de EMAIL). Se ejecuta en orden sobre una base recién
iniciada; para que los resultados sean deterministas conviene arrancar con
`--notificaciones.emisor.tasa-fallo=0` (instrucciones en el encabezado del archivo).

## Guía para la demo

1. Arrancar sin fallos simulados, para que los resultados sean siempre los mismos:
   `./mvnw spring-boot:run "-Dspring-boot.run.arguments=--notificaciones.emisor.tasa-fallo=0"`
2. Abrir Swagger (http://localhost:8089/swagger-ui.html), tocar **Authorize** y pegar un JWT válido de
   Usuarios con rol `ADMIN`.
3. **Caso principal:** crear una notificación (API-01), consultar su estado (API-02) y sus intentos (API-11).
4. **Historial y bandeja interna:** crear una IN_APP (plantilla 3), marcarla como leída (API-12) y ver el
   historial del usuario 10 (API-04).
5. **Manejo de un error:** pedir la plantilla 2 (PUSH) para el usuario 11 → `422 RECIPIENT_CONTACT_MISSING`;
   reintentar una notificación ENVIADA → `409`.
6. **Seguridad:** repetir una llamada sin token → `401`; deshabilitar un canal con el token `CLIENTE` → `403`;
   pedir el historial de otro usuario con un token `CLIENTE` → `403`.
7. **Reintento (CU-05):** reiniciar con `--notificaciones.emisor.tasa-fallo=1`, crear una notificación EMAIL
   (queda `FALLIDA`) y reintentarla hasta que pase a `FALLIDA_DEFINITIVA` al tercer intento.
8. **Caída de Usuarios:** reiniciar con `--notificaciones.usuarios.simular-caida=true` y crear una notificación
   → `503 USERS_SERVICE_UNAVAILABLE`.

9. **Integración real con Usuarios:** levantar los dos módulos y ejecutar `doc/integracion-usuarios.http`
   (registro → bienvenida → historial con el JWT de Usuarios → 403 sobre otro usuario). Ver
   [`doc/INTEGRACION.md`](doc/INTEGRACION.md).

Los pasos 1 a 8 están como requests listas para ejecutar en `doc/notificaciones.http`.

## Documentación

| Archivo | Contenido |
|---|---|
| `doc/TPO_Notificaciones_Grupo6_v1.3.docx` | Análisis y diseño: casos de uso, reglas de negocio, modelo, diagramas y decisiones técnicas. |
| `doc/openapi.yaml` | Contrato OpenAPI, generado desde la aplicación (`/v3/api-docs.yaml`). |
| `doc/notificaciones.http` | Colección de requests para la demo. |
| `doc/INTEGRACION.md` | Qué es real y qué es simulado, contrato con Usuarios y evidencia de la prueba integrada. |
| `doc/integracion-usuarios.http` | Requests para la demo con Usuarios real. |


```bash
./mvnw test        # en Windows: mvnw.cmd test
```

Incluye tests de services y de controllers (MockMvc) para los 10 casos de uso, y además del
`X-Correlation-Id`, del cliente HTTP de Usuarios (200, 404, 500 y timeout) y de la seguridad (401, 403 por rol y
por dueño de la notificación, y CORS).
Los tests de controllers envían automáticamente un token con rol `ADMIN` (`TokenDePrueba`).

## Limitaciones conocidas y pendientes

- **Usuarios es simulado por defecto** (ver [INT-01](#int-01--notificaciones-consulta-a-usuarios)), para poder
  probar el módulo solo. La integración real se activa con `NOTIFICACIONES_USUARIOS_MODO=http`.
- **Usuarios no publica `pushToken`:** con la integración real, una notificación PUSH responde
  `422 RECIPIENT_CONTACT_MISSING`. EMAIL e IN_APP funcionan.
- **Portal Web:** no hay repositorio del Portal para probar; solo está configurado CORS.
- **El envío por EMAIL y PUSH es simulado**: no se manda ningún mail ni push real; solo se registra el intento
  y falla ~30 % de las veces a propósito.
- **H2 en memoria**: los datos (incluidas las notificaciones) se pierden al reiniciar la aplicación.
- **Configuración JWT:** el código ya sigue los claims acordados. Para validar tokens reales, `JWT_SECRET`
  debe coincidir con la clave de Usuarios.
- **Pendiente de decisión del equipo**: renombrar paquetes y clases (`services` → `service`,
  `NotificacionServices` → ...).
- **Pendiente de acordar con los otros equipos**: las fechas son `LocalDateTime` sin zona horaria; pasar a un
  formato con zona cambia el contrato JSON.
