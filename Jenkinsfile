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
  }

  stages {
    stage('Checkout Source') {
      steps {
        dir(env.CHECKOUT_DIR) {
          git branch: "${BRANCH}", url: "${GIT_URL}"
        }
      }
    }

    stage('Stop Existing Containers') {
      steps {
        dir(env.CHECKOUT_DIR) {
          sh 'docker compose down || true'
        }
      }
    }

    stage('Build Docker Images') {
      steps {
        dir(env.CHECKOUT_DIR) {
          sh 'docker compose build --no-cache'
        }
      }
    }

    stage('Deploy') {
      steps {
        dir(env.CHECKOUT_DIR) {
          sh 'docker compose up -d'
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
