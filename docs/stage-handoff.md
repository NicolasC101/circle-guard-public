# Handoff - Stage pipeline

## Estado actual
- La rama `stage` ya incluye el pipeline de Kubernetes para el punto 4.
- El fallo actual de Jenkins era porque el smoke test de stage intentaba conectar a los servicios antes de que el entorno terminara de aceptar conexiones.
- Se corrigió haciendo el smoke test con reintentos sobre el login de Auth y dejando comentada la limpieza de imágenes `stage` para acelerar la depuración.
- El cambio más reciente quedó subido en el commit `15488b1`.

## Archivos relevantes
- [Jenkinsfile](../Jenkinsfile)
- [infra/jenkins/docker-compose.yml](../infra/jenkins/docker-compose.yml)
- [infra/README.md](../infra/README.md)
- [infra/k8s/stage/apps.yaml](../infra/k8s/stage/apps.yaml)
- [services/circleguard-auth-service/src/test/java/com/circleguard/auth/integration/StageEnvironmentSmokeTest.java](../services/circleguard-auth-service/src/test/java/com/circleguard/auth/integration/StageEnvironmentSmokeTest.java)

## Qué hace el pipeline de stage
1. Hace checkout de la rama `stage`.
2. Descarga `kind`, `kubectl` y el Docker CLI en el workspace si no existen.
3. Espera a que el daemon Docker de Docker Desktop responda por TCP.
4. Ejecuta pruebas unitarias seleccionadas.
5. Construye los JAR y las imágenes Docker de Auth, Identity y Gateway.
6. Carga las imágenes en `kind`.
7. Aplica los manifiestos de Kubernetes de stage.
8. Ejecuta el smoke test contra el entorno desplegado, con reintentos si los servicios todavía están arrancando.
9. La limpieza de imágenes `stage` queda comentada por ahora para facilitar la depuración.

## Corrección aplicada al fallo actual
- Antes Jenkins intentaba usar el Docker del host y fallaba con:
  - `Cannot connect to the Docker daemon at unix:///var/run/docker.sock`
- Ahora Jenkins usa `DOCKER_HOST=tcp://host.docker.internal:2375`.
- El pipeline descarga el Docker CLI en `.ci-tools` y espera a `docker info` antes de invocar `kind`.
- La espera de Docker ahora es de hasta 5 minutos para cubrir arranques lentos del daemon.
- Esto evita depender de `docker:dind` y usa el daemon TCP expuesto por Docker Desktop.
- El kubeconfig generado para `kind` se reescribe para usar `host.docker.internal` y se fuerza `tls-server-name=localhost` para respetar el SAN del certificado.
- El smoke test de stage reintenta el login hasta que Auth responde, para tolerar arranques lentos.
- La limpieza de imágenes `stage` quedó comentada temporalmente.

## Requisito del entorno
- En Docker Desktop debe estar habilitado `Expose daemon on tcp://localhost:2375 without TLS`.
- El contenedor de Jenkins debe poder resolver `host.docker.internal`.

## Siguiente paso sugerido
1. Recrear el stack local de Jenkins con el compose actualizado.
2. Reintentar el job multibranch sobre `stage`.
3. Si vuelve a fallar, revisar si el problema está en la conexión al servicio `docker` del compose o en la creación del cluster `kind`.

## Nota
- E2E sigue reservado para `master`.
- El stage actual solo cubre build, despliegue en Kubernetes local y smoke tests de integración.
