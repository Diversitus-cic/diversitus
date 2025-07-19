/**
 * Pulumi Job Seeding Script
 * Adds jobs directly to DynamoDB via Pulumi infrastructure
 */

import * as aws from "@pulumi/aws";
import { marshall } from "@aws-sdk/util-dynamodb";
import { v4 as uuidv4 } from 'uuid';

// Import the job data
const { jobs, COMPANIES } = require('./seed-jobs');

// Company ID mapping to match fixed UUIDs from infrastructure seeding
const COMPANY_ID_MAPPING = {
    "550e8400-e29b-41d4-a716-446655440001": "550e8400-e29b-41d4-a716-446655440001", // Creative Co.
    "550e8400-e29b-41d4-a716-446655440002": "550e8400-e29b-41d4-a716-446655440002", // Logic Inc.
    "550e8400-e29b-41d4-a716-446655440003": "550e8400-e29b-41d4-a716-446655440003"  // DataDriven Corp
};

// Function to create DynamoDB table items for jobs
export function seedJobsInPulumi(jobsTableName: string) {
    const jobItems: aws.dynamodb.TableItem[] = [];
    
    jobs.forEach((job: any, index: number) => {
        // Map the placeholder company ID to actual UUID
        const actualCompanyId = COMPANY_ID_MAPPING[job.companyId as keyof typeof COMPANY_ID_MAPPING];
        
        if (!actualCompanyId) {
            console.warn(`⚠️  Skipping job "${job.title}" - company ID ${job.companyId} not found in mapping`);
            return;
        }
        
        const jobWithActualCompanyId = {
            ...job,
            companyId: actualCompanyId,
            id: uuidv4() // Generate fresh UUID for each job
        };
        
        const tableItem = new aws.dynamodb.TableItem(`seeded-job-item-${index}`, {
            tableName: jobsTableName,
            hashKey: "id",
            item: JSON.stringify(marshall(jobWithActualCompanyId)),
        });
        
        jobItems.push(tableItem);
    });
    
    console.log(`📊 Created ${jobItems.length} job items for Pulumi deployment`);
    return jobItems;
}

// Alternative: Create jobs as separate resources for easier management
export function createJobResources(jobsTableName: string) {
    const jobResources: { [key: string]: aws.dynamodb.TableItem } = {};
    
    jobs.forEach((job: any, index: number) => {
        const actualCompanyId = COMPANY_ID_MAPPING[job.companyId as keyof typeof COMPANY_ID_MAPPING];
        
        if (!actualCompanyId) {
            return; // Skip jobs with unmapped company IDs
        }
        
        const jobWithActualCompanyId = {
            ...job,
            companyId: actualCompanyId,
            id: uuidv4()
        };
        
        const resourceName = `seeded-job-${index}-${job.title.toLowerCase().replace(/[^a-z0-9]/g, '-')}`;
        
        jobResources[resourceName] = new aws.dynamodb.TableItem(resourceName, {
            tableName: jobsTableName,
            hashKey: "id", 
            item: JSON.stringify(marshall(jobWithActualCompanyId)),
        });
    });
    
    return jobResources;
}

// Usage instructions for integration with main infrastructure
/*
To use this in your main infrastructure file:

1. First, get the actual company UUIDs from your DynamoDB:
   - Check the companies table to get real UUIDs
   - Update COMPANY_ID_MAPPING above

2. Import and use in infrastructure/index.ts:
   
   import { seedJobsInPulumi } from './scripts/seed-jobs-pulumi';
   
   // After creating the jobs table:
   const jobItems = seedJobsInPulumi(jobsTable.name);

3. Or create as separate resources:
   
   import { createJobResources } from './scripts/seed-jobs-pulumi';
   
   const seededJobs = createJobResources(jobsTable.name);
*/

export { jobs, COMPANIES };