# pinpoint-experiment

Amazon Pinpoint allows you to communicate (email, text, push notifications) with users by configuring user segments ("all US users", etc) and campaigns. It looks like it also allows you to track user activity, but it's not a part of this experiment.

* `./tool.sh deploy` to deploy.
* `./tool.sh undeploy` to undeploy.

0. Verify your email address with SES.
1. Update `app.yml` with your email address.
2. Update `us-users-segment.csv` with your email address.
3. Do `./tool.sh deploy`
4. Check the inbox and see 3 email messages there.
5. Check the CloudWatch log - it should have 3 `_email.send` and 3 `_email.delivered` events.
6. In you inbox, open any 1 of 3 messages.
7. Check the CloudWatch log - a new `_email.open` event should appear.
8. Open a link.
9. Check the CloudWatch log - a new `_email.click` event should appear.
