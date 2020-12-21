# codebuild-fancy-notifications-experiment

CodeBuild project + Event rule + SNS allow you to send build notification email messages. This mechanism, however, has limitation and requires you to use ugly syntax to construct multi-line message text. The message then has the unexplainable `"` in the beginning and in the end of each line.

The goal of this lab is to show how AWS Lambda allows you to send _nicer_ build notifications.

1. `./tool.sh deploy <BranchName>` to deploy.
2. Check inbox for messages from AWS and confirm your subscription.
3. Note that this step will also create a web hook in the GitHub repository.
4. Create a merge request and see how it has the "All checks have passed" section.
5. `./tool.sh undeploy` to undeploy.

## Notes

* With SNS, you still can't use arbitrary HTML - only the plain text. Consider using SES if you want HTML.
