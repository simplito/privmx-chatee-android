import java.util.Properties

pluginManagement {
    val properties = java.util.Properties()
    properties.load(File(rootDir.absolutePath + "/local.properties").inputStream())
    val privmxGithubMavenUsername = properties.getProperty("privmxGithubMavenUsername")
    val privmxGithubMavenPassword = properties.getProperty("privmxGithubMavenPassword")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        maven {
            name="PrivmxGithubMaven"
            url = uri("https://maven.pkg.github.com/simplito/privmx-maven-repository")
            credentials{
                username = privmxGithubMavenUsername
                password = privmxGithubMavenPassword
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    val properties = Properties()
    properties.load(File(rootDir.absolutePath + "/local.properties").inputStream())
    val privmxGithubMavenUsername = properties.getProperty("privmxGithubMavenUsername")
    val privmxGithubMavenPassword = properties.getProperty("privmxGithubMavenPassword")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            name="PrivmxGithubMaven"
            url = uri("https://maven.pkg.github.com/simplito/privmx-maven-repository")
            credentials{
                username = privmxGithubMavenUsername
                password = privmxGithubMavenPassword
            }
        }
    }
}

rootProject.name = "Privmx Chatee Android"
include(":app")
 