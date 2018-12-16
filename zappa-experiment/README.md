# zappa-experiment

This project illustrates what it looks like when you build a serverless app with Python/[Flask](http://flask.pocoo.org/). The app gets packaged and deployed to AWS by [Zappa](https://github.com/Miserlou/Zappa). The app uses [Amazon Comprehend](https://aws.amazon.com/comprehend/) for named entity recognition.

## Running locally and deploying

1. Have [pyenv](https://github.com/pyenv/pyenv) and [pipenv](https://github.com/pypa/pipenv) installed.
2. Use `pipenv install` to install the dependencies.
3. Use `pipenv shell` to activate the environment, and then:
   * `flask run` to run the app locally
   * `zappa deploy dev` to deploy the initial version.
   * `zappa update dev` to update the deployment.
   * `zappa undeploy dev` to undeploy.
