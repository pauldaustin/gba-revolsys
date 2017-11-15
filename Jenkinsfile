node ('master') {
  def server = Artifactory.server 'revolsys'
  def rtMaven = Artifactory.newMavenBuild()
  def buildInfo

  stage ('SCM prepare') {
    dir('source') {
      deleteDir()
      checkout([
        $class: 'GitSCM',
        branches: [[name: '${gitBranch}']],
        doGenerateSubmoduleConfigurations: false,
        extensions: [],
        gitTool: 'Default',
        submoduleCfg: []
      ])
      withMaven(jdk: 'jdk', maven: 'm3') {
        sh '''
mvn versions:set -DnewVersion="${version}" -DgenerateBackupPoms=false
git commit -a -m "Version ${version}"
git tag -f -a ${version} -m "Version ${version}"
        '''
      }
    }
  }
  
  stage ('Artifactory configuration') {
    dir('source') {
      rtMaven.tool = 'm3'
      rtMaven.deployer releaseRepo: 'libs-release-local', snapshotRepo: 'libs-snapshot-local', server: server
      rtMaven.resolver releaseRepo: 'repo', snapshotRepo: 'repo', server: server
      rtMaven.deployer.deployArtifacts = false 
      buildInfo = Artifactory.newBuildInfo()
    }
  }

  stage ('Maven Install') {
    dir('source') {
      rtMaven.run pom: 'pom.xml', goals: 'clean install -DskipTests=true', buildInfo: buildInfo
    }
  }
  
  stage ('Artifactory Deploy') {
    dir('source') {
      rtMaven.deployer.deployArtifacts buildInfo
    }
  }
  
  stage ('Artifactory Publish build info') {
    dir('source') {
      server.publishBuildInfo buildInfo
    }
  }
}
