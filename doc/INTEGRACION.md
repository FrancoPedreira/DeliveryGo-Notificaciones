# Integración de Notificaciones con el ecosistema

Este documento registra qué integraciones son reales y cuáles son simuladas, el contrato acordado con Usuarios
y la evidencia de la prueba con los dos módulos levantados.

**Prueba del 23/11/2026.** Versiones probadas:

- Notificaciones: rama `feature/correcciones-profe`.
- Usuarios: [`CarlaGorgal2002/deliverygo-usuarios-auth`](https://github.com/CarlaGorgal2002/deliverygo-usuarios-auth),
  commit `d6041ca`.

## Qué es real y qué es simulado

| Pieza | Estado | Cómo se identifica |
|---|---|---|
| Usuarios → Notificaciones (bienvenida y recuperación de contraseña) | **Real**, probado | Usuarios llama a `GET /api/v1/templates` y `POST /api/v1/notifications` |
| Notificaciones → Usuarios (INT-01, perfil del destinatario) | **Real** con `notificaciones.usuarios.modo=http`; **simulado** por defecto | Al arrancar se loguea `INT-01 REAL: ...` o un `WARN INT-01 SIMULADA: ...`. `GET /status` informa `integracionUsuarios: http` o `simulado` |
| JWT | **Real**: lo emite Usuarios en el login y Notificaciones lo valida con la misma clave | Un token de `POST /api/v1/auth/login` de Usuarios funciona en esta API |
| Envío EMAIL y PUSH | **Simulado**: no sale ningún mail ni push | `EmisorEmailSimulado` y `EmisorPushSimulado` loguean `EMAIL simulado enviado...` |
| Envío IN_APP | **Real**: es la bandeja interna; el mensaje queda en la base y se consulta con el historial | `EmisorInApp` |
| Portal Web | **No integrado**: no hay repositorio del Portal para probar | Solo está configurado CORS para `http://localhost:5173` |
| Pedidos, Pagos, Logística y los demás | **No probados** | El contrato que deben usar es `POST /api/v1/notifications` |

## Contrato con Usuarios

| Dato | Usuarios | Notificaciones |
|---|---|---|
| Id de usuario | `Long` | `destinatarioId` es `Long`; el historial usa el mismo id en `/users/{id}/notifications` |
| JWT | HS256, `sub` = id del usuario como texto, `role` sin prefijo (`CLIENTE`, `COMERCIO`, `REPARTIDOR`, `ADMIN`) | `JwtAuthFilter` convierte `sub` a `Long` y acepta esos 4 roles; otro rol → `401` |
| Clave del JWT | `JWT_SECRET` | `JWT_SECRET`, con el mismo valor |
| Perfil | `GET /api/v1/users/{id}`, `estado` = `ACTIVO` / `INACTIVO` | `UsuariosClientHttp` lee `id`, `nombre` y `email`, y convierte `estado` en `activo` |
| Usuario inexistente | `404` | `422 RECIPIENT_NOT_FOUND` |
| Usuarios caído o lento | — | `503 USERS_SERVICE_UNAVAILABLE` (timeout de 2 s) |
| Credencial entre módulos | Ninguna (acuerdo del 17/11) | `GET /api/v1/templates` y `POST /api/v1/notifications` son públicos; Notificaciones llama a Usuarios sin credencial |
| Plantillas que usa Usuarios | Las busca por nombre: `USUARIO_BIENVENIDA_EMAIL` y `RECUPERACION_PASSWORD_EMAIL` | Las carga el `DataLoader` |
| Trazabilidad | Envía y loguea `X-Correlation-Id` | Lo toma, lo devuelve, lo loguea y lo propaga a Usuarios |

## Autorización sobre las notificaciones

Estar autenticado no alcanza: cada persona solo ve y modifica **sus propias** notificaciones. El `sub` del token
tiene que coincidir con el destinatario; `ADMIN` puede ver todas. Si no coincide, la respuesta es
`403 FORBIDDEN`. Aplica al historial (API-04) y a consultar, reintentar, ver intentos y marcar como leída
(API-02, 03, 11 y 12). Lo resuelve `ControlDeAcceso` (paquete `security`) y lo prueban los tests de `AccesoPorUsuarioTest`.

Antes de este cambio, un usuario CLIENTE podía leer el historial de otro, incluido el código de recuperación de
contraseña que envía Usuarios. La prueba real de abajo muestra que ahora responde 403.

## Cómo levantar los dos módulos

Los dos usan H2 en memoria: no hace falta Docker. Usuarios se levanta en el 8080 porque Notificaciones usa el 8089.

Terminal 1, Usuarios (desde la raíz de su repositorio):

```powershell
$env:SERVER_PORT = '8080'
$env:JWT_SECRET = 'deliverygo-integracion-local-2026-no-produccion'
$env:NOTIFICACIONES_MODO = 'http'
$env:NOTIFICACIONES_URL = 'http://localhost:8089'
$env:ADMIN_EMAIL = 'admin@deliverygo.test'
$env:ADMIN_PASSWORD = 'AdminDemo2026!'
mvn spring-boot:run
```

Terminal 2, Notificaciones (desde la raíz de este repositorio):

```powershell
$env:JWT_SECRET = 'deliverygo-integracion-local-2026-no-produccion'
$env:NOTIFICACIONES_USUARIOS_MODO = 'http'
$env:USUARIOS_BASE_URL = 'http://localhost:8080'
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--notificaciones.emisor.tasa-fallo=0"
```

Son claves de ejemplo, solo para uso local. Para comprobar el modo: `GET http://localhost:8089/status` debe
devolver `"integracionUsuarios": "http"`.

Después, ejecutar en orden [`integracion-usuarios.http`](integracion-usuarios.http) (REST Client de VS Code).

## Resultado de la prueba

| # | Paso | Resultado |
|---|---|---|
| 1 | Registro de Ana y de Beto en Usuarios | `201`. Usuarios pide la bienvenida a Notificaciones |
| 2 | Ana consulta su historial con el token del login de Usuarios | `200`, bienvenida `ENVIADA` por EMAIL |
| 3 | Ana consulta su notificación y sus intentos | `200`, 1 intento `EXITOSO` |
| 4 | Beto pide recuperar su contraseña | `202`. Usuarios pide la notificación de recuperación |
| 5 | Ana intenta leer el historial de Beto | **`403 FORBIDDEN`** |
| 6 | El ADMIN de Usuarios lee el historial de Beto | `200`, 2 notificaciones |
| 7 | Notificación para un id que no existe en Usuarios | `422 RECIPIENT_NOT_FOUND`: Usuarios respondió 404 |
| 8 | Notificación con Usuarios apagado | `503 USERS_SERVICE_UNAVAILABLE`: la llamada es real |

### Seguimiento de una notificación entre los dos módulos

El registro de Ana se envió con `X-Correlation-Id: demo-registro-ana`. El mismo id aparece en los logs de
los dos módulos, desde el pedido de Usuarios hasta el envío:

```text
notificaciones  [demo-registro-ana] NotificacionServices   : Notificación 1 creada: destinatario 3, plantilla 4, canal EMAIL
notificaciones  [demo-registro-ana] EmisorEmailSimulado    : EMAIL simulado enviado al destinatario 3
notificaciones  [demo-registro-ana] NotificacionServices   : Notificación 1: intento 1 EXITOSO -> estado ENVIADA
usuarios        [demo-registro-ana] NotificacionesClientHttp: [INT-01] Notificaciones aceptó el envío para el usuario 3: notificación 1 en estado ENVIADA
```

Después, la lectura (`PATCH /notifications/{id}/read`) deja `Notificación N leída por el destinatario X`, y cada
reintento deja `reintento solicitado` y el resultado del nuevo intento. El mensaje no se loguea porque puede
tener datos sensibles, como el código de recuperación.

### Qué pasa cuando falla el envío

Prueba con Notificaciones levantado con `--notificaciones.emisor.tasa-fallo=1`, para que todos los envíos
fallen:

| Paso | Resultado |
|---|---|
| Registro en Usuarios | `201`: el registro no se cae aunque falle la bienvenida |
| Estado de la bienvenida | `FALLIDA`, 1 intento |
| Reintento 1 (`POST /notifications/{id}/retry`) | `200`, `FALLIDA`, 2 intentos |
| Reintento 2 | `200`, `FALLIDA_DEFINITIVA`, 3 intentos (máximo de `notificaciones.max-intentos`) |
| Reintento 3 | `409 MAX_RETRIES_EXCEEDED` |
| Intentos (`GET /notifications/{id}/attempts`) | 3 intentos `FALLIDO`, cada uno con su fecha y su detalle |

## Para la defensa: recorrido de una notificación

1. **Cómo se genera.** Otro módulo, por ejemplo Usuarios al registrar a alguien, llama a
   `POST /api/v1/notifications` con `destinatarioId`, `plantillaId` y `parametros`. Notificaciones valida la plantilla
   y el canal, consulta a Usuarios si el destinatario existe y está activo (INT-01), arma el mensaje y lo guarda
   en estado `PENDIENTE`.
2. **Cómo llega.** En la misma transacción, el emisor del canal hace el primer intento. EMAIL y PUSH son
   simulados; IN_APP queda en la bandeja interna. El intento se registra y la notificación queda `ENVIADA` o
   `FALLIDA`.
3. **Cómo se consulta.** El destinatario, con su JWT, ve su historial (`GET /users/{id}/notifications`),
   el estado (`GET /notifications/{id}`) y los intentos (`/attempts`), y marca las IN_APP como leídas
   (`PATCH /read`). Otro usuario recibe 403.
4. **Qué pasa si falla.** Queda `FALLIDA` con el detalle del error. Se reintenta con `POST /retry` hasta
   `notificaciones.max-intentos` (3); al llegar al máximo pasa a `FALLIDA_DEFINITIVA` y un nuevo reintento da 409.
   Si lo que falla es Usuarios, la notificación no se crea y la respuesta es 503.

## Limitaciones

- **Usuarios no publica `pushToken`.** Con la integración real, una notificación PUSH responde
  `422 RECIPIENT_CONTACT_MISSING`. EMAIL e IN_APP funcionan.
- **El modo por defecto es `simulado`**, para que el módulo se pueda probar solo. La demo de integración se
  hace con `NOTIFICACIONES_USUARIOS_MODO=http`.
- **Portal Web y los demás módulos** no se probaron contra este módulo (ver la tabla del principio).
