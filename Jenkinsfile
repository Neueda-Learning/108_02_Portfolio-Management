// pipeline {
//   agent any
//
//   options {
// 	timestamps()
// 	disableConcurrentBuilds()
// 	skipDefaultCheckout(true)
//   }
//
//   environment {
// 	REGISTRY = 'docker.io'
// 	REGISTRY_NAMESPACE = 'admin'
// 	CHECKOUT_DIR = 'repo'
// 	BACKEND_REPO = 'portfolio-backend'
// 	FRONTEND_REPO = 'portfolio-frontend'
//   }
//
//   stages {
// 	stage('Checkout') {
// 	  steps {
// 		dir(env.CHECKOUT_DIR) {
// 		  checkout scm
// 		}
// 	  }
// 	}
//
// 	stage('Build backend jar') {
// 	  steps {
// 		script {
// 		  dir(env.CHECKOUT_DIR) {
// 			if (isUnix()) {
// 			  sh 'chmod +x mvnw && ./mvnw -B -Dmaven.test.skip=true clean package'
// 			} else {
// 			  bat 'mvnw.cmd -B -Dmaven.test.skip=true clean package'
// 			}
// 		  }
// 		}
// 	  }
// 	}
//
// 	stage('Build frontend assets') {
// 	  steps {
// 		script {
// 		  dir(env.CHECKOUT_DIR) {
// 			if (isUnix()) {
// 			  sh "docker build -f frontend/Dockerfile -t ${env.REGISTRY}/${env.REGISTRY_NAMESPACE}/${env.FRONTEND_REPO}:frontend-check ."
// 			} else {
// 			  bat "docker build -f frontend/Dockerfile -t ${env.REGISTRY}/${env.REGISTRY_NAMESPACE}/${env.FRONTEND_REPO}:frontend-check ."
// 			}
// 		  }
// 		}
// 	  }
// 	}
//
// 	stage('Build Docker images') {
// 	  steps {
// 		script {
// 		  dir(env.CHECKOUT_DIR) {
// 			def shortCommit = env.GIT_COMMIT ? env.GIT_COMMIT.take(7) : 'local'
// 			env.IMAGE_TAG = "${env.BUILD_NUMBER}-${shortCommit}"
// 			env.BACKEND_IMAGE = "${env.REGISTRY}/${env.REGISTRY_NAMESPACE}/${env.BACKEND_REPO}"
// 			env.FRONTEND_IMAGE = "${env.REGISTRY}/${env.REGISTRY_NAMESPACE}/${env.FRONTEND_REPO}"
//
// 			if (isUnix()) {
// 			  sh "docker build -f Dockerfile.backend -t ${env.BACKEND_IMAGE}:${env.IMAGE_TAG} ."
// 			  sh "docker build -f frontend/Dockerfile -t ${env.FRONTEND_IMAGE}:${env.IMAGE_TAG} ."
// 			} else {
// 			  bat "docker build -f Dockerfile.backend -t ${env.BACKEND_IMAGE}:${env.IMAGE_TAG} ."
// 			  bat "docker build -f frontend/Dockerfile -t ${env.FRONTEND_IMAGE}:${env.IMAGE_TAG} ."
// 			}
// 		  }
// 		}
// 	  }
// 	}
//
// 	stage('Push images') {
// 	  steps {
// 		withCredentials([usernamePassword(credentialsId: 'admin', usernameVariable: 'admin', passwordVariable: 'n3u3da!')]) {
// 		  script {
// 			dir(env.CHECKOUT_DIR) {
// 			  if (isUnix()) {
// 				sh 'echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin'
// 				sh "docker push ${env.BACKEND_IMAGE}:${env.IMAGE_TAG}"
// 				sh "docker push ${env.FRONTEND_IMAGE}:${env.IMAGE_TAG}"
// 			  } else {
// 				bat 'echo %DOCKER_PASS% | docker login -u %DOCKER_USER% --password-stdin'
// 				bat "docker push ${env.BACKEND_IMAGE}:${env.IMAGE_TAG}"
// 				bat "docker push ${env.FRONTEND_IMAGE}:${env.IMAGE_TAG}"
// 			  }
// 			}
// 		  }
// 		}
// 	  }
// 	}
//
// 	stage('Deploy (main only)') {
// 	  when {
// 		branch 'main'
// 	  }
// 	  steps {
// 		withCredentials([
// 		  string(credentialsId: 'portfolio-db-password', variable: 'DB_PASSWORD'),
// 		  string(credentialsId: 'portfolio-mysql-root-password', variable: 'MYSQL_ROOT_PASSWORD')
// 		]) {
// 		  script {
// 			dir(env.CHECKOUT_DIR) {
// 			  if (isUnix()) {
// 				sh '''
// cat > .env <<EOF
// BACKEND_IMAGE=${REGISTRY}/${REGISTRY_NAMESPACE}/${BACKEND_REPO}
// FRONTEND_IMAGE=${REGISTRY}/${REGISTRY_NAMESPACE}/${FRONTEND_REPO}
// IMAGE_TAG=${IMAGE_TAG}
// DB_NAME=portfolio_db
// DB_USER=root
// DB_PASSWORD=${DB_PASSWORD}
// MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
// EOF
//
// docker compose --env-file .env -f docker-compose.prod.yml pull
// docker compose --env-file .env -f docker-compose.prod.yml up -d
// '''
// 			  } else {
// 				bat '''
// (
//   echo BACKEND_IMAGE=%REGISTRY%/%REGISTRY_NAMESPACE%/%BACKEND_REPO%
//   echo FRONTEND_IMAGE=%REGISTRY%/%REGISTRY_NAMESPACE%/%FRONTEND_REPO%
//   echo IMAGE_TAG=%IMAGE_TAG%
//   echo DB_NAME=portfolio_db
//   echo DB_USER=root
//   echo DB_PASSWORD=%DB_PASSWORD%
//   echo MYSQL_ROOT_PASSWORD=%MYSQL_ROOT_PASSWORD%
// ) > .env
//
// docker compose --env-file .env -f docker-compose.prod.yml pull
// docker compose --env-file .env -f docker-compose.prod.yml up -d
// '''
// 			  }
// 			}
// 		  }
// 		}
// 	  }
// 	}
//   }
//
//   post {
// 	always {
// 	  script {
// 		dir(env.CHECKOUT_DIR) {
// 		  if (isUnix()) {
// 			sh 'docker logout || true'
// 		  } else {
// 			bat 'docker logout'
// 		  }
// 		}
// 	  }
// 	}
//   }
// }

pipeline {

    agent any

    environment {
        GIT_URL = 'https://github.com/Neueda-Learning/108_02_Portfolio-Management.git'
        BRANCH = 'main'
    }

    stages {

        stage('Checkout Source') {
            steps {
                git branch: "${BRANCH}",
                    url: "${GIT_URL}"
            }
        }

        stage('Stop Existing Containers') {
            steps {
                sh 'docker-compose down || true'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh 'docker-compose build --no-cache'
            }
        }

        stage('Deploy') {
            steps {
                sh 'docker-compose up -d'
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