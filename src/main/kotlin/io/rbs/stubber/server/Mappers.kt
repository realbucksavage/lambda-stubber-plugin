package io.rbs.stubber.server

import com.sun.net.httpserver.HttpExchange
import io.rbs.stubber.server.handlers.APIGatewayProxyRequestMapper
import io.rbs.stubber.server.handlers.API_GATEWAY_PROXY_REQUEST

interface RequestMapper {
    fun mapRequest(exchange: HttpExchange): Map<String, Any>
}

private val registeredMappers = mapOf<String, RequestMapper>(
    API_GATEWAY_PROXY_REQUEST to APIGatewayProxyRequestMapper(),
)

fun createRequestObject(clazz: Class<*>, exchange: HttpExchange): Any {
    val mapper = registeredMappers[clazz.name] ?: throw IllegalArgumentException("$clazz mappers are not yet supported")
    val resolvedMethods = mapper.mapRequest(exchange)

    val instance = clazz.getDeclaredConstructor().newInstance()

     clazz.declaredMethods.forEach { method ->
        if (method.parameterCount == 1 && resolvedMethods.containsKey(method.name)) {
            val valueToSet = resolvedMethods[method.name]!!
            try {
                if (method.parameterTypes[0].isAssignableFrom(valueToSet::class.java)) {
                    method.invoke(instance, valueToSet)
                } else {
                    throw IllegalArgumentException("Type mismatch for method ${method.name}")
                }
            } catch (e: Exception) {
                throw RuntimeException("Failed to set property for method ${method.name}", e)
            }
        }
    }

    return instance
}