package io.rbs.stubber.server

import com.sun.net.httpserver.HttpServer
import io.rbs.stubber.LambdaStubberPlugin
import io.rbs.stubber.ServerExtension
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.reflect.ParameterizedType
import java.net.InetSocketAddress

private val LOGGER: Logger = LoggerFactory.getLogger("StubberServer")

private const val REQUEST_HANDLER_INTERFACE = "com.amazonaws.services.lambda.runtime.RequestHandler"
private const val CONTEXT_INTERFACE = "com.amazonaws.services.lambda.runtime.Context"

fun createServer(extension: ServerExtension): HttpServer =
    HttpServer.create(InetSocketAddress(extension.port), 0).apply {
        LOGGER.info("Registering server routes...")

        extension.lambdaHandlers.forEach { (handlerClass, handlerConfig) ->
            val clazz = LambdaStubberPlugin.implementorClassLoader.loadClass(handlerClass)

            val genericInterfaces = clazz.genericInterfaces.first {
                if (it !is ParameterizedType) {
                    return@first false
                }

                if (it.rawType.typeName != REQUEST_HANDLER_INTERFACE) {
                    return@first false
                }

                val typeArgs = it.actualTypeArguments
                if (typeArgs.size != 2) {
                    throw IllegalArgumentException("The RequestHandler implementation of $clazz requires exactly 2 typed arguments")
                }

                return@first true
            } ?: throw IllegalArgumentException("$clazz doesn't correctly implements a $REQUEST_HANDLER_INTERFACE")

            val requestClass = (genericInterfaces as ParameterizedType).actualTypeArguments[0] as Class<*>

            val constructor = clazz.getDeclaredConstructor()
            val handlerInstance = constructor.newInstance()

            val handlerMethod = clazz.declaredMethods.first {
                if (it.name != handlerConfig.handlerMethodName) {
                    return@first false
                }

                if (it.parameters.size != 2) {
                    return@first false
                }

                if (it.parameters[0].type.name != requestClass.name && it.parameters[1].type.name != CONTEXT_INTERFACE) {
                    return@first false
                }

                return@first true
            }

            createContext(handlerConfig.handlerPattern) { call ->
                try {
                    val mappedRequest = createRequestObject(requestClass, call)
                    val responseObject = handlerMethod.invoke(handlerInstance, mappedRequest, null)
                    handleResponse(responseObject, call)
                } catch (e: Exception) {
                    LOGGER.error("failed to call Lambda", e)

                    val response = "<h1>Internal Server Error</h1><pre>${e.stackTraceToString()}".toByteArray()
                    call.sendResponseHeaders(500, response.size.toLong())
                    call.responseBody.use {
                        it.write(response)
                    }
                }
            }
        }
    }