# awslimitchecker-experiment

[awslimitchecker](https://github.com/jantman/awslimitchecker) is a tool that checks current AWS resource usage and compares it to known service limits.

* `./tool.sh test <AWS PROFILE NAME>` to run it. Note that profile you specify should only use `aws_access_key_id` and `aws_secret_access_key` (no MFA, etc).
