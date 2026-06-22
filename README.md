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
- **/products**: Endpoints para gestión de productos (crear, listar, actualizar, eliminar) — Controlador: `ProductController`.
- **/users**: Endpoints para gestión de usuarios — Controlador: `UserController`.
- **/export**: Endpoints para exportar datos en Excel (`/export/clients`, `/export/products`, `/export/sales`). Controlador: `ExportController`.
- **/backup**: Endpoint para crear backups de configuración (`POST /backup/config`). Controlador: `BackupController`.
- **/public-status**: Endpoint público para comprobar salud/estado (Controlador: `PublicStatusController`).