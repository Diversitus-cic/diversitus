#!/usr/bin/env node
/**
 * API Job Seeding Script
 * Posts jobs to the Diversitus API to seed the database
 */

const https = require('https');
const { jobs } = require('./seed-jobs');

const API_BASE_URL = 'https://api.diversitus.uk';

// Function to make HTTP POST request
function postJob(job) {
    return new Promise((resolve, reject) => {
        const data = JSON.stringify(job);
        
        const options = {
            hostname: 'api.diversitus.uk',
            port: 443,
            path: '/jobs',
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Content-Length': data.length
            }
        };

        const req = https.request(options, (res) => {
            let responseData = '';
            
            res.on('data', (chunk) => {
                responseData += chunk;
            });
            
            res.on('end', () => {
                if (res.statusCode >= 200 && res.statusCode < 300) {
                    resolve({ success: true, data: responseData, job: job.title });
                } else {
                    reject({ 
                        success: false, 
                        statusCode: res.statusCode, 
                        data: responseData, 
                        job: job.title 
                    });
                }
            });
        });

        req.on('error', (error) => {
            reject({ success: false, error: error.message, job: job.title });
        });

        req.write(data);
        req.end();
    });
}

// Function to seed jobs with rate limiting
async function seedJobs() {
    console.log(`🚀 Starting to seed ${jobs.length} jobs...`);
    
    let successful = 0;
    let failed = 0;
    
    for (let i = 0; i < jobs.length; i++) {
        const job = jobs[i];
        
        try {
            const result = await postJob(job);
            successful++;
            console.log(`✅ ${i + 1}/${jobs.length}: Created "${result.job}"`);
        } catch (error) {
            failed++;
            console.error(`❌ ${i + 1}/${jobs.length}: Failed "${error.job}" - ${error.statusCode || error.error}`);
        }
        
        // Rate limiting: wait 100ms between requests
        if (i < jobs.length - 1) {
            await new Promise(resolve => setTimeout(resolve, 100));
        }
    }
    
    console.log(`\n📊 Seeding Complete:`);
    console.log(`   ✅ Successful: ${successful}`);
    console.log(`   ❌ Failed: ${failed}`);
    console.log(`   📈 Total: ${successful + failed}`);
}

// Run if called directly
if (require.main === module) {
    seedJobs().catch(console.error);
}

module.exports = { seedJobs };