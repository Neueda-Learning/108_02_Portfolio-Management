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
        COMPOSE_CMD = ''
    }

    stages {

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
                script {
                    def v2Status = sh(script: 'docker compose version >/dev/null 2>&1', returnStatus: true)
                    def v1Status = sh(script: 'docker-compose version >/dev/null 2>&1', returnStatus: true)
                    if (v2Status == 0) {
                        env.COMPOSE_CMD = 'docker compose'
                    } else if (v1Status == 0) {
                        env.COMPOSE_CMD = 'docker-compose'
                    } else {
                        error('Neither docker compose nor docker-compose is available on this Jenkins agent.')
                    }
                    echo "Using compose command: ${env.COMPOSE_CMD}"
                }
            }
        }

        stage('Stop Existing Containers') {
            steps {
                dir("${CHECKOUT_DIR}") {
                    sh '${COMPOSE_CMD} down || true'
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                dir("${CHECKOUT_DIR}") {
                    sh '${COMPOSE_CMD} build --no-cache'
                }
            }
        }

        stage('Deploy') {
            steps {
                dir("${CHECKOUT_DIR}") {
                    sh '${COMPOSE_CMD} up -d'
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