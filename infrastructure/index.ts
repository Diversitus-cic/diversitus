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

// 4. Create an IAM role for the Fargate Task.
const taskRole = new aws.iam.Role("diversitus-task-role", {
    assumeRolePolicy: aws.iam.assumeRolePolicyForPrincipal({ Service: "ecs-tasks.amazonaws.com" }),
});

// 5. Create and attach an inline policy to the role, allowing it to access DynamoDB.
new aws.iam.RolePolicy("diversitus-db-access-policy", {
    role: taskRole.id,
    policy: jobsTable.arn.apply(arn => JSON.stringify({
        Version: "2012-10-17",
        Statement: [{
            Action: ["dynamodb:Scan", "dynamodb:Query", "dynamodb:GetItem"],
            Effect: "Allow",
            Resource: arn,
        }],
    })),
});

// 6. Create an ECS Cluster, Load Balancer, and the Fargate Service.
const cluster = new aws.ecs.Cluster("diversitus-cluster");
const alb = new awsx.lb.ApplicationLoadBalancer("diversitus-lb", {
    // Configure the default target group to expect targets on port 8080, matching the Ktor application.
    defaultTargetGroup: {
        port: 8080,
    },
});
const service = new awsx.ecs.FargateService("diversitus-fargate-service", {
    cluster: cluster.arn,
    // The task definition is defined inline.
    taskDefinitionArgs: {
        // Pass the ARN of the role we created explicitly.
        taskRole: { roleArn: taskRole.arn },
        // Define the container to run.
        container: {
            name: "app", // A required name for the container within the task definition.
            image: image.imageUri,
            cpu: 256,
            memory: 512,
            // Map the container's port to the load balancer's target group.
            portMappings: [{
                containerPort: 8080, // The port Ktor listens on inside the container.
                // For awsvpc network mode, hostPort must be specified and must match containerPort.
                hostPort: 8080,
                targetGroup: alb.defaultTargetGroup,
            }],
            environment: [
                { name: "JOBS_TABLE_NAME", value: jobsTable.name },
                { name: "AWS_REGION", value: aws.getRegion().then(r => r.name) },
            ],
        },
    },
    desiredCount: 1, // Run one instance of our container
    // By default, Fargate services are placed in private subnets. The default VPC
    // often lacks private subnets, causing an error. Setting assignPublicIp to true
    // ensures the service is placed in public subnets and can be reached by the ALB.
    assignPublicIp: true,
});

// 8. Export the public URL of the Load Balancer.
export const url = alb.loadBalancer.dnsName;