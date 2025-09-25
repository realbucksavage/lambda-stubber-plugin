package io.rbs.stubber

open class ServerExtension {
    var port: Int = 9795
    var lambdaHandlers: Map<String, LambdaConfiguration> = mutableMapOf()
}

open class LambdaConfiguration {
    var handlerPattern: String = "/"
}