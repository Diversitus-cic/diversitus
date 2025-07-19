#!/usr/bin/env node
/**
 * Cleanup Orphaned Jobs Script
 * Deletes jobs that reference company IDs that don't exist in the companies table
 * Preserves jobs that are properly linked to existing companies
 */

const https = require('https');

const API_BASE_URL = 'https://api.diversitus.uk';

// Function to make HTTP GET request
function makeGetRequest(path) {
    return new Promise((resolve, reject) => {
        const options = {
            hostname: 'api.diversitus.uk',
            port: 443,
            path: path,
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        };

        const req = https.request(options, (res) => {
            let responseData = '';
            
            res.on('data', (chunk) => {
                responseData += chunk;
            });
            
            res.on('end', () => {
                if (res.statusCode >= 200 && res.statusCode < 300) {
                    try {
                        resolve(JSON.parse(responseData));
                    } catch (e) {
                        resolve(responseData);
                    }
                } else {
                    reject({ 
                        statusCode: res.statusCode, 
                        data: responseData 
                    });
                }
            });
        });

        req.on('error', (error) => {
            reject({ error: error.message });
        });

        req.end();
    });
}

// Function to make HTTP DELETE request
function makeDeleteRequest(path) {
    return new Promise((resolve, reject) => {
        const options = {
            hostname: 'api.diversitus.uk',
            port: 443,
            path: path,
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json'
            }
        };

        const req = https.request(options, (res) => {
            let responseData = '';
            
            res.on('data', (chunk) => {
                responseData += chunk;
            });
            
            res.on('end', () => {
                if (res.statusCode >= 200 && res.statusCode < 300) {
                    resolve({ success: true, data: responseData });
                } else {
                    reject({ 
                        success: false,
                        statusCode: res.statusCode, 
                        data: responseData 
                    });
                }
            });
        });

        req.on('error', (error) => {
            reject({ success: false, error: error.message });
        });

        req.end();
    });
}

// Main cleanup function
async function cleanupOrphanedJobs() {
    console.log('🔍 Starting orphaned jobs cleanup...');
    
    try {
        // Get all companies to identify valid company IDs
        console.log('📋 Fetching companies...');
        const companies = await makeGetRequest('/companies');
        const validCompanyIds = new Set(companies.map(company => company.id));
        
        console.log(`✅ Found ${validCompanyIds.size} valid companies:`);
        companies.forEach(company => {
            console.log(`   - ${company.name} (${company.id})`);
        });
        
        // Get all jobs
        console.log('\n📋 Fetching jobs...');
        const jobs = await makeGetRequest('/jobs');
        
        console.log(`✅ Found ${jobs.length} total jobs`);
        
        // Identify orphaned jobs (jobs with company IDs that don't exist)
        const orphanedJobs = jobs.filter(job => !validCompanyIds.has(job.companyId));
        const linkedJobs = jobs.filter(job => validCompanyIds.has(job.companyId));
        
        console.log(`\n📊 Analysis:`);
        console.log(`   📎 Properly linked jobs: ${linkedJobs.size}`);
        console.log(`   🚫 Orphaned jobs: ${orphanedJobs.length}`);
        
        if (linkedJobs.length > 0) {
            console.log(`\n🔗 Jobs that will be PRESERVED:`);
            linkedJobs.forEach(job => {
                const company = companies.find(c => c.id === job.companyId);
                console.log(`   ✅ "${job.title}" → ${company?.name || 'Unknown'} (${job.companyId})`);
            });
        }
        
        if (orphanedJobs.length === 0) {
            console.log('\n🎉 No orphaned jobs found! All jobs are properly linked.');
            return;
        }
        
        console.log(`\n🗑️  Jobs that will be DELETED (orphaned):`);
        orphanedJobs.forEach(job => {
            console.log(`   ❌ "${job.title}" → Invalid Company ID: ${job.companyId}`);
        });
        
        // Ask for confirmation (in real scenario - for script we'll proceed)
        console.log(`\n⚠️  About to delete ${orphanedJobs.length} orphaned jobs...`);
        
        // Delete orphaned jobs
        let successful = 0;
        let failed = 0;
        
        for (let i = 0; i < orphanedJobs.length; i++) {
            const job = orphanedJobs[i];
            
            try {
                await makeDeleteRequest(`/jobs/${job.id}`);
                successful++;
                console.log(`🗑️  ${i + 1}/${orphanedJobs.length}: Deleted "${job.title}"`);
            } catch (error) {
                failed++;
                console.error(`❌ ${i + 1}/${orphanedJobs.length}: Failed to delete "${job.title}" - ${error.statusCode || error.error}`);
            }
            
            // Rate limiting: wait 100ms between requests
            if (i < orphanedJobs.length - 1) {
                await new Promise(resolve => setTimeout(resolve, 100));
            }
        }
        
        console.log(`\n🎯 Cleanup Complete:`);
        console.log(`   🗑️  Deleted: ${successful}`);
        console.log(`   ❌ Failed: ${failed}`);
        console.log(`   📎 Preserved: ${linkedJobs.length}`);
        console.log(`   📈 Total processed: ${successful + failed}`);
        
        // Final verification
        console.log('\n🔍 Verifying cleanup...');
        const remainingJobs = await makeGetRequest('/jobs');
        const stillOrphaned = remainingJobs.filter(job => !validCompanyIds.has(job.companyId));
        
        if (stillOrphaned.length === 0) {
            console.log('✅ All remaining jobs are properly linked!');
        } else {
            console.log(`⚠️  Warning: ${stillOrphaned.length} orphaned jobs still remain`);
        }
        
    } catch (error) {
        console.error('💥 Error during cleanup:', error);
    }
}

// Run if called directly
if (require.main === module) {
    cleanupOrphanedJobs().catch(console.error);
}

module.exports = { cleanupOrphanedJobs };