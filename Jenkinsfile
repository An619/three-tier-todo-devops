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
            description: 'Push scanned images to Docker Hub. Required automatically for Kubernetes deployments.'
        )
        choice(
            name: 'DEPLOY_ENV',
            choices: ['none', 'killercoda', 'eks'],
            description: 'Deployment target. Select none for CI-only validation.'
        )
    }

    environment {
        BACKEND_LOCAL_IMAGE = 'three-tier-todo-backend'
        FRONTEND_LOCAL_IMAGE = 'three-tier-todo-frontend'
        IMAGE_TAG = ''
        DOCKERHUB_USERNAME = ''
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

        stage('Push Images') {
            when {
                expression { return params.PUBLISH_IMAGES || params.DEPLOY_ENV != 'none' }
            }
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-creds',
                    usernameVariable: 'DOCKER_USERNAME',
                    passwordVariable: 'DOCKER_PASSWORD'
                )]) {
                    script {
                        env.DOCKERHUB_USERNAME = env.DOCKER_USERNAME
                    }
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
            }
        }

        stage('Deploy with Helm') {
            when {
                expression { return params.DEPLOY_ENV != 'none' }
            }
            steps {
                script {
                    def kubeCredential = "kubeconfig-${params.DEPLOY_ENV}"
                    withCredentials([
                        file(credentialsId: kubeCredential, variable: 'KUBECONFIG'),
                        string(credentialsId: 'mysql-root-password', variable: 'MYSQL_ROOT_PASSWORD'),
                        string(credentialsId: 'mysql-app-password', variable: 'MYSQL_APP_PASSWORD')
                    ]) {
                        sh '''
                            set +x
                            set -eu
                            kubectl create namespace taskmanager --dry-run=client -o yaml | kubectl apply -f -
                            secret_dir="$(mktemp -d)"
                            trap 'rm -rf "$secret_dir"' EXIT
                            umask 077
                            printf '%s' "$MYSQL_ROOT_PASSWORD" > "$secret_dir/mysql-root-password"
                            printf '%s' 'taskapp' > "$secret_dir/db-username"
                            printf '%s' "$MYSQL_APP_PASSWORD" > "$secret_dir/db-password"
                            kubectl create secret generic taskmanager-db -n taskmanager \
                              --from-file=mysql-root-password="$secret_dir/mysql-root-password" \
                              --from-file=db-username="$secret_dir/db-username" \
                              --from-file=db-password="$secret_dir/db-password" \
                              --dry-run=client -o yaml | kubectl apply -f -
                            helm lint ./helm/taskmanager -f "./helm/taskmanager/values-$DEPLOY_ENV.yaml"
                            helm upgrade --install todo ./helm/taskmanager \
                              --namespace taskmanager --create-namespace \
                              -f "./helm/taskmanager/values-$DEPLOY_ENV.yaml" \
                              --set image.backend.repository="$DOCKERHUB_USERNAME/three-tier-todo-backend" \
                              --set image.backend.tag="$IMAGE_TAG" \
                              --set image.frontend.repository="$DOCKERHUB_USERNAME/three-tier-todo-frontend" \
                              --set image.frontend.tag="$IMAGE_TAG" \
                              --wait --timeout 10m
                            kubectl rollout status deployment/todo-taskmanager-backend -n taskmanager --timeout=300s
                            kubectl rollout status deployment/todo-taskmanager-frontend -n taskmanager --timeout=300s
                            kubectl get pods,services -n taskmanager -o wide
                        '''
                    }
                }
            }
        }
    }

    post {
        always {
            sh '''
                docker image rm "$BACKEND_LOCAL_IMAGE:$IMAGE_TAG" 2>/dev/null || true
                docker image rm "$FRONTEND_LOCAL_IMAGE:$IMAGE_TAG" 2>/dev/null || true
            '''
        }
        success {
            echo 'Pipeline completed. Review the test and scan results before promoting this build.'
        }
        failure {
            echo 'Pipeline failed. Inspect the first failing stage and preserve logs before retrying.'
        }
    }
}
