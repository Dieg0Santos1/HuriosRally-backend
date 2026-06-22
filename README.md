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


