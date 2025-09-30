package io.github.realbucksavage.stubber.server

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import java.lang.reflect.Proxy

class MockedLambdaContext(
    private val functionName: String = "mocked-lambda",
    private val memoryLimit: Int = 1024,
    private val timeout: Int = 30
) {
    fun getAwsRequestId(): String = "stubbed-request-id"

    fun getLogGroupName(): String = "/aws/lambda/$functionName"

    fun getLogStreamName(): String = "2025/09/30/[$functionName]stubbed-log-stream"

    fun getFunctionName(): String = functionName

    fun getFunctionVersion(): String = "stubbed-version"

    fun getInvokedFunctionArn(): String =
        "arn:aws:lambda:us-east-1:123456789012:function:$functionName"

    fun getIdentity(): Any? = null

    fun getClientContext(): Any? = null

    fun getRemainingTimeInMillis(): Int = timeout * 1000

    fun getMemoryLimitInMB(): Int = memoryLimit

    fun getLogger(): Any =
        object {
            val logger: Logger = Logging.getLogger(this::class.java)

            fun log(message: String?) {
                logger.lifecycle("[LambdaLogger] $message")
            }

            fun log(message: ByteArray) {
                this.log(String(message))
            }
        }
}

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