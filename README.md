# diversitus
Experiment around Neurodiversity Job Matching

Backend
* Kotlin Matching Service
* Currently Dummy Data
* Deployed to an AWS ECS cluster
* Dynamo DB database

Deployment
* Pulumi with state stored in AWS
* Includes DB seeding currently

Matching Engine

Here is a step-by-step breakdown of how it works:

* Data Aggregation: The service first fetches all jobs and their corresponding company data.
* Trait Combination: For each job, it creates an effectiveJobTraits map by combining the company's general traits with the job's specific traits. The + operator in Kotlin maps ensures that if a trait exists in both, the job's trait value takes precedence, which is the correct logic.
* Distance Calculation:
It identifies the traits that are common between the user's profile and the job.
It then calculates the "distance" between the user and the job by summing the squared differences for each common trait. This is the core of the Euclidean distance formula. A perfect match would have a distance of 0.
* Similarity Score Conversion: The calculated distance (where lower is better) is converted into a final similarity score using the formula 1.0 / (1.0 + sqrt(sumOfSquares)).
This elegantly transforms the distance into a score between 0.0 and 1.0, where 1.0 represents a perfect match.
As the difference between the user and the job increases, the score gracefully decreases towards zero.
* Sorting: Finally, it sorts the results so that the jobs with the highest similarity scores appear first.

Updated 30/06/2025