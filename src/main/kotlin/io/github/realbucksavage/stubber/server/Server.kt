package io.github.realbucksavage.stubber.server

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.github.realbucksavage.stubber.LambdaConfiguration
import io.github.realbucksavage.stubber.ServerExtension
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch

private val LOGGER: Logger = Logging.getLogger("StubServer")

private const val REQUEST_HANDLER_INTERFACE = "com.amazonaws.services.lambda.runtime.RequestHandler"
private const val CONTEXT_INTERFACE = "com.amazonaws.services.lambda.runtime.Context"

data class HandlerInfo(
    val handlerInstance: Any,
    val handlerMethod: Method,
    val requestClass: Class<*>,
    val responseClass: Class<*>,
    val config: LambdaConfiguration
)

fun startStubServer(extension: ServerExtension, classLoader: ClassLoader) {
    val server: HttpServer = createServer(extension, classLoader)
    val latch = CountDownLatch(1)

    Runtime.getRuntime().addShutdownHook(Thread {
        LOGGER.lifecycle("Stopping stubber server…")
        try {
            server.stop(0)
        } catch (e: Exception) {
            LOGGER.error("Error while stopping server", e)
        } finally {
            latch.countDown()
        }
    })

    server.start()
    LOGGER.lifecycle("Stubber server running on http://${extension.host}:${extension.port}. Press Ctrl+C to stop.")

    try {
        latch.await()
    } catch (ie: InterruptedException) {
        LOGGER.lifecycle("Thread interrupted. Stopping the server.")
        Thread.currentThread().interrupt()
        server.stop(0)
    }
}

private fun createServer(extension: ServerExtension, classLoader: ClassLoader): HttpServer =
    HttpServer.create(InetSocketAddress(extension.port), 0).apply {
        executor = null

        LOGGER.info("Registering server routes...")

        val handlerInfos = extension.lambdaHandlers.map { (handlerClassName, handlerConfig) ->
            val clazz = classLoader.loadClass(handlerClassName)

            // Find RequestHandler<T, R> implementation
            val matchingInterface = clazz.genericInterfaces
                .filterIsInstance<ParameterizedType>()
                .firstOrNull {
                    it.rawType.typeName == REQUEST_HANDLER_INTERFACE &&
                            it.actualTypeArguments.size == 2
                }
                ?: throw IllegalArgumentException("Clazz ${clazz.name} must implement $REQUEST_HANDLER_INTERFACE<T, R>")

            val typeArgs = matchingInterface.actualTypeArguments
            val requestClass = typeArgs[0] as? Class<*>
                ?: throw IllegalArgumentException("Cannot determine request type of handler $clazz")

            val responseClass = typeArgs[0] as? Class<*>
                ?: throw IllegalArgumentException("Cannot determine response type of handler $clazz")

            val handlerInstance = run {
                val handlerCtor = clazz.getDeclaredConstructor()
                handlerCtor.isAccessible = true
                handlerCtor.newInstance()
            }

            val method = clazz.methods.firstOrNull {
                it.name == handlerConfig.handlerMethodName &&
                        it.parameterCount == 2 && // Request Type and Context
                        it.parameterTypes[0].isAssignableFrom(requestClass) &&
                        it.parameterTypes[1].typeName == CONTEXT_INTERFACE &&
                        it.returnType.isAssignableFrom(responseClass)
            }
                ?: throw java.lang.IllegalArgumentException("Method `${responseClass.name} ${handlerConfig.handlerMethodName}(${requestClass.name}, $CONTEXT_INTERFACE)`")

            HandlerInfo(handlerInstance, method, requestClass, responseClass, handlerConfig)
        }

        handlerInfos.forEach {
            createContext(it.config.handlerPattern) { exchange ->
                handleExchange(exchange, it, extension)
            }
        }
    }


private fun handleExchange(exchange: HttpExchange, handlerInfo: HandlerInfo, extension: ServerExtension) {
    try {
        val mappedRequest = createRequestObject(handlerInfo.requestClass, exchange)
        val responseObject = handlerInfo.handlerMethod.invoke(handlerInfo.handlerInstance, mappedRequest, null)
        handleResponse(responseObject, exchange)
    } catch (ex: InvocationTargetException) {
        LOGGER.error("Error during handler method invocation: ${ex.targetException.message}", ex.targetException)
        sendErrorResponse(exchange, 500, ex.targetException, extension.showStacktrace)
    } catch (ex: Exception) {
        LOGGER.error("Error during request handling: ${ex.message}", ex)
        sendErrorResponse(exchange, 500, ex, extension.showStacktrace)
    } finally {
        exchange.close()
    }
}

private fun sendErrorResponse(exchange: HttpExchange, statusCode: Int, thr: Throwable, showStacktrace: Boolean) {
    var message = "Internal server error"
    if (showStacktrace) {
        message += "\n\n${thr.stackTraceToString()}"
    }

    val bytes = message.toByteArray(Charsets.UTF_8)
    try {
        exchange.sendResponseHeaders(statusCode, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    } catch (ioe: IOException) {
        LOGGER.warn("Failed writing error response: ${ioe.message}", ioe)
    }
}