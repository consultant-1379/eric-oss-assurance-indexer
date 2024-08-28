# Javascript helpers for testing

## Set Up
These javascript and json files are only used for lab testing and must never be 
used in the runtime application.  Therefore, the first thing to do before starting to do 
any lab testing is to change the name of the directory _package_json_ 
to _package.json_.  After testing, change the name of the directory back to 
package_json to avoid having these packages brought into the list of 
dependencies for the runtime application. 

## Generating "IndexerSpecs"

File `../samples/soa.json` is a manually created `IndexerSpec` for producing 
documents of the mock data given in `../samples/data.json`. 
From that `../samples/soa.json` file, which specifies many writters, 
we generate a new IndexerSpecs per, each writing to a different topic. 

By running

```bash
> node ./genSpecs_js
```

we generate all corresponding `IndexerSpecs` into
`./indexerSpecs/soa.*`. Those are the files we POST to Indexer:

```bash
> for i in ./indexerSpecs/soa.*; do curl -X POST -H "Content-Type: application/json" -d @$i http://localhost:8080/v1/indexer-info/indexer; done
```

## Generate and send out AVRO records 

Script `./index_js` will "reverse-engeener" the `../samples/soa.json` file and
transform the mock `../samples/data.json` into kafka AVRO records (as they could
be produced by PMSC) and then send them to kafka.

Inorder to run the script, you need:
1. run `npm install` to install all `js` dependencies
2. to have kafka and schema registry running on localhost (e.g., via port-forwarding -- see below):

- kafka: `localhost:9092`
- schema registry `http://localhost:8081`

Then, you can run the script:  

```bash
> node index_js
```

The above script generates 115 AVRO records and sends them to the following kafka topics:
`soa.nf`, `soa.nf_generic`, `soa.nsi`, `soa.site`, `soa.site_generic`, `soa.snssai`.


## Running tests

### Local port forwarding

```bash
kubectl -n ais-deploy port-forward svc/eric-oss-assurance-indexer 8080:8080 &
kubectl -n ais-deploy port-forward svc/eric-data-search-engine 9200:9200 &
kubectl -n ais-deploy port-forward services/eric-schema-registry-sr 8081:8081 &
kubectl -n ais-deploy port-forward eric-oss-dmm-kf-op-sz-kafka-0 9092:9092 &
# kubectl -n ais-deploy port-forward eric-oss-dmm-kf-op-sz-kafka-1 9092:9092
# kubectl -n ais-deploy port-forward eric-oss-dmm-kf-op-sz-kafka-2 9092:9092
```

I also had to modify my `/etc/hosts` file to add the following entries:

```bash
127.0.0.1 eric-oss-dmm-kf-op-sz-kafka-2.eric-oss-dmm-kf-op-sz-kafka-brokers.ais-deploy.svc 
127.0.0.1 eric-oss-dmm-kf-op-sz-kafka-1.eric-oss-dmm-kf-op-sz-kafka-brokers.ais-deploy.svc 
127.0.0.1 eric-oss-dmm-kf-op-sz-kafka-0.eric-oss-dmm-kf-op-sz-kafka-brokers.ais-deploy.svc
```

The choice of the kafka broker is dependent where the given topic is located. 
I found it by try-and-error and for my deployment is:

```
['site', 'site_generic']; // kafka-0
['nf', 'snssai'];         // kafka-1
['nsi', 'nf_generic'];    // kafka-2
```

### Generate AVRO records on Kafka

Once I found out the location of the topics, I chose `kafka-2` for my tests. 
Since the script `index.js` writes to many topics, I added a selector (constant `seleced_topics`)
which holds the list of topics of the selected kafka. I manually adjusted that value to:

```javascript
const selected_topics = ['nsi', 'nf_generic'];
```

Then I ran the script:

```bash
> for i in {1..100}; do node index.js; done
```

That step is slow, about 20secs * 100 = 2000secs = 33mins and 
it puts 55 * 100 = 5500 records in the two kafka topics.

To speed up the population process, run the script in parallel:

```bash
> for j in {1..10}; do for i in {1..10}; do node index.js & done; done
```

### Execute the tests

#### Stop the indexer

```bash
kubectl -n ais-deploy scale deployment eric-oss-assurance-indexer --replicas 0
```

#### Reset the Kafka consumer group offset

```bash
kubectl -n ais-deploy exec -it eric-oss-dmm-kf-op-sz-kafka-2 bash
cd /opt/kafka/
./bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group indexer-007 --describe
sleep 60 # or more!
./bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group indexer-007 --reset-offsets --to-earliest --execute --all-topics
```

#### Start the indexer and `watch` the logs

```bash
kubectl -n ais-deploy scale deployment eric-oss-assurance-indexer --replicas 1
kubectl logs -n ais-deploy pods/eric-oss-assurance-indexer-6c947bb679-svldk -f > logs/ais-svldk.log
```

#### Testing processing of ~5000 records

The all Kafka topices are cleared (consumed) in about 1 minute. After that, I stopped gathering logs.
In the following example I use `k`, an alternative to `jq`. To install `k` do `npm install -g @fraczak/k`.
Or you could use `jq` instead.

```bash
cat logs/ais-perf-logs.log | grep "Received\|Adding" | head -n 30 | k '[.timestamp, ": ", .message] CONCAT' 
"2023-09-28T17:49:33.273+0000: At StartUp Time: RecordsReceivedCount:0.0 RecordsProcessedCounter:0.0"
"2023-09-28T17:49:33.373+0000: RecordsReceived: 0.0 RecordsProcessed: 0.0"
"2023-09-28T17:49:47.193+0000: [Consumer clientId=consumer-indexer-007-1, groupId=indexer-007] Adding newly assigned partitions: soa.nf-0, soa.nf_generic-0, soa.nsi-0, soa.site-0, soa.site_generic-0, soa.snssai-0"
"2023-09-28T17:49:49.675+0000: Received 500 records"
"2023-09-28T17:49:53.176+0000: Adding 1500 documents to index: 'assurance-soa'."
"2023-09-28T17:49:53.679+0000: Received 500 records"
"2023-09-28T17:49:55.372+0000: Adding 1500 documents to index: 'assurance-soa'."
"2023-09-28T17:49:55.671+0000: Received 500 records"
"2023-09-28T17:49:57.571+0000: Adding 1500 documents to index: 'assurance-soa'."
"2023-09-28T17:49:57.770+0000: Received 500 records"
"2023-09-28T17:49:58.882+0000: Adding 1500 documents to index: 'assurance-soa'."
"2023-09-28T17:49:59.076+0000: Received 500 records"
"2023-09-28T17:49:59.875+0000: Adding 1500 documents to index: 'assurance-soa'."
"2023-09-28T17:49:59.979+0000: Received 500 records"
"2023-09-28T17:50:00.880+0000: Adding 1500 documents to index: 'assurance-soa'."
"2023-09-28T17:50:01.078+0000: Received 500 records"
"2023-09-28T17:50:02.076+0000: Adding 1500 documents to index: 'assurance-soa'."
"2023-09-28T17:50:02.179+0000: Received 500 records"
"2023-09-28T17:50:03.473+0000: Adding 1500 documents to index: 'assurance-soa'."
"2023-09-28T17:50:03.580+0000: Received 500 records"
"2023-09-28T17:50:04.374+0000: Adding 1500 documents to index: 'assurance-soa'."
"2023-09-28T17:50:04.571+0000: Received 449 records"
"2023-09-28T17:50:05.370+0000: Adding 1347 documents to index: 'assurance-soa'."
"2023-09-28T17:50:05.472+0000: Received 47 records"
"2023-09-28T17:50:05.624+0000: Adding 141 documents to index: 'assurance-soa'."
"2023-09-28T17:50:05.680+0000: Received 3 records"
"2023-09-28T17:50:05.801+0000: Adding 9 documents to index: 'assurance-soa'."
"2023-09-28T17:50:05.881+0000: Received 1 records"
"2023-09-28T17:50:05.995+0000: Adding 3 documents to index: 'assurance-soa'."
"2023-09-28T17:50:06.174+0000: Received 1 records"
...
```

On avarage, a full batch (500 records generating 1500 documents) takes less than 2 seconds, 
i.e, 5000 records are processed in less than 20 seconds.

#### Testing processing of ~20K records

```bash

```bash
[wojtek@cenx logs]$ cat ./logs/ais-9r5dw.log | grep "Received" | k '[.timestamp, ": ", .message] CONCAT'
"2023-09-29T18:32:36.425+0000: At StartUp Time: RecordsReceivedCount:0.0 RecordsProcessedCounter:0.0"
"2023-09-29T18:32:36.428+0000: RecordsReceived: 0.0 RecordsProcessed: 0.0"
"2023-09-29T18:32:53.314+0000: Received 500 records"
"2023-09-29T18:32:57.725+0000: Received 500 records"
"2023-09-29T18:32:59.324+0000: Received 500 records"
"2023-09-29T18:33:00.516+0000: Received 500 records"
"2023-09-29T18:33:01.523+0000: Received 500 records"
"2023-09-29T18:33:02.622+0000: Received 500 records"
"2023-09-29T18:33:03.525+0000: Received 500 records"
"2023-09-29T18:33:05.925+0000: Received 500 records"
"2023-09-29T18:33:08.324+0000: Received 500 records"
"2023-09-29T18:33:10.218+0000: Received 500 records"
"2023-09-29T18:33:11.125+0000: Received 500 records"
"2023-09-29T18:33:12.415+0000: Received 500 records"
"2023-09-29T18:33:13.526+0000: Received 500 records"
"2023-09-29T18:33:14.824+0000: Received 500 records"
"2023-09-29T18:33:15.723+0000: Received 500 records"
"2023-09-29T18:33:16.520+0000: Received 500 records"
"2023-09-29T18:33:17.528+0000: Received 500 records"
"2023-09-29T18:33:19.018+0000: Received 500 records"
"2023-09-29T18:33:20.420+0000: Received 500 records"
"2023-09-29T18:33:21.917+0000: Received 500 records"
"2023-09-29T18:33:23.421+0000: Received 500 records"
"2023-09-29T18:33:24.722+0000: Received 500 records"
"2023-09-29T18:33:26.118+0000: Received 500 records"
"2023-09-29T18:33:26.618+0000: Received 500 records"
"2023-09-29T18:33:27.420+0000: Received 500 records"
"2023-09-29T18:33:28.129+0000: Received 500 records"
"2023-09-29T18:33:28.717+0000: Received 500 records"
"2023-09-29T18:33:29.621+0000: Received 84 records"
"2023-09-29T18:33:30.022+0000: Received 500 records"
"2023-09-29T18:33:31.020+0000: Received 500 records"
"2023-09-29T18:33:32.227+0000: Received 500 records"
"2023-09-29T18:33:33.416+0000: Received 500 records"
"2023-09-29T18:33:34.619+0000: Received 500 records"
"2023-09-29T18:33:36.217+0000: Received 500 records"
"2023-09-29T18:33:37.517+0000: Received 500 records"
"2023-09-29T18:33:39.220+0000: Received 500 records"
"2023-09-29T18:33:40.323+0000: Received 500 records"
"2023-09-29T18:33:40.931+0000: Received 500 records"
"2023-09-29T18:33:41.516+0000: Received 500 records"
"2023-09-29T18:33:42.014+0000: Received 500 records"
"2023-09-29T18:33:42.917+0000: Received 500 records"
"2023-09-29T18:33:43.328+0000: Received 500 records"
"2023-09-29T18:33:43.920+0000: Received 500 records"
"2023-09-29T18:33:44.618+0000: Received 500 records"
"2023-09-29T18:33:45.415+0000: Received 500 records"
"2023-09-29T18:33:46.524+0000: Received 353 records"
```

The above shows that the indexer (one instance) is able to process over 20K
records in 1 minute.

