package io.github.realbucksavage.stubber.server

import com.sun.net.httpserver.HttpExchange
import io.github.realbucksavage.stubber.server.handlers.APIGatewayProxyResponseHandler
import io.github.realbucksavage.stubber.server.handlers.API_GATEWAY_PROXY_RESPONSE

interface ResponseHandler {
    fun handleResponse(responseObject: Any, exchange: HttpExchange)
}

private val mappedHandlers = mapOf<String, ResponseHandler>(
    API_GATEWAY_PROXY_RESPONSE to APIGatewayProxyResponseHandler()
)

fun handleResponse(responseObject: Any, exchange: HttpExchange) {
    val responseClassName = responseObject::class.java.name
    val handler = mappedHandlers[responseClassName]
        ?: throw IllegalArgumentException("No response handler defined for $responseClassName")

    handler.handleResponse(responseObject, exchange)
}

inline fun <reified T> invokeGetter(getterName: String, instance: Any): T? {
    val getterMethod = instance::class.java.methods.firstOrNull { it.name == getterName && it.parameterCount == 0 }
        ?: return null

    val value = getterMethod.invoke(instance)
    return value as? T
}