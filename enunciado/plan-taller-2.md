# Plan de trabajo - Taller 2: pruebas y lanzamiento

## Objetivo
Definir y ejecutar, paso a paso, la estrategia de pipelines, pruebas y despliegue para cumplir el Taller 2 sobre el proyecto CircleGuard, empezando por la configuración de al menos seis microservicios que se comuniquen entre sí y continuando con las actividades de pruebas, despliegue y documentación.

## Microservicios seleccionados
Se trabajará inicialmente con estos seis servicios porque forman un flujo funcional coherente y permiten pruebas cruzadas entre sí:

1. Auth Service
2. Identity Service
3. Gateway Service
4. Form Service
5. Promotion Service
6. Notification Service

### Relación funcional esperada
- Auth autentica usuarios y emite credenciales o tokens.
- Gateway valida accesos y consume la autenticación para el ingreso al sistema.
- Identity gestiona la desidentificación y el enlace seguro entre identidad real y anónima.
- Form recibe y procesa cuestionarios de salud.
- Promotion consume eventos o resultados del formulario para cambiar estados de riesgo.
- Notification publica avisos asociados a cambios de estado, validaciones o eventos operativos.

## Estrategia general de trabajo
El taller se resolverá en este orden para minimizar retrabajo y asegurar trazabilidad:

1. Preparar la base técnica de Jenkins, Docker y Kubernetes.
2. Definir los pipelines de desarrollo para los seis microservicios seleccionados.
3. Implementar y/o ajustar pruebas unitarias, de integración, E2E y de rendimiento.
4. Construir el pipeline de stage con validación sobre Kubernetes.
5. Construir el pipeline master con build, validación integral, despliegue y release notes.
6. Documentar resultados, capturas, análisis y material de entrega.

## Fase 1: Configuración base de la plataforma
### Meta
Dejar disponibles Jenkins, Docker y Kubernetes para soportar la automatización de CI/CD.

### Actividades
- Levantar la infraestructura local o de laboratorio con Docker Compose.
- Verificar conectividad entre Jenkins y el entorno de despliegue.
- Definir un cluster Kubernetes para stage y master.
- Establecer credenciales, secretos y variables de entorno compartidas.

### Entregables
- Evidencia de instalación y configuración.
- Plantilla de Jenkinsfile o configuración equivalente.
- Manifiestos o plantillas base de Kubernetes.

## Fase 2: Pipelines de desarrollo para los seis microservicios
### Meta
Definir pipelines por servicio para construir, probar y publicar artefactos listos para uso en entorno dev.

### Actividades
- Estandarizar una estructura común de pipeline para backend.
- Incluir etapas de checkout, build, pruebas unitarias y empaquetado.
- Publicar imágenes Docker por servicio.
- Habilitar despliegue automático en entorno dev cuando aplique.

### Resultado esperado
- Pipelines independientes pero consistentes.
- Imagen o artefacto por cada microservicio.
- Validación inicial de que los servicios levantan y se comunican en dev.

## Fase 3: Implementación de pruebas
### Meta
Cubrir componentes individuales, interacción entre servicios, flujos completos de usuario y comportamiento bajo carga.

### Plan de pruebas
- Agregar al menos cinco pruebas unitarias nuevas para lógica interna relevante.
- Agregar al menos cinco pruebas de integración entre servicios seleccionados.
- Agregar al menos cinco pruebas E2E sobre flujos completos.
- Diseñar pruebas de rendimiento y estrés con Locust.

### Criterios de selección de pruebas
- Deben corresponder a funcionalidades existentes, ajustadas o agregadas.
- Deben involucrar el flujo entre Auth, Gateway, Identity, Form, Promotion y Notification.
- Deben generar resultados medibles para análisis posterior.

## Fase 4: Pipeline de stage
### Meta
Construir una etapa de validación sobre Kubernetes antes de promover cambios a master.

### Actividades
- Desplegar los servicios seleccionados en un namespace de stage.
- Ejecutar pruebas de integración y pruebas de sistema sobre el despliegue vivo.
- Verificar configuración, networking y dependencias entre servicios.
- Recolectar métricas de ejecución y estabilidad.

### Entregables
- Pipeline de stage completo.
- Capturas de ejecución y resultados.
- Evidencia de pruebas contra servicios desplegados en Kubernetes.

## Fase 5: Pipeline master
### Meta
Definir el flujo completo de entrega: build, pruebas, validación del sistema, despliegue y trazabilidad de release.

### Actividades
- Ejecutar build y pruebas unitarias como puerta de entrada.
- Ejecutar validaciones de integración y sistema.
- Desplegar en Kubernetes para el entorno master o producción simulada.
- Generar Release Notes automáticas con base en cambios y versiones.

### Entregables
- Pipeline master con fases definidas y ordenadas.
- Release Notes automáticas.
- Evidencia del despliegue exitoso.

## Fase 6: Documentación y evidencia
### Meta
Producir el material final exigido por el taller.

### Contenido del reporte
- Configuración de los pipelines.
- Resultado de ejecución exitoso con pantallazos.
- Análisis de pruebas, especialmente rendimiento y estrés.
- Métricas de tiempo de respuesta, throughput y tasa de errores.
- Explicación del flujo de despliegue y del proceso de release.

### Material adicional
- Video corto de máximo 8 minutos.
- Zip con pipelines, pruebas y cambios realizados.

## Secuencia de ejecución propuesta
1. Confirmar microservicios y dependencias entre ellos.
2. Levantar infraestructura base de soporte.
3. Crear pipelines de desarrollo para los seis servicios.
4. Implementar pruebas unitarias e integración mínimas necesarias.
5. Construir flujos E2E y escenarios Locust.
6. Integrar stage en Kubernetes y validar despliegue.
7. Construir el pipeline master con release notes.
8. Documentar evidencias, métricas y conclusiones.

## Criterios de éxito
- Al menos seis microservicios con pipelines funcionales.
- Pruebas unitarias, de integración, E2E y rendimiento ejecutadas con evidencia.
- Despliegue validado en Kubernetes para stage y master.
- Release Notes generadas automáticamente.
- Documentación clara, reproducible y alineada con el enunciado.

## Observación de alcance
Si durante la implementación aparece una dependencia funcional adicional, se podrá incorporar un séptimo servicio solo si mejora la calidad de las pruebas o la coherencia del flujo, pero no será necesario para cumplir el requisito mínimo.