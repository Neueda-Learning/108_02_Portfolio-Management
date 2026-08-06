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
            def v2Status = sh(script: 'docker compose version >/dev/null 2>&1', returnStatus: true)
            def v1Status = sh(script: 'docker-compose version >/dev/null 2>&1', returnStatus: true)

            if (v2Status == 0) {
              env.COMPOSE_CMD = 'docker compose'
            } else if (v1Status == 0) {
              env.COMPOSE_CMD = 'docker-compose'
            } else {
              error("Neither `docker compose` nor `docker-compose` is available on this Jenkins agent. v2Status=${v2Status}, v1Status=${v1Status}")
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
