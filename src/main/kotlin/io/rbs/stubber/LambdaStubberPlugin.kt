package io.rbs.stubber

import com.sun.net.httpserver.HttpServer
import io.rbs.stubber.server.createServer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URLClassLoader
import java.util.concurrent.CountDownLatch

class LambdaStubberPlugin : Plugin<Project> {

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(LambdaStubberPlugin::class.java)

        private var server: HttpServer? = null
        private val stopLatch = CountDownLatch(1)

        private const val TASK_GROUP = "development"

        var implementorClassLoader: ClassLoader = LambdaStubberPlugin::class.java.classLoader
    }

    override fun apply(project: Project) {

        val serverExtension = project.extensions.create("stubServer", ServerExtension::class.java)

        project.tasks.register("startStubServer") {
            dependsOn("classes")

            group = TASK_GROUP
            description = "Starts the stub server."

            doLast {
                val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
                val mainSourceSets = sourceSets.getByName("main")
                val urls = (mainSourceSets.output + mainSourceSets.runtimeClasspath).map {
                    it.toURI().toURL()
                }.toTypedArray()

                implementorClassLoader = URLClassLoader(urls, javaClass.classLoader)

                if (server != null) {
                    LOGGER.info("Server is already running...")
                    return@doLast
                }

                server = createServer(serverExtension).apply {
                    start()
                    LOGGER.info("Server started on http://localhost:${serverExtension.port}")

                    stopLatch.await()
                }
            }
        }

        project.tasks.register("stopStubServer") {
            group = TASK_GROUP
            description = "Stops the stub server"

            doLast {
                if (server == null) {
                    LOGGER.info("Server is not running.")
                    return@doLast
                }

                server?.stop(0)
                server = null
                stopLatch.countDown()
                LOGGER.info("Server stopped.")
            }
        }
    }
}