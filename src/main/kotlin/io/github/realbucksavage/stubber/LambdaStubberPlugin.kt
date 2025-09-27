package io.github.realbucksavage.stubber

import io.github.realbucksavage.stubber.server.startStubServer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import java.net.URLClassLoader

class LambdaStubberPlugin : Plugin<Project> {
    companion object {
        private const val TASK_GROUP = "development"
    }

    override fun apply(project: Project) {

        val serverExtension = project.extensions.create("stubServer", ServerExtension::class.java)

        project.tasks.register("startStubServer") {
            group = TASK_GROUP
            description = "Starts the stub server (background). Depends on classes."

            dependsOn("classes")

            doLast {
                val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
                val main = sourceSets.getByName("main")
                val urls = (main.output + main.runtimeClasspath).map { it.toURI().toURL() }.toTypedArray()

                val urlCl = URLClassLoader(urls, LambdaStubberPlugin::class.java.classLoader)

                startStubServer(serverExtension, urlCl)
            }
        }
    }
}