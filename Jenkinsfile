pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timeout(time: 45, unit: 'MINUTES')
    }

    parameters {
        booleanParam(
            name: 'PUBLISH_IMAGES',
            defaultValue: false,
            description: 'Push successfully tested and scanned immutable images to Docker Hub.'
        )
    }

    environment {
        BACKEND_LOCAL_IMAGE = 'three-tier-todo-backend'
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

        stage('Backend Tests') {
            steps {
                dir('backend') {
                    sh 'mvn -B clean verify'
                }
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

        stage('Build Container Images') {
            steps {
                sh '''
                    set -eu
                    docker build --pull -t "$BACKEND_LOCAL_IMAGE:$IMAGE_TAG" ./backend
                    docker build --pull -t "$FRONTEND_LOCAL_IMAGE:$IMAGE_TAG" ./frontend
                '''
            }
        }

        stage('Container Vulnerability Scan') {
            steps {
                sh '''
                    set -eu
                    trivy image --no-progress --exit-code 1 --severity HIGH,CRITICAL "$BACKEND_LOCAL_IMAGE:$IMAGE_TAG"
                    trivy image --no-progress --exit-code 1 --severity HIGH,CRITICAL "$FRONTEND_LOCAL_IMAGE:$IMAGE_TAG"
                '''
            }
        }

        stage('Publish Immutable Images') {
            when {
                expression { return params.PUBLISH_IMAGES }
            }
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-creds',
                    usernameVariable: 'DOCKER_USERNAME',
                    passwordVariable: 'DOCKER_PASSWORD'
                )]) {
                    sh '''
                        set +x
                        set -eu
                        echo "$DOCKER_PASSWORD" | docker login --username "$DOCKER_USERNAME" --password-stdin
                        docker tag "$BACKEND_LOCAL_IMAGE:$IMAGE_TAG" "$DOCKER_USERNAME/$BACKEND_LOCAL_IMAGE:$IMAGE_TAG"
                        docker tag "$FRONTEND_LOCAL_IMAGE:$IMAGE_TAG" "$DOCKER_USERNAME/$FRONTEND_LOCAL_IMAGE:$IMAGE_TAG"
                        docker push "$DOCKER_USERNAME/$BACKEND_LOCAL_IMAGE:$IMAGE_TAG"
                        docker push "$DOCKER_USERNAME/$FRONTEND_LOCAL_IMAGE:$IMAGE_TAG"
                        docker logout
                    '''
                }
                echo 'Images published with immutable build-and-commit tags.'
                echo 'Promote by updating helm/taskmanager/values-argocd.yaml in a reviewed Git commit or pull request.'
            }
        }
    }

    post {
        always {
            sh '''
                if [ -n "$IMAGE_TAG" ]; then
                    docker image rm "$BACKEND_LOCAL_IMAGE:$IMAGE_TAG" 2>/dev/null || true
                    docker image rm "$FRONTEND_LOCAL_IMAGE:$IMAGE_TAG" 2>/dev/null || true
                fi
            '''
        }
        success {
            echo 'CI pipeline completed. Argo CD owns Kubernetes delivery; promote only a validated image tag through Git.'
        }
        failure {
            echo 'Pipeline failed. Inspect the first failing stage and preserve logs before retrying.'
        }
    }
}
