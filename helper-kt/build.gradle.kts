import org.objectweb.asm.ClassVisitor
import com.android.build.api.instrumentation.*
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
    id("signing")
}

android {
    namespace = "io.github.libxposed.helper.kt"
    compileSdk = 33

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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = listOf(
            "-Xno-param-assertions",
            "-Xno-call-assertions",
            "-Xno-receiver-assertions",
        )
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

abstract class ClassVisitorFactory :
    AsmClassVisitorFactory<InstrumentationParameters.None> {

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        return object : ClassVisitor(Opcodes.ASM9, nextClassVisitor) {
            override fun visitMethod(
                access: Int,
                name: String?,
                descriptor: String?,
                signature: String?,
                exceptions: Array<out String>?
            ): MethodVisitor? =
                if (exceptions?.contains("io/github/libxposed/helper/WOException") == true) null
                else super.visitMethod(access, name, descriptor, signature, exceptions)
        }
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        return classData.className.run { startsWith("io.github.libxposed.helper") && endsWith("KtImpl") }
    }
}

androidComponents.onVariants { variant ->
    variant.instrumentation.transformClassesWith(
        ClassVisitorFactory::class.java,
        InstrumentationScope.PROJECT
    ) {}
}

dependencies {
    compileOnly("io.github.libxposed:api:100")
    implementation(project(":helper"))
}

publishing {
    publications {
        register<MavenPublication>("helperKt") {
            artifactId = "helper-kt"
            group = "io.github.libxposed"
            version = "100.0.1"
            pom {
                name.set("helper-kt")
                description.set("Modern Xposed Helper for Kotlin")
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

signing {
    val signingKey = findProperty("signingKey") as String?
    val signingPassword = findProperty("signingPassword") as String?
    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
    }
    sign(publishing.publications)
}
