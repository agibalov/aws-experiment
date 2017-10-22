const { spawnSync } = require('child_process');

const gap = require('grunt-as-promised');

module.exports = (grunt) => {
    gap.configure(grunt);

    const region = 'us-east-1';
    const stackName = 'dummy1';
    const bucketName = 'wer23r23r23r2r2';

    grunt.registerPromiseTask('outputs', async () => {
        const websiteUrl = getStackOutput('WebsiteURL');
        console.log(`websiteUrl: ${websiteUrl}`);

        const CF = require('aws-sdk/clients/cloudformation');
        const cf = new CF({
            region
        });
        const data = await cf.describeStacks({
            StackName: stackName
        }).promise();

        const outputs = data.Stacks[0].Outputs;
        const outputMap = {};
        for(var output of outputs) {
            outputMap[output.OutputKey] = output.OutputValue;
        }

        console.log(outputMap);
    });

    grunt.registerPromiseTask('test', async () => {
        const axios = require('axios');
        const websiteUrl = getStackOutput('WebsiteURL');
        try {
            const result = await axios.get(websiteUrl);
            console.log('result:', result.data.substring(0, 100) + "...");
        } catch(error) {
            console.log('error:', error);
        }
    });

    grunt.registerTask('deploy', 'Create or update CF stack', function() {
        shell(`aws cloudformation deploy \
            --template-file cf.yml \
            --stack-name ${stackName} \
            --capabilities CAPABILITY_IAM \
            --region ${region} \
            --parameter-overrides \
            MyBucketName=${bucketName}`);

        shell(`aws s3 cp public s3://${bucketName} --recursive --acl public-read`);
    });

    grunt.registerTask('undeploy', 'Destroy CF stack', function() {
        shell(`aws s3 rm s3://${bucketName} --recursive`);
        shell(`aws cloudformation delete-stack --stack-name ${stackName} --region ${region}`);
        shell(`aws cloudformation wait stack-delete-complete --stack-name ${stackName} --region ${region}`);
    });

    function shell(command) {
        console.log(command);

        const result = spawnSync(command, [], {
            shell: '/bin/bash',
            stdio: 'inherit'
        });

        if(result.error) {
            throw result.error;
        }

        if(result.status != 0) {
            throw 'status != 0';
        }

        return result;
    }

    function getStackOutput(outputKey) {
        const result = spawnSync(`aws cloudformation describe-stacks \
            --stack-name ${stackName} \
            --query 'Stacks[0].Outputs[?OutputKey==\`'${outputKey}'\`].OutputValue' \
            --output text \
            --region ${region}`, [], {
            shell: '/bin/bash'
        });

        if(result.error) {
            throw result.error;
        }

        if(result.status != 0) {
            throw 'status != 0';
        }

        return result.stdout.toString().trim();
    }
};
