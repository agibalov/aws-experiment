# codeartifact-experiment

The AWS CodeArtifact hello world. The goal is to learn how to use CodeArtifact as a cache for public package registries (registry.npmjs.org and Maven Central).

## How to deploy and run

* `branch=<BRANCH> ./tool.sh deploy` to deploy.
  * Trigger the `test-npm-project` to see how it works for NPM. 
* `./tool.sh undeploy` to undeploy.
