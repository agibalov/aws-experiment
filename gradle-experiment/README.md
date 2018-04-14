# gradle-experiment

An attempt to use Gradle to organize AWS deployment logic. There's a custom Gradle plugin that calls the `aws` utility to deploy (`./gradlew deploy`) and undeploy (`./gradlew undeploy`) a stack.

Also illustrates the use of Gradle's `@Option` feature to specify task parameters in the command line (`./gradlew ping --hostname 8.8.8.8`)
