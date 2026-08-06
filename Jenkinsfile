pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
    skipDefaultCheckout(true)
  }

  parameters {
    string(name: 'GIT_URL', defaultValue: 'https://github.com/Neueda-Learning/108_02_Portfolio-Management.git', description: 'Repository URL')
    string(name: 'BRANCH', defaultValue: 'main', description: 'Git branch to build/deploy')
    string(name: 'REGISTRY', defaultValue: 'ghcr.io', description: 'Container registry host')
    string(name: 'REGISTRY_NAMESPACE', defaultValue: 'sammed1174t', description: 'Registry namespace/user (GitHub org or username)')
    string(name: 'GITHUB_CREDENTIALS_ID', defaultValue: 'github-creds', description: 'Jenkins credentials ID for GitHub Container Registry (PAT with write:packages)')
    string(name: 'BACKEND_HOST_PORT', defaultValue: '8081', description: 'Host port mapped to backend container port 8080')
  }

  environment {
    CHECKOUT_DIR = '.'
    BACKEND_REPO = 'portfolio-backend'
    FRONTEND_REPO = 'portfolio-frontend'
  }

  stages {

    stage('Workspace') {
      steps {
        echo 'Using existing workspace content; no clone stage will run.'
      }
    }

    stage('Detect Compose CLI') {
      steps {
        script {
          def v2Status = sh(script: 'docker compose version >/dev/null 2>&1', returnStatus: true)
          def v1Status = sh(script: 'docker-compose version >/dev/null 2>&1', returnStatus: true)
          if (v2Status != 0 && v1Status != 0) {
            error("Neither docker compose nor docker-compose is available on this Jenkins agent.")
          }
          echo "Compose CLI detected (v2=${v2Status == 0})"
        }
      }
    }

    stage('Build Backend Jar') {
      steps {
        dir(env.CHECKOUT_DIR) {
          sh 'chmod +x mvnw && ./mvnw -B -Dmaven.test.skip=true clean package'
        }
      }
    }

    stage('Build Frontend Assets') {
      steps {
        dir(env.CHECKOUT_DIR) {
          sh 'docker build --target build -f frontend/Dockerfile -t frontend-build-check:${BUILD_NUMBER} .'
        }
      }
    }

    stage('Build Docker Images') {
      steps {
        script {
          dir(env.CHECKOUT_DIR) {
            def shortCommit = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
            env.IMAGE_TAG = "${env.BUILD_NUMBER}-${shortCommit}"
            def registryNamespace = (params.REGISTRY_NAMESPACE ?: '').toLowerCase()
            if (!registryNamespace) {
              error('REGISTRY_NAMESPACE cannot be empty.')
            }
            env.BACKEND_IMAGE = "${params.REGISTRY}/${registryNamespace}/${env.BACKEND_REPO}"
            env.FRONTEND_IMAGE = "${params.REGISTRY}/${registryNamespace}/${env.FRONTEND_REPO}"

            sh "docker build -f Dockerfile.backend -t ${env.BACKEND_IMAGE}:${env.IMAGE_TAG} ."
            sh "docker build -f frontend/Dockerfile -t ${env.FRONTEND_IMAGE}:${env.IMAGE_TAG} ."
          }
        }
      }
    }

    stage('Push Docker Images') {
      steps {
        script {
          withCredentials([usernamePassword(credentialsId: params.GITHUB_CREDENTIALS_ID, usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
            sh 'printf "%s" "$DOCKER_PASS" | docker login ' + params.REGISTRY + ' -u "$DOCKER_USER" --password-stdin'
            sh "docker push ${env.BACKEND_IMAGE}:${env.IMAGE_TAG}"
            sh "docker push ${env.FRONTEND_IMAGE}:${env.IMAGE_TAG}"
          }
        }
      }
    }

    stage('Deploy (main only)') {
      when {
        expression { params.BRANCH == 'main' }
      }
      steps {
        dir(env.CHECKOUT_DIR) {
          sh '''
cat > .env <<EOF
BACKEND_IMAGE=${BACKEND_IMAGE}
FRONTEND_IMAGE=${FRONTEND_IMAGE}
IMAGE_TAG=${IMAGE_TAG}
DB_NAME=portfolio_db
DB_USER=root
DB_PASSWORD=n3u3da!
MYSQL_ROOT_PASSWORD=n3u3da!
MYSQL_ROOT_HOST=%
BACKEND_HOST_PORT=${BACKEND_HOST_PORT}
EOF
if docker compose version >/dev/null 2>&1; then
  COMPOSE_BIN="docker compose"
else
  COMPOSE_BIN="docker-compose"
fi
$COMPOSE_BIN -f docker-compose.prod.yml pull
$COMPOSE_BIN -f docker-compose.prod.yml up -d --remove-orphans
'''
        }
      }
    }

    stage('Verify') {
      steps {
        sh 'docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"'
      }
    }
  }

  post {
    always {
      script {
        sh 'docker logout ghcr.io || true'
      }
    }
  }
}

