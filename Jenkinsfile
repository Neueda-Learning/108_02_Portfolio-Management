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
  }

  environment {
    CHECKOUT_DIR = '.'
    BACKEND_REPO = 'portfolio-backend'
    FRONTEND_REPO = 'portfolio-frontend'
  }

  stages {

    stage('Workspace') {
      steps {
        echo 'PIPELINE_REV=2026-08-06-8081-only'
        dir(env.CHECKOUT_DIR) {
          sh '''
if [ ! -d .git ]; then
  echo "Workspace is missing .git metadata. Please run this job as Pipeline from SCM or prepare a git workspace."
  exit 1
fi
git fetch --all --prune
git checkout "$BRANCH"
git reset --hard "origin/$BRANCH"
'''
        }
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
BACKEND_HOST_PORT=8081
EOF
if docker compose version >/dev/null 2>&1; then
  COMPOSE_BIN="docker compose"
else
  COMPOSE_BIN="docker-compose"
fi
# Safety check: fail fast if the workspace still has legacy 8080 backend mappings.
# Auto-correct stale workspace files that may still contain old 8080 mappings.
sed -i 's/8080:8080/8081:8081/g' docker-compose.prod.yml || true
sed -i 's#backend:8080#backend:8081#g' frontend/nginx.conf || true
if grep -Eq '8080:8080|backend:8080' docker-compose.prod.yml frontend/nginx.conf; then
  echo 'Legacy 8080 backend config still present after auto-correction.'
  echo '--- docker-compose.prod.yml backend snippet ---'
  sed -n '/backend:/,/frontend:/p' docker-compose.prod.yml || true
  echo '--- frontend/nginx.conf api snippet ---'
  awk 'index($0,"location /api/"),/}/' frontend/nginx.conf || true
  exit 1
fi
# Free the previous backend container name in case it is left behind from older runs.
docker rm -f portfolio-backend >/dev/null 2>&1 || true

# Show effective backend port mapping so pipeline logs clearly indicate the bound host port.
$COMPOSE_BIN -f docker-compose.prod.yml config | sed -n '/backend:/,/frontend:/p'

# Best-effort visibility into existing listeners for troubleshooting bind failures.
ss -ltn 2>/dev/null | grep -E ':80 |:8081 ' || true

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

