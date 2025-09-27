plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    id("signing")
}

group = "io.rbs"
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
    plugins {
        create("lambdaStubberPlugin") {
            id = "io.rbs.lambda-stubber"
            implementationClass = "io.rbs.stubber.LambdaStubberPlugin"
            displayName = "Lambda Stubber Plugin"
            description = "Runs a local stub server for AWS Lambda development and testing"
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("pluginMaven") {
            artifact(tasks["jar"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])

            pom {
                name.set("Lambda Stubber Plugin")
                description.set("A Gradle plugin to run local stub servers for AWS Lambda")
                url.set("https://github.com/realbucksavage/lambda-stubber-plugin")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("realbucksavage")
                        name.set("Jay")
                        email.set("jgodarastpl@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/realbucksavage/lambda-stubber-plugin.git")
                    developerConnection.set("scm:git:ssh://github.com/realbucksavage/lambda-stubber-plugin.git")
                    url.set("https://github.com/realbucksavage/lambda-stubber-plugin")
                }
            }
        }
    }

    repositories {
        maven {
            name = "OSSRH"
            url = uri(
                if (version.toString().endsWith("SNAPSHOT"))
                    "https://ossrh-staging-api.central.sonatype.com/content/repositories/snapshots/"
                else
                    "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/"
            )
            credentials {
                username = project.findProperty("ossrhUsername") as String? ?: ""
                password = project.findProperty("ossrhPassword") as String? ?: ""
            }
        }
    }
}

signing {
    sign(publishing.publications["pluginMaven"])
}

tasks.register("printVersion") {
    doLast {
        println("Project version: $version")
    }
}
