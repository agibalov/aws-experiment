# aws-ci-experiment

The very opinionated AWS CodePipeline/CodeBuild hello world.

* Use `./tool.sh deploy-pipeline <EnvTag> <GitHubRepositoryOwner> <GitHubRepositoryName> <GitHubRepositoryBranchName> <GitHubOAuthToken>` to deploy the CI pipeline. Example: `./tool.sh deploy-pipeline env1 agibalov aws-experiment master 0123456789012345678901234567890123456789`. 

  You can run this command multiple times. Only the first run requires `<GitHubOAuthToken>` to be present, next runs will reuse the token you specified initially. Still, you may provide the token if you want to update it.
  
  `<GitHubOAuthToken>` should have the `repo` and `admin:repo_hook` scopes (https://github.com/settings/tokens)
* Use `./tool.sh undeploy-pipeline <EnvTag>` to undeploy the CI pipeline.
* Use `./tool.sh start-pipeline <EnvTag>` to trigger the pipeline.
* Use `./tool.sh deploy-app <EnvTag> <BuildArtifactsDirectory>` to deploy the app. `<BuildArtifactsDirectory>` is optional, the current directory is going to be considered a build artifacts directory if you don't specify it.
* Use `./tool.sh undeploy-app <EnvTag>` to undeploy the app.

### Notes

* As of 3/1/2019 AWS CodePipeline doesn't support wildcard branch names, so you can't apply the same pipeline to more than 1 branch. This is an issue if your goal is to build/test every single feature branch you have. However, if your goal is to have a few static environments, this is not an issue.
