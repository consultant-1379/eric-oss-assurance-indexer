{{/* vim: set filetype=mustache: */}}

{{/*
Create a map from ".Values.global" with defaults if missing in values file.
This hides defaults from values file.
*/}}
{{ define "eric-oss-assurance-indexer.global" }}
  {{- $globalDefaults := dict "nodeSelector" (dict) -}}
  {{- $globalDefaults := merge $globalDefaults (dict "registry" (dict "pullSecret" "eric-oss-assurance-indexer-secret")) -}}
  {{- $globalDefaults := merge $globalDefaults (dict "externalIPv4" (dict "enabled")) -}}
  {{- $globalDefaults := merge $globalDefaults (dict "externalIPv6" (dict "enabled")) -}}
  {{ if .Values.global }}
    {{- mergeOverwrite $globalDefaults .Values.global | toJson -}}
  {{ else }}
    {{- $globalDefaults | toJson -}}
  {{ end }}
{{ end }}

{{/*
Expand the name of the chart.
*/}}
{{- define "eric-oss-assurance-indexer.name" }}
  {{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create chart version as used by the chart label.
*/}}
{{- define "eric-oss-assurance-indexer.version" }}
{{- printf "%s" .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Expand the name of the chart.
*/}}
{{- define "eric-oss-assurance-indexer.fullname" -}}
{{- if .Values.fullnameOverride -}}
  {{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
  {{- $name := default .Chart.Name .Values.nameOverride -}}
  {{- printf "%s" $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "eric-oss-assurance-indexer.chart" }}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create image pull secrets for global (outside of scope)
*/}}
{{- define "eric-oss-assurance-indexer.pullSecret.global" -}}
{{- $pullSecret := "" -}}
{{- if .Values.global -}}
  {{- if .Values.global.pullSecret -}}
    {{- $pullSecret = .Values.global.pullSecret -}}
  {{- end -}}
  {{- end -}}
{{- print $pullSecret -}}
{{- end -}}

{{/*
Create image pull secret, service level parameter takes precedence
*/}}
{{- define "eric-oss-assurance-indexer.pullSecret" -}}
{{- $pullSecret := (include "eric-oss-assurance-indexer.pullSecret.global" . ) -}}
{{- if .Values.imageCredentials -}}
  {{- if .Values.imageCredentials.pullSecret -}}
    {{- $pullSecret = .Values.imageCredentials.pullSecret -}}
  {{- end -}}
{{- end -}}
{{- print $pullSecret -}}
{{- end -}}

{{- define "eric-oss-assurance-indexer.mainImagePath" -}}
    {{- $productInfo := fromYaml (.Files.Get "eric-product-info.yaml") -}}
    {{- $registryUrl := (index $productInfo "images" "eric-oss-assurance-indexer" "registry") -}}
    {{- $repoPath := (index $productInfo "images" "eric-oss-assurance-indexer" "repoPath") -}}
    {{- $name := (index $productInfo "images" "eric-oss-assurance-indexer" "name") -}}
    {{- $tag := (index $productInfo "images" "eric-oss-assurance-indexer" "tag") -}}
    {{- if .Values.global -}}
        {{- if .Values.global.registry -}}
            {{- if .Values.global.registry.url -}}
                {{- $registryUrl = .Values.global.registry.url -}}
            {{- end -}}
            {{- if not (kindIs "invalid" .Values.global.registry.repoPath) -}}
                {{- $repoPath = .Values.global.registry.repoPath -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
    {{- if .Values.imageCredentials -}}
        {{- if not (kindIs "invalid" .Values.imageCredentials.repoPath) -}}
            {{- $repoPath = .Values.imageCredentials.repoPath -}}
        {{- end -}}
        {{- if (index .Values "imageCredentials" "eric-oss-assurance-indexer") -}}
            {{- if (index .Values "imageCredentials" "eric-oss-assurance-indexer" "registry") -}}
                {{- if (index .Values "imageCredentials" "eric-oss-assurance-indexer" "registry" "url") -}}
                    {{- $registryUrl = (index .Values "imageCredentials" "eric-oss-assurance-indexer" "registry" "url") -}}
                {{- end -}}
            {{- end -}}
            {{- if not (kindIs "invalid" (index .Values "imageCredentials" "eric-oss-assurance-indexer" "repoPath")) -}}
                {{- $repoPath = (index .Values "imageCredentials" "eric-oss-assurance-indexer" "repoPath") -}}
            {{- end -}}
        {{- end -}}
        {{- if not (kindIs "invalid" .Values.imageCredentials.repoPath) -}}
            {{- $repoPath = .Values.imageCredentials.repoPath -}}
        {{- end -}}
    {{- end -}}
    {{- if $repoPath -}}
        {{- $repoPath = printf "%s/" $repoPath -}}
    {{- end -}}
    {{- printf "%s/%s%s:%s" $registryUrl $repoPath $name $tag -}}
{{- end -}}

{{- define "eric-oss-assurance-indexer.testImagePath" }}
    {{- $productInfo := fromYaml (.Files.Get "eric-product-info.yaml") -}}
    {{- $registryUrl := (index $productInfo "images" "eric-oss-assurance-indexerTest" "registry") -}}
    {{- $repoPath := (index $productInfo "images" "eric-oss-assurance-indexerTest" "repoPath") -}}
    {{- $name := (index $productInfo "images" "eric-oss-assurance-indexerTest" "name") -}}
    {{- $tag := (index $productInfo "images" "eric-oss-assurance-indexerTest" "tag") -}}
    {{- if .Values.global -}}
        {{- if .Values.global.registry -}}
            {{- if .Values.global.registry.url -}}
                {{- $registryUrl = .Values.global.registry.url -}}
            {{- end -}}
            {{- if not (kindIs "invalid" .Values.global.registry.repoPath) -}}
                {{- $repoPath = .Values.global.registry.repoPath -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
    {{- if .Values.imageCredentials -}}
        {{- if not (kindIs "invalid" .Values.imageCredentials.repoPath) -}}
            {{- $repoPath = .Values.imageCredentials.repoPath -}}
        {{- end -}}
        {{- if (index .Values "imageCredentials" "eric-oss-assurance-indexerTest") -}}
            {{- if (index .Values "imageCredentials" "eric-oss-assurance-indexerTest" "registry") -}}
                {{- if (index .Values "imageCredentials" "eric-oss-assurance-indexerTest" "registry" "url") -}}
                    {{- $registryUrl = (index .Values "imageCredentials" "eric-oss-assurance-indexerTest" "registry" "url") -}}
                {{- end -}}
            {{- end -}}
            {{- if not (kindIs "invalid" (index .Values "imageCredentials" "eric-oss-assurance-indexerTest" "repoPath")) -}}
                {{- $repoPath = (index .Values "imageCredentials" "eric-oss-assurance-indexerTest" "repoPath") -}}
            {{- end -}}
        {{- end -}}
        {{- if not (kindIs "invalid" .Values.imageCredentials.repoPath) -}}
            {{- $repoPath = .Values.imageCredentials.repoPath -}}
        {{- end -}}
    {{- end -}}
    {{- if $repoPath -}}
        {{- $repoPath = printf "%s/" $repoPath -}}
    {{- end -}}
    {{- printf "%s/%s%s:%s" $registryUrl $repoPath $name $tag -}}
{{- end -}}

{{/*
Timezone variable
*/}}
{{- define "eric-oss-assurance-indexer.timezone" }}
  {{- $timezone := "UTC" }}
  {{- if .Values.global }}
    {{- if .Values.global.timezone }}
      {{- $timezone = .Values.global.timezone }}
    {{- end }}
  {{- end }}
  {{- print $timezone | quote }}
{{- end -}}

{{/*
Common labels
*/}}
{{- define "eric-oss-assurance-indexer.common-labels" }}
app.kubernetes.io/name: {{ include "eric-oss-assurance-indexer.name" . }}
helm.sh/chart: {{ include "eric-oss-assurance-indexer.chart" . }}
{{ include "eric-oss-assurance-indexer.selectorLabels" . }}
app.kubernetes.io/version: {{ include "eric-oss-assurance-indexer.version" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
sidecar.istio.io/inject: "false"
{{- end -}}

{{/*
Create a user defined label (DR-D1121-068, DR-D1121-060)
*/}}
{{ define "eric-oss-assurance-indexer.config-labels" }}
  {{- $global := (.Values.global).labels -}}
  {{- $service := .Values.labels -}}
  {{- include "eric-oss-assurance-indexer.mergeLabels" (dict "location" .Template.Name "sources" (list $global $service)) }}
{{- end }}

{{/*
Merged labels for Default, which includes Standard and Config
*/}}
{{- define "eric-oss-assurance-indexer.labels" -}}
  {{- $common := include "eric-oss-assurance-indexer.common-labels" . | fromYaml -}}
  {{- $config := include "eric-oss-assurance-indexer.config-labels" . | fromYaml -}}
  {{- include "eric-oss-assurance-indexer.mergeLabels" (dict "location" .Template.Name "sources" (list $common $config)) | trim }}
{{- end -}}

{{/*
Return the fsgroup set via global parameter if it's set, otherwise 10000
*/}}
{{- define "eric-oss-assurance-indexer.fsGroup.coordinated" -}}
  {{- if .Values.global -}}
    {{- if .Values.global.fsGroup -}}
      {{- if .Values.global.fsGroup.manual -}}
        {{ .Values.global.fsGroup.manual }}
      {{- else -}}
        {{- if eq .Values.global.fsGroup.namespace true -}}
          # The 'default' defined in the Security Policy will be used.
        {{- else -}}
          10000
      {{- end -}}
    {{- end -}}
  {{- else -}}
    10000
  {{- end -}}
  {{- else -}}
    10000
  {{- end -}}
{{- end -}}

{{/*
Selector labels
*/}}
{{- define "eric-oss-assurance-indexer.selectorLabels" -}}
app.kubernetes.io/name: {{ include "eric-oss-assurance-indexer.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "eric-oss-assurance-indexer.serviceAccountName" -}}
  {{- if .Values.serviceAccount.create }}
    {{- default (include "eric-oss-assurance-indexer.fullname" .) .Values.serviceAccount.name }}
  {{- else }}
    {{- default "default" .Values.serviceAccount.name }}
  {{- end }}
{{- end }}

{{/*
Create container level annotations
*/}}
{{- define "eric-oss-assurance-indexer.container-annotations" }}
{{- $appArmorValue := .Values.appArmorProfile.type -}}
    {{- if .Values.appArmorProfile -}}
        {{- if .Values.appArmorProfile.type -}}
            {{- if eq .Values.appArmorProfile.type "localhost" -}}
                {{- $appArmorValue = printf "%s/%s" .Values.appArmorProfile.type .Values.appArmorProfile.localhostProfile }}
            {{- end}}
container.apparmor.security.beta.kubernetes.io/eric-oss-assurance-indexer: {{ $appArmorValue | quote }}
        {{- end}}
    {{- end}}
{{- end}}

{{/*
Seccomp profile section (DR-1123-128)
*/}}
{{- define "eric-oss-assurance-indexer.seccomp-profile" }}
    {{- if .Values.seccompProfile }}
      {{- if .Values.seccompProfile.type }}
          {{- if eq .Values.seccompProfile.type "Localhost" }}
              {{- if .Values.seccompProfile.localhostProfile }}
seccompProfile:
  type: {{ .Values.seccompProfile.type }}
  localhostProfile: {{ .Values.seccompProfile.localhostProfile }}
            {{- end }}
          {{- else }}
seccompProfile:
  type: {{ .Values.seccompProfile.type }}
          {{- end }}
        {{- end }}
      {{- end }}
{{- end }}

{{/*
Annotations for Product Name and Product Number (DR-D1121-064 DR-D1121-067).
*/}}
{{- define "eric-oss-assurance-indexer.product-info" }}
ericsson.com/product-name: {{ (fromYaml (.Files.Get "eric-product-info.yaml")).productName | quote }}
ericsson.com/product-number: {{ (fromYaml (.Files.Get "eric-product-info.yaml")).productNumber | quote }}
ericsson.com/product-revision: {{regexReplaceAll "(.*)[+|-].*" .Chart.Version "${1}" | quote }}
{{- end }}

{{/*
Create a user defined annotation (DR-D1121-065, DR-D1121-060)
*/}}
{{ define "eric-oss-assurance-indexer.config-annotations" }}
  {{- $global := (.Values.global).annotations -}}
  {{- $service := .Values.annotations -}}
  {{- include "eric-oss-assurance-indexer.mergeAnnotations" (dict "location" .Template.Name "sources" (list $global $service)) }}
{{- end }}

{{/*
Merged annotations for Default, which includes productInfo and config
*/}}
{{- define "eric-oss-assurance-indexer.annotations" -}}
  {{- $productInfo := include "eric-oss-assurance-indexer.product-info" . | fromYaml -}}
  {{- $config := include "eric-oss-assurance-indexer.config-annotations" . | fromYaml -}}
  {{- include "eric-oss-assurance-indexer.mergeAnnotations" (dict "location" .Template.Name "sources" (list $productInfo $config)) | trim }}
{{- end -}}

{{/*
Create prometheus info (DR-D470223-010)
*/}}
{{- define "eric-oss-assurance-indexer.prometheus-vars" -}}
prometheus.io/path: {{ .Values.prometheus.path | quote }}
prometheus.io/port: {{ .Values.service.port | quote }}
prometheus.io/scrape: {{ .Values.prometheus.scrape | quote }}
prometheus.io/scrape-role: {{ .Values.prometheus.scrapeRole | quote }}
{{- end -}}

{{/*
Merged annotations with Prometheus
*/}}
{{- define "eric-oss-assurance-indexer.prometheus" -}}
  {{- $prometheus := include "eric-oss-assurance-indexer.prometheus-vars" . | fromYaml -}}
  {{- $annotations := include "eric-oss-assurance-indexer.annotations" . | fromYaml -}}
  {{- include "eric-oss-assurance-indexer.mergeAnnotations" (dict "location" .Template.Name "sources" (list $prometheus $annotations)) | trim }}
{{- end -}}

{{/*
Define the role reference for security policy
*/}}
{{- define "eric-oss-assurance-indexer.securityPolicy.reference" -}}
  {{- if .Values.global -}}
    {{- if .Values.global.security -}}
      {{- if .Values.global.security.policyReferenceMap -}}
        {{ $mapped := index .Values "global" "security" "policyReferenceMap" "default-restricted-security-policy" }}
        {{- if $mapped -}}
          {{ $mapped }}
        {{- else -}}
          default-restricted-security-policy
        {{- end -}}
      {{- else -}}
        default-restricted-security-policy
      {{- end -}}
    {{- else -}}
      default-restricted-security-policy
    {{- end -}}
  {{- else -}}
    default-restricted-security-policy
  {{- end -}}
{{- end -}}

{{/*
Define the annotations for security policy
*/}}
{{- define "eric-oss-assurance-indexer.securityPolicy.annotations" -}}
{{/* Automatically generated annotations for documentation purposes. */}}
{{- end -}}

{{/*
Define Pod Disruption Budget value taking into account its type (int or string)
*/}}
{{- define "eric-oss-assurance-indexer.pod-disruption-budget" -}}
  {{- if kindIs "string" .Values.podDisruptionBudget.minAvailable -}}
    {{- print .Values.podDisruptionBudget.minAvailable | quote -}}
  {{- else -}}
    {{- print .Values.podDisruptionBudget.minAvailable | atoi -}}
  {{- end -}}
{{- end -}}

{{/*
Define upper limit for TerminationGracePeriodSeconds
*/}}
{{- define "eric-oss-assurance-indexer.terminationGracePeriodSeconds" -}}
{{- if .Values.terminationGracePeriodSeconds -}}
  {{- .Values.terminationGracePeriodSeconds -}}
{{- end -}}
{{- end -}}

{{/*
Define tolerations to comply to DR-D1120-060 and DR-D1120-061
*/}}
{{- define "eric-oss-assurance-indexer.tolerations" -}}
  {{- $tolerations := (list) -}}
  {{- if .Values.tolerations -}}
    {{- if ne (len (index .Values "tolerations") ) 0 -}}
      {{- range $t := (index .Values "tolerations") -}}
        {{- $tolerations = append $tolerations $t -}}
      {{- end -}}
    {{- end -}}
  {{- end -}}
  {{- if .Values.global -}}
    {{- if .Values.global.tolerations -}}
      {{- if ne (len .Values.global.tolerations) 0 -}}
        {{- range $t := .Values.global.tolerations -}}
          {{- $tolerations = append $tolerations $t -}}
        {{- end -}}
      {{- end -}}
    {{- end -}}
  {{- end -}}
  {{- toYaml $tolerations}}
{{- end -}}

{{/*
Create a merged set of nodeSelectors from global and service level.
*/}}
{{ define "eric-oss-assurance-indexer.nodeSelector" }}
  {{- $g := fromJson (include "eric-oss-assurance-indexer.global" .) -}}
  {{- $global := $g.nodeSelector -}}
  {{- $service := .Values.nodeSelector -}}
  {{- include "eric-oss-assurance-indexer.aggregatedMerge" (dict "context" "nodeSelector" "location" .Template.Name "sources" (list $global $service)) }}
{{ end }}

{{/*
    Define Image Pull Policy
*/}}
{{- define "eric-oss-assurance-indexer.registryImagePullPolicy" -}}
    {{- $globalRegistryPullPolicy := "IfNotPresent" -}}
    {{- if .Values.global -}}
        {{- if .Values.global.registry -}}
            {{- if .Values.global.registry.imagePullPolicy -}}
                {{- $globalRegistryPullPolicy = .Values.global.registry.imagePullPolicy -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
    {{- print $globalRegistryPullPolicy -}}
{{- end -}}

{{/*
Define JVM heap size (DR-D1126-010 | DR-D1126-011)
*/}}
{{- define "eric-oss-assurance-indexer.jvmHeapSettings" -}}
    {{- $initRAM := "" -}}
    {{- $maxRAM := "" -}}
    {{/*
       ramLimit is set by default to 1.0, this is if the service is set to use anything less than M/Mi
       Rather than trying to cover each type of notation,
       if a user is using anything less than M/Mi then the assumption is its less than the cutoff of 1.3GB
       */}}
    {{- $ramLimit := 1.0 -}}
    {{- $ramComparison := 1.3 -}}

    {{- if not (index .Values "resources" "eric-oss-assurance-indexer" "limits" "memory") -}}
        {{- fail "memory limit for eric-oss-assurance-indexer is not specified" -}}
    {{- end -}}

    {{- if (hasSuffix "Gi" (index .Values "resources" "eric-oss-assurance-indexer" "limits" "memory")) -}}
        {{- $ramLimit = trimSuffix "Gi" (index .Values "resources" "eric-oss-assurance-indexer" "limits" "memory") | float64 -}}
    {{- else if (hasSuffix "G" (index .Values "resources" "eric-oss-assurance-indexer" "limits" "memory")) -}}
        {{- $ramLimit = trimSuffix "G" (index .Values "resources" "eric-oss-assurance-indexer" "limits" "memory") | float64 -}}
    {{- else if (hasSuffix "Mi" (index .Values "resources" "eric-oss-assurance-indexer" "limits" "memory")) -}}
        {{- $ramLimit = (div (trimSuffix "Mi" (index .Values "resources" "eric-oss-assurance-indexer" "limits" "memory") | float64) 1000) | float64  -}}
    {{- else if (hasSuffix "M" (index .Values "resources" "eric-oss-assurance-indexer" "limits" "memory")) -}}
        {{- $ramLimit = (div (trimSuffix "M" (index .Values "resources" "eric-oss-assurance-indexer" "limits" "memory")| float64) 1000) | float64  -}}
    {{- end -}}


    {{- if (index .Values "resources" "eric-oss-assurance-indexer" "jvm") -}}
        {{- if (index .Values "resources" "eric-oss-assurance-indexer" "jvm" "initialMemoryAllocationPercentage") -}}
            {{- $initRAM = (index .Values "resources" "eric-oss-assurance-indexer" "jvm" "initialMemoryAllocationPercentage") -}}
            {{- $initRAM = printf "-XX:InitialRAMPercentage=%f" $initRAM -}}
        {{- else -}}
            {{- fail "initialMemoryAllocationPercentage not set" -}}
        {{- end -}}
        {{- if and (index .Values "resources" "eric-oss-assurance-indexer" "jvm" "smallMemoryAllocationMaxPercentage") (index .Values "resources" "eric-oss-assurance-indexer" "jvm" "largeMemoryAllocationMaxPercentage") -}}
            {{- if lt $ramLimit $ramComparison -}}
                {{- $maxRAM = (index .Values "resources" "eric-oss-assurance-indexer" "jvm" "smallMemoryAllocationMaxPercentage") -}}
                {{- $maxRAM = printf "-XX:MaxRAMPercentage=%f" $maxRAM -}}
            {{- else -}}
                {{- $maxRAM = (index .Values "resources" "eric-oss-assurance-indexer" "jvm" "largeMemoryAllocationMaxPercentage") -}}
                {{- $maxRAM = printf "-XX:MaxRAMPercentage=%f" $maxRAM -}}
            {{- end -}}
        {{- else -}}
            {{- fail "smallMemoryAllocationMaxPercentage | largeMemoryAllocationMaxPercentage not set" -}}
        {{- end -}}
    {{- else -}}
        {{- fail "jvm heap percentages are not set" -}}
    {{- end -}}
{{- printf "%s %s" $initRAM $maxRAM -}}
{{- end -}}

{{/*----------------------------------- ADP Logging----------------------------------*/}}

{{/*
Define the log streaming method (DR-470222-010)
*/}}
{{- define "eric-oss-assurance-indexer.streamingMethod" -}}
{{- $streamingMethod := "direct" -}}
{{- if .Values.global -}}
  {{- if .Values.global.log -}}
      {{- if .Values.global.log.streamingMethod -}}
        {{- $streamingMethod = .Values.global.log.streamingMethod }}
      {{- end -}}
  {{- end -}}
{{- end -}}
{{- if .Values.log -}}
  {{- if .Values.log.streamingMethod -}}
    {{- $streamingMethod = .Values.log.streamingMethod }}
  {{- end -}}
{{- end -}}
{{- print $streamingMethod -}}
{{- end -}}

{{/*
Define the label needed for reaching eric-log-transformer (DR-470222-010)
*/}}
{{- define "eric-oss-assurance-indexer.directStreamingLabel" -}}
{{- $streamingMethod := (include "eric-oss-assurance-indexer.streamingMethod" .) -}}
{{- if or (eq "direct" $streamingMethod) (eq "dual" $streamingMethod) }}
logger-communication-type: "direct"
{{- end -}}
{{- end -}}

{{/*
Define logging environment variables (DR-470222-010)
*/}}
{{- define "eric-oss-assurance-indexer.loggingEnv" -}}
{{ $logtls := include "eric-oss-assurance-indexer.log-tls-enabled" . }}
    - name: LOGBACK_CONFIG_FILE
      value: {{ include "eric-oss-assurance-indexer.log-back-config-file" . }}
    - name: LOGSTASH_DESTINATION
      value: eric-log-transformer
      {{- if eq "true" $logtls }}
    - name: LOGSTASH_PORT
      value: "9443"
    - name: ERIC_LOG_TRANSFORMER_KEYSTORE
      value: /tmp/log/keystore.p12
    - name: ERIC_LOG_TRANSFORMER_KEYSTORE_PW
      value: changeit
    - name: ERIC_LOG_TRANSFORMER_TRUSTSTORE
      value: /tmp/root/truststore.p12
    - name: ERIC_LOG_TRANSFORMER_TRUSTSTORE_PW
      value: changeit
      {{- else }}
    - name: LOGSTASH_PORT
      value: "9080"
      {{- end }}
    - name: POD_NAME
      valueFrom:
        fieldRef:
          fieldPath: metadata.name
    - name: POD_UID
      valueFrom:
        fieldRef:
          fieldPath: metadata.uid
    - name: CONTAINER_NAME
      value: eric-oss-assurance-indexer
    - name: NODE_NAME
      valueFrom:
        fieldRef:
          fieldPath: spec.nodeName
    - name: NAMESPACE
      valueFrom:
        fieldRef:
          fieldPath: metadata.namespace
    - name: RUN_TIME_LEVEL_CONTROL
      value: {{ .Values.log.control.enabled | quote }}
    - name: LOG_CTRL_FILE
      value: {{ .Values.log.control.file | quote }}
{{- end }}

{{/*
    Define logback config file
*/}}
{{ define "eric-oss-assurance-indexer.log-back-config-file" }}
{{- $streamingMethod := (include "eric-oss-assurance-indexer.streamingMethod" .) -}}
{{- $logtls := include "eric-oss-assurance-indexer.log-tls-enabled" . | trim -}}
{{- $configFile := "classpath:logback-json.xml" -}}

  {{- if and (eq "direct" $streamingMethod) (eq "true" $logtls) }}
    {{- $configFile = "classpath:logback-https.xml" -}}
  {{- else if eq "direct" $streamingMethod }}
    {{- $configFile = "classpath:logback-http.xml" -}}
  {{- end }}
  {{- if eq "dual" $streamingMethod }}
    {{- if eq "true" $logtls }}
        {{- $configFile = "classpath:logback-dual-sec.xml" -}}
    {{- else }}
        {{- $configFile = "classpath:logback-dual.xml" -}}
    {{- end }}
  {{- end }}
  {{- print $configFile -}}
{{ end }}

{{/*
    Define Service Port based on TLS
*/}}
{{- define "eric-oss-assurance-indexer.service-port" -}}
    {{- if  (eq (include "eric-oss-assurance-indexer.global-security-tls-enabled" .) "true") }}
        {{- .Values.service.tls.port -}}
    {{- else -}}
        {{- .Values.service.port -}}
    {{- end -}}
{{- end -}}

{{/*
    Define Liveness - Readiness probe command
*/}}
{{- define "eric-oss-assurance-indexer.probe-command" -}}
  {{- if  (eq (include "eric-oss-assurance-indexer.global-security-tls-enabled" .) "true") }}
  {{ $readPath := "$TLS_READ_ROOT_PATH/server/$KEYSTORE_REL_DIR/" }}
  {{ $certPath := print $readPath "srvcert.crt" }}
  {{ $keyPath := print $readPath "srvprivkey.pem" }}
    exec:
      command:
        - /bin/sh
        - -c
        - curl -kf --cert {{ $certPath }} --key {{ $keyPath }} https://localhost:{{ .Values.service.tls.port }}/actuator/health
  {{- else }}
    httpGet:
      path: /actuator/health
      port: http
  {{- end }}
{{- end -}}

{{/*
    Define log Security enabled
*/}}
{{- define "eric-oss-assurance-indexer.log-tls-enabled" -}}
    {{- $logtls := include "eric-oss-assurance-indexer.global-security-tls-enabled" . | trim -}}

      {{- if  .Values.log -}}
        {{- if  .Values.log.tls -}}
         {{- $logtls = .Values.log.tls.enabled | toString -}}
        {{- end -}}
      {{- end -}}
          {{- print $logtls -}}
{{- end -}}

{{/*
    Define Global Security enabled
*/}}
{{- define "eric-oss-assurance-indexer.global-security-tls-enabled" -}}
    {{- if  .Values.global -}}
      {{- if  .Values.global.security -}}
        {{- if  .Values.global.security.tls -}}
           {{- .Values.global.security.tls.enabled | toString -}}
        {{- else -}}
           {{- "false" -}}
        {{- end -}}
      {{- else -}}
           {{- "false" -}}
      {{- end -}}
    {{- else -}}
        {{- "false" -}}
    {{- end -}}
{{- end -}}

{{/*
    Define OpenSearch variables
*/}}
{{- define "eric-oss-assurance-indexer.open-search" -}}
   {{- $port := "9200" -}}
   {{- $scheme := "http" -}}
   {{- $host := "eric-data-search-engine" -}}

   {{- if  (eq (include "eric-oss-assurance-indexer.global-security-tls-enabled" .) "true") }}
        {{- $scheme = "https" -}}
        {{- $host = "eric-data-search-engine-tls" -}}
        {{- if  (index .Values "open-search" ) }}
            {{- if  (index .Values "open-search" "scheme" ) }}
                {{- $scheme = index .Values "open-search" "scheme" -}}
            {{- end -}}
        {{- end -}}
   {{- end -}}

   {{- if  (index .Values "open-search" ) }}
        {{- if  (index .Values "open-search" "host" ) }}
            {{- $host = index .Values "open-search" "host" -}}
        {{- end -}}
        {{- if  (index .Values "open-search" "port" ) }}
            {{- $port = index .Values "open-search" "port" -}}
        {{- end -}}
    {{- end -}}

- name: OPENSEARCH_HOST
  value: {{ print $host }}
- name: OPENSEARCH_PORT
  value: {{ print $port | quote }}
- name: OPENSEARCH_SCHEME
  value: {{ print $scheme }}
{{- end -}}

{{/*
    Define Kafka variables
*/}}
{{- define "eric-oss-assurance-indexer.kafka" -}}
    {{- $bootstrapServers := "eric-oss-dmm-kf-op-sz-kafka-bootstrap:9092" -}}
    {{- $schemaRegistryUrl := "http://eric-schema-registry-sr:8081" -}}
    {{- if  (eq (include "eric-oss-assurance-indexer.global-security-tls-enabled" .) "true") }}
        {{- $schemaRegistryUrl = "https://eric-schema-registry-sr:8082" -}}
    {{- end -}}
    {{- if  (eq (include "eric-oss-assurance-indexer.global-security-tls-enabled" .) "true") }}
        {{- $bootstrapServers = "eric-oss-dmm-kf-op-sz-kafka-bootstrap:9093" -}}
    {{- end -}}

   {{- if  (index .Values "kafka" ) }}
        {{- if  (index .Values "kafka" "bootstrap-servers" ) }}
            {{- $bootstrapServers = index .Values "kafka" "bootstrap-servers" -}}
        {{- end -}}
        {{- if  (index .Values "kafka" "schema-registry-url" ) }}
            {{- $schemaRegistryUrl = index .Values "kafka" "schema-registry-url" -}}
        {{- end -}}
    {{- end -}}

- name: KAFKA_GROUPID
  value: {{ index .Values "kafka" "group-id" | quote }}
- name: KAFKA_BOOTSTRAPSERVERS
  value: {{ print $bootstrapServers }}
- name: KAFKA_SCHEMAREGISTRYURL
  value: {{ print $schemaRegistryUrl }}
- name: KAFKA_AUTOOFFSETRESETCONFIG
  value: {{ index .Values "kafka" "auto-offset-reset-config" | quote  }}
- name: KAFKA_ENABLEAUTOCOMMITCONFIG
  value: {{ index .Values "kafka" "enable-auto-commit-config" | quote  }}
{{- end -}}
