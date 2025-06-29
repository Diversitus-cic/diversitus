package com.diversitus

import com.mercateo.ktor.server.lambda.KtorLambda

// This class is the entry point for AWS Lambda.
// It inherits the logic from KtorLambda to translate API Gateway events
// into Ktor requests and pass them to your application module.
class LambdaHandler : KtorLambda()