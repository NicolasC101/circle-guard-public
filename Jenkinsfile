pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'chmod +x ./gradlew'
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
                        --tests 'com.circleguard.promotion.service.HealthStatusReevaluationTest' \
                        --tests 'com.circleguard.promotion.service.FloorServiceTest' \
                        --tests 'com.circleguard.promotion.service.AdministrativeCorrectionTest' \
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
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: '**/build/test-results/test/*.xml'
            archiveArtifacts allowEmptyArchive: true, artifacts: '**/build/reports/tests/test/**'
        }
    }
}