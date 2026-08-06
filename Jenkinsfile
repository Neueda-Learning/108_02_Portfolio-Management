pipeline {
  agent any

  options {
	timestamps()
	disableConcurrentBuilds()
  }

  environment {
	REGISTRY = 'docker.io'
	REGISTRY_NAMESPACE = 'your-dockerhub-username'
	BACKEND_REPO = 'portfolio-backend'
	FRONTEND_REPO = 'portfolio-frontend'
  }

  stages {
	stage('Checkout') {
	  steps {
		checkout scm
	  }
	}

	stage('Build backend jar') {
	  steps {
		script {
		  if (isUnix()) {
			sh 'chmod +x mvnw && ./mvnw -B -Dmaven.test.skip=true clean package'
		  } else {
			bat 'mvnw.cmd -B -Dmaven.test.skip=true clean package'
		  }
		}
	  }
	}

	stage('Build frontend assets') {
	  steps {
		script {
		  if (isUnix()) {
			sh 'docker run --rm -v "$PWD/frontend:/app" -w /app node:20-alpine sh -lc "npm ci --no-audit --no-fund && npm run build"'
		  } else {
			bat 'docker run --rm -v "%cd%\\frontend:/app" -w /app node:20-alpine sh -lc "npm ci --no-audit --no-fund && npm run build"'
		  }
		}
	  }
	}

	stage('Build Docker images') {
	  steps {
		script {
		  def shortCommit = env.GIT_COMMIT ? env.GIT_COMMIT.take(7) : 'local'
		  env.IMAGE_TAG = "${env.BUILD_NUMBER}-${shortCommit}"
		  env.BACKEND_IMAGE = "${env.REGISTRY}/${env.REGISTRY_NAMESPACE}/${env.BACKEND_REPO}"
		  env.FRONTEND_IMAGE = "${env.REGISTRY}/${env.REGISTRY_NAMESPACE}/${env.FRONTEND_REPO}"

		  if (isUnix()) {
			sh "docker build -f Dockerfile.backend -t ${env.BACKEND_IMAGE}:${env.IMAGE_TAG} ."
			sh "docker build -f frontend/Dockerfile -t ${env.FRONTEND_IMAGE}:${env.IMAGE_TAG} ."
		  } else {
			bat "docker build -f Dockerfile.backend -t ${env.BACKEND_IMAGE}:${env.IMAGE_TAG} ."
			bat "docker build -f frontend/Dockerfile -t ${env.FRONTEND_IMAGE}:${env.IMAGE_TAG} ."
		  }
		}
	  }
	}

	stage('Push images') {
	  steps {
		withCredentials([usernamePassword(credentialsId: 'dockerhub-creds', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
		  script {
			if (isUnix()) {
			  sh 'echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin'
			  sh "docker push ${env.BACKEND_IMAGE}:${env.IMAGE_TAG}"
			  sh "docker push ${env.FRONTEND_IMAGE}:${env.IMAGE_TAG}"
			} else {
			  bat 'echo %DOCKER_PASS% | docker login -u %DOCKER_USER% --password-stdin'
			  bat "docker push ${env.BACKEND_IMAGE}:${env.IMAGE_TAG}"
			  bat "docker push ${env.FRONTEND_IMAGE}:${env.IMAGE_TAG}"
			}
		  }
		}
	  }
	}

	stage('Deploy (main only)') {
	  when {
		branch 'main'
	  }
	  steps {
		withCredentials([
		  string(credentialsId: 'portfolio-db-password', variable: 'DB_PASSWORD'),
		  string(credentialsId: 'portfolio-mysql-root-password', variable: 'MYSQL_ROOT_PASSWORD')
		]) {
		  script {
			if (isUnix()) {
			  sh '''
cat > .env <<EOF
BACKEND_IMAGE=${REGISTRY}/${REGISTRY_NAMESPACE}/${BACKEND_REPO}
FRONTEND_IMAGE=${REGISTRY}/${REGISTRY_NAMESPACE}/${FRONTEND_REPO}
IMAGE_TAG=${IMAGE_TAG}
DB_NAME=portfolio_db
DB_USER=portfolio
DB_PASSWORD=${DB_PASSWORD}
MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
EOF

docker compose --env-file .env -f docker-compose.prod.yml pull
docker compose --env-file .env -f docker-compose.prod.yml up -d
'''
			} else {
			  bat '''
(
  echo BACKEND_IMAGE=%REGISTRY%/%REGISTRY_NAMESPACE%/%BACKEND_REPO%
  echo FRONTEND_IMAGE=%REGISTRY%/%REGISTRY_NAMESPACE%/%FRONTEND_REPO%
  echo IMAGE_TAG=%IMAGE_TAG%
  echo DB_NAME=portfolio_db
  echo DB_USER=portfolio
  echo DB_PASSWORD=%DB_PASSWORD%
  echo MYSQL_ROOT_PASSWORD=%MYSQL_ROOT_PASSWORD%
) > .env

docker compose --env-file .env -f docker-compose.prod.yml pull
docker compose --env-file .env -f docker-compose.prod.yml up -d
'''
			}
		  }
		}
	  }
	}
  }

  post {
	always {
	  script {
		if (isUnix()) {
		  sh 'docker logout || true'
		} else {
		  bat 'docker logout'
		}
	  }
	}
  }
}

