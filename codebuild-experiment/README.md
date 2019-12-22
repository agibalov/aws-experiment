# codebuild-experiment

Build all Github repository branches using a single CodeBuild project.

Before deploying this, go to CodeBuild and start creating a new project manually. In the Source section, select "GitHub" and click "Connect to GitHub" (there may be no "Connect to GitHub" button, in this case just skip this step).

1. `./tool.sh deploy` to deploy. 
2. Check inbox for messages from AWS and confirm your subscription.
3. Note that this step will also create a web hook in the GitHub repository.
4. Push changes to the repository and see how this triggers a build.
5. Go to GitHub project's Settings -> Branches and create a branch protection rule for "master" branch and enable "Require status checks to pass before merging" and "AWS CodeBuild us-east-1 (DummyProject)". Create a feature branch, make some changes and create a merge request. See how this triggers a build and GitHub now displays the "All checks have passed" section in the merge request.
6. `./tool.sh undeploy` to undeploy.
