node ('master') {
  def rtMaven = Artifactory.newMavenBuild()
  def buildInfo

  stage ('SCM globals') {
     sh '''
git config --global user.email "paul.austin@revolsys.com"
git config --global user.name "Paul Austin"
     '''
  }

  stage ('Tag') {
    dir('source') {
      deleteDir()
      checkout([
        $class: 'GitSCM',
        branches: [[name: '${gitBranch}']],
        doGenerateSubmoduleConfigurations: false,
        extensions: [],
        gitTool: 'Default',
        submoduleCfg: [],
        userRemoteConfigs: [[url: 'ssh://git@github.com/revolsys/com.revolsys.open.git']]
      ])
    }
  }

  stage ('Set Version') {
    dir('source') {
      sh 'git checkout -B version-${gitTag}'
      withMaven(jdk: 'jdk', maven: 'm3') {
        sh 'mvn versions:set -DnewVersion="${gitTag}" -DgenerateBackupPoms=false'
      }
    }
  }

  stage ('Tag') {
    dir('source') {
      sh '''
git commit -a -m "Version ${gitTag}"
git tag -f -a ${gitTag} -m "Version ${gitTag}"
git push origin ${gitTag}
      '''
    }
  }
}
