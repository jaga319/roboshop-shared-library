def call(Map configMap) 
{
pipeline {
  agent {
    node {
        label 'AGENT-1'
    }
  } 
  options{
        timeout(time: 1,unit:'HOURS')
        disableConcurrentBuilds()
        ansiColor('xterm')
    }
    parameters {
                booleanParam(name: 'deploy', defaultValue: false, description: 'confirm to deploy ?')

    }
    // Build //
    environment {
        packageversion = ''
    }
    stages {
        stage('get_version') {
            steps {
                script {
                        def PackageJson = readJSON file: 'package.json'
                        packageversion = PackageJson.version
                        echo "application version is ${packageversion}"
                }
            }
        }
        stage ('install dependencies'){
            steps{
                sh """

                 npm install

                """
            }
        }
        stage ('unit-testing'){
            steps{
                sh """
                   echo " performing unit testing "

                """
            }
        }
        stage ('sonar-scan'){
            steps{
                sh """
                  sonar-scanner

                """
            }
        }
        stage ('Build'){
            steps{
                sh """
                   ls -la
                   zip -q -r ${configMap.component}.zip ./* -x ".git" -x ".zip"
                   ls -ltr
                """
            }
        }
        stage ('publish artifact'){
            steps{
                nexusArtifactUploader(
                    nexusVersion: 'nexus3',
                    protocol: 'http',
                    nexusUrl: '172.31.93.251:8081',
                    groupId: 'com.roboshop',
                    version: "${packageversion}",
                    repository: "${configMap.component}",
                    credentialsId: 'nexus-auth',
                    artifacts: [
                        [artifactId: "${configMap.component}",
                        classifier: '',
                        file: "${configMap.component}.zip",
                        type: 'zip']
                    ]
                )
            }
              
        }
        stage("deploy") {
              when{
                expression{
                 params.deploy == 'true'
                }
              }
              steps{
                   script {
                    build job: "${configMap.component}-deploy", wait: true , parameters: [string(name: 'version', value: "${packageversion}")]
                   }

              }
            }


        
    }
    // post build 
     post { 
        always { 

            echo 'I will always run'
            echo " we running central library jenkins "
            deleteDir()
        }
        failure{
            echo 'job is failed , creating an alarm'
        }
        success{
            echo 'job completed successfully'
        }
    }
  }
}