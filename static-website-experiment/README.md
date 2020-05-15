Host a static website using an S3 bucket.

* `./tool.sh deploy-basic` - demonstrates the very basic use-case.
* `./tool.sh deploy-spa` - demonstrates the SPA scenario: all 404s resolve to index.html to support HTML5 URLs.
* `./tool.sh deploy-restricted` - demonstrates how to restrict access to the website based on the `User-Agent` header contents.
* `./tool.sh test` - send requests to the website.
* `./tool.sh undeploy` - undeploy the website.
