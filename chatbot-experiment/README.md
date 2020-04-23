# chatbot-experiment

AWS Chatbot allows you to send notifications from *some* AWS services to Slack. Even though they say "hey, SNS!", it doesn't mean you can just subscribe Slack to SNS topic and send a message. This won't work. They only support [*some* services](https://docs.aws.amazon.com/chatbot/latest/adminguide/related-services.html). In this experiment I'm making CodeBuild send notifications to Slack.

Note: [CloudWatch Events](https://github.com/agibalov/aws-experiment/blob/master/codebuild-experiment/template.yml) don't work in this scenario - you have to use [CodeStar Notifications](https://docs.aws.amazon.com/codestar-notifications/latest/userguide/welcome.html). I wasn't able to understand why Notifications do even exist.

How to deploy it:

1. Get your Slack Channel ID. In Slack, right-click a channel name and do "Copy link". From that link remove everything but the last segment that looks like `C012T7N7N7K`. Update `SlackChannelId` in template.yml.
2. Get your Slack Workspace ID. If you're doing it for the first time, go to AWS Chatbot and do "Configure new client". This will make you give AWS access to your Slack workspace. Once done, the workspace will appear on the "Configured clients" page. Open this new client and see the Workspace ID (looks like `T012D7G72LV`). Update `SlackWorkspaceId` in template.yml.
3. `./tool.sh deploy`
4. Go to CodeBuild project and start the build.
5. Messages should appear in Slack.
6. `./tool.sh undeploy`

**Notes:**

* You can't send your own messages using CLI.
* You can't customize notification contents.
