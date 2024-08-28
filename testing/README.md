# TESTING

This directory contains scripts and data to test the indexer using scripts written in `javascript` and `bash`.

## Directory structure

- `./js/` contains scripts to generate AVRO records and send them to kafka
- `./logs/` contains logs from the indexer gathered during test execution
- `./samples/` contains some input data the tests are based on


# deploying using helm:

1. Prepare cluster by creating secrets in the target namespace (once per namespace)

```bash
  > kubectl create secret docker-registry k8s-registry \
    --docker-server=armdocker.rnd.ericsson.se \
    --docker-username=$DOCKER_USERNAME \
    --docker-password=$DOCKER_PASSWORD_OR_API_key \
    --docker-email=$EMAIL_ADDRESS --namespace=$NAMESPACE
```

2. Setting up helm charts

    - add dependencies to `./charts/eric-oss-assurance-indexer/Chart.yaml`

```yaml
dependencies:
- name: eric-oss-schema-registry-sr
   repository: https://arm.seli.gic.ericsson.se/artifactory/proj-ec-son-drop-helm/
   version: 1.24.0+1
- name: eric-data-coordinator-zk
   repository: https://arm.sero.gic.ericsson.se/artifactory/proj-adp-gs-all-helm/
   version: 2.0.0-6
- name: eric-data-message-bus-kf
   repository: https://arm.sero.gic.ericsson.se/artifactory/proj-adp-gs-all-helm/
   version: 2.11.0-80
- name: eric-data-search-engine
   repository: https://arm.rnd.ki.sw.ericsson.se/artifactory/proj-adp-gs-released-helm/
   version: 12.3.0+29
```

    - add/update product info `./charts/eric-oss-assurance-indexer/eric-product-info.yaml` by
      replacing `REPO_PATH` by `proj-eric-oss-drop` (or `proj-eric-oss-ci-internal` if not merged yet)
      and `VERSION` by, e.g., `1.23.0-h89d4eda` (see docker image version in Jenkins)

3. Install or upgrade `indexer` into the cloud (namespace `wojtek`)

```bash
  > cd ./charts/
  > helm -n wojtek install indexer eric-oss-assurance-indexer --debug --wait --timeout 10m \
    --set eric-data-search-engine.autoSetRequiredWorkerNodeSysctl=true \
    --set global.pullSecret=k8s-registry,global.security.tls.enabled=false \
    --set eric-data-search-engine.metrics.enabled=false
```

4. Check deployment:

```bash
  > kubectl -n wojtek get all
```

5. Calling Indexer REST-apis

   First, we set port forwarding to the indexer service:

      > kubectl -n wojtek port-forward svc/eric-oss-assurance-indexer 8080:8080

   In a different shell (do not interrupt the previous command), do:

      > curl http://localhost:8080/v1/indexer-info/indexer-list

   Other endpoints are available as well:
   
   ```bash
      > curl http://localhost:8080/v1/indexer-info/indexer-list | jq .
      > curl -X POST -H "Content-Type: application/json" -d @path-to-indexerSpec.json http://localhost:8080/v1/indexer-info/indexer
      > curl -X DELETE http://localhost:8080/v1/indexer-info/indexer?name=indexerA
      > curl http://localhost:8080/v1/indexer-info/indexer?name=indexerA | jq .
      > curl http://localhost:8080/v1/indexer-info/spec/fullcontexts?searchEngineIndexName=an-index | jq .
      > curl "http://localhost:8080/v1/indexer-info/spec/values-for-fullcontext?searchEngineIndexName=an-index&fullContextName=Context1_c1"
      > curl http://localhost:8080/v1/indexer-info/search-engine-index-list
   ```

6. Sending AVRO messages to Kafka

   Firstly, we set port forwarding for Kafka and Schema Registry services (each in its own shell):

          > kubectl -n wojtek port-forward services/eric-data-message-bus-kf 9092:9092
          > kubectl -n wojtek port-forward services/eric-[oss-]schema-registry-sr 8081:8081

   I also had to add the following line to my `/etc/hosts` file:

       127.0.0.1 eric-data-message-bus-kf-2.eric-data-message-bus-kf.wojtek.svc.cluster.local \
          eric-data-message-bus-kf-1.eric-data-message-bus-kf.wojtek.svc.cluster.local \
          eric-data-message-bus-kf-0.eric-data-message-bus-kf.wojtek.svc.cluster.local

   Then, we can send AVRO messages to Kafka using `./testing/js/index.js` script:

          > cd ./testing/js/
          > npm install
          > node index.js
   
   Edit `./testing/js/index.js` to send your AVRO messages to Kafka.

   __WARNING__: I had to play with scaling kafka down and up as I was getting errors:

         > "This server does not host this topic-partition" (with 3 replicas)
         
   or

         > "Messages are rejected since there are fewer in-sync replicas than required" (with 1 replica)
 
   Eventually it worked for me with 2 replicas.
   
         > kubectl -n wojtek scale statefulset eric-data-message-bus-kf --replicas 2
