package io.github.realbucksavage.stubber.server

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import java.lang.reflect.Proxy

fun createReflectiveContext(functionName: String = "stubbed-function", classLoader: ClassLoader): Any {
    val contextClass = classLoader.loadClass("com.amazonaws.services.lambda.runtime.Context")
    val logger: Logger = Logging.getLogger(contextClass)

    val handler = { proxy: Any, method: java.lang.reflect.Method, args: Array<out Any>? ->
        when (method.name) {
            "getAwsRequestId" -> "stubbed-request-id"
            "getLogGroupName" -> "/aws/lambda/$functionName"
            "getLogStreamName" -> "stubbed-log-stream"
            "getFunctionName" -> functionName
            "getFunctionVersion" -> "stubbed-version"
            "getInvokedFunctionArn" -> "arn:aws:lambda:us-east-1:123456789012:function:$functionName"
            "getRemainingTimeInMillis" -> 30_000
            "getMemoryLimitInMB" -> 512
            "getLogger" -> Proxy.newProxyInstance(
                contextClass.classLoader,
                arrayOf(classLoader.loadClass("com.amazonaws.services.lambda.runtime.LambdaLogger"))
            ) { _, m, args ->
                if (m.name == "log" && args != null) {
                    logger.lifecycle("[$functionName] ${args[0]}")
                }

                null
            }

            else -> null
        }
    }

    return Proxy.newProxyInstance(
        classLoader,
        arrayOf(contextClass),
        handler
    )
}