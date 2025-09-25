plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("maven-publish")
}

group = "io.rbs"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("ch.qos.logback:logback-classic:1.5.12")

    testImplementation(gradleTestKit())
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

gradlePlugin {
    plugins {
        create("lambda-stubber") {
            id = "io.rbs.lambda-stubber"
            implementationClass = "io.rbs.stubber.LambdaStubberPlugin"
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }

    repositories {
        mavenLocal()
        // TODO: add Maven Central or Plugin Portal repo
    }
}
