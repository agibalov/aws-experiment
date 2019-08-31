import sys
from awsglue.transforms import *
from awsglue.utils import getResolvedOptions
from pyspark.context import SparkContext
from awsglue.context import GlueContext
from awsglue.job import Job

args = getResolvedOptions(sys.argv, [
    'JOB_NAME',
    'glue_database',
    'glue_table',
    'target_s3_prefix'])

sc = SparkContext()
glueContext = GlueContext(sc)
spark = glueContext.spark_session
job = Job(glueContext)
job.init(args['JOB_NAME'], args)

datasource0 = glueContext.create_dynamic_frame.from_catalog(
    database = args['glue_database'],
    table_name = args['glue_table'],
    transformation_ctx = "datasource0")

applymapping1 = ApplyMapping.apply(
    frame = datasource0,
    mappings = [
        ("id", "string", "id", "string"),
        ("salary", "int", "sss", "int")
    ],
    transformation_ctx = "applymapping1")

datasink2 = glueContext.write_dynamic_frame.from_options(
    frame = applymapping1,
    connection_type = "s3",
    connection_options = {
        "path": args['target_s3_prefix']
    },
    format = "csv",
    transformation_ctx = "datasink2")

job.commit()
