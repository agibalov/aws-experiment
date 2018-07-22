# aws-emr-experiment

Learning Elastic Map Reduce.

Do `npm i` to install dependencies for `./tool`.

* `./tool create-key` to create an EC2 key pair.
* `./tool deploy` to deploy everything.
* `./tool undeploy` to undeploy everything.
* `./tool delete-key` to delete an EC2 key pair.

## Experiment #1 - running the custom JAR

* `./gradlew clean build` to build the custom JAR.
* `./tool submit-jar` to create an EMR step.
* After it, go to AWS console, wait for the step to finish and the check the step's stdout.

## Experiment #2 - Hive and S3

* `./tool submit-hive` to create an EMR step.
* Go to AWS console, wait for the step to finish and look for output in S3 bucket.

## Experiment #3 - Hive, S3 and Dynamo

Hive runs on top of EMR and allows one to work with S3 CSVs and DynamoDB table in an SQL-like manner. SSH to EMR master:

```
$(./tool ssh)
```

Then run `hive` for Hive CLI. Create a table that maps to a CSV in S3 bucket:

```
create external table SampleTable(id string, text string)
row format delimited fields terminated by ','
location 's3://emr-experiment-bucket/sample/';

OK
Time taken: 1.339 seconds
```

Select all:

```
select * from SampleTable;

OK
1	hello
2	world
3	world
Time taken: 0.385 seconds, Fetched: 3 row(s)
```

Select count:

```
select count(*) from SampleTable;

Query ID = hadoop_20180722222856_62a9a781-2d77-4d16-a867-c771099129a1
Total jobs = 1
Launching Job 1 out of 1
Status: Running (Executing on YARN cluster with App id application_1532295962582_0004)

----------------------------------------------------------------------------------------------
        VERTICES      MODE        STATUS  TOTAL  COMPLETED  RUNNING  PENDING  FAILED  KILLED  
----------------------------------------------------------------------------------------------
Map 1 .......... container     SUCCEEDED      1          1        0        0       0       0  
Reducer 2 ...... container     SUCCEEDED      1          1        0        0       0       0  
----------------------------------------------------------------------------------------------
VERTICES: 02/02  [==========================>>] 100%  ELAPSED TIME: 26.91 s    
----------------------------------------------------------------------------------------------
OK
3
Time taken: 46.829 seconds, Fetched: 1 row(s)
```

Select group by:

```
select text, count(*) from SampleTable group by text;

Query ID = hadoop_20180722223001_c609a9ed-d6ce-4d7d-8fa9-45f6343ef785
Total jobs = 1
Launching Job 1 out of 1
Status: Running (Executing on YARN cluster with App id application_1532295962582_0004)

----------------------------------------------------------------------------------------------
        VERTICES      MODE        STATUS  TOTAL  COMPLETED  RUNNING  PENDING  FAILED  KILLED  
----------------------------------------------------------------------------------------------
Map 1 .......... container     SUCCEEDED      1          1        0        0       0       0  
Reducer 2 ...... container     SUCCEEDED      1          1        0        0       0       0  
----------------------------------------------------------------------------------------------
VERTICES: 02/02  [==========================>>] 100%  ELAPSED TIME: 19.45 s    
----------------------------------------------------------------------------------------------
OK
hello	1
world	2
Time taken: 22.82 seconds, Fetched: 2 row(s)
```

Create a table that maps to a DynamoDB table:

```
create external table DummyTable(id string, text string)
stored by 'org.apache.hadoop.hive.dynamodb.DynamoDBStorageHandler' 
tblproperties (
  "dynamodb.table.name" = "DummyTable", 
  "dynamodb.column.mapping" = "id:id,text:text"
);

OK
Time taken: 0.188 seconds
```

Verify that the table is empty:

```
select count(*) from DummyTable;

OK
0
Time taken: 0.552 seconds, Fetched: 1 row(s)
```

Insert into DummyTable (Dynamo) from SampleTable (S3 CSV):

```
insert into DummyTable(id, text) select id, text from SampleTable;

Query ID = hadoop_20180722223641_502487ba-b22e-4990-b154-527ceb117fdd
Total jobs = 1
Launching Job 1 out of 1
Tez session was closed. Reopening...
Session re-established.
Status: Running (Executing on YARN cluster with App id application_1532295962582_0005)

----------------------------------------------------------------------------------------------
        VERTICES      MODE        STATUS  TOTAL  COMPLETED  RUNNING  PENDING  FAILED  KILLED  
----------------------------------------------------------------------------------------------
Map 1 .......... container     SUCCEEDED      1          1        0        0       0       0  
----------------------------------------------------------------------------------------------
VERTICES: 01/01  [==========================>>] 100%  ELAPSED TIME: 36.98 s    
----------------------------------------------------------------------------------------------
OK
Time taken: 56.657 seconds
```

Verify the table is not empty anymore:

```
select * from DummyTable;

OK
2	world
1	hello
3	world
Time taken: 0.413 seconds, Fetched: 3 row(s)
```

Create another S3-mapped table:

```
create external table OutputTable(text string, count int)
row format delimited fields terminated by ',' 
location 's3://emr-experiment-bucket/output/';

OK
Time taken: 0.535 seconds
```

Do "insert from select":

```
insert into OutputTable(text, count) select text, count(*) from DummyTable group by text;

Query ID = hadoop_20180722224453_c3c3ebbd-3688-449b-ad34-75a408570cf1
Total jobs = 1
Launching Job 1 out of 1
Status: Running (Executing on YARN cluster with App id application_1532295962582_0005)

----------------------------------------------------------------------------------------------
        VERTICES      MODE        STATUS  TOTAL  COMPLETED  RUNNING  PENDING  FAILED  KILLED  
----------------------------------------------------------------------------------------------
Map 1 .......... container     SUCCEEDED      1          1        0        0       0       0  
Reducer 2 ...... container     SUCCEEDED      1          1        0        0       0       0  
----------------------------------------------------------------------------------------------
VERTICES: 02/02  [==========================>>] 100%  ELAPSED TIME: 25.48 s    
----------------------------------------------------------------------------------------------
Loading data to table default.outputtable
OK
Time taken: 32.219 seconds
```

Check what the data looks like:

```
select * from OutputTable;

OK
hello	1
world	2
Time taken: 0.273 seconds, Fetched: 2 row(s)
```

Also check the actual CSV at `s3://emr-experiment-bucket/output/`:

```
hello,1
world,2
```

NOTE: while SELECTs and INSERTs are totally OK, DELETEs and UPDATEs are not supported (at least by Dynamo).
