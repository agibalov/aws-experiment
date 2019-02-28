# aws-ci-experiment

* `./tool.sh create <GitHubOAuthToken>` to deploy CI.
* `./tool.sh update <GitHubOAuthToken>` to update deployment.
* `./tool.sh delete` to undeploy.

`<GitHubOAuthToken>` should have the `repo` and `admin:repo_hook` scopes (https://github.com/settings/tokens).
