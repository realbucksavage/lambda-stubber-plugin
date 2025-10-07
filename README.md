lambda-stubber-plugin
---
This Gradle plugin provides a zero-dependency solution to stub out
AWS Lambdas during local development. Unlike SAM CLI, this plugin doesn't
require any external dependency like Docker.

## Installation
Add the plugin to your project's `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.realbucksavage.lambda-stubber") version "0.1.0"
}
```

## Configuration

The stub server can be configured in the `build.gradle.kts` using the `stubServer` extension:

```kotlin
stubServer {
    host = "127.0.0.1"      // default value
    port = 9795             // default value
    showStacktrace = true   // default value
    lambdaHandlers = mapOf(
        "com.my.handlers.LambdaHandler1" to LambdaConfiguration().apply {
            handlerPattern = "/"                // default value
            handlerMethodName = "handleRequest" // default value
        },
        "com.my.handlers.LambdaHandler2" to LambdaConfiguration().apply {
            handlerPattern = "/handler-2"
        }
    )
}
```
> NOTE: Handlers must implement `RequestHandler<T, R>` to be usable with this plugin

## Usage

### Start the server

```bash
./gradlew startStubServer
```

- The server will start in blocking mode, similar to `gradle run`.
- Press `Ctrl+C` to stop the server.

### Sending requests

- Request to `http://127.0.0.1:9795` will go to `LambdaHandler1`
- Requests to `http://127.0.0.1:9795/handler-2` will go to `LambdaHandler2`