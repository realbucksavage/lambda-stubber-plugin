package io.rbs.stubber.server

import com.sun.net.httpserver.HttpExchange

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

    clazz.declaredMethods.forEach {
        if (!resolvedMethods.containsKey(it.name) || it.parameterCount != 1) {
            return@forEach
        }

        val valueToSet = resolvedMethods[it.name]!!
        if (it.parameterTypes[0] != valueToSet::class.java) {
            return@forEach
        }

        it.invoke(instance, valueToSet)
    }

    return instance
}