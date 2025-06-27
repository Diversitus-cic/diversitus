import * as pulumi from "@pulumi/pulumi";
import * as aws from "@pulumi/aws";
import * as awsx from "@pulumi/awsx";
import * as path from "path";

const config = new pulumi.Config();

// 1. Create an ECR repository to store our Docker image.
// `awsx` provides a higher-level component that simplifies this.
const repo = new awsx.ecr.Repository("diversitus-repo", {
    forceDelete: true, // Useful for development, consider removing for production
});

// 2. Build and publish the Docker image to the ECR repository.
// This command tells Pulumi to build the Dockerfile in the `../app` directory
// and push the resulting image to our ECR repository.
const image = new awsx.ecr.Image("diversitus-image", {
    repositoryUrl: repo.url,
    // The build context is the project root directory, so Gradle can find all modules.
    path: path.join(__dirname, ".."),
    // The Dockerfile is now located in the 'backend/app' subdirectory.
    dockerfile: "backend/app/Dockerfile",
});

// 3. Create a DynamoDB table to store job listings.
const jobsTable = new aws.dynamodb.Table("diversitus-jobs-table", {
    attributes: [
        { name: "id", type: "S" }, // S for String Partition Key
    ],
    hashKey: "id",
    billingMode: "PAY_PER_REQUEST", // Most cost-effective for new/spiky workloads
    tags: {
        Project: "Diversitus",
    },
});

// Seed the table with initial data. This makes the service usable immediately after deployment.
// The format is standard DynamoDB JSON.
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

// 3. Create an IAM role and policy for the Lambda function.
// The Lambda needs permission to be executed by AWS services and to write logs.
const lambdaRole = new aws.iam.Role("diversitus-lambda-role", {
    assumeRolePolicy: aws.iam.assumeRolePolicyForPrincipal({ Service: "lambda.amazonaws.com" }),
});

// Create a specific policy that grants our Lambda access to the new DynamoDB table.
const dbAccessPolicy = new aws.iam.Policy("diversitus-db-access-policy", {
    policy: jobsTable.arn.apply(arn => JSON.stringify({
        Version: "2012-10-17",
        Statement: [{
            Action: ["dynamodb:Scan", "dynamodb:Query", "dynamodb:GetItem"],
            Effect: "Allow",
            Resource: arn,
        }],
    })),
});

// Attach the DynamoDB access policy to the Lambda's role.
new aws.iam.RolePolicyAttachment("diversitus-db-policy-attachment", {
    role: lambdaRole.name,
    policyArn: dbAccessPolicy.arn,
});

new aws.iam.RolePolicyAttachment("diversitus-lambda-policy", {
    role: lambdaRole.name,
    policyArn: aws.iam.ManagedPolicy.AWSLambdaBasicExecutionRole,
});

// 4. Create the AWS Lambda function itself, using the container image.
const lambda = new aws.lambda.Function("diversitus-lambda", {
    packageType: "Image",
    imageUri: image.imageUri,
    role: lambdaRole.arn,
    timeout: 30, // seconds
    memorySize: 512, // MB
    // Pass the table name to the function as an environment variable.
    environment: {
        variables: {
            JOBS_TABLE_NAME: jobsTable.name,
        },
    },
});

// 5. Create an API Gateway (HTTP API) to make the Lambda accessible from the internet.
const api = new aws.apigatewayv2.Api("diversitus-api", {
    protocolType: "HTTP",
    target: lambda.arn,
});

// 6. Give API Gateway permission to invoke the Lambda function.
new aws.lambda.Permission("api-gateway-permission", {
    action: "lambda:InvokeFunction",
    function: lambda.name,
    principal: "apigateway.amazonaws.com",
    sourceArn: pulumi.interpolate`${api.executionArn}/*/*`,
});

// 7. Export the public URL of the API Gateway.
// After running `pulumi up`, this URL will be printed to the console.
export const apiUrl = api.apiEndpoint;