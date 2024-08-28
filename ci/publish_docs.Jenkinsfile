#!/usr/bin/env groovy
import org.jenkinsci.plugins.pipeline.modeldefinition.Utils

def bob = "./bob/bob"
def ruleset = "ci/local_ruleset.yaml"

stage('Upload Marketplace Documentation') {
    withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS'),
                 string(credentialsId: 'AIS_MARKETPLACE_TOKEN', variable: 'AIS_MARKETPLACE_TOKEN')]) {
        sh "${bob} -r ${ruleset} generate-docs"
        sh "${bob} -r ${ruleset} generate-doc-zip-package"
        if (env.RELEASE) {
            echo "Upload in Release Docs to marketplace"
            sh "${bob} -r ${ruleset} marketplace-upload-release"
        } else {
            echo "Upload in Development Docs to marketplace"
            sh "${bob} -r ${ruleset} marketplace-upload-dev"
        }
    }
}
