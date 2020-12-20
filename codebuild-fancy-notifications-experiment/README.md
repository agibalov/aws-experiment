# codebuild-fancy-notifications-experiment

It's a common solution to use `AWS::Events::Rule` + SNS to make CodeBuild send email messages on each build. These messages, however, look ugly and don't provide for customizations. The goal of this lab is to figure out the way to send _nice_ email messages with all the necessary details.

1. `./tool.sh deploy` to deploy.
2. Check inbox for messages from AWS and confirm your subscription.
3. Note that this step will also create a web hook in the GitHub repository.
4. Create a feature branch, make some changes, and push it. See how this triggers a build.
5. Create a merge request and see how it has the "All checks have passed" section.
6. `./tool.sh undeploy` to undeploy.
