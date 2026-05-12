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
- `infra/jenkins/Jenkinsfile.dev`

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
2. Crea un proyecto `Pipeline` y ponle un nombre como `circleguard-dev-unit-tests`.
3. En la seccion `Pipeline`, elige `Pipeline script from SCM`.
4. Selecciona `Git` como SCM y pega la URL del repositorio.
5. Usa la rama `master` o la rama que estes trabajando.
6. En `Script Path`, escribe `infra/jenkins/Jenkinsfile.dev`.
7. Guarda el job y ejecuta `Build Now`.
8. Revisa `Stage View`, la consola y los reportes JUnit para confirmar que solo corrio el bloque de pruebas unitarias.

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
