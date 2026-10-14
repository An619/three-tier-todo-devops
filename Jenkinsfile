pipeline {
    agent any
    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timeout(time: 60, unit: 'MINUTES')
    }
    parameters {
        booleanParam(name: 'PUBLISH_IMAGES', defaultValue: false, description: 'Push tested and scanned immutable service images to Docker Hub.')
    }
    environment {
        AUTH_LOCAL_IMAGE = 'three-tier-todo-auth-service'
        TASK_LOCAL_IMAGE = 'three-tier-todo-task-service'
        FRONTEND_LOCAL_IMAGE = 'three-tier-todo-frontend'
        IMAGE_TAG = ''
    }
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_SHORT = sh(script: 'git rev-parse --short=7 HEAD', returnStdout: true).trim()
                    env.IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_SHORT}"
                    echo "Building immutable image tag: ${env.IMAGE_TAG}"
                }
            }
        }
        stage('Validate Toolchain') {
            steps {
                sh '''
                    set -eu
                    git diff --check
                    java -version
                    mvn -version
                    node --version
                    npm --version
                    docker info
                    trivy --version
                '''
            }
        }
        stage('Backend and Service Tests') {
            steps {
                sh '''
                    set -eu
                    mvn -B -f backend/pom.xml clean verify
                    mvn -B -f services/auth-service/pom.xml clean verify
                    mvn -B -f services/task-service/pom.xml clean verify
                '''
            }
        }
        stage('SonarQube Analysis') {
            steps {
                script {
                    def scannerHome = tool 'sonar-scanner'
                    withSonarQubeEnv('sonar') {
                        sh "${scannerHome}/bin/sonar-scanner"
                    }
                }
            }
        }
        stage('SonarQube Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
        stage('Frontend Quality and Build') {
            steps {
                dir('frontend') {
                    sh '''
                        set -eu
                        npm ci
                        npm run lint
                        npm run build
                    '''
                }
            }
        }
        stage('Build Microservices Images') {
            steps {
                sh '''
                    set -eu
                    docker build --pull -t "$AUTH_LOCAL_IMAGE:$IMAGE_TAG" ./services/auth-service
                    docker build --pull -t "$TASK_LOCAL_IMAGE:$IMAGE_TAG" ./services/task-service
                    docker build --pull -f frontend/Dockerfile.microservices -t "$FRONTEND_LOCAL_IMAGE:$IMAGE_TAG" .
                '''
            }
        }
        stage('Container Vulnerability Scan and SBOM') {
            steps {
                sh '''
                    set -eu
                    mkdir -p reports
                    trivy image --no-progress --format json --output reports/auth-trivy.json --exit-code 1 --severity HIGH,CRITICAL "$AUTH_LOCAL_IMAGE:$IMAGE_TAG"
                    trivy image --no-progress --format json --output reports/task-trivy.json --exit-code 1 --severity HIGH,CRITICAL "$TASK_LOCAL_IMAGE:$IMAGE_TAG"
                    trivy image --no-progress --format json --output reports/frontend-trivy.json --exit-code 1 --severity HIGH,CRITICAL "$FRONTEND_LOCAL_IMAGE:$IMAGE_TAG"
                    trivy image --no-progress --format cyclonedx --output reports/auth-sbom.cdx.json "$AUTH_LOCAL_IMAGE:$IMAGE_TAG"
                    trivy image --no-progress --format cyclonedx --output reports/task-sbom.cdx.json "$TASK_LOCAL_IMAGE:$IMAGE_TAG"
                    trivy image --no-progress --format cyclonedx --output reports/frontend-sbom.cdx.json "$FRONTEND_LOCAL_IMAGE:$IMAGE_TAG"
                '''
            }
        }
        stage('Publish Immutable Images') {
            when { expression { return params.PUBLISH_IMAGES } }
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-creds', usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
                    sh '''
                        set +x
                        set -eu
                        trap 'docker logout >/dev/null 2>&1 || true' EXIT
                        echo "$DOCKER_PASSWORD" | docker login --username "$DOCKER_USERNAME" --password-stdin
                        for image in "$AUTH_LOCAL_IMAGE" "$TASK_LOCAL_IMAGE" "$FRONTEND_LOCAL_IMAGE"; do
                          docker tag "$image:$IMAGE_TAG" "$DOCKER_USERNAME/$image:$IMAGE_TAG"
                          docker push "$DOCKER_USERNAME/$image:$IMAGE_TAG"
                        done
                    '''
                }
                echo 'Images published with immutable build-and-commit tags. Promote them through a reviewed Helm values change; Argo CD owns delivery.'
            }
        }
    }
    post {
        always {
            archiveArtifacts artifacts: 'reports/**', allowEmptyArchive: true, fingerprint: true
            sh '''
                for image in "$AUTH_LOCAL_IMAGE" "$TASK_LOCAL_IMAGE" "$FRONTEND_LOCAL_IMAGE"; do
                  if [ -n "$IMAGE_TAG" ]; then docker image rm "$image:$IMAGE_TAG" 2>/dev/null || true; fi
                done
            '''
        }
        success { echo 'Microservices CI passed. Kubernetes delivery remains a separate Argo CD GitOps operation.' }
        failure { echo 'Pipeline failed. Inspect the first failing stage and archived reports before retrying.' }
    }
}
