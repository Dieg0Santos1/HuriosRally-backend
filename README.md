# HuriosRally Backend

Base del backend en Spring Boot para que el equipo clone `main` y empiece a implementar sobre una estructura ya creada.

Incluye:
- estructura de carpetas del backend
- `pom.xml` con dependencias base
- Maven Wrapper (`mvnw`, `mvnw.cmd`, `.mvn`)
- clase principal de Spring Boot
- `application.properties` inicial

No incluye codigo de negocio ni credenciales reales.

## APIs utilizadas

A continuación se listan las APIs y servicios utilizados por el proyecto, agrupadas por tipo, con una breve descripción de qué hace cada una.

### APIs externas / servicios terceros
- **Supabase (Postgres + Storage)**: Base de datos PostgreSQL hospedada en Supabase y servicio de almacenamiento (Storage) usado para guardar imágenes y archivos. El servicio se llama mediante llamadas HTTP a la API de Storage desde `FileStorageService`.
- **Resend (resend.com) / SMTP**: Servicio preferente para envío de correos vía API (`Resend`). Si no está configurado, el proyecto usa SMTP (configurable en `application.properties`) mediante `JavaMailSender` en `EmailService`.
- **Micrometer / Prometheus**: Biblioteca de métricas (`io.micrometer`) usada para instrumentar métricas de negocio y exponerlas para Prometheus/Grafana (configuración en `MetricsConfiguration`).
- **OpenAPI / Swagger (springdoc-openapi)**: Generación de especificación OpenAPI y UI Swagger para la API REST (configurada en `OpenApiConfig`).
- **jjwt (JSON Web Tokens)**: Librería `io.jsonwebtoken` para generación y validación de JWT usados en autenticación (implementado en `JwtUtil`).

### Dependencias/servicios del entorno
- **Maven**: Gestión de dependencias y construcción del proyecto (`pom.xml`).
- **SMTP externo**: Cualquier proveedor SMTP configurado para envíos si no se usa Resend.

### Endpoints REST principales (API del backend)
Las rutas principales expuestas por el backend y una breve descripción de cada una:
- **/auth**: Endpoints de autenticación y gestión de usuarios (registro, login, envío/verificación de códigos, reinicio de contraseña). Controlador: `AuthController`.
- **/payments**: Endpoints para procesar pagos y consultar ventas. Manejo de ventas y registro de `Sale` en base de datos (no integra directamente un gateway de pagos en el backend; `PaymentService` registra la transacción). Controlador: `PaymentController`.
- **/api/images**: Endpoint para subir imágenes (`POST /api/images/upload`). Usa `FileStorageService` para subir a Supabase Storage o almacenamiento local.
- **/products**: Endpoints para gestión de productos (crear, listar, actualizar, eliminar) - Controlador: `ProductController`.
- **/user**: Endpoints para gestión del perfil del usuario autenticado - Controlador: `UserController`.
- **/export**: Endpoints para exportar datos en Excel (`/export/clients`, `/export/products`, `/export/sales`). Controlador: `ExportController`.
- **/backup**: Endpoint para crear backups de configuración (`POST /backup/config`). Controlador: `BackupController`.
- **/health-public** y **/**: Endpoints públicos para comprobar salud/estado (Controlador: `PublicStatusController`).

## Lista de APIs creadas (Swagger)

Listado de endpoints visibles en Swagger UI, agrupados por controlador:

### user-controller
- `GET /user/profile`: Obtiene el perfil del usuario autenticado desde su token JWT.
- `PUT /user/profile`: Actualiza datos del perfil (nombre, teléfono, dirección o imagen).
- `POST /user/profile-image`: Sube una nueva imagen de perfil y la asocia al usuario.

### product-controller
- `GET /products/{id}`: Devuelve el detalle de un producto por su ID.
- `PUT /products/{id}`: Actualiza información de un producto existente.
- `DELETE /products/{id}`: Elimina un producto por su ID.
- `PUT /products/{id}/add-stock`: Incrementa el stock disponible de un producto.
- `GET /products`: Lista todos los productos registrados.
- `POST /products`: Crea un nuevo producto en el inventario.
- `GET /products/search`: Busca productos por nombre usando el parámetro `q`.

### payment-controller
- `POST /payments/process`: Procesa una compra y registra la venta para el usuario autenticado.
- `GET /payments/{id}`: Consulta una venta específica por su ID.
- `GET /payments/my-orders`: Lista las órdenes/ventas del usuario autenticado.
- `GET /payments/all`: Lista todas las ventas registradas (uso administrativo).

### backup-controller
- `POST /backup/config`: Genera un respaldo de archivos de configuración del backend.

### auth-controller
- `POST /auth/verify-code`: Valida el código de verificación y devuelve sesión/JWT si es correcto.
- `POST /auth/send-verification-code`: Envía un código de verificación al correo del usuario.
- `POST /auth/reset-password`: Restablece la contraseña usando token de recuperación.
- `POST /auth/request-password-reset`: Solicita el inicio del flujo de recuperación de contraseña.
- `POST /auth/register`: Registra un nuevo usuario en la plataforma.
- `POST /auth/login`: Autentica al usuario y devuelve respuesta de acceso.

### image-upload-controller
- `POST /api/images/upload`: Sube una imagen de producto y devuelve su URL pública.

### public-status-controller
- `GET /health-public`: Endpoint público para verificar que el backend está en línea.
- `GET /`: Endpoint raíz público que devuelve estado básico del servicio.

### export-controller
- `GET /export/sales`: Exporta las ventas a un archivo Excel descargable.
- `GET /export/products`: Exporta los productos a un archivo Excel descargable.
- `GET /export/clients`: Exporta los clientes a un archivo Excel descargable.