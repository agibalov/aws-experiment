# codebuild-experiment

Build all Github repository branches using a single CodeBuild project.

Before deploying this, go to CodeBuild and start creating a new project manually. In the Source section, select "GitHub" and click "Connect to GitHub" (there may be no "Connect to GitHub" button, in this case just skip this step).

1. `./tool.sh deploy` to deploy. 
2. Check inbox for messages from AWS and confirm your subscription.
3. Note that this step will also create a web hook in the GitHub repository.
4. Create a feature branch, make some changes, and push it. See how this triggers a build.
5. Create a merge request and see how it has the "All checks have passed" section.
6. `./tool.sh undeploy` to undeploy.
