#!/usr/bin/env groovy
import org.jenkinsci.plugins.pipeline.modeldefinition.Utils
 
def bob = "./bob/bob"
def ruleset = "ci/local_ruleset.yaml"
def ci_ruleset = "ci/common_ruleset2.0.yaml"

stage('Vulnerability Analysis') {
    parallel(
        "Hadolint": {
            script {
                if (env.HADOLINT_ENABLED == "true") {
                    sh "${bob} -r ${ruleset} hadolint-scan"
                    echo "Evaluating Hadolint Scan Resultcodes..."
                    sh "${bob} -r ${ci_ruleset} evaluate-design-rule-check-resultcodes"
                    archiveArtifacts "build/va-reports/hadolint-scan/**.*"
                } else {
                    echo "stage Hadolint skipped"
                }
            }
        },
        "Kubehunter": {
            script {
                if (env.KUBEHUNTER_ENABLED == "true") {
                    configFileProvider([configFile(fileId: "${K8S_CLUSTER_ID}", targetLocation: "${env.KUBECONFIG}")]) {}
                    sh "${bob} -r ${ruleset} kubehunter-scan"
                    archiveArtifacts "build/va-reports/kubehunter-report/**/*"
                } else {
                    echo "stage Kubehunter skipped"
                }
            }
        },
        "Kubeaudit": {
            script {
                if (env.KUBEAUDIT_ENABLED == "true") {
                    sh "${bob} -r ${ruleset} kube-audit"
                    archiveArtifacts "build/va-reports/kube-audit-report/**/*"
                } else {
                    echo "stage Kubeaudit skipped"
                }
            }
        },
        "Kubesec": {
            script {
                if (env.KUBESEC_ENABLED == "true") {
                    sleep(10)
                    sh "${bob} -r ${ruleset} kubesec-scan"
                    archiveArtifacts "build/va-reports/kubesec-reports/*"
                } else {
                    echo "stage Kubsec skipped"
                }
            }
        },
        "Trivy": {
            script {
                if (env.TRIVY_ENABLED == "true") {
                    sh "${bob} -r ${ruleset} trivy-inline-scan"
                    archiveArtifacts "build/va-reports/trivy-reports/**.*"
                    archiveArtifacts "trivy_metadata.properties"
                } else {
                    echo "stage Trivy skipped"
                }
            }
        },
        "X-Ray": {
            script {
                if (env.XRAY_ENABLED == "true") {
                    sleep(60)
                    withCredentials([usernamePassword(credentialsId: 'XRAY_SELI_ARTIFACTORY', usernameVariable: 'XRAY_USER', passwordVariable: 'XRAY_APIKEY')]) {
                        ci_pipeline_scripts.retryMechanism("${bob} -r ${ruleset} fetch-xray-report", 3)
                    }
                    archiveArtifacts "build/va-reports/xray-reports/xray_report.json"
                    archiveArtifacts "build/va-reports/xray-reports/raw_xray_report.json"
                } else {
                    echo "stage X-Ray skipped"
                }
            }
        },
        "Anchore-Grype": {
            script {
                if (env.ANCHORE_ENABLED == "true") {
                    sh "${bob} -r ${ruleset} anchore-grype-scan"
                    archiveArtifacts "build/va-reports/anchore-reports/**.*"
                } else {
                    echo "stage Anchore-Grype skipped"
                }
            }
        },
        "NMAP Unicorn": {
            script {
                withCredentials([usernamePassword(credentialsId: 'SERO_ARTIFACTORY', usernameVariable: 'SERO_ARTIFACTORY_REPO_USER', passwordVariable: 'SERO_ARTIFACTORY_REPO_PASS')]){
                    if (env.NMAP_ENABLED == "true") {
                        configFileProvider([configFile(fileId: "${K8S_CLUSTER_ID}", targetLocation: "${env.KUBECONFIG}")]) {}
                        sh "${bob} -r ${ruleset} nmap-port-scanning"
                        archiveArtifacts "build/va-reports/nmap_reports/**/**.*"
                    }else{
                        echo "stage NMAP Unicorn skipped"
                    }
                }
            }
        },
        "ZAP": {
            script {
                if (env.ZAP_ENABLED == "true") {
                    withCredentials([usernamePassword(credentialsId: 'SERO_ARTIFACTORY', usernameVariable: 'SERO_ARTIFACTORY_REPO_USER', passwordVariable: 'SERO_ARTIFACTORY_REPO_PASS')]) {
                        sh "${bob} -r ${ruleset} zap-scan"
                        archiveArtifacts "build/va-reports/zap-reports/**/**.*"
                    }
                } else {
                    echo "stage ZAP skipped"
                }
            }
        }
    )
}

stage('Generate Vulnerability report V2.0'){
   sh "${bob} -r ${ruleset} generate-VA-report-V2:create-va-folders"
   sh "${bob} -r ${ruleset} generate-VA-report-V2:no-upload"
   archiveArtifacts allowEmptyArchive: true, artifacts: 'build/va-reports/Vulnerability_Report_2.0.md'
}