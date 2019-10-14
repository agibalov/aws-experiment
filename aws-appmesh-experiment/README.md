# aws-appmesh-experiment

App Mesh is an application-level networking technology based on [envoy](https://www.envoyproxy.io/).

```
A -> LoadBalancer -> (B1 or B2)
```

App Mesh allows you to make LoadBalancer a part of "A" node instead of making it a dedicated networking component like ELB. This is achieved by running an Envoy container as a part of each ECS service.

* `./tool.sh deploy` to deploy.
* `./tool.sh undeploy` to undeploy.

After deployment, go to CloudWatch logs for "Tester" and see how it says "hello" or "world" randomly.

Explanation for `HelloWorldServiceRegistry` and `HelloWorldServiceRegistryDummyInstance` in `template.yml` ([docs](https://docs.aws.amazon.com/app-mesh/latest/userguide/virtual_services.html)):

> You can choose any name, but the service discovery name of the real service that you're targeting, such as my-service.default.svc.cluster.local, is recommended to make it easier to correlate your virtual services to real services and so that you don't need to change your code to reference a different name than your code currently references. **The name that you specify must resolve to a non-loopback IP address because the app container must be able to successfully resolve the name before the request is sent to the Envoy proxy. You can use any non-loopback IP address because neither the app or proxy containers communicate with this IP address.** The proxy communicates with other virtual services through the names you’ve configured for them in App Mesh, not through IP addresses that the names resolve to.
