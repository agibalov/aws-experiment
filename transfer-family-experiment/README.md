# transfer-family-experiment

An AWS Transfer Family hello world. An SFTP facade to an S3 bucket.

## How to deploy and undeploy

* `./tool.sh generate-key` to generate an SSH key for the SFTP user. 
* `./tool.sh deploy-basic` to deploy the server with service-managed authentication.
* `./tool.sh deploy-custom-auth` to deploy the server with custom authentication.
* `./tool.sh undeploy` to undeploy.

```
TODO: sftp-key
TODO: sftp-password
```

## How to play with it

* Once deployed, use `./tool.sh sftp` to connect to the server.
* `pwd` will report `/`.
* `ls` will report no contents.
* `put README.md 1.md` to upload local `README.md` to the server as `1.md`.
* Go to S3 bucket and see that there's a `dummyuser` directory, and there's a `1.md` in that directory.
* Upload `README.md` to S3 bucket's `dummyuser` directory.
* In `sftp` do `ls` and see that `README.md` is there now.
* `get README.md 2.md` to download `README.md` as `2.md`
