plugins {
    id("com.android.library")
    id("maven-publish")
    id("signing")
}

android {
    namespace = "io.github.libxposed.helper"
    compileSdk = 33
    buildToolsVersion = "33.0.2"

    defaultConfig {
        minSdk = 21
        targetSdk = 33
    }

    buildFeatures {
        resValues = false
        buildConfig = false
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles("proguard-rules.pro")
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

publishing {
    publications {
        register<MavenPublication>("helper") {
            artifactId = "helper"
            group = "io.github.libxposed"
            version = "100.0.1"
            pom {
                name.set("helper")
                description.set("Modern Xposed Helper")
                url.set("https://github.com/libxposed/helper")
                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://github.com/libxposed/service/blob/master/LICENSE")
                    }
                }
                developers {
                    developer {
                        name.set("libxposed")
                        url.set("https://libxposed.github.io")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/libxposed/helper.git")
                    url.set("https://github.com/libxposed/helper")
                }
            }
            afterEvaluate {
                from(components.getByName("release"))
            }
        }
    }
    repositories {
        maven {
            name = "ossrh"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials(PasswordCredentials::class)
        }
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/libxposed/helper")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

dependencies {
    compileOnly("androidx.annotation:annotation-experimental:1.3.0")
    compileOnly("androidx.annotation:annotation:1.5.0")
    compileOnly("io.github.libxposed:api:100")
}
