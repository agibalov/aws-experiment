# systems-manager-experiment

* Deploy: `./tool.sh deploy`
* Undeploy: `./tool.sh undeploy`
* Test: `./tool.sh test`

## Notes

* SSM can't generate secrets and CF doesn't support parameters of type SecureString. This means that if you need a password, you'll have to use CLI to construct the parameter.
* `{{resolve:ssm-secure:...:...}}` doesn't work for Lambda environment variables.
