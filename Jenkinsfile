pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
    }

    environment {
        DOCKER_HOST = 'tcp://host.docker.internal:2375'
        DOCKER_TLS_CERTDIR = ''
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'chmod +x ./gradlew'
            }
        }

        stage('Prepare tools') {
            steps {
                script {
                    env.PATH = "${env.WORKSPACE}/.ci-tools:${env.PATH}"
                }

                sh '''
                    set -e
                    TOOLS_DIR="${WORKSPACE}/.ci-tools"
                    mkdir -p "$TOOLS_DIR"

                    if [ ! -x "$TOOLS_DIR/docker" ]; then
                      curl -fsSL -o "$TOOLS_DIR/docker.tgz" https://download.docker.com/linux/static/stable/x86_64/docker-27.3.1.tgz
                      tar -xzf "$TOOLS_DIR/docker.tgz" -C "$TOOLS_DIR" --strip-components=1 docker/docker
                      rm -f "$TOOLS_DIR/docker.tgz"
                      chmod +x "$TOOLS_DIR/docker"
                    fi

                    if [ ! -x "$TOOLS_DIR/kind" ]; then
                      curl -fsSL -o "$TOOLS_DIR/kind" https://github.com/kubernetes-sigs/kind/releases/download/v0.31.0/kind-linux-amd64
                      chmod +x "$TOOLS_DIR/kind"
                    fi

                    if [ ! -x "$TOOLS_DIR/kubectl" ]; then
                      curl -fsSL -o "$TOOLS_DIR/kubectl" https://dl.k8s.io/release/v1.31.0/bin/linux/amd64/kubectl
                      chmod +x "$TOOLS_DIR/kubectl"
                    fi

                                        for attempt in $(seq 1 60); do
                                            if docker info >/dev/null 2>&1; then
                                                break
                                            fi

                                            if [ "$attempt" -eq 60 ]; then
                                                echo "Docker daemon is not ready after 5 minutes"
                                                exit 1
                                            fi

                                            sleep 5
                                        done

                    "$TOOLS_DIR/kind" get kubeconfig --name circleguard > "$TOOLS_DIR/kubeconfig"
                                        sed -i 's/127.0.0.1/host.docker.internal/g' "$TOOLS_DIR/kubeconfig"
                                        "$TOOLS_DIR/kubectl" config set-cluster kind-circleguard --tls-server-name=localhost --kubeconfig="$TOOLS_DIR/kubeconfig"
                '''
            }
        }

        stage('Auth unit tests') {
            steps {
                sh '''
                    ./gradlew :services:circleguard-auth-service:test \
                        --tests 'com.circleguard.auth.controller.LoginControllerTest' \
                        --tests 'com.circleguard.auth.service.QrTokenServiceUnitTest' \
                        --tests 'com.circleguard.auth.service.JwtTokenServiceUnitTest' \
                        --tests 'com.circleguard.auth.service.CustomUserDetailsServiceUnitTest' \
                        --tests 'com.circleguard.auth.security.JwtAuthenticationFilterUnitTest' \
                        --tests 'com.circleguard.auth.security.DualChainAuthenticationProviderUnitTest'
                '''
            }
        }

        stage('Identity unit tests') {
            steps {
                sh '''
                    ./gradlew :services:circleguard-identity-service:test \
                        --tests 'com.circleguard.identity.util.IdentityEncryptionConverterTest' \
                        --tests 'com.circleguard.identity.controller.IdentityVaultControllerTest'
                '''
            }
        }

        stage('Gateway unit tests') {
            steps {
                sh '''
                    ./gradlew :services:circleguard-gateway-service:test \
                        --tests 'com.circleguard.gateway.service.QrValidationServiceTest' \
                        --tests 'com.circleguard.gateway.service.QrValidationServiceUnitTest' \
                        --tests 'com.circleguard.gateway.controller.GateControllerTest'
                '''
            }
        }

        stage('Form unit tests') {
            steps {
                sh '''
                    ./gradlew :services:circleguard-form-service:test \
                        --tests 'com.circleguard.form.service.SymptomMapperTest' \
                        --tests 'com.circleguard.form.controller.QuestionnaireControllerTest' \
                        --tests 'com.circleguard.form.controller.HealthSurveyControllerTest' \
                        --tests 'com.circleguard.form.controller.AttachmentControllerTest'
                '''
            }
        }

        stage('Promotion unit tests') {
            steps {
                sh '''
                    ./gradlew :services:circleguard-promotion-service:test \
                        --tests 'com.circleguard.promotion.service.StatusLifecycleTest' \
                        --tests 'com.circleguard.promotion.service.HealthStatusServiceTest' \
                        --tests 'com.circleguard.promotion.service.FloorServiceTest' \
                        --tests 'com.circleguard.promotion.listener.SurveyListenerTest' \
                        --tests 'com.circleguard.promotion.controller.HealthStatusControllerTest'
                '''
            }
        }

        stage('Notification unit tests') {
            steps {
                sh '''
                    ./gradlew :services:circleguard-notification-service:test \
                        --tests 'com.circleguard.notification.service.TemplateServiceTest' \
                        --tests 'com.circleguard.notification.service.RoomReservationServiceTest' \
                        --tests 'com.circleguard.notification.service.PriorityAlertListenerTest' \
                        --tests 'com.circleguard.notification.service.NotificationRetryTest' \
                        --tests 'com.circleguard.notification.service.NotificationDispatcherTest' \
                        --tests 'com.circleguard.notification.service.LmsServiceTest' \
                        --tests 'com.circleguard.notification.service.ExposureNotificationListenerTest'
                '''
            }
        }

        stage('Build stage images') {
            steps {
                sh '''
                    set -e
                    ./gradlew :services:circleguard-auth-service:bootJar \
                        :services:circleguard-identity-service:bootJar \
                        :services:circleguard-gateway-service:bootJar

                    docker build -t circleguard-auth-service:stage \
                        -f services/circleguard-auth-service/Dockerfile \
                        services/circleguard-auth-service

                    docker build -t circleguard-identity-service:stage \
                        -f services/circleguard-identity-service/Dockerfile \
                        services/circleguard-identity-service

                    docker build -t circleguard-gateway-service:stage \
                        -f services/circleguard-gateway-service/Dockerfile \
                        services/circleguard-gateway-service
                '''
            }
        }

        stage('Load images into kind') {
            steps {
                sh '''
                    set -e
                    TOOLS_DIR="${WORKSPACE}/.ci-tools"
                    "$TOOLS_DIR/kind" load docker-image circleguard-auth-service:stage --name circleguard
                    "$TOOLS_DIR/kind" load docker-image circleguard-identity-service:stage --name circleguard
                    "$TOOLS_DIR/kind" load docker-image circleguard-gateway-service:stage --name circleguard
                '''
            }
        }

        stage('Deploy stage apps') {
            steps {
                sh '''
                    set -e
                    TOOLS_DIR="${WORKSPACE}/.ci-tools"
                    export KUBECONFIG="$TOOLS_DIR/kubeconfig"

                    "$TOOLS_DIR/kubectl" apply -f infra/k8s/namespaces.yaml
                    "$TOOLS_DIR/kubectl" apply -f infra/k8s/stage/apps.yaml

                    "$TOOLS_DIR/kubectl" rollout status deployment/circleguard-auth-service -n circleguard-stage --timeout=180s
                    "$TOOLS_DIR/kubectl" rollout status deployment/circleguard-identity-service -n circleguard-stage --timeout=180s
                    "$TOOLS_DIR/kubectl" rollout status deployment/circleguard-gateway-service -n circleguard-stage --timeout=180s
                '''
            }
        }

        stage('Stage integration tests') {
            steps {
                sh '''
                    AUTH_BASE_URL=http://host.docker.internal:30080 \
                    GATEWAY_BASE_URL=http://host.docker.internal:30082 \
                    CIRCLEGUARD_USERNAME=super_admin \
                    CIRCLEGUARD_PASSWORD=password \
                    ./gradlew :services:circleguard-auth-service:test \
                        --tests 'com.circleguard.auth.integration.StageEnvironmentSmokeTest'
                '''
            }
        }
    }

    post {
        always {
            sh '''
                set +e
                docker image rm -f circleguard-auth-service:stage >/dev/null 2>&1
                docker image rm -f circleguard-identity-service:stage >/dev/null 2>&1
                docker image rm -f circleguard-gateway-service:stage >/dev/null 2>&1
            '''
            junit allowEmptyResults: true, testResults: '**/build/test-results/test/*.xml'
            archiveArtifacts allowEmptyArchive: true, artifacts: '**/build/reports/tests/test/**'
        }
    }
}