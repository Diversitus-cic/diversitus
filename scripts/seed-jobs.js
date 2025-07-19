/**
 * Job Seeding Script for Diversitus
 * Creates 50+ jobs with diverse trait combinations for testing the matching service
 */

const { v4: uuidv4 } = require('uuid');

// Company IDs (these match the fixed UUIDs in Pulumi infrastructure seeding)
const COMPANIES = [
    { 
        id: "550e8400-e29b-41d4-a716-446655440001", 
        name: "Creative Co.",
        baseTraits: { "work_life_balance": 9, "collaboration": 8, "working_from_home": 10 }
    },
    { 
        id: "550e8400-e29b-41d4-a716-446655440002", 
        name: "Logic Inc.",
        baseTraits: { "deep_focus": 9, "autonomy": 7, "quiet_office": 9, "working_from_home": 8 }
    },
    { 
        id: "550e8400-e29b-41d4-a716-446655440003", 
        name: "DataDriven Corp",
        baseTraits: { "pattern_recognition": 9, "deep_focus": 8, "quiet_office": 7 }
    }
];

// Official Diversitus neurodiversity traits
const TRAIT_POOL = [
    "attention_to_detail",    // Noticing fine details and accuracy in work
    "autonomy",              // Working independently and making decisions
    "collaboration",         // Working effectively with others in teams
    "deep_focus",            // Maintaining concentration for extended periods
    "empathy",               // Understanding and relating to others emotions
    "pattern_recognition",   // Identifying patterns and connections
    "problem_solving",       // Finding solutions to complex challenges
    "quiet_office",          // Low noise, minimal distractions workspace
    "systematic_thinking",   // Approaching problems methodically
    "visual_thinking",       // Processing and understanding visual information
    "work_life_balance",     // Maintaining healthy balance between work and personal life
    "working_from_home"      // Remote work capabilities and preferences
];

// Job templates with varying trait requirements
const JOB_TEMPLATES = [
    // Creative roles
    {
        title: "UI/UX Designer",
        description: "Design intuitive user interfaces with attention to accessibility and user experience.",
        traits: { "visual_thinking": 9, "empathy": 8, "attention_to_detail": 7, "autonomy": 9 }
    },
    {
        title: "Graphic Designer", 
        description: "Create visual content for marketing materials and brand identity.",
        traits: { "visual_thinking": 10, "autonomy": 9, "attention_to_detail": 8, "working_from_home": 8 }
    },
    {
        title: "Creative Director",
        description: "Lead creative projects and mentor design teams.",
        traits: { "visual_thinking": 8, "collaboration": 9, "empathy": 8, "autonomy": 9 }
    },
    
    // Technical roles
    {
        title: "Software Engineer",
        description: "Develop robust applications using modern programming languages and frameworks.",
        traits: { "problem_solving": 9, "systematic_thinking": 8, "deep_focus": 8, "autonomy": 7 }
    },
    {
        title: "Senior Backend Developer",
        description: "Architect and implement scalable server-side systems and APIs.",
        traits: { "systematic_thinking": 9, "problem_solving": 9, "deep_focus": 9, "quiet_office": 8 }
    },
    {
        title: "Frontend Developer",
        description: "Build responsive web applications with focus on user experience.",
        traits: { "visual_thinking": 7, "attention_to_detail": 9, "problem_solving": 8, "working_from_home": 8 }
    },
    {
        title: "DevOps Engineer",
        description: "Automate deployment pipelines and manage cloud infrastructure.",
        traits: { "systematic_thinking": 10, "problem_solving": 8, "autonomy": 8, "work_life_balance": 7 }
    },
    {
        title: "Mobile App Developer",
        description: "Create native mobile applications for iOS and Android platforms.",
        traits: { "problem_solving": 8, "attention_to_detail": 9, "visual_thinking": 6, "autonomy": 8 }
    },
    
    // Data roles
    {
        title: "Data Scientist",
        description: "Extract insights from complex datasets using statistical analysis and machine learning.",
        traits: { "pattern_recognition": 10, "systematic_thinking": 9, "deep_focus": 9, "quiet_office": 8 }
    },
    {
        title: "Data Analyst",
        description: "Analyze business data to support decision-making and identify trends.",
        traits: { "pattern_recognition": 9, "attention_to_detail": 9, "systematic_thinking": 8, "quiet_office": 7 }
    },
    {
        title: "Business Intelligence Analyst",
        description: "Create dashboards and reports to visualize business performance metrics.",
        traits: { "pattern_recognition": 8, "visual_thinking": 7, "systematic_thinking": 8, "attention_to_detail": 8 }
    },
    {
        title: "Machine Learning Engineer",
        description: "Build and deploy ML models to solve business problems.",
        traits: { "systematic_thinking": 10, "problem_solving": 9, "pattern_recognition": 9, "deep_focus": 9 }
    },
    
    // Support roles
    {
        title: "Quality Assurance Tester",
        description: "Test software applications to ensure quality and reliability.",
        traits: { "attention_to_detail": 10, "systematic_thinking": 8, "pattern_recognition": 7, "quiet_office": 8 }
    },
    {
        title: "Technical Writer",
        description: "Create clear documentation for technical products and APIs.",
        traits: { "attention_to_detail": 9, "systematic_thinking": 7, "systematic_thinking": 9, "working_from_home": 8 }
    },
    {
        title: "Customer Support Specialist",
        description: "Provide technical support and help customers solve problems.",
        traits: { "empathy": 9, "problem_solving": 7, "systematic_thinking": 8, "collaboration": 6 }
    },
    
    // Specialized roles
    {
        title: "Cybersecurity Analyst",
        description: "Monitor and protect systems from security threats.",
        traits: { "pattern_recognition": 9, "attention_to_detail": 10, "systematic_thinking": 8, "deep_focus": 8 }
    },
    {
        title: "Database Administrator",
        description: "Maintain and optimize database systems for performance and reliability.",
        traits: { "systematic_thinking": 9, "attention_to_detail": 9, "problem_solving": 7, "quiet_office": 8 }
    },
    {
        title: "Research Scientist",
        description: "Conduct research to advance knowledge in computer science and AI.",
        traits: { "deep_focus": 10, "systematic_thinking": 9, "pattern_recognition": 8, "autonomy": 9 }
    }
];

// Generate trait variations for more diverse jobs
function generateTraitVariations(baseTraits, variationCount = 3) {
    const variations = [];
    
    for (let i = 0; i < variationCount; i++) {
        const traits = { ...baseTraits };
        
        // Add 1-3 random additional traits
        const additionalTraitCount = Math.floor(Math.random() * 3) + 1;
        for (let j = 0; j < additionalTraitCount; j++) {
            const randomTrait = TRAIT_POOL[Math.floor(Math.random() * TRAIT_POOL.length)];
            if (!traits[randomTrait]) {
                traits[randomTrait] = Math.floor(Math.random() * 5) + 6; // Random value 6-10
            }
        }
        
        // Slightly modify existing trait values
        Object.keys(traits).forEach(trait => {
            const variation = Math.floor(Math.random() * 3) - 1; // -1, 0, or 1
            traits[trait] = Math.max(1, Math.min(10, traits[trait] + variation));
        });
        
        variations.push(traits);
    }
    
    return variations;
}

// Generate jobs with seniority levels
function generateJobsWithLevels(template, company) {
    const levels = [
        { prefix: "Junior", traitModifier: -1 },
        { prefix: "", traitModifier: 0 },
        { prefix: "Senior", traitModifier: 1 },
        { prefix: "Lead", traitModifier: 2 }
    ];
    
    return levels.map(level => {
        const modifiedTraits = {};
        Object.keys(template.traits).forEach(trait => {
            modifiedTraits[trait] = Math.max(1, Math.min(10, template.traits[trait] + level.traitModifier));
        });
        
        const title = level.prefix ? `${level.prefix} ${template.title}` : template.title;
        
        return {
            id: uuidv4(),
            companyId: company.id,
            title: title,
            description: template.description,
            traits: modifiedTraits,
            createdAt: new Date().toISOString()
        };
    });
}

// Generate the complete job dataset
function generateJobs() {
    const jobs = [];
    
    // Create jobs from templates across all companies
    JOB_TEMPLATES.forEach(template => {
        COMPANIES.forEach(company => {
            // Generate variations of each job template
            const variations = generateTraitVariations(template.traits, 2);
            
            variations.forEach((traits, index) => {
                const job = {
                    id: uuidv4(),
                    companyId: company.id,
                    title: index === 0 ? template.title : `${template.title} (${company.name})`,
                    description: template.description,
                    traits: traits,
                    createdAt: new Date().toISOString()
                };
                jobs.push(job);
            });
        });
    });
    
    // Add some specialized roles with seniority levels
    const specializationTemplates = JOB_TEMPLATES.slice(0, 5); // First 5 templates
    specializationTemplates.forEach(template => {
        const randomCompany = COMPANIES[Math.floor(Math.random() * COMPANIES.length)];
        const levelJobs = generateJobsWithLevels(template, randomCompany);
        jobs.push(...levelJobs);
    });
    
    // Add test jobs for Matthew Parker's profile for algorithm validation
    const matthewTestJobs = [
        // Perfect Match Job (Score = 1.0)
        {
            id: uuidv4(),
            title: "Perfect Match Test Job - Matthew Parker",
            description: "Exactly matches Matthew's trait profile for testing",
            companyId: "550e8400-e29b-41d4-a716-446655440001", // Creative Co.
            traits: {
                "work_life_balance": 5,
                "autonomy": 7,
                "empathy": 9,
                "visual_thinking": 5,
                "systematic_thinking": 5,
                "collaboration": 6,
                "pattern_recognition": 4,
                "problem_solving": 7,
                "quiet_office": 1,
                "working_from_home": 9,
                "attention_to_detail": 8,
                "deep_focus": 3
            },
            createdAt: new Date().toISOString()
        },
        // Close Match +1 Job (Score ≈ 0.22)
        {
            id: uuidv4(),
            title: "Close Match +1 Test Job - Matthew Parker",
            description: "One point higher on each trait for testing",
            companyId: "550e8400-e29b-41d4-a716-446655440002", // Logic Inc.
            traits: {
                "work_life_balance": 6,      // 5+1
                "autonomy": 8,               // 7+1
                "empathy": 10,               // 9+1
                "visual_thinking": 6,        // 5+1
                "systematic_thinking": 6,    // 5+1
                "collaboration": 7,          // 6+1
                "pattern_recognition": 5,    // 4+1
                "problem_solving": 8,        // 7+1
                "quiet_office": 2,           // 1+1
                "working_from_home": 10,     // 9+1
                "attention_to_detail": 9,    // 8+1
                "deep_focus": 4              // 3+1
            },
            createdAt: new Date().toISOString()
        },
        // Close Match -1 Job (Score ≈ 0.22)
        {
            id: uuidv4(),
            title: "Close Match -1 Test Job - Matthew Parker",
            description: "One point lower on each trait for testing",
            companyId: "550e8400-e29b-41d4-a716-446655440003", // DataDriven Corp
            traits: {
                "work_life_balance": 4,      // 5-1
                "autonomy": 6,               // 7-1
                "empathy": 8,                // 9-1
                "visual_thinking": 4,        // 5-1
                "systematic_thinking": 4,    // 5-1
                "collaboration": 5,          // 6-1
                "pattern_recognition": 3,    // 4-1
                "problem_solving": 6,        // 7-1
                "quiet_office": 0,           // 1-1 (minimum 0)
                "working_from_home": 8,      // 9-1
                "attention_to_detail": 7,    // 8-1
                "deep_focus": 2              // 3-1
            },
            createdAt: new Date().toISOString()
        }
    ];
    
    jobs.push(...matthewTestJobs);
    
    return jobs;
}

// Main execution
const jobs = generateJobs();

console.log(`Generated ${jobs.length} jobs for seeding`);
console.log('Sample jobs:');
jobs.slice(0, 3).forEach(job => {
    console.log(`- ${job.title} at ${COMPANIES.find(c => c.id === job.companyId)?.name}`);
    console.log(`  Traits: ${JSON.stringify(job.traits)}`);
});

// Export for use in infrastructure or API calls
module.exports = { jobs, COMPANIES };

// If run directly, output JSON for manual import
if (require.main === module) {
    console.log('\n=== JOBS JSON ===');
    console.log(JSON.stringify(jobs, null, 2));
}