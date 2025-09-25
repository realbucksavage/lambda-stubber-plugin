package io.rbs.stubber.server

import com.sun.net.httpserver.HttpExchange

const val API_GATEWAY_PROXY_REQUEST = "com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent"
const val API_GATEWAY_PROXY_RESPONSE = "com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent"

class APIGatewayProxyRequestMapper : RequestMapper {
    override fun mapRequest(exchange: HttpExchange): Map<String, Any> {
        val requestHeaders = mutableMapOf<String, String>()
        exchange.requestHeaders.forEach {
            requestHeaders[it.key] = it.value.first()
        }

        return mapOf(
            "setHeaders" to requestHeaders,
            "setBody" to exchange.requestBody.use { String(it.readAllBytes()) },
            "setPath" to exchange.requestURI.path,
        )
    }
}

class APIGatewayProxyResponseHandler : ResponseHandler {
    override fun handleResponse(responseObject: Any, exchange: HttpExchange) {
        invokeGetter<Map<String, String>>("getHeaders", responseObject)?.forEach {
            exchange.responseHeaders[it.key] = it.value
        }

        val statusCode = invokeGetter<Int>("getStatusCode", responseObject) ?: 200
        val responseBytes = invokeGetter<String>("getBody", responseObject)?.toByteArray()
        val contentLength = responseBytes?.size?.toLong() ?: 0
        exchange.responseHeaders["Content-Length"] = contentLength.toString()

        exchange.sendResponseHeaders(statusCode, contentLength)
        exchange.responseBody.use {
            if (responseBytes != null) {
                it.write(responseBytes)
            }
        }
    }
}