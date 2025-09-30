plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.2.1"
}

group = "io.github.realbucksavage"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(gradleTestKit())
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

java {
    withSourcesJar()
    withJavadocJar()
}

gradlePlugin {
    website = "https://github.com/realbucksavage/lambda-stubber-plugin"
    vcsUrl = "https://github.com/realbucksavage/lambda-stubber-plugin"

    plugins {
        create("lambdaStubberPlugin") {
            id = "io.github.realbucksavage.lambda-stubber"
            implementationClass = "io.github.realbucksavage.stubber.LambdaStubberPlugin"
            displayName = "Lambda Stubber Plugin"
            description = "Runs a local stub server for AWS Lambda development and testing"
            tags.set(listOf("aws", "lambda", "testing", "stub", "local-development"))
        }
    }
}

tasks.register("printVersion") {
    doLast {
        println("Project version: $version")
    }
}
