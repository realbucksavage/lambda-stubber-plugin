package io.github.realbucksavage.stubber.server

import com.sun.net.httpserver.HttpExchange
import io.github.realbucksavage.stubber.server.handlers.APIGatewayProxyRequestMapper
import io.github.realbucksavage.stubber.server.handlers.API_GATEWAY_PROXY_REQUEST
import io.github.realbucksavage.stubber.server.handlers.APPLICATION_LOAD_BALANCER_REQUEST
import io.github.realbucksavage.stubber.server.handlers.ApplicationLoadBalancerRequestMapper

interface RequestMapper {
    fun mapRequest(exchange: HttpExchange): Map<String, Any>
}

private val registeredMappers = mapOf(
    API_GATEWAY_PROXY_REQUEST to APIGatewayProxyRequestMapper(),
    APPLICATION_LOAD_BALANCER_REQUEST to ApplicationLoadBalancerRequestMapper()
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