pipeline {
	options {
		timeout(time: 40, unit: 'MINUTES')
		buildDiscarder(logRotator(numToKeepStr:'10'))
		disableConcurrentBuilds(abortPrevious: true)
	}
	agent {
		label "centos-latest"
	}
	tools {
		maven 'apache-maven-latest'
		jdk 'openjdk-jdk17-latest'
	}
	stages {
		stage('Build') {
			steps {
				wrap([$class: 'Xvnc', useXauthority: true]) {
					sh 'mvn clean verify -B -P baseline-check,coverage -Dmaven.repo.local=$WORKSPACE/.m2/repository -Dmaven.test.failure.ignore=true -Dmaven.test.error.ignore=true -Ddash.fail=false'
				}
			}
			post {
				always {
					archiveArtifacts artifacts: '**/target/repository/**/*,**/target/*.zip,**/target/work/data/.metadata/.log'
					junit '**/target/surefire-reports/TEST-*.xml'
				}
			}
		}
	}
}
