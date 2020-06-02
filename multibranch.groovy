pipeline {

    agent any

    tools {
        
        maven "Maven363"
    }

    environment {
		
        // This can be nexus3 or nexus2
        NEXUS_VERSION = "nexus3"
        // This can be http or https
        NEXUS_PROTOCOL = "http"
        // Where your Nexus is running. 'nexus-3' is defined in the docker-compose file
        NEXUS_URL = "18.207.203.164:8081"
        // Jenkins credential id to authenticate to Nexus OSS
        NEXUS_CREDENTIAL_ID = "NEXUS_CREDENTIALS"
    }

    stages {
		stage("nexusrep") {
            steps {
                script {
                    if (env.BRANCH_NAME == 'develop'){
                      NEXUS_REPOSITORY = 'dev'
                    }else if (env.BRANCH_NAME == 'release'){
                      NEXUS_REPOSITORY = 'qa'
                    }else if (env.BRANCH_NAME == 'master'){
                      NEXUS_REPOSITORY = 'stage'
                    }else if (env.TAG_NAME != 'null'){
                      NEXUS_REPOSITORY = 'prod'
                    }
                }
            }
        }

        stage("mvn build") {
            steps {
                script {
                    // If you are using Windows then you should use "bat" step
                    // Since unit testing is out of the scope we skip them
                    sh "mvn package"
                }
            }
        }

        stage("publish to nexus") {
            steps {
                script {
                    // Read POM xml file using 'readMavenPom' step , this step 'readMavenPom' is included in: https://plugins.jenkins.io/pipeline-utility-steps
                    
                    pomversion = sh(returnStdout: true,script:"sed -n '/<parent>/,/<\\/parent>/p' gameoflife-web/pom.xml| sed -ne '/<version>/p'|sed -e 's/<version>//' -e 's/<\\/version>//'").toString().trim()
                    
                    pom = readMavenPom file: "gameoflife-web/pom.xml";
                    // Find built artifact under target folder
                    filesByGlob = findFiles(glob: "gameoflife-web/target/*.${pom.packaging}");
                    // Print some info from the artifact found
                    echo "${filesByGlob[0].name} ${filesByGlob[0].path} ${filesByGlob[0].directory} ${filesByGlob[0].length} ${filesByGlob[0].lastModified}"
                    // Extract the path from the File found
                    artifactPath = filesByGlob[0].path;
                    // Assign to a boolean response verifying If the artifact name exists
                    artifactExists = fileExists artifactPath;
                    
                    if(artifactExists) {
                        echo "*** File: ${artifactPath}, group: ${pom.groupId}, packaging: ${pom.packaging}, version ${pom.version}";
                        
                        nexusArtifactUploader(
                            nexusVersion: NEXUS_VERSION,
                            protocol: NEXUS_PROTOCOL,
                            nexusUrl: NEXUS_URL,
                            groupId: pom.groupId,
                            version: pomversion,
                            repository: NEXUS_REPOSITORY,
                            credentialsId: NEXUS_CREDENTIAL_ID,
                            artifacts: [
                                // Artifact generated such as .jar, .ear and .war files.
                                [artifactId: pom.artifactId,
                                classifier: '',
                                file: artifactPath,
                                type: pom.packaging]
                                ]
                        );

                    } else {
                        error "*** File: ${artifactPath}, could not be found";
                    }
                }
            }
        }
        stage("DeployToTomcat") {
            steps {
                script {
                    // If you are using Windows then you should use "bat" step
                    // Since unit testing is out of the scope we skip them
                  sh "ansible-playbook -e env=${NEXUS_REPOSITORY} deploy.yaml"
                }
            }
        }
    }
	post { 
        always { 
            cleanWs()
        }
    }
}
