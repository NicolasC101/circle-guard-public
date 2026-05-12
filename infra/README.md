# Infraestructura local - Punto 1 del Taller 2

Esta carpeta contiene la base para configurar Jenkins, Docker y Kubernetes antes de avanzar a los pipelines de los seis microservicios seleccionados.

## Orden de puesta en marcha
1. Levantar los servicios de soporte del proyecto con Docker Compose.
2. Crear el cluster local de Kubernetes con `kind`.
3. Aplicar los namespaces base del proyecto.
4. Levantar Jenkins como contenedor Docker.
5. Validar accesos a Jenkins y al cluster con `kubectl`.

## Jenkins
Archivo de arranque:
- `infra/jenkins/docker-compose.yml`

Ejecución:
```bash
docker compose -f infra/jenkins/docker-compose.yml up -d
```

Acceso inicial:
- `http://localhost:8080`
- El password inicial se obtiene en los logs del contenedor con `docker logs circleguard-jenkins` o en `/var/jenkins_home/secrets/initialAdminPassword`.
- En el wizard de Jenkins, sigue la opción de instalar plugins sugeridos para dejar el entorno listo más rápido.

## Pipeline de desarrollo (punto 2)
Archivo del pipeline:
- `Jenkinsfile`

Que hace:
- Hace checkout del repositorio y habilita `gradlew`.
- Ejecuta solo pruebas unitarias por microservicio seleccionado.
- No necesita Dockerfiles ni manifiestos de Kubernetes para esta fase.

Pruebas incluidas por servicio:
- Auth: `LoginControllerTest`, `QrTokenServiceUnitTest`, `JwtTokenServiceUnitTest`, `CustomUserDetailsServiceUnitTest`, `JwtAuthenticationFilterUnitTest`, `DualChainAuthenticationProviderUnitTest`.
- Identity: `IdentityEncryptionConverterTest`, `IdentityVaultControllerTest`.
- Gateway: `QrValidationServiceTest`, `QrValidationServiceUnitTest`, `GateControllerTest`.
- Form: `SymptomMapperTest`, `QuestionnaireControllerTest`, `HealthSurveyControllerTest`, `AttachmentControllerTest`.
- Promotion: `StatusLifecycleTest`, `HealthStatusServiceTest`, `HealthStatusReevaluationTest`, `FloorServiceTest`, `AdministrativeCorrectionTest`, `SurveyListenerTest`, `HealthStatusControllerTest`.
- Notification: `TemplateServiceTest`, `RoomReservationServiceTest`, `PriorityAlertListenerTest`, `NotificationRetryTest`, `NotificationDispatcherTest`, `LmsServiceTest`, `ExposureNotificationListenerTest`.

Configuracion en Jenkins desde `http://localhost:8080`:
1. Entra con el usuario administrador y abre `New Item`.
2. Crea un proyecto `Multibranch Pipeline` y ponle un nombre como `circleguard-dev`.
3. En `Branch Sources`, agrega `Git`.
4. Pega la URL del repositorio.
5. Si el repositorio es publico, deja `Credentials` en `None`.
6. Si Jenkins exige credenciales, crea una credencial de tipo `Username with password` con tu usuario de GitHub y un token personal como password, o usa el token que ya tengas configurado.
7. En `Behaviors`, puedes dejar la deteccion por defecto para descubrir ramas remotas.
8. Guarda el job.
9. Abre `Scan Multibranch Pipeline Now` para que Jenkins detecte la rama `dev`.
10. Cuando aparezca la rama `dev`, entra al subjob y ejecuta `Build Now`.
11. Revisa `Console Output`, `Stage View` y los reportes JUnit para confirmar que corrieron solo las pruebas unitarias.
12. Para futuras ejecuciones, cada push a `dev` disparara el pipeline automaticamente si el webhook queda configurado.

Configuracion opcional de webhook:
1. En GitHub, abre el repositorio.
2. Ve a `Settings > Webhooks`.
3. Agrega la URL del webhook de Jenkins, normalmente `http://localhost:8080/github-webhook/` si Jenkins es accesible desde GitHub o desde un tunnel local.
4. Selecciona `application/json` y el evento `Just the push event`.
5. Guarda el webhook.

Nota:
- En este punto no necesitas `infra/jenkins/Jenkinsfile.dev`; el archivo de raiz `Jenkinsfile` es el que Jenkins multibranch va a descubrir.

## Kubernetes
Archivo de cluster:
- `infra/k8s/kind-config.yaml`

Namespaces base:
- `infra/k8s/namespaces.yaml`

Ejecución:
```bash
kind create cluster --name circleguard --config infra/k8s/kind-config.yaml
kubectl apply -f infra/k8s/namespaces.yaml
kubectl get nodes
kubectl get namespaces
```

## Validación esperada
- Docker activo y con los contenedores de soporte en ejecución.
- Jenkins disponible en el puerto 8080.
- `kubectl` apuntando al cluster `circleguard`.
- Namespaces listos para `dev`, `stage` y `master`.
