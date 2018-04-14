package io.agibalov

import org.gradle.api.Plugin
import org.gradle.api.Project

class DeploymentPlugin implements Plugin<Project> {
    private final static GROUP_NAME = 'deployment'

    @Override
    void apply(Project project) {
        project.extensions.create('deployment', DeploymentPluginExtension)

        project.tasks.create('ping', PingTask) { task ->
            task.group = GROUP_NAME
            task.description = 'Ping a host'
        }

        project.tasks.create('deploy', DeployTask) { task ->
            task.group = GROUP_NAME
            task.description = 'Deploy a stack'
        }

        project.tasks.create('undeploy', UndeployTask) { task ->
            task.group = GROUP_NAME
            task.description = 'Undeploy a stack'
        }
    }
}
