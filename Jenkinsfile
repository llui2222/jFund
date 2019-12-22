node ('javabuild') {
	try {
		stage('Preparation') {

			checkout changelog: false, poll: false, scm: [$class: 'GitSCM', doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'jFund']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'jenkins', url: 'git@gitlab.xm.com:xmdevs/jFund.git']]]
		}

		stage('Build') {
			// Run the gradle build
			if (isUnix()) {
				sh "pwd"
				sh "cd jFund; chmod +x ./gradlew;"
				sh "cd jFund; ./gradlew clean jar;"

				gitlabCommitStatus(name : 'Build') {}
			} else {
				echo "No windows build support!"
				error "No windows build support for now!"
			}
		}

		stage('QA') {
			echo 'Running static code analysis  ... '

			withSonarQubeEnv('sonarqube.xm') {

				sh 'cd jFund; ./gradlew sonarqube'
			}

			echo 'Running tests ... '

			sh 'cd jFund; ./gradlew cleanTest test;'

			//publish test results
			publishHTML(target: [
                allowMissing: false,
                alwaysLinkToLastBuild: false,
                keepAll: true,
                reportDir: 'jFund/build/reports/tests/test',
                reportFiles: 'index.html',
                reportName: 'Unit Test Report'
                ]
            )

			gitlabCommitStatus(name : 'QA Passed') {}
		}

		stage('Post Build') {

			//archive artifacts to nexus
			sh "cd jFund; ./gradlew uploadArchives "

			gitlabCommitStatus(name : 'Artifact uploaded') {}
		}
		
		emailext (
			subject: 'Build SUCCESSFUL - ${JOB_NAME} build #${BUILD_NUMBER}',
			body: '${JOB_NAME} build #${BUILD_NUMBER} is SUCCESSFUL.',
			recipientProviders:  [ [$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider'], [$class: 'UpstreamComitterRecipientProvider'] ],
			to: env.gitlabUserEmail?:''
		)
	} catch(e) {

		handleException(e)
		throw e
	}
}

def handleException(ex) {
	
	echo "error: ${ex}"
	
	emailext (
		subject: 'Build FAILED - ${JOB_NAME} build #${BUILD_NUMBER}',
		body: "Build failed, reason: ${ex} .  Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'. Please check the console output at ${BUILD_URL}console ",
		recipientProviders:  [[$class: 'CulpritsRecipientProvider'], [$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider'], [$class: 'UpstreamComitterRecipientProvider'], [$class: 'FirstFailingBuildSuspectsRecipientProvider']],
		to: env.gitlabUserEmail?:''
	)
}