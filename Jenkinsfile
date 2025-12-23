pipeline {
    agent { label 'linux-agent' }

    parameters {
        choice(name: 'ACTION', choices: ['Deploy New Version', 'Rollback'], description: 'Build/Deploy or Rollback.')
        string(name: 'VERSION_TAG', defaultValue: 'v1.0.0', description: 'Docker Tag (e.g., v1.0.0)')
    }

    environment {
        // --- CONFIGURATION ---
        NEXUS_REGISTRY = 'registry.nchldemo.com'
        IMAGE_NAME     = 'e2e-app-trainer'
        NEXUS_CRED     = 'nexus-auth'
        CONTAINER_NAME = 'e2e-app-trainer'
        APP_PORT       = '9091' // The port exposed to the host
    }

    tools {
        maven 'maven3'
    }

    stages {
        // --- STAGE 1: Unit Tests
        stage('Unit Tests') {
            when { expression { params.ACTION == 'Deploy New Version' } }
            steps {
                script {
                    echo "--- 1. Compiling & Testing (JDK 21) ---"
                    sh 'mvn clean test' 
                }
            }
        }

        // --- STAGE 2: Build & Package ---
        stage('Build Artifact & Image') {
            when { expression { params.ACTION == 'Deploy New Version' } }
            steps {
                script {
                    echo "--- 2. Packaging JAR & Building Docker Image ---"
                    sh 'mvn package -DskipTests'
                    
                    echo "Building Docker Image: ${NEXUS_REGISTRY}/${IMAGE_NAME}:${params.VERSION_TAG}"
                    docker.build("${NEXUS_REGISTRY}/${IMAGE_NAME}:${params.VERSION_TAG}")
                }
            }
        }

        // --- STAGE 3: Security Scan (Trivy) ---
        stage('Trivy Security Scan') {
            when { expression { params.ACTION == 'Deploy New Version' } }
            steps {
                script {
                    echo "--- 3. Scanning for Vulnerabilities ---"
                    sh """
                        docker run --rm \
                        -v /var/run/docker.sock:/var/run/docker.sock \
                        aquasec/trivy image \
                        --severity HIGH,CRITICAL \
                        --exit-code 0 \
                        ${NEXUS_REGISTRY}/${IMAGE_NAME}:${params.VERSION_TAG}
                    """
                }
            }
        }

        // --- STAGE 4: Push to Nexus ---
        stage('Push to Nexus') {
            when { expression { params.ACTION == 'Deploy New Version' } }
            steps {
                script {
                    echo "--- 4. Pushing to Registry ---"
                    docker.withRegistry("http://${NEXUS_REGISTRY}", "${NEXUS_CRED}") {
                        docker.image("${NEXUS_REGISTRY}/${IMAGE_NAME}:${params.VERSION_TAG}").push()
                    }
                }
            }
        }

        // --- STAGE 5: Deploy Container ---
        stage('Deploy to Environment') {
            steps {
                script {
                    echo "--- 5. Deploying Container on Port ${APP_PORT} ---"
                    
                    // Remove existing container
                    sh "docker rm -f ${CONTAINER_NAME} || true"

                    docker.withRegistry("http://${NEXUS_REGISTRY}", "${NEXUS_CRED}") {
                        sh "docker pull ${NEXUS_REGISTRY}/${IMAGE_NAME}:${params.VERSION_TAG}"
                        
                        sh """
                            docker run -d \
                            --name ${CONTAINER_NAME} \
                            -p ${APP_PORT}:8080 \
                            ${NEXUS_REGISTRY}/${IMAGE_NAME}:${params.VERSION_TAG}
                        """
                    }
                }
            }
        }

        // --- STAGE 6: Automated QA (API & E2E) ---
        stage('Automated QA') {
            steps {
                script {
                    echo "Waiting for Spring Boot to initialize..."
                    sh "sleep 10"

                    // Dynamic Port Replacement for Tests
                    // We swap '8080' in the test files with our actual running port '9091'
                    sh "sed -i 's/8080/${APP_PORT}/g' tests/api_collection.json || true"
                    // For python, we can just edit the file or pass args. 
                    // Simple hack: update the hardcoded port in the python script too
                    sh "sed -i 's/8080/${APP_PORT}/g' tests/e2e_test.py || true"

                    echo "--- Running API Tests (Newman) ---"
                    sh "newman run tests/api_collection.json"

                    echo "--- Running E2E Tests (Selenium) ---"
                    sh "python3 tests/e2e_test.py"
                }
            }
        }
    }

    post {
        always {
            script {
                echo '--- Cleanup ---'
                sh "docker image prune -f"
                // Clean up the image tag to save disk space
                sh "docker rmi ${NEXUS_REGISTRY}/${IMAGE_NAME}:${params.VERSION_TAG} || true"
            }
        }
        failure {
            echo 'Build Failed. Stopping Container.'
            sh "docker rm -f ${CONTAINER_NAME} || true"
        }
    }
}