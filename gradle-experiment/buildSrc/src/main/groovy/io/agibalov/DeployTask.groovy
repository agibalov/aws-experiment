package io.agibalov

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import static io.agibalov.ShellService.shell

class DeployTask extends DefaultTask {
    @TaskAction
    void deploy() {
        DeploymentPluginExtension deployment = project.deployment

        shell("""aws cloudformation deploy \
            --template-file cf.yml \
            --stack-name ${deployment.stackName} \
            --capabilities CAPABILITY_IAM \
            --region ${deployment.region} \
            --parameter-overrides \
            MyBucketName=${deployment.bucketName}""")

        shell("""aws s3 cp public s3://${deployment.bucketName} \
            --recursive \
            --acl public-read""")
    }
}
