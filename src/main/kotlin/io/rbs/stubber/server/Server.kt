package io.rbs.stubber.server

import com.sun.net.httpserver.HttpServer
import io.rbs.stubber.ServerExtension
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress

private val LOGGER: Logger = LoggerFactory.getLogger("StubberServer")

fun createServer(extension: ServerExtension): HttpServer =
    HttpServer.create(InetSocketAddress(extension.port), 0).apply {
        LOGGER.info("Registering server routes...")

        extension.lambdaHandlers.forEach { (handlerClass, handlerConfig) ->
            createContext(handlerConfig.handlerPattern) { call ->
                val response = "Handling request for $handlerClass".toByteArray()
                call.sendResponseHeaders(200, response.size.toLong())
                call.responseBody.use {
                    it.write(response)
                }
            }
        }
    }