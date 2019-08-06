package io.agibalov;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.athena.AmazonAthena;
import com.amazonaws.services.athena.AmazonAthenaClientBuilder;
import com.amazonaws.services.athena.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static org.jooq.impl.DSL.*;
import static org.jooq.impl.DSL.field;

public class DummyTest {
    @Test
    public void itShouldWork() throws InterruptedException {
        AmazonS3 amazonS3 = AmazonS3ClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();

        String bucketName = UUID.randomUUID().toString();

        amazonS3.createBucket(new CreateBucketRequest(bucketName));
        try {
            AmazonAthena amazonAthena = AmazonAthenaClientBuilder.standard()
                    .withRegion(Regions.US_EAST_1)
                    .build();

            executeQuery(amazonAthena, bucketName, "create database if not exists test");
            try {
                executeQuery(amazonAthena, bucketName, "CREATE EXTERNAL TABLE if not exists test.planet (\n" +
                        "  id BIGINT,\n" +
                        "  type STRING,\n" +
                        "  tags MAP<STRING,STRING>,\n" +
                        "  lat DECIMAL(9,7),\n" +
                        "  lon DECIMAL(10,7),\n" +
                        "  nds ARRAY<STRUCT<ref: BIGINT>>,\n" +
                        "  members ARRAY<STRUCT<type: STRING, ref: BIGINT, role: STRING>>,\n" +
                        "  changeset BIGINT,\n" +
                        "  timestamp TIMESTAMP,\n" +
                        "  uid BIGINT,\n" +
                        "  user STRING,\n" +
                        "  version BIGINT\n" +
                        ")\n" +
                        "STORED AS ORCFILE\n" +
                        "LOCATION 's3://osm-pds/planet/'");

                String query = makeQuery(Arrays.asList("Jersey City", "Voronezh", "Chirchiq"));
                executeQuery(amazonAthena, bucketName, query);
            } finally {
                executeQuery(amazonAthena, bucketName, "drop database if exists test cascade");
            }
        } finally {
            emptyBucket(amazonS3, bucketName);
            amazonS3.deleteBucket(bucketName);
        }
    }

    private static void executeQuery(
            AmazonAthena amazonAthena,
            String bucketName,
            String query) throws InterruptedException {

        StartQueryExecutionResult startQueryExecutionResult = amazonAthena
                .startQueryExecution(new StartQueryExecutionRequest()
                        .withResultConfiguration(new ResultConfiguration()
                                .withOutputLocation("s3://" + bucketName + "/"))
                        .withQueryString(query));

        GetQueryExecutionRequest getQueryExecutionRequest = new GetQueryExecutionRequest()
                .withQueryExecutionId(startQueryExecutionResult.getQueryExecutionId());
        GetQueryExecutionResult getQueryExecutionResult;
        while(true) {
            getQueryExecutionResult = amazonAthena.getQueryExecution(getQueryExecutionRequest);
            String state = getQueryExecutionResult.getQueryExecution().getStatus().getState();
            System.out.printf("state: %s\n", state);

            if (state.equals(QueryExecutionState.FAILED.toString())) {
                throw new RuntimeException("Failed");
            } else if(state.equals(QueryExecutionState.CANCELLED.toString())) {
                throw new RuntimeException("Cancelled");
            } else if(state.equals(QueryExecutionState.SUCCEEDED.toString())) {
                break;
            } else if(state.equals(QueryExecutionState.QUEUED.toString()) ||
                    state.equals(QueryExecutionState.RUNNING.toString())) {
                Thread.sleep(1000);
            } else {
                throw new RuntimeException("Unexpected state");
            }
        }

        GetQueryResultsRequest getQueryResultsRequest = new GetQueryResultsRequest()
                .withQueryExecutionId(startQueryExecutionResult.getQueryExecutionId());
        List<ColumnInfo> columns = null;
        for(int batchIndex = 0; ; ++batchIndex) {
            GetQueryResultsResult getQueryResultsResult = amazonAthena.getQueryResults(getQueryResultsRequest);

            if(batchIndex == 0) {
                columns = getQueryResultsResult.getResultSet()
                        .getResultSetMetadata().getColumnInfo();
            }

            List<Row> rows = getQueryResultsResult.getResultSet().getRows();
            for(int rowIndex = 1; rowIndex < rows.size(); ++rowIndex) { // for some reason row 0 is header
                Row row = rows.get(rowIndex);
                System.out.printf("Row %d\n", rowIndex);

                List<Datum> data = row.getData();
                for(int columnIndex = 0; columnIndex < data.size(); ++columnIndex) {
                    ColumnInfo column = columns.get(columnIndex);
                    Datum datum = data.get(columnIndex);
                    System.out.printf("  %s (%s): %s\n",
                            column.getName(),
                            column.getType(),
                            datum.getVarCharValue());
                }
            }

            if(getQueryResultsResult.getNextToken() == null) {
                break;
            }
            getQueryResultsRequest = getQueryResultsRequest.withNextToken(getQueryResultsResult.getNextToken());
        }
    }

    private static void emptyBucket(AmazonS3 amazonS3, String bucketName) {
        ObjectListing objectListing = amazonS3.listObjects(bucketName);
        while(true) {
            Iterator<S3ObjectSummary> it = objectListing.getObjectSummaries().iterator();
            while(it.hasNext()) {
                amazonS3.deleteObject(bucketName, it.next().getKey());
            }

            if(objectListing.isTruncated()) {
                objectListing = amazonS3.listNextBatchOfObjects(objectListing);
            } else {
                break;
            }
        }
    }

    /*
    select
      id,
      tags['name'] as name,
      coalesce(tags['is_in'], tags['addr:country']) as location,
      lat,
      lon
    from planet
    where
      type = 'node' and
      tags['place'] = 'city' and
      coalesce(tags['name:en'], tags['name']) in ('Jersey City', 'Voronezh', 'Chirchiq')
    limit 10
     */
    private static String makeQuery(List<String> cities) {
        DSLContext dslContext = DSL.using(SQLDialect.DEFAULT);
        return dslContext.select(
                field("id"),
                field("tags['name']").as("name"),
                coalesce(field("tags['is_in']"), field("tags['addr:country']")).as("location"),
                field("lat"),
                field("lon")
        ).from(table("planet"))
                .where(and(
                        field("type").eq("node"),
                        field("tags['place']").eq("city"),
                        coalesce(field("tags['name:en']"), field("tags['name']")).in(cities)
                ))
                .limit(10)
                .getSQL(true);
    }
}
