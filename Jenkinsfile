pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
    // Prevent Jenkins from checking out into a polluted workspace root.
    skipDefaultCheckout(true)
  }

  environment {
    GIT_URL = 'https://github.com/Neueda-Learning/108_02_Portfolio-Management.git'
    BRANCH = 'main'
    CHECKOUT_DIR = 'repo'
    COMPOSE_CMD = ''
  }

  stages {
    stage('Checkout Source') {
      steps {
        dir(env.CHECKOUT_DIR) {
          git branch: "${BRANCH}", url: "${GIT_URL}"
        }
      }
    }

    stage('Detect Compose CLI') {
      steps {
        script {
          dir(env.CHECKOUT_DIR) {
            env.COMPOSE_CMD = sh(
              script: '''
if docker compose version >/dev/null 2>&1; then
  echo "docker compose"
elif command -v docker-compose >/dev/null 2>&1; then
  echo "docker-compose"
fi
''',
              returnStdout: true
            ).trim()

            if (!env.COMPOSE_CMD) {
              error('Neither `docker compose` nor `docker-compose` is available on this Jenkins agent.')
            }

            echo "Using Compose command: ${env.COMPOSE_CMD}"
          }
        }
      }
    }

    stage('Stop Existing Containers') {
      steps {
        dir(env.CHECKOUT_DIR) {
          sh "${env.COMPOSE_CMD} down || true"
        }
      }
    }

    stage('Build Docker Images') {
      steps {
        dir(env.CHECKOUT_DIR) {
          sh "${env.COMPOSE_CMD} build --no-cache"
        }
      }
    }

    stage('Deploy') {
      steps {
        dir(env.CHECKOUT_DIR) {
          sh "${env.COMPOSE_CMD} up -d"
        }
      }
    }

    stage('Verify') {
      steps {
        sh 'docker ps'
      }
    }
  }
}
