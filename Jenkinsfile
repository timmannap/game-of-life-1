pipeline {
    agent any 
    tools { 
        maven 'maven' 
      
    }
stages { 
     
 stage('Preparation') { 
     steps {
// for display purpose

      // Get some code from a GitHub repository

      git 'https://github.com/phanikumartenka/game-of-life.git'

      // Get the Maven tool.
     
 // ** NOTE: This 'M3' Maven tool must be configured
 
     // **       in the global configuration.   
     }
   }

   stage('Build') {
       steps {
       // Run the maven build

      //if (isUnix()) {
         sh label: '', script: 'mvn clean package'
      //} 
      //else {
      //   bat(/"${mvnHome}\bin\mvn" -Dmaven.test.failure.ignore clean package/)
       }
//}
   }
 
  stage('Unit Test Results') {
      steps {
      junit '**/target/surefire-reports/TEST-*.xml'
      
     }
 }
 stage('Sonarqube') {
    environment {
        def scannerHome = tool 'sonarqube';
    }
    steps {
      withSonarQubeEnv('sonarqube') {
            sh "${scannerHome}/bin/sonar-scanner"
        }
        timeout(time: 10, unit: 'MINUTES') {
          waitForQualityGate abortPipeline: true
        }
    }
}
     stage('Artifact upload') {
      steps {
       nexusArtifactUploader credentialsId: '5ba1df6b-2282-402a-9887-e07858823ceb', groupId: 'phanindra', nexusUrl: 'http://3.133.58.28:8081/nexus', nexusVersion: 'nexus2', protocol: 'http', repository: 'phanindra', version: '$BUILD_ID'
      }
     }
    stage('Deploy War') {
      steps {
        sh label: '', script: 'ansible-playbook deploy.yml'
      }
 }
//}
//post {
  //     success {
    //        archiveArtifacts 'gameoflife-web/target/*.war'
      //  }
       //failure {
         //  mail to:"raknas000@gmail.com", subject:"FAILURE: ${currentBuild.fullDisplayName}", body: "Build failed"
       // }
    }       
}
