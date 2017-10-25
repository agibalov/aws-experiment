#!/usr/bin/python

import subprocess
import string
import sys

if len(sys.argv) < 2:
    print "No command specified"
    sys.exit(1)

command = sys.argv[1]

def shell(commandTemplate, **kwargs):
    command = string.Template(commandTemplate).substitute(kwargs)
    print "Executing: %s" % (command,)
    process = subprocess.Popen(command, shell = True, executable = "/bin/bash", stdout = subprocess.PIPE)
    process.wait()
    print "Status code: %d" % (process.returncode,)

templateFile = "cf.yml"
stackName = "dummy1"
region = "us-east-1"
bucketName = "er32r23r23r23"

if command == "deploy":
    print "DEPLOYING"

    shell('''aws cloudformation deploy \
        --template-file ${templateFile} \
        --stack-name ${stackName} \
        --capabilities CAPABILITY_IAM \
        --region ${region} \
        --parameter-overrides \
        MyBucketName=${bucketName}''',
          templateFile = templateFile,
          stackName = stackName,
          region = region,
          bucketName = bucketName)

    shell('''aws s3 cp public s3://${bucketName}/ \
        --recursive \
        --acl public-read''',
          bucketName = bucketName)

elif command == "undeploy":
    print "UNDEPLOYING"

    shell('''aws s3 rm s3://${bucketName}/ \
        --recursive''',
          bucketName = bucketName)

    shell('''aws cloudformation delete-stack \
        --stack-name ${stackName} \
        --region ${region}''',
          stackName = stackName,
          region = region)

    shell('''aws cloudformation wait stack-delete-complete \
        --stack-name ${stackName} \
        --region ${region}''',
          stackName = stackName,
          region = region)

else:
    print "Unknown command '%s'" % (command,)
