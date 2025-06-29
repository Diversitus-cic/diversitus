import * as pulumi from "@pulumi/pulumi";
import * as aws from "@pulumi/aws";
import * as awsx from "@pulumi/awsx";
import * as path from "path";

const config = new pulumi.Config();

// 1. Create an ECR repository to store our Docker image.
const repo = new awsx.ecr.Repository("diversitus-repo", {
    forceDelete: true, // Useful for development, consider removing for production
});

// 2. Build and publish the Docker image to the ECR repository.
const image = new awsx.ecr.Image("diversitus-image", {
    repositoryUrl: repo.url,
    context: path.join(__dirname, ".."),
    dockerfile: path.join(__dirname, "..", "backend", "app", "Dockerfile"),
    // Add platform specification for consistency
    platform: "linux/amd64",
});

// 3. Create a DynamoDB table to store job listings.
const jobsTable = new aws.dynamodb.Table("diversitus-jobs-table", {
    attributes: [
        { name: "id", type: "S" },
    ],
    hashKey: "id",
    billingMode: "PAY_PER_REQUEST",
    tags: {
        Project: "Diversitus",
    },
});

// Seed the table with initial data
const initialJobs = [
    { id: { S: "job-1" }, title: { S: "Frontend Developer" }, company: { S: "Creative Co." }, requirements: { M: { "attention_to_detail": { N: "8" } } }, benefits: { L: [{ S: "remote" }, { S: "flexible-hours" }] } },
    { id: { S: "job-2" }, title: { S: "Backend Engineer" }, company: { S: "Logic Inc." }, requirements: { M: { "problem_solving": { N: "9" } } }, benefits: { L: [{ S: "remote" }, { S: "quiet-office" }] } },
    { id: { S: "job-3" }, title: { S: "UX Designer" }, company: { S: "UserFirst Ltd." }, requirements: { M: { "visual_thinking": { N: "9" } } }, benefits: { L: [{ S: "flexible-hours" }, { S: "collaborative-team" }] } },
];

initialJobs.forEach((job, i) => {
    new aws.dynamodb.TableItem(`job-item-${i}`, {
        tableName: jobsTable.name,
        hashKey: jobsTable.hashKey,
        item: JSON.stringify(job),
    });
});

// 4. Create an IAM role and policy for the Lambda function.
const lambdaRole = new aws.iam.Role("diversitus-lambda-role", {
    assumeRolePolicy: aws.iam.assumeRolePolicyForPrincipal({ Service: "lambda.amazonaws.com" }),
});

// Create a specific policy that grants our Lambda access to the DynamoDB table.
const dbAccessPolicy = new aws.iam.Policy("diversitus-db-access-policy", {
    policy: jobsTable.arn.apply(arn => JSON.stringify({
        Version: "2012-10-17",
        Statement: [{
            Action: [
                "dynamodb:Scan", 
                "dynamodb:Query", 
                "dynamodb:GetItem",
                "dynamodb:PutItem",
                "dynamodb:UpdateItem",
                "dynamodb:DeleteItem"
            ],
            Effect: "Allow",
            Resource: arn,
        }],
    })),
});

// Attach policies to the Lambda's role.
new aws.iam.RolePolicyAttachment("diversitus-db-policy-attachment", {
    role: lambdaRole.name,
    policyArn: dbAccessPolicy.arn,
});

new aws.iam.RolePolicyAttachment("diversitus-lambda-policy", {
    role: lambdaRole.name,
    policyArn: aws.iam.ManagedPolicy.AWSLambdaBasicExecutionRole,
});

// 5. Create the AWS Lambda function using the container image.
const lambda = new aws.lambda.Function("diversitus-lambda", {
    packageType: "Image",
    imageUri: image.imageUri,
    role: lambdaRole.arn,
    timeout: 30,
    memorySize: 512,
    environment: {
        variables: {
            JOBS_TABLE_NAME: jobsTable.name,
            // Add AWS region for SDK
            AWS_REGION: aws.getRegion().then(region => region.name),
        },
    },
});

// 6. Create an API Gateway (HTTP API) to make the Lambda accessible.
const api = new aws.apigatewayv2.Api("diversitus-api", {
    protocolType: "HTTP",
    // Remove the target here - we'll create explicit routes
});

// 7. Create a Lambda integration
const integration = new aws.apigatewayv2.Integration("diversitus-integration", {
    apiId: api.id,
    integrationType: "AWS_PROXY",
    integrationUri: lambda.arn,
    payloadFormatVersion: "2.0",
});

// 8. Create routes - catch all routes to forward to Lambda
const route = new aws.apigatewayv2.Route("diversitus-route", {
    apiId: api.id,
    routeKey: "$default", // Catches all routes
    target: pulumi.interpolate`integrations/${integration.id}`,
});

// 9. Create a stage for the API
const stage = new aws.apigatewayv2.Stage("diversitus-stage", {
    apiId: api.id,
    name: "$default",
    autoDeploy: true,
});

// 10. Give API Gateway permission to invoke the Lambda function.
new aws.lambda.Permission("api-gateway-permission", {
    action: "lambda:InvokeFunction",
    function: lambda.name,
    principal: "apigateway.amazonaws.com",
    sourceArn: pulumi.interpolate`${api.executionArn}/*/*`,
});

// 11. Export the public URL of the API Gateway.
export const apiUrl = api.apiEndpoint;