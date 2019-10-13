# aws-cloudmap-experiment

AWS Cloud Map is a service registry:

* You describe services ("calculator service")
* You dynamically register and deregister service instances ("calculator service #132 at 1.2.3.4:8080")
* Whenever you need a list of known instances of the service, you use "discover-instances" to get a list of them.
* Also supports health-checking, but it's not covered in this experiment.

### Deploying and testing it

* `./tool.sh deploy dns` to deploy the `PrivateDnsNamespace` setup.
* `./tool.sh deploy http` to deploy the `HttpNamespace` setup.
* `./tool.sh undeploy` to undeploy.
* `./tool.sh register-instance` to register service instance.
* `./tool.sh deregister-instance` to deregister service instance.
* `./tool.sh discover-instances` to discover service instances.

### Public DNS vs private DNS vs HTTP

Cloud Map has 3 implementations:

* `AWS::ServiceDiscovery::PrivateDnsNamespace` creates a private Route53 hosted zone. Instances can be resolved either with `discover-instances` or with DNS (only from inside the VPC)
* `AWS::ServiceDiscovery::PublicDnsNamespace` creates a public Route53 hosted zone. Instances can be resolved either with `discover-instances` or with DNS. Note that if you have a zone like `xxx.com` and you create a public DNS namespace like `aaa.xxx.com`, it will create a zone for `aaa.xxx.com`, but it won't create an NS record in `xxx.com` to delegate `aaa` to this new `aaa.xxx.com` zone.
* `AWS::ServiceDiscovery::HttpNamespace` no DNS at all - only `discover-instances` is available.
