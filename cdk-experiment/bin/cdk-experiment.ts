#!/usr/bin/env node
import 'source-map-support/register';
import cdk = require('@aws-cdk/cdk');
import { WebsiteStack } from '../lib/website-stack';

const app = new cdk.App();
const cdkExperimentStack = new WebsiteStack(app, 'WebsiteStack');
cdkExperimentStack.node.apply(new cdk.Tag('SomeTag', 'SomeTagValue'));
