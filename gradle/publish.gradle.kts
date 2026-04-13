apply(plugin = "maven-publish")

extensions.configure<org.gradle.api.publish.PublishingExtension> {
    publications {
        register<org.gradle.api.publish.maven.MavenPublication>("release") {
            afterEvaluate {
                from(components["release"])
            }

            groupId = "com.github.answufeng"
            artifactId = "aw-store"
            version = property("VERSION_NAME")?.toString() ?: "1.0.0"

            pom {
                name.set("aw-store")
                description.set("Android key-value storage library based on MMKV")
                url.set("https://github.com/answufeng/aw-store")
                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
                developers {
                    developer {
                        id.set("answufeng")
                        name.set("answufeng")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/answufeng/aw-store.git")
                    url.set("https://github.com/answufeng/aw-store")
                }
            }
        }
    }
}
