import * as pulumi from "@pulumi/pulumi";
import * as aws from "@pulumi/aws";
import * as awsx from "@pulumi/awsx";
import * as path from "path";
import { marshall } from "@aws-sdk/util-dynamodb";

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

// 3b. Create a DynamoDB table to store company data.
const companiesTable = new aws.dynamodb.Table("diversitus-companies-table", {
    attributes: [{ name: "id", type: "S" }],
    hashKey: "id",
    billingMode: "PAY_PER_REQUEST",
    tags: { Project: "Diversitus" },
});

// Seed the companies table with initial data.
const initialCompaniesData = [
    { id: "comp-1", name: "Creative Co.", traits: { "work_life_balance": 9, "collaboration": 8 } },
    { id: "comp-2", name: "Logic Inc.", traits: { "deep_focus": 9, "autonomy": 7 } },
    { id: "comp-3", name: "DataDriven Corp", traits: { "pattern_recognition": 9, "deep_focus": 8 } },
];

initialCompaniesData.forEach((company, i) => {
    new aws.dynamodb.TableItem(`company-item-${i}`, {
        tableName: companiesTable.name,
        hashKey: companiesTable.hashKey,
        item: JSON.stringify(marshall(company)),
    });
});

// Seed the jobs table with updated data linking to companies.
const initialJobsData = [
    { id: "job-1", companyId: "comp-1", title: "Frontend Developer", description: "Build beautiful and accessible user interfaces.", traits: { "attention_to_detail": 8, "visual_thinking": 9 } },
    { id: "job-2", companyId: "comp-2", title: "Backend Engineer", description: "Design and implement scalable server-side logic.", traits: { "problem_solving": 9, "systematic_thinking": 8 } },
    { id: "job-3", companyId: "comp-1", title: "UX Designer", description: "Create intuitive and user-friendly application flows.", traits: { "empathy": 9, "visual_thinking": 10 } },
    { id: "job-4", companyId: "comp-3", title: "Data Analyst", description: "Find insights and patterns in large datasets.", traits: { "pattern_recognition": 10, "attention_to_detail": 9 } },
    { id: "job-5", companyId: "comp-2", title: "DevOps Engineer", description: "Automate and streamline our infrastructure and deployment pipelines.", traits: { "systematic_thinking": 9, "problem_solving": 8 } },
];

initialJobsData.forEach((job, i) => {
    new aws.dynamodb.TableItem(`job-item-${i}`, {
        tableName: jobsTable.name,
        hashKey: jobsTable.hashKey,
        item: JSON.stringify(marshall(job)),
    });
});

// 4. Create an IAM role for the Fargate Task.
const taskRole = new aws.iam.Role("diversitus-task-role", {
    assumeRolePolicy: aws.iam.assumeRolePolicyForPrincipal({ Service: "ecs-tasks.amazonaws.com" }),
});

// 5. Create and attach an inline policy to the role, allowing it to access DynamoDB.
new aws.iam.RolePolicy("diversitus-db-access-policy", {
    role: taskRole.id,
    policy: pulumi.all([jobsTable.arn, companiesTable.arn]).apply(([jobsArn, companiesArn]) => JSON.stringify({
        Version: "2012-10-17",
        Statement: [{
            Action: ["dynamodb:Scan", "dynamodb:Query", "dynamodb:GetItem", "dynamodb:BatchGetItem"],
            Effect: "Allow",
            Resource: [jobsArn, companiesArn],
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
                { name: "COMPANIES_TABLE_NAME", value: companiesTable.name },
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