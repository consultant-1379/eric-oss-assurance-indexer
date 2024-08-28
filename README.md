# OSS Assurance Indexer Service (AIS)

The Spring Boot Microservice Chassis is a typical spring boot
application with a few additions to enable the service to be built,
tested, containerized and deployed on a Kubernetes cluster.  The
Chassis is available as a Gerrit repository that can be cloned and
duplicated to create new microservice.  While there may be a need to
create multiple chassis templates based on the choice of build tool,
application frameworks and dependencies the current implementation is
a Java and Spring Boot Maven project.

## Contact Information

#### Team Members
Please See the list of team members at the [team's page](https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/IDUN/Team+Aggregators)

##### AIS
[Team Aggregators](https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/IDUN/Team+Aggregators) is currently the acting development team working on the Indexer Service.
Please email the Aggregators Team at <a href="mailto:PDLPDLSWOR@pdl.internal.ericsson.com">PDLTHEAGGR@pdl.internal.ericsson.com</a> for support.

##### CI Pipeline
The CI Pipeline aspect of the Microservice Chassis is now owned, developed and maintained by [Team Hummingbirds](https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/ACE/Hummingbirds+Home) in the DE (Development Environment) department of PDU OSS.

#### Support
Guardians for this project can be reached through the <a href="https://teams.microsoft.com/_#/conversations/General?threadId=19:HCjb2m8J207mafc2X6QK_X_WW0e-M-CeNP-T1ZM1rpI1@thread.tacv2&ctx=channel">Aggregators Teams Channel</a>.

## Maven Dependencies
The chassis has the following Maven dependencies:
  - Spring Boot Start Parent version 3.1.3
  - Spring Boot Starter Web
  - Spring Boot Starter Validation
  - Spring Boot Starter WebFlux
  - Spring Boot Actuator
  - Spring Cloud Sleuth
  - Spring Boot Started Test
  - OpenSearch Client
  - OpenSearch Java
  - Apache Avro
  - Kafka Avro Serializer
  - Spring Kafka
  - Apache HttpComponents Client5
  - JaCoCo Code Coverage Plugin
  - Sonar Maven Plugin
  - Spotify Dockerfile Maven Plugin
  - Common Logging utility for logback created by Vortex team
  - Micrometer Registry Prometheus
```
<version.spring-cloud>2020.0.3</version.spring-cloud>
```

## Build related artifacts
The main build tool is BOB provided by ADP. For convenience, maven wrapper is provided to allow the developer to build in an isolated workstation that does not have access to ADP.
  - [ruleset2.0.yaml](ruleset2.0.yaml) - for more details on BOB please see [Bob 2.0 User Guide](https://gerrit-gamma.gic.ericsson.se/plugins/gitiles/adp-cicd/bob/+/refs/heads/master/USER_GUIDE_2.0.md).
     You can also see an example of Bob usage in a Maven project in [BOB](https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/PCNCG/Adopting+BOB+Into+the+MVP+Project).
  - [precoderview.Jenkinsfile](precodereview.Jenkinsfile) - for pre code review Jenkins pipeline that runs when patch set is pushed.
  - [publish.Jenkinsfile](publish.Jenkinsfile) - for publish Jenkins pipeline that runs after patch set is merged to master.
  - [.bob.env](.bob.env) - if you are running Bob for the first time this file will not be available on your machine.
    For more details on how to set it up please see [Bob 2.0 User Guide](https://gerrit-gamma.gic.ericsson.se/plugins/gitiles/adp-cicd/bob/+/refs/heads/master/USER_GUIDE_2.0.md).

If the developer wishes to manually build the application in the local workstation, the ```bob clean init-dev build image package-local``` command can be used once BOB is configured in the workstation.  
Note: The ```mvn clean install``` command will be required before running the bob command above.  
See the "Containerization and Deployment to Kubernetes cluster" section for more details on deploying the built application.

Stub jar files are necessary to allow contract tests to run. The stub jars are stored in JFrog (Artifactory).
To allow the contract test to access and retrieve the stub jars, the .bob.env file must be configured as follows.
```
SELI_ARTIFACTORY_REPO_USER=<LAN user id>
SELI_ARTIFACTORY_REPO_PASS=<JFrog encripted LAN PWD or API key>
HOME=<path containing .m2, e.g. /c/Users/<user>/>
```
To retrieve an encrypted LAN password or API key, login to [JFrog](https://arm.seli.gic.ericsson.se) and select "Edit Profile".
For info in setting the .bob.env file see [Bob 2.0 User Guide](https://gerrit-gamma.gic.ericsson.se/plugins/gitiles/adp-cicd/bob/+/refs/heads/master/USER_GUIDE_2.0.md).

## Containerization and Deployment to Kubernetes cluster.
Following artifacts contains information related to building a container and enabling deployment to a Kubernetes cluster:
- [charts](charts/) folder - used by BOB to lint, package and upload helm chart to helm repository.
  -  Once the project is built in the local workstation using the ```bob clean init-dev build image package-local``` command, a packaged helm chart is available in the folder ```.bob/eric-oss-assurance-indexer-internal/``` folder.
     This chart can be manually installed in Kubernetes using ```helm install``` command. [P.S. required only for Manual deployment from local workstation]
- [Dockerfile](Dockerfile) - used by Spotify dockerfile maven plugin to build docker image.
  - The base image for the chassis application is ```sles-jdk8``` available in ```armdocker.rnd.ericsson.se```.

## Source
The [src](src/) folder of the java project contains the core spring boot application and corresponding java unit tests.

```
src
├── main
│   ├── java
│   │   ├── META-INF
│   │   │   └── MANIFEST.MF
│   │   └── com
│   │       └── ericsson
│   │           └── oss
│   │               └── air
│   │                   ├── AppConfig.java
│   │                   ├── CoreApplication.java
│   │                   ├── IndexerDB.java
│   │                   ├── RecordConsumer.java
│   │                   ├── Writer.java
│   │                   ├── configuration
│   │                   │   └── metrics
│   │                   │       ├── IndexerMetrics.java
│   │                   │       ├── IndexerMetricsRegistration.java
│   │                   │       └── MetricDescription.java
│   │                   ├── opensearch
│   │                   │       ├── OpenSearchConfig.java
│   │                   │       └── OpenSearchProperties.java
│   │                   ├── controller
│   │                   │   ├── IndexerApiImpl.java
│   │                   │   ├── health
│   │                   │   │   ├── HealthCheck.java
│   │                   │   │   └── package-info.java
│   │                   │   └── package-info.java
│   │                   ├── exception
│   │                   │   ├── HttpNotFoundException.java
│   │                   │   └── IndexerRestExceptionHandler.java
│   │                   ├── log
│   │                   │   └── LoggerHandler.java
│   │                   ├── security
│   │                   │   ├── CertificateEventChangeDetector.java
│   │                   │   ├── ChangeDetector.java
│   │                   │   ├── CustomSslStoreBundle.java
│   │                   │   ├── RestTemplateReloader.java
│   │                   │   ├── config
│   │                   │   │   ├── CertificateChangeConfiguration.java
│   │                   │   │   ├── CertificateId.java
│   │                   │   │   ├── RestTemplateConfiguration.java
│   │                   │   │   ├── SecurityProperties.java
│   │                   │   │   └── ServerConfiguration.java
│   │                   │   └── utils
│   │                   │       ├── KeystoreUtil.java
│   │                   │       ├── SecurityUtil.java
│   │                   │       └── exceptions
│   │                   │           └── InternalRuntimeException.java
│   │                   ├── services
│   │                   │   └── OpenSearchService.java
│   │                   └── util
│   │                       └── IndexerProcessingTimeTracker.java
│   └── resources
│       ├── application.yaml
│       ├── jmx
│       │   ├── jmxremote.access
│       │   └── jmxremote.password
│       ├── open-search
│       │   ├── internal-index-mapping.json
│       │   └── mapping.json
│       └── v1
│           ├── eric-oss-assurance-indexer-openapi.yaml
│           └── index.html
└── test
    ├── java
    │   └── com
    │       └── ericsson
    │           └── oss
    │               └── air
    │                   ├── AppConfigTest.java
    │                   ├── CoreApplicationTest.java
    │                   ├── IndexerDBTest.java
    │                   ├── RecordConsumerTest.java
    │                   ├── WriterTest.java
    │                   ├── ais
    │                   │   └── contract
    │                   │       ├── AssuranceIndexerApiBase.java
    │                   │       ├── IndexTestUtil.java
    │                   │       └── package-info.java
    │                   ├── business
    │                   │   └── package-info.java
    │                   ├── configuration
    │                   │   ├── metrics
    │                   │   │   └── MetricsRegistrationTest.java
    │                   │   └── opensearch
    │                   │       ├── OpenSearchConfigTest.java
    │                   │       └── OpenSearchConfigTlsTest.java
    │                   ├── controller
    │                   │   ├── ApiImplTest.java
    │                   │   ├── IndexerSpecValidationTest.java
    │                   │   └── health
    │                   │       ├── HealthCheckTest.java
    │                   │       └── package-info.java
    │                   ├── logging
    │                   │   ├── LogDualAppenderTest.java
    │                   │   ├── LogDualTlsAppenderTest.java
    │                   │   ├── LogHttpAppenderTest.java
    │                   │   ├── LogHttpsAppenderTest.java
    │                   │   ├── LogJsonAppenderTest.java
    │                   │   ├── LogPlainTextAppenderTest.java
    │                   │   └── LoggingWiremockTestSetup.java
    │                   ├── security
    │                   │   ├── AbstractTestSetup.java
    │                   │   ├── AbstractTlsTestSetup.java
    │                   │   ├── CertificateEventChangeIntegrationTest.java
    │                   │   ├── CertificateEventChangeTest.java
    │                   │   ├── CustomSslStoreBundleTest.java
    │                   │   ├── KeystoreUtilsTest.java
    │                   │   ├── RestTemplateReloaderTest.java
    │                   │   └── utils
    │                   │       ├── CertificateEventChangeTestInitializer.java
    │                   │       └── SecurityTestUtils.java
    │                   ├── package-info.java
    │                   ├── producer
    │                   │   ├── SimpleKafkaAvroProducer.java
    │                   │   └── TimingKafkaAvroProducer.java
    │                   └── services
    │                       └── OpenSearchServiceTest.java
    └── resources
        ├── META-INF
        │   └── MANIFEST.MF
        ├── avro-schemas
        │   ├── aRecord.avsc
        │   ├── schema1.avsc
        │   ├── schema2.avsc
        │   ├── schema3.avsc
        │   └── schema4.avsc
        ├── contracts
        │   └── v1
        │       ├── DeleteIndexerContractTest.yaml
        │       ├── DeleteMissingIndexerContractTest.yaml
        │       ├── GetIndexerContractTest.yaml
        │       ├── GetMissingIndexerContractTest.yaml
        │       ├── GetRegisteredIndexerListContractTest.yaml
        │       └── PutIndexerContractTest.yaml
        ├── json-files
        │   ├── indexer-spec-3.json
        │   ├── indexer-spec-4.json
        │   ├── indexer-spec-full.json
        │   ├── indexer-spec.json
        │   ├── indexer.json
        │   └── search-engine-documents.json
        ├── mockito-extensions
        │   └── org.mockito.plugins.MockMaker
        └── security
            ├── log
            │   └── keystore
            │      ├── clientcert.crt
            │       └── clientkey.pem
            ├── log-appender
            │   └── keystore.jks
            ├── log-updated
            │   └── keystore
            │       ├── clientcert.crt
            │       └── clientkey.pem
            ├── root
            │  └── truststore
            │       └── cacert.crt
            ├── server
            │   ├── keys
            │   │   ├── cakey.pem
            │   │   ├── clientkey-updated.pem
            │   │   └── clientkey.pem
            │   ├── keystore
            │   │   ├── srvcert.crt
            │   │   └── srvprivkey.pem
            │   ├── keystore.p12
            │   ├── truststore
            │   │   ├── cacert.crt
            │   │   ├── cacert.srl
            │   │   └── clientcert.crt
            │   └── truststore.p12
            ├── server-empty
            │   └── keystore.p12
            ├── server-invalid
            │   └── keystore
            │       ├── srvcert.crt
            │       └── srvprivkey.pem
            └── server-updated
                ├── keystore
                │   ├── srvcert.crt
                │   └── srvprivkey.pem
                ├── keystore.p12
                ├── truststore
                │   └── clientcert-updated.crt
                └── truststore.p12
```

## Setting up CI Pipeline
-  Docker Registry is used to store and pull Docker images. At Ericsson official chart repository is maintained at the org-level JFrog Artifactory.
   Follow the link to set up a [Docker registry](https://confluence.lmera.ericsson.se/pages/viewpage.action?spaceKey=ACD&title=How+to+create+new+docker+repository+in+ARM+artifactory).
-  Helm repo is a location where packaged charts can be stored and shared. The official chart repository is maintained at the org-level JFrog Artifactory.
   Follow the link to set up a [Helm repo](https://confluence.lmera.ericsson.se/display/ACD/How+to+setup+Helm+repositories+for+ADP+e2e+CICD).
-  Follow instructions at [Jenkins Pipeline setup](https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/PCNCG/Microservice+Chassis+CI+Pipeline+Start+Guide#MicroserviceChassisCIPipelineStartGuide-JenkinsPipelinesetup)
   to use out-of-box Jenkinsfiles which comes along with eric-oss-assurance-indexer.
-  Jenkins Setup involves master and agent machines. If there is not any Jenkins master setup, follow instructions at [Jenkins Master](https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/PCNCG/Microservice+Chassis+CI+Pipeline+Start+Guide#MicroserviceChassisCIPipelineStartGuide-JenkinsMaster-2.89.2(FEMJenkins)) - 2.89.2 (FEM Jenkins).
-  Request a node from the GIC (Note: RHEL 7 GridEngine Nodes have been successfully tested).
   [Request Node](https://estart.internal.ericsson.com/).
-  To setup [Jenkins Agent](https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/PCNCG/Microservice+Chassis+CI+Pipeline+Start+Guide#MicroserviceChassisCIPipelineStartGuide-Prerequisites)
   for Jenkins, jobs execution follow the instructions at Jenkins Agent Setup.
-  The provided ruleset is designed to work in standard environments, but in case you need, you can fine tune the automatically generated ruleset to adapt to your project needs.
   Take a look at [Bob 2.0 User Guide](https://gerrit-gamma.gic.ericsson.se/plugins/gitiles/adp-cicd/bob/+/refs/heads/master/USER_GUIDE_2.0.md) for details about ruleset configuration.

   [Gerrit Repos](https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/PCNCG/Design+and+Development+Environment)  
   [BOB](https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/PCNCG/Adopting+BOB+Into+the+MVP+Project)  
   [Bob 2.0 User Guide](https://gerrit-gamma.gic.ericsson.se/plugins/gitiles/adp-cicd/bob/+/refs/heads/master/USER_GUIDE_2.0.md)  
   [Docker registry](https://confluence.lmera.ericsson.se/pages/viewpage.action?spaceKey=ACD&title=How+to+create+new+docker+repository+in+ARM+artifactory)  
   [Helm repo](https://confluence.lmera.ericsson.se/display/ACD/How+to+setup+Helm+repositories+for+ADP+e2e+CICD)  
   [Jenkins Master](https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/PCNCG/Microservice+Chassis+CI+Pipeline+Start+Guide#MicroserviceChassisCIPipelineStartGuide-JenkinsMaster-2.89.2(FEMJenkins))  
   [Jenkins Agent](https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/PCNCG/Microservice+Chassis+CI+Pipeline+Start+Guide#MicroserviceChassisCIPipelineStartGuide-Prerequisites)  
   [Jenkins Pipeline setup](https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/PCNCG/Microservice+Chassis+CI+Pipeline+Start+Guide#MicroserviceChassisCIPipelineStartGuide-JenkinsPipelinesetup)  
   [EO Common Logging](https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/ESO/EO+Common+Logging+Library)  
   [SLF4J](https://logging.apache.org/log4j/2.x/log4j-slf4j-impl/index.html)  
   [JFrog](https://arm.seli.gic.ericsson.se)  
   [Request Node](https://estart.internal.ericsson.com/)

### Custom Pipeline Stage for Documentation Upload to ADP Marketplace
A custom pipeline stage exists for uploading documents to the ADP Marketplace.  A local_ruleset.yaml file exists for that purpose and with potential for further use.
Config files were added to the ci directory for this purpose.  The Documents to be uploaded are controlled by rules in the local_ruleset file and these ci/config files.
There is also a jenkins file that exists for uploading the documents. See [Hybrid MS CI Pipeline V2 documentation](https://confluence-oss.seli.wh.rnd.internal.ericsson.com/pages/viewpage.action?spaceKey=DGBase&title=Hybrid+MS+CI+Pipeline+-+v2) for details.

## Using the Helm Repo API Token
The Helm Repo API Token is usually set using credentials on a given Jenkins FEM.
If the project you are developing is part of IDUN/Aeonic this will be pre-configured for you.
However, if you are developing an independent project please refer to the 'Helm Repo' section:
[Places records from DMM into a ADP Search Engine index for use in exploring the data CI Pipeline Guide](https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/PCNCG/Microservice+Chassis+CI+Pipeline+Start+Guide#MicroserviceChassisCIPipelineStartGuide-HelmRepo)

Once the Helm Repo API Token is made available via the Jenkins job credentials the precodereview and publish Jenkins jobs will accept the credentials (ex. HELM_SELI_REPO_API_TOKEN' or 'HELM_SERO_REPO_API_TOKEN) and create a variable HELM_REPO_API_TOKEN which is then used by the other files.

Credentials refers to a user or a functional user. This user may have access to multiple Helm repos.
In the event where you want to change to a different Helm repo, that requires a different access rights, you will need to update the set credentials.

## Artifactory Set-up Explanation
The Places records from DMM into a ADP Search Engine index for use in exploring the data Artifactory repos (dev, ci-internal and drop) are set up following the ADP principles: [ADP Repository Principles](https://confluence.lmera.ericsson.se/pages/viewpage.action?spaceKey=AA&title=2+Repositories)

The commands: "bob init-dev build image package" will ensure that you are pushing a Docker image to:
[Docker registry - Dev](https://arm.seli.gic.ericsson.se/artifactory/docker-v2-global-local/proj-eric-oss-dev/)

The Precodereview Jenkins job pushes a Docker image to:
[Docker registry - CI Internal](https://arm.seli.gic.ericsson.se/artifactory/docker-v2-global-local/proj-eric-oss-ci-internal/)

This is intended behaviour which mimics the behavior of the Publish Jenkins job.
This job presents what will happen when the real microservice image is being pushed to the drop repository.
Furthermore, the 'Helm Install' stage needs a Docker image which has been previously uploaded to a remote repository, hence why making a push to the CI Internal is necessary.

The Publish job also pushes to the CI-Internal repository, however the Publish stage promotes the Docker image and Helm chart to the drop repo:
[Docker registry - Drop](https://arm.seli.gic.ericsson.se/artifactory/docker-v2-global-local/proj-eric-oss-drop/)

Similarly, the Helm chart is being pushed to three separate repositories:
[Helm registry - Dev](https://arm.seli.gic.ericsson.se/artifactory/proj-eric-oss-dev-helm/)

The Precodereview Jenkins job pushes the Helm chart to:
[Helm registry - CI Internal](https://arm.seli.gic.ericsson.se/artifactory/proj-eric-oss-ci-internal-helm/)

This is intended behaviour which mimics the behavior of the Publish Jenkins job.
This job presents what will happen when the real Helm chart is being pushed to the drop repository.
The Publish Jenkins job pushes the Helm chart to:
[Helm registry - Drop](https://arm.seli.gic.ericsson.se/artifactory/proj-eric-oss-drop-helm/)

## Logging

AIS uses [EO Common Logging](https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/ESO/EO+Common+Logging+Library#EOCommonLoggingLibrary-Howtousethelibrarysoastoadoptdirectstreaming/) compliant with the [LOG General Design Rules](https://eteamspace.internal.ericsson.com/display/AA/LOG+general+design+rules/) from the ADP Architecture Framework as an interface to the [ADP logging-system](https://adp.ericsson.se/workinginadpframework/tutorials/logging-in-adp/introduction-to-logging-architecture/).  

There is one [use-case for design](https://eteamspace.internal.ericsson.com/pages/viewpage.action?pageId=1161863207), though there are others for deployment and run-time, which says all logs must conform to the log schema.  The common-logging library takes care of providing information for many of the fields based on configuration we provide for it to use.

AIS uses the common-logging package as an interface/wrapper to the ADP logging system results in the log flow through all components as seen in this [log-flow](https://confluence-oss.seli.wh.rnd.internal.ericsson.com/pages/viewpage.action?spaceKey=ESO&title=Log+flow).  Note the ADP logging system components as seen in the [logging tutorial](https://adp.ericsson.se/workinginadpframework/tutorials/logging-in-adp/introduction-to-logging-architecture) is represented in the use of ***common-logging***.

The logging system has several [log collection patterns](https://eteamspace.internal.ericsson.com/display/AA/Log+Collection+Patterns) and as recommended for production, the **STDOUT indirect streaming assisted by the K8S infrastructure** is used.  (In the future we may progress to using a direct streaming method for security reasons.)

Logs an be output in JSON or as plain text formats.

With current configurations, they come out in json format.  An example from one of the test-cases is:

    `{"timestamp":"2022-12-14T15:22:12.185-05:00","version":"0.3.0","message":"Sample service called sample","logger":"com.ericsson.oss.air.controller.example.SampleApiControllerImpl","thread":"main","service_id":"unknown","severity":"info"}`

where the program statement was:

    `LOG.info("Sample service called sample")`

## AIS Custom Metrics

AIS exposes the following custom metrics via prometheus:

- **ais_startup_time_seconds** : Number of seconds for AIS to startUp after the
  application is declared Ready by Spring-Boot
  - Also see related system metrics **_application_started_time_seconds_** and
    **_application_ready_time_seconds_**
- **ais_index_records_received_total** : Total number of index records received
  by AIS
- **ais_index_records_processed_total** : Total number of index records
  processed
  by AIS

To access these metrics, run the AIS application and the metrics should be available
at [http://localhost:<port-number>/actuator/prometheus](http://localhost:8080/actuator/prometheus)
and in prometheus where one can select and graph the time-based metrics: [http://localhost:9090/](http://localhost:9090).
