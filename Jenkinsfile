node ('master'){
  def gitProjectUrl = 'https://github.com/revolsys/com.revolsys.open.git'
  /*
def server = Artifactory.server 'prod'
  def rtMaven = Artifactory.newMavenBuild()
  def buildInfo
*/
  stage ('SCM prepare'){
    dir (path: 'scm-checkout') {
      deleteDir()
      checkout([
        $class: 'GitSCM', 
        branches: [[name: 'refs/tags/${gitTag}']],
        doGenerateSubmoduleConfigurations: false,
        extensions: [],
        gitTool: 'Default',
        submoduleCfg: [],
        userRemoteConfigs: [[url: gitProjectUrl]]
      ])
    }
  }

  stage ('Artifactory configuration'){
/*
    rtMaven.tool = 'm3' // Tool name from Jenkins configuration
    rtMaven.deployer releaseRepo: 'libs-release-local', snapshotRepo: 'libs-snapshot-local', server: server
    rtMaven.resolver releaseRepo: 'repo', snapshotRepo: 'repo', server: server
    rtMaven.deployer.deployArtifacts = false // Disable artifacts deployment during Maven run
    buildInfo = Artifactory.newBuildInfo()
*/
  }

  stage ('Maven Install'){
/*
    rtMaven.run pom: 'pom.xml', goals: 'clean install', buildInfo: buildInfo
*/
  }

  stage ('Artifactory Deploy'){
/*
    rtMaven.deployer.deployArtifacts buildInfo
*/
  }

  stage ('Artifactory Publish build info'){
/*
    server.publishBuildInfo buildInfo
*/
  }
}

