pipeline {
  agent any;
  
  environment {
    AWS_DEFAULT_REGION = 'ap-northeast-2'
    S3_BUCKET          = 'monitory-wearable/android'

    GRADLE_OPTS = "-Dorg.gradle.jvmargs=-Xmx4g"
    APK_PATH    = "mobile/build/outputs/apk/debug/mobile-debug.apk"

    GH_CHECK_NAME = "Wearable assembleDebug"

    SLACK_CHANNEL      = '#ci-cd'
    SLACK_CRED_ID      = 'slack-factoreal-token'   // Slack App OAuth Token
  }

  stages {

    stage('Checkout & Environment Setup') {
      steps {
        checkout scm
        script {
          def rawUrl = sh(script: "git config --get remote.origin.url",
                        returnStdout: true).trim()
          env.REPO_URL = rawUrl.replaceAll(/\.git$/, '')
          env.COMMIT_MSG = sh(script: "git log -1 --pretty=format:'%s'",returnStdout: true).trim()
        }
      }
    }

    /* google-services.json & PEM 파일 복원 */
    stage('Inject secrets') {
      steps {
        withCredentials([
          string(credentialsId: 'google-services-json', variable: 'GOOGLE_JSON'),
          string(credentialsId: 'iot-certificate',     variable: 'IOT_CERT'),
          string(credentialsId: 'iot-private-pem',     variable: 'IOT_PEM'),
          string(credentialsId: 'amazon-root-ca1',       variable: 'AMAZON_ROOT_CA1'),
        ]) {
          sh '''
            # 경로 준비
            mkdir -p mobile/src/main
            mkdir -p mobile/src/main/assets

            # google-services.json
            echo "$GOOGLE_JSON" > mobile/google-services.json
            chmod 644 mobile/google-services.json

            # PEMs
            echo "$IOT_CERT" > mobile/src/main/assets/c060311b74ab5d78e0c9918acc72ebeb07d4d48accfab78d7296dae0e3718872-certificate.pem.crt
            chmod 644 mobile/src/main/assets/*-certificate.pem.crt

            echo "$IOT_PEM"  > mobile/src/main/assets/c060311b74ab5d78e0c9918acc72ebeb07d4d48accfab78d7296dae0e3718872-private.pem.crt
            chmod 644 mobile/src/main/assets/*-private.pem.crt

            echo "$AMAZON_ROOT_CA1" > mobile/src/main/assets/AmazonRootCA1.pem
            chmod 644 mobile/src/main/assets/AmazonRootCA1.pem
          '''
        }
      }
    }

    stage('Gradle assembleDebug') {
      when { not { changeRequest() } }
      steps {
        sh 'chmod +x ./gradlew'
        sh './gradlew :mobile:assembleDebug --stacktrace --warn'
        }
      post {
        success {
          /* 상태 = SUCCESS */
          publishChecks name: env.GH_CHECK_NAME,
                        conclusion: 'SUCCESS',
                        detailsURL: env.BUILD_URL
        }
        failure {
          /* 상태 = FAILURE + 로그 링크 */
          publishChecks name: env.GH_CHECK_NAME,
                        conclusion: 'FAILURE',
                        detailsURL: "${env.BUILD_URL}console"
          slackSend channel: env.SLACK_CHANNEL,
                    tokenCredentialId: env.SLACK_CRED_ID,
                    color: '#ff0000',
                    message: """:x: *Wearable assembleDebug 실패*
          파이프라인: <${env.BUILD_URL}|열기>
          커밋: `${env.GIT_COMMIT}` – `${env.COMMIT_MSG}`
          (<${env.REPO_URL}/commit/${env.GIT_COMMIT}|커밋 보기>)
          """
        }
      }
    }

    stage('Archive APK && Upload to S3') {
      when {
        allOf {
          anyOf {
            branch 'develop'
            branch 'main'
          }
          not { changeRequest() }   // PR 빌드는 건너뜀
        }
      }
      steps {
        archiveArtifacts artifacts: "${APK_PATH}", fingerprint: true
        withCredentials([[$class: 'AmazonWebServicesCredentialsBinding',
                          credentialsId: 'jenkins-access']]) {
          sh '''
aws s3 cp ${APK_PATH} s3://${S3_BUCKET}/
'''
        }
      }
      post {
        success {
          slackSend channel: env.SLACK_CHANNEL,
                    tokenCredentialId: env.SLACK_CRED_ID,
                    color: '#36a64f',
                    message: """:white_check_mark: *Wearable CI/CD 성공*
파이프라인: <${env.BUILD_URL}|열기>
커밋: `${env.GIT_COMMIT}` – `${env.COMMIT_MSG}`
(<${env.REPO_URL}/commit/${env.GIT_COMMIT}|커밋 보기>)
"""
        }
        failure {
          slackSend channel: env.SLACK_CHANNEL,
                    tokenCredentialId: env.SLACK_CRED_ID,
                    color: '#ff0000',
                    message: """:x: *Wearable CI/CD 실패*
파이프라인: <${env.BUILD_URL}|열기>
커밋: `${env.GIT_COMMIT}` – `${env.COMMIT_MSG}`
(<${env.REPO_URL}/commit/${env.GIT_COMMIT}|커밋 보기>)
"""
        }
      }
    }
  }
}