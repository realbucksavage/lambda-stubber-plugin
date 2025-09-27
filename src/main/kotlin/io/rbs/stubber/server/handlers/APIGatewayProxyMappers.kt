package io.rbs.stubber.server.handlers

import com.sun.net.httpserver.HttpExchange
import io.rbs.stubber.server.RequestMapper
import io.rbs.stubber.server.ResponseHandler
import io.rbs.stubber.server.invokeGetter
import java.nio.charset.StandardCharsets

const val API_GATEWAY_PROXY_REQUEST = "com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent"
const val API_GATEWAY_PROXY_RESPONSE = "com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent"

/**
 * APIGatewayProxyRequestMapper implements the RequestMapper interface to map an HttpExchange
 * to com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent.
 */
class APIGatewayProxyRequestMapper : RequestMapper {
    override fun mapRequest(exchange: HttpExchange): Map<String, Any> {
        // Map headers: single value per key (first)
        val requestHeaders = exchange.requestHeaders.mapValues { it.value.firstOrNull().orEmpty() }

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

        // Optionally, you can add stageVariables or pathParameters as needed
        val stageVariables = emptyMap<String, String>()
        val pathParameters = emptyMap<String, String>()

        // Return a map of setter names to values (matches APIGatewayProxyRequestEvent setters)
        return mapOf(
            "setHeaders" to requestHeaders,
            "setBody" to body,
            "setPath" to path,
            "setHttpMethod" to httpMethod,
            "setQueryStringParameters" to queryParams,
            "setStageVariables" to stageVariables,
            "setPathParameters" to pathParameters
        )
    }
}

/**
 * APIGatewayProxyResponseHandler maps a Lambda APIGatewayProxyResponseEvent to HttpExchange.
 */
class APIGatewayProxyResponseHandler : ResponseHandler {
    override fun handleResponse(responseObject: Any, exchange: HttpExchange) {
        // Set headers from the response object
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