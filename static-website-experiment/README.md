Host a static website using an S3 bucket.

* `./deploy.sh deploy-basic` - demonstrates the very basic use-case.
* `./deploy.sh deploy-spa` - demonstrates the SPA scenario: all 404s resolve to index.html to support HTML5 URLs.
* `./deploy.sh deploy-restricted` - demonstrates how to restrict access to the website based on the `User-Agent` header contents.
* `./deploy.sh test` - send requests to the website.
* `./deploy.sh undeploy` - undeploy the website.
