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

Nota importante:
- Jenkins usa el daemon TCP de Docker Desktop en `host.docker.internal:2375`.
- El pipeline de `stage` instala el cliente Docker en el workspace y usa ese daemon para hablar con Docker Desktop.
- El pipeline de `stage` espera hasta 5 minutos a que `docker info` responda antes de seguir con `kind`.
- Si cambias el compose, vuelve a recrear el stack con `docker compose -f infra/jenkins/docker-compose.yml up -d --force-recreate`.
- El host donde corre Docker debe estar activo antes de lanzar Jenkins.

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

## Pipeline de stage (punto 4)
Archivo del pipeline:
- `Jenkinsfile`

Que hace:
- Ejecuta las pruebas unitarias como puerta de entrada.
- Construye las imagenes Docker de Auth, Identity y Gateway.
- Carga esas imagenes en el cluster local `kind`.
- Aplica los manifiestos de Kubernetes de stage.
- Ejecuta pruebas de integracion contra la aplicacion ya desplegada.

Archivos de soporte:
- `services/circleguard-auth-service/Dockerfile`
- `services/circleguard-identity-service/Dockerfile`
- `services/circleguard-gateway-service/Dockerfile`
- `infra/k8s/stage/apps.yaml`
- `services/circleguard-auth-service/src/test/java/com/circleguard/auth/integration/StageEnvironmentSmokeTest.java`

Flujo esperado en Jenkins:
1. Hacer checkout de la rama `stage`.
2. Descargar `kind` y `kubectl` dentro del workspace si no existen.
3. Ejecutar las pruebas unitarias.
4. Construir los JAR y las imagenes Docker.
5. Cargar las imagenes en el cluster `kind`.
6. Aplicar el namespace y los manifiestos de stage.
7. Esperar a que los deployments esten listos.
8. Correr las pruebas de integracion contra los endpoints publicados por el cluster.

Puertos expuestos en kind:
- `30080` para Auth.
- `30081` para Identity.
- `30082` para Gateway.

Notas de configuracion:
- Auth ya no apunta a `localhost` para Identity; ahora usa `circleguard.identity-service.url`.
- Las pruebas de integracion usan `host.docker.internal` para llegar desde Jenkins a los NodePorts del cluster local.
- Jenkins necesita acceso al socket Docker del host para crear y cargar las imagenes de `kind`.
- El stage despliega Redis dentro de `kind` y el gateway valida QR contra ese servicio interno.
- El pipeline espera el rollout de Redis antes de ejecutar el smoke test.
- Las pruebas E2E se ejecutan en el pipeline de `master` junto con Locust y las Release Notes.

## Pipeline de master (punto 5)
Archivo del pipeline:
- `Jenkinsfile`

Que hace:
- Ejecuta las pruebas unitarias como base de calidad.
- Ejecuta pruebas de sistema o E2E antes del despliegue.
- Construye y carga las imagenes Docker de Auth, Identity y Gateway.
- Despliega la aplicacion en `kind` bajo `circleguard-master`.
- Valida el despliegue con smoke tests de integracion.
- Ejecuta escenarios de carga y estres con Locust.
- Genera Release Notes automaticas como artefacto del build.

Flujo esperado en Jenkins:
1. Hacer checkout de la rama `master`.
2. Descargar `kind`, `kubectl` y la herramienta de Locust dentro del workspace si no existen.
3. Ejecutar las pruebas unitarias.
4. Ejecutar las pruebas de sistema/E2E.
5. Construir los JAR y las imagenes Docker.
6. Cargar las imagenes en el cluster `kind`.
7. Aplicar el namespace y los manifiestos de `master`.
8. Esperar a que los deployments esten listos.
9. Correr las pruebas de integracion contra los endpoints publicados por el cluster.
10. Correr los escenarios de Locust contra la aplicacion desplegada.
11. Generar y archivar las Release Notes.

Puertos expuestos en kind para `master`:
- `30180` para Auth.
- `30181` para Identity.
- `30182` para Gateway.

Notas de configuracion:
- El stage sigue usando `circleguard-stage` y `30080/30081/30082`.
- Master usa `circleguard-master` y `30180/30181/30182`.
- Las pruebas de integracion y Locust usan `host.docker.internal` para llegar a los NodePorts del cluster local.
- El pipeline genera `build/reports/release-notes/RELEASE-NOTES.md` con el resumen de cambios del build.
- Las Release Notes quedan archivadas junto con los reportes de pruebas.

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
