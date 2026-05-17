plugins {
    alias(libs.plugins.agp.lib)
    `maven-publish`
    signing
}

android {
    namespace = "io.github.libxposed.helper.legacy"
    compileSdk = 37
    buildToolsVersion = "37.0.0"
    androidResources.enable = false
    enableKotlin = false

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("proguard-rules.pro")
    }

    buildFeatures {
        buildConfig = false
    }

    compileOptions {
        targetCompatibility = JavaVersion.VERSION_17
        sourceCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    compileOnly(libs.libxposed.api)
    compileOnly(libs.androidx.annotation)
    implementation(libs.commons.lang3)
}

val androidJavadoc by tasks.registering(Javadoc::class) {
    title = "libxposed API $version"
    source(android.sourceSets["main"].java.srcDirs)
    destinationDir = layout.buildDirectory.dir("javadoc").get().asFile

    (options as StandardJavadocDocletOptions).apply {
        links("https://docs.oracle.com/en/java/javase/17/docs/api/")
        links("https://developer.android.com/reference/")
        encoding = "UTF-8"
        charSet = "UTF-8"
        docEncoding = "UTF-8"
        addBooleanOption("Xdoclint:all,-missing", true)
    }

    isFailOnError = false

    val bootCp = project.extensions.getByType<com.android.build.api.variant.LibraryAndroidComponentsExtension>()
        .sdkComponents.bootClasspath

    doFirst {
        classpath = files(bootCp.get()) + configurations["releaseCompileClasspath"]
    }
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    dependsOn(androidJavadoc)
    from(androidJavadoc.map { it.destinationDir!! })
}

publishing {
    publications {
        register<MavenPublication>("helper-legacy") {
            artifactId = "helper-legacy"
            group = "io.github.libxposed"
            version = "101.0.0"
            artifact(javadocJar)
            pom {
                name.set("helper-legacy")
                description.set("Legacy Helper for libxposed")
                url.set("https://github.com/libxposed/helper")
                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://github.com/libxposed/helper/blob/master/LICENSE")
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
            url = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
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

signing {
    val signingKey = findProperty("signingKey") as String?
    val signingPassword = findProperty("signingPassword") as String?
    if (!signingKey.isNullOrBlank() && !signingPassword.isNullOrBlank()) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    }
}
