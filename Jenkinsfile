pipeline {
    agent { label 'linux-agent' }

    parameters {
        choice(name: 'ACTION', choices: ['Deploy New Version', 'Rollback'], description: 'Build/Deploy or Rollback.')
        string(name: 'VERSION_TAG', defaultValue: 'v1.0.0', description: 'Docker Tag (e.g., v1.0.0)')
    }

    environment {
        // --- CONFIGURATION ---
        NEXUS_REGISTRY = 'registry.nchldemo.com'
        IMAGE_NAME     = 'e2e-app-sapanaji'
        NEXUS_CRED     = 'nexus-auth'
        CONTAINER_NAME = 'e2e-app-sapanaji'
        APP_PORT       = '8089' // Add your unique port here
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
        // stage('Trivy Security Scan') {
        //     when { expression { params.ACTION == 'Deploy New Version' } }
        //     steps {
        //         script {
        //             echo "--- 3. Scanning for Vulnerabilities ---"
        //             sh """
        //                 docker run --rm \
        //                 -v /var/run/docker.sock:/var/run/docker.sock \
        //                 aquasec/trivy image \
        //                 --severity HIGH,CRITICAL \
        //                 --exit-code 0 \
        //                 ${NEXUS_REGISTRY}/${IMAGE_NAME}:${params.VERSION_TAG}
        //             """
        //         }
        //     }
        // }

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

                    // Port Replacement (Same as before)
                    sh "sed -i 's/8080/${APP_PORT}/g' tests/api_collection.json || true"
                    sh "sed -i 's/8080/${APP_PORT}/g' tests/e2e_test.py || true"

                    echo "--- Running API Tests (Newman) ---"
                    // Add --reporters junit to generate API XML reports too!
                    sh "newman run tests/api_collection.json --reporters cli,junit --reporter-junit-export test-reports/api-results.xml"

                    echo "--- Running E2E Tests (Selenium) ---"
                    // This now generates XML files in 'test-reports' folder
                    sh "python3 tests/e2e_test.py"
                }
            }
        }
    }

    post {
        always {
            script {
                echo '--- Publishing Test Reports ---'
                // This command tells Jenkins to look for XML files and create the graph
                junit 'test-reports/*.xml'
                sh "docker image prune -f"
                // Clean up the image tag to save disk space
                sh "docker rmi ${NEXUS_REGISTRY}/${IMAGE_NAME}:${params.VERSION_TAG} || true"
            }
        }
    }
}