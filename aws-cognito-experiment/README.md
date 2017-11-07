# aws-cognito-experiment

Learning AWS Cognito. This is a hello world application that consists of mock REST API secured with Cognito User Pool-based authorizer. The expected scenario is:

* Look up the website URL and go to Sign Up page.
* Sign up with any username and password, but use the real email. Cognito will send an activation code to the email address you specify.
* Wait for activation code to appear in your inbox and copy it.
* Go to Confirm page and use your username and activation code to activate the account.
* Go to Sign In page and use your username and password to sign in. As a result you should get a few JWT tokens. Copy the ID token.
* Go to Test page and paste your ID token. Click the Test button and see how API responds.

Do `npm i` to install dependencies before using it.

* `./tool deploy` to deploy everything.
* `./tool website` to redeploy the website.
* `./tool undeploy` to undeploy everything.
