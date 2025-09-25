package io.rbs.stubber.server

import com.sun.net.httpserver.HttpExchange

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
    val clazz = instance::class.java
    val getterMethod = clazz.getDeclaredMethod(getterName)

    val value = getterMethod.invoke(instance)
    if (value == null || value !is T) {
        return null
    }

    return value
}