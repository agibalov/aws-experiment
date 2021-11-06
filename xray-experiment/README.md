# xray-experiment

An AWS X-Ray hello world. The goal is to figure out what insights X-Ray can collect from the Java/Spring app.

* `./tool.sh deploy-shared` and `./tool.sh undeploy-shared` to deploy and undeploy the shared stack.
* `./tool.sh deploy-app` and `./tool.sh undeploy-app` to deploy and undeploy the app stack. Make sure to `./gradlew clean bootJar` before it.
