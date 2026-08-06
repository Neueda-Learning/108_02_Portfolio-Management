pipeline {

    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
    }

    environment {
        GIT_URL = 'https://github.com/Neueda-Learning/108_02_Portfolio-Management.git'
        BRANCH = 'main'
        CHECKOUT_DIR = 'repo'
    }

    stages {

        stage('Prepare Workspace') {
            steps {
                script {
                    // Isolate each run from stale root-owned files in old workspaces.
                    env.CHECKOUT_DIR = "repo-${env.BUILD_NUMBER}"
                    echo "Using checkout directory: ${env.CHECKOUT_DIR}"
                }
            }
        }

        stage('Checkout Source') {
            steps {
                dir("${CHECKOUT_DIR}") {
                    git branch: "${BRANCH}",
                        url: "${GIT_URL}"
                }
            }
        }

        stage('Detect Compose CLI') {
            steps {
                sh '''
if docker compose version >/dev/null 2>&1; then
  echo "Using compose command: docker compose"
elif docker-compose version >/dev/null 2>&1; then
  echo "Using compose command: docker-compose"
else
  echo "Neither docker compose nor docker-compose is available on this Jenkins agent."
  exit 1
fi
'''
            }
        }

        stage('Stop Existing Containers') {
            steps {
                dir("${CHECKOUT_DIR}") {
                    sh '''
if docker compose version >/dev/null 2>&1; then
  docker compose down || true
else
  docker-compose down || true
fi
'''
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                dir("${CHECKOUT_DIR}") {
                    sh '''
if docker compose version >/dev/null 2>&1; then
  docker compose build --no-cache
else
  docker-compose build --no-cache
fi
'''
                }
            }
        }

        stage('Deploy') {
            steps {
                dir("${CHECKOUT_DIR}") {
                    sh '''
if docker compose version >/dev/null 2>&1; then
  docker compose up -d
else
  docker-compose up -d
fi
'''
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
// pipeline {
//     agent any
//     stages {
//     stage('checkout') {
//         steps{
//             git branch: 'main',
//             url: 'https://github.com/neueda-learning/kk-04-rest-sol.git'
//         }
//     }
//     stage ('Environment'){
//     environment {
//         JAVA_HOME = '/usr/lib/jvm/java-21-amazon-corretto.x86_64'
//         PATH = "${JAVA_HOME}/bin:${PATH}"
//     }
//     steps{
//             echo 'Java Version'
//             sh 'java -version'
//             echo 'Javac Version'
//             sh 'javac -version'
//             echo 'Maven Version'
//             sh 'mvn -version'
//             echo 'JAVA_HOME'
//             sh 'echo $JAVA_HOME'
//             echo 'PATH'
//             sh 'echo $PATH'
//         }
//     }
//     stage('Build') {
//         steps {
//             echo 'building...'
//             // Add test steps here
// //             sh 'mvn clean package -DskipTests'
//         }
//     }
//         stage('Deploy') {
//             steps {
//                 echo 'Deploying...'
//                 // Add deploy steps here
//                 sh 'docker compose down || true'
//                 sh 'docker compose up -d --build'
//             }
//         }
//     }
// }