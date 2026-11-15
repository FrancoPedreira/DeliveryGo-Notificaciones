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

## Seguridad (JWT y CORS)

Todas las APIs requieren el header `Authorization: Bearer <token>` con un JWT.

**Notificaciones no emite tokens ni tiene login**: los emite el módulo Usuarios cuando el usuario inicia sesión.
Este módulo solo los **valida** (firma y vencimiento) con la misma clave con la que Usuarios los firma, y lee
dos datos del token: `sub` (el usuario) y `role` (su rol).

| Ruta | Quién puede |
|---|---|
| `/swagger-ui.html`, `/v3/api-docs`, `/h2-console` | Público |
| `POST` / `PUT /api/v1/templates`, `PATCH /api/v1/channels/{tipo}` | Solo rol `ADMIN` |
| El resto de las APIs | Cualquier token válido |

Sin token, o con un token inválido o vencido, la respuesta es `401 UNAUTHORIZED`; con un token válido pero sin
el rol necesario, `403 FORBIDDEN`. Los dos usan el formato de error común.

Las clases están en el paquete `security/`: `JwtService` (valida el token), `JwtAuthFilter` (lo lee del
header en cada solicitud) y `SecurityConfig` (reglas por ruta y CORS).

**CORS:** el Portal Web puede llamar a esta API desde el navegador. Los orígenes permitidos se configuran con
`notificaciones.cors.origenes-permitidos` (por defecto `http://localhost:5173`).

**Tokens de desarrollo.** Mientras Usuarios no emita los tokens reales, se pueden usar estos, firmados con la
clave de desarrollo (vencen en 2030). En Swagger UI se cargan con el botón **Authorize**.

- Rol `ADMIN`: `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJBRE1JTiIsImV4cCI6MTg5MzQ1NjAwMH0.gOaJcaOHfCedaEnmkyVrUjg81bRnGE4bO6kw4D0gnaA`
- Rol `CLIENTE`: `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjbGllbnRlIiwicm9sZSI6IkNMSUVOVEUiLCJleHAiOjE4OTM0NTYwMDB9.ljb7dAe-oEly2-iVwIoOk8huNvOFsNOkm9PQOUUp6Qw`

> **Pendiente de acordar con el equipo de Usuarios:** la clave y el algoritmo de firma (hoy HS256 con clave
> compartida) y los nombres de los claims (`sub`, `role`). Si usan otros, se ajustan `JwtService` y
> `JwtAuthFilter`.

## Endpoints

Base: `/api/v1`. Todos devuelven JSON. Los errores usan el formato común DTO-03
(`timestamp`, `status`, `code`, `message`, `path`, `correlationId`).

| API | Método | Ruta | Caso de uso |
|---|---|---|---|
| API-01 | `POST` | `/api/v1/notifications` | CU-01 Crear/enviar una notificación |
| API-02 | `GET` | `/api/v1/notifications/{id}` | CU-04 Consultar el estado de una notificación |
| API-03 | `POST` | `/api/v1/notifications/{id}/retry` | CU-05 Reintentar una notificación fallida |
| API-04 | `GET` | `/api/v1/users/{id}/notifications` | CU-06 Historial de un usuario (filtros `estado`, `canal`, `leida`; paginado `page`, `size`, `sort`) |
| API-05 | `GET` | `/api/v1/templates` | CU-07 Listar plantillas |
| API-06 | `POST` | `/api/v1/templates` | CU-07 Crear plantilla |
| API-07 | `GET` | `/api/v1/templates/{id}` | CU-07 Consultar plantilla |
| API-08 | `PUT` | `/api/v1/templates/{id}` | CU-07 Modificar plantilla |
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

Para crear una notificación, este módulo consulta los datos de contacto del destinatario
(`GET /api/v1/users/{id}/contact`, respuesta DTO-04: `id`, `nombre`, `email`, `pushToken`, `activo`).

> **⚠️ La integración con Usuarios hoy es SIMULADA.** El equipo de Usuarios todavía no publicó su API, así
> que `UsuariosClientSimulado` devuelve usuarios fijos en memoria (10, 11 y 12). El endpoint y los campos de
> DTO-04 son una propuesta nuestra y se ajustarán cuando Usuarios publique su contrato. El módulo depende de
> la interfaz `UsuariosClient`, por lo que reemplazar la implementación no toca la lógica de negocio.

Existe además un cliente HTTP real (`UsuariosClientHttp`, basado en `RestClient`) que se activa con
`notificaciones.usuarios.modo=http`; por defecto el modo es `simulado`, así que nada cambia hasta que se
configure. Llama a `GET {base-url}/api/v1/users/{id}/contact`, propaga el `X-Correlation-Id` y el JWT recibido, y tiene un
timeout configurable (`notificaciones.usuarios.timeout-ms`, 2000 ms por defecto):

| Respuesta de Usuarios | Resultado |
|---|---|
| `200` | Contacto del usuario |
| `404` | Usuario inexistente (`422 RECIPIENT_NOT_FOUND` en nuestra API) |
| Timeout, error de conexión, `5xx` u otra respuesta inesperada | `503 USERS_SERVICE_UNAVAILABLE` |

Esta implementación solo se probó contra servidores de prueba; no contra el módulo Usuarios real, porque su
API todavía no está publicada.

Si Usuarios no responde, la API devuelve `503` con código `USERS_SERVICE_UNAVAILABLE` y no se registra nada.

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
2. Abrir Swagger (http://localhost:8089/swagger-ui.html), tocar **Authorize** y pegar el token `ADMIN` de la
   sección [Seguridad](#seguridad-jwt-y-cors).
3. **Caso principal:** crear una notificación (API-01), consultar su estado (API-02) y sus intentos (API-11).
4. **Historial y bandeja interna:** crear una IN_APP (plantilla 3), marcarla como leída (API-12) y ver el
   historial del usuario 10 (API-04).
5. **Manejo de un error:** pedir la plantilla 2 (PUSH) para el usuario 11 → `422 RECIPIENT_CONTACT_MISSING`;
   reintentar una notificación ENVIADA → `409`.
6. **Seguridad:** repetir una llamada sin token → `401`; deshabilitar un canal con el token `CLIENTE` → `403`.
7. **Reintento (CU-05):** reiniciar con `--notificaciones.emisor.tasa-fallo=1`, crear una notificación EMAIL
   (queda `FALLIDA`) y reintentarla hasta que pase a `FALLIDA_DEFINITIVA` al tercer intento.
8. **Caída de Usuarios:** reiniciar con `--notificaciones.usuarios.simular-caida=true` y crear una notificación
   → `503 USERS_SERVICE_UNAVAILABLE`.

Los mismos pasos están como requests listas para ejecutar en `doc/notificaciones.http`.

## Documentación

| Archivo | Contenido |
|---|---|
| `doc/TPO_Notificaciones_Grupo6_v1.3.docx` | Análisis y diseño: casos de uso, reglas de negocio, modelo, diagramas y decisiones técnicas. |
| `doc/openapi.yaml` | Contrato OpenAPI, generado desde la aplicación (`/v3/api-docs.yaml`). |
| `doc/notificaciones.http` | Colección de requests para la demo. |


```bash
./mvnw test        # en Windows: mvnw.cmd test
```

Incluye tests de services y de controllers (MockMvc) para los 10 casos de uso, y además del
`X-Correlation-Id`, del cliente HTTP de Usuarios (200, 404, 500 y timeout) y de la seguridad (401, 403 y CORS).
Los tests de controllers envían automáticamente un token con rol `ADMIN` (`TokenDePrueba`).

## Limitaciones conocidas y pendientes

- **El historial no está restringido por usuario**: cualquier token válido puede consultar el historial de
  cualquier usuario. Restringirlo al propio usuario depende de qué dato trae el claim `sub` (id o username),
  que hay que acordar con el equipo de Usuarios.
- **Usuarios es simulado por defecto** (ver [INT-01](#int-01--notificaciones-consulta-a-usuarios)). El cliente HTTP real existe pero está sin probar contra el módulo real.
- **El envío por EMAIL y PUSH es simulado**: no se manda ningún mail ni push real; solo se registra el intento
  y falla ~30 % de las veces a propósito.
- **H2 en memoria**: los datos (incluidas las notificaciones) se pierden al reiniciar la aplicación.
- **JWT sin integrar con Usuarios**: la validación está implementada, pero la clave y los claims todavía
  no se acordaron con el equipo de Usuarios; por ahora se prueba con tokens de desarrollo (ver
  [Seguridad](#seguridad-jwt-y-cors)).
- **Pendiente de decisión del equipo**: renombrar paquetes y clases (`services` → `service`,
  `NotificacionServices` → ...).
- **Pendiente de acordar con los otros equipos**: las fechas son `LocalDateTime` sin zona horaria; pasar a un
  formato con zona cambia el contrato JSON.
