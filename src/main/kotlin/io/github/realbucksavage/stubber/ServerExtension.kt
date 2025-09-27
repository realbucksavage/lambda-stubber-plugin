package io.github.realbucksavage.stubber

open class ServerExtension {
    val host: String = "127.0.0.1"
    var port: Int = 9795
    var lambdaHandlers: Map<String, LambdaConfiguration> = mutableMapOf()
    var showStacktrace: Boolean = true
}

open class LambdaConfiguration {
    var handlerPattern: String = "/"
    val handlerMethodName: String = "handleRequest"
}