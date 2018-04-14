package io.agibalov

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import static io.agibalov.ShellService.shell

class UndeployTask extends DefaultTask {
    @TaskAction
    void undeploy() {
        DeploymentPluginExtension deployment = project.deployment

        shell("""aws s3 rm s3://${deployment.bucketName} \
            --recursive""")

        shell("""aws cloudformation delete-stack \
            --stack-name ${deployment.stackName} \
            --region ${deployment.region}""")

        shell("""aws cloudformation wait stack-delete-complete \
            --stack-name ${deployment.stackName} \
            --region ${deployment.region}""")
    }
}
