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
- Spring Boot 4.1.1 (Spring MVC, Spring Data JPA), base H2 en memoria, springdoc-openapi (Swagger UI).

## Instalación y ejecución

```bash
git clone <url-del-repo>
cd Notificacion
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
| `notificaciones.usuarios.modo` | `simulado` | Implementación del cliente de Usuarios (INT-01): `simulado` (usuarios fijos en memoria) o `http` (llama al módulo Usuarios real). |
| `notificaciones.usuarios.base-url` | `http://localhost:8080` | URL base del módulo Usuarios. Solo se usa con `modo=http`; el valor por defecto es un placeholder. |
| `notificaciones.usuarios.timeout-ms` | `2000` | Timeout en milisegundos (conexión y lectura) de las llamadas a Usuarios. Solo con `modo=http`. |

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
configure. Llama a `GET {base-url}/api/v1/users/{id}/contact`, propaga el `X-Correlation-Id` y tiene un
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
  -d '{
        "destinatarioId": 10,
        "plantillaId": 1,
        "parametros": { "nombre": "Ana", "pedidoId": "55" }
      }'
```

`canal` es opcional: si se omite se usa el de la plantilla; si se indica debe coincidir con el de la plantilla.
El resultado es una notificación en estado `ENVIADA` o `FALLIDA` (por la tasa de fallo simulada).

## Tests

```bash
./mvnw test        # en Windows: mvnw.cmd test
```

Incluye tests de services y de controllers (MockMvc) para los 10 casos de uso.

## Limitaciones conocidas y pendientes

- **Usuarios es simulado por defecto** (ver [INT-01](#int-01--notificaciones-consulta-a-usuarios)). El cliente HTTP real existe pero está sin probar contra el módulo real.
- **El envío por EMAIL y PUSH es simulado**: no se manda ningún mail ni push real; solo se registra el intento
  y falla ~30 % de las veces a propósito.
- **H2 en memoria**: los datos (incluidas las notificaciones) se pierden al reiniciar la aplicación.
- **Sin autenticación JWT todavía**: no hay Spring Security ni Bearer token. Depende de cómo emita el token
  el módulo Usuarios y está pendiente de consultar con el docente.
- **Pendiente de decisión del equipo**: renombrar paquetes y clases (`services` → `service`,
  `NotificacionServices` → ...).
- **Pendiente de acordar con los otros equipos**: las fechas son `LocalDateTime` sin zona horaria; pasar a un
  formato con zona cambia el contrato JSON.
