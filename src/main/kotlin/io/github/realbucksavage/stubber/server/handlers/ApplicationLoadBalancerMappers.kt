package io.github.realbucksavage.stubber.server.handlers

import com.sun.net.httpserver.HttpExchange
import io.github.realbucksavage.stubber.server.RequestMapper
import io.github.realbucksavage.stubber.server.ResponseHandler
import io.github.realbucksavage.stubber.server.invokeGetter
import java.nio.charset.StandardCharsets
import java.util.*

const val APPLICATION_LOAD_BALANCER_REQUEST =
    "com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent"
const val APPLICATION_LOAD_BALANCER_RESPONSE =
    "com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerResponseEvent"

class ApplicationLoadBalancerRequestMapper : RequestMapper {
    override fun mapRequest(exchange: HttpExchange): Map<String, Any> {
        val requestHeaders = exchange.requestHeaders.mapValues { it.value.firstOrNull().orEmpty() }
            .toMap(TreeMap(String.CASE_INSENSITIVE_ORDER))

        // Map query parameters
        val queryParams = exchange.requestURI.query
            ?.split("&")
            ?.mapNotNull { param ->
                val parts = param.split("=", limit = 2)
                if (parts.isNotEmpty()) parts[0] to (parts.getOrNull(1) ?: "") else null
            }?.toMap()
            ?: emptyMap()

        // Construct body as string
        val body = exchange.requestBody.use { String(it.readAllBytes()) }

        // Map HTTP method
        val httpMethod = exchange.requestMethod

        // Map path
        val path = exchange.requestURI.path

        return mapOf(
            "setHttpMethod" to httpMethod,
            "setPath" to path,
            "setHeaders" to requestHeaders,
            "setQueryStringParameters" to queryParams,
            "setBody" to body,
        )
    }
}

class ApplicationLoadBalancerResponseHandler : ResponseHandler {
    override fun handleResponse(responseObject: Any, exchange: HttpExchange) {
        invokeGetter<Map<String, String>>("getHeaders", responseObject)?.forEach { (key, value) ->
            exchange.responseHeaders[key] = value
        }

        // Get status code, default to 200
        val statusCode = invokeGetter<Int>("getStatusCode", responseObject) ?: 200

        // Get body as bytes
        val responseBodyString = invokeGetter<String>("getBody", responseObject) ?: ""
        val responseBytes = responseBodyString.toByteArray(StandardCharsets.UTF_8)
        val contentLength = responseBytes.size.toLong()

        // Always set Content-Length header
        exchange.responseHeaders["Content-Length"] = contentLength.toString()

        // Send response headers
        exchange.sendResponseHeaders(statusCode, contentLength)

        // Write response body
        exchange.responseBody.use { os ->
            os.write(responseBytes)
        }
    }
}
