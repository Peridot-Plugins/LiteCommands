plugins {
    id("java-library")
    id("maven-publish")
}

group = "dev.rollczi"
version = "3.4.4-SNAPSHOT"

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    repositories {
        mavenLocal()

        maven(
            name = "titanvale-releases",
            url = "https://repo.titanvale.net",
            username = "MAVEN_USERNAME",
            password = "MAVEN_PASSWORD",
            snapshots = false,
            beta = false,
        )

        maven(
            name = "titanvale-snapshots",
            url = "https://repo.titanvale.net",
            username = "MAVEN_USERNAME",
            password = "MAVEN_PASSWORD",
            snapshots = true,
            beta = true,
        )
    }
}

fun RepositoryHandler.maven(
    name: String,
    url: String,
    username: String,
    password: String,
    snapshots: Boolean = true,
    beta: Boolean = false
) {
    val isSnapshot = version.toString().endsWith("-SNAPSHOT")

    if (isSnapshot && !snapshots) {
        return
    }

    val isBeta = version.toString().contains("-BETA")

    if (isBeta && !beta) {
        return
    }

    this.maven {
        this.name =
            if (isSnapshot) "${name}Snapshots"
            else "${name}Releases"

        this.url =
            if (isSnapshot) uri("$url/snapshots")
            else uri("$url/releases")

        this.credentials {
            this.username = System.getenv(username)
            this.password = System.getenv(password)
        }
    }
}

interface LitePublishExtension {
    var artifactId: String
}

val extension = extensions.create<LitePublishExtension>("litecommandsPublish")

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                artifactId = extension.artifactId
                from(project.components["java"])
            }
        }
    }
}