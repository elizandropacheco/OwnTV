buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Kotlin 2.4 metadata requires R8 >= 9.1.29. This stable Google Maven
        // release keeps the current AGP/Gradle versions while updating D8/R8.
        classpath("com.android.tools:r8:9.1.56")
    }
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            // These are build-tool transitive dependencies, not APK libraries.
            // Keep related modules aligned and do not use dynamic versions.
            val patchedVersion = when {
                requested.group == "com.google.protobuf" &&
                    requested.name in setOf("protobuf-java", "protobuf-java-util") -> "3.25.8"
                requested.group == "io.netty" && !requested.name.startsWith("netty-tcnative") -> "4.1.137.Final"
                requested.group == "org.bouncycastle" && requested.name.endsWith("-jdk18on") -> "1.85"
                requested.group == "commons-io" && requested.name == "commons-io" -> "2.22.0"
                requested.group == "org.apache.commons" && requested.name == "commons-compress" -> "1.28.0"
                requested.group == "org.bitbucket.b_c" && requested.name == "jose4j" -> "0.9.7"
                requested.group == "org.jdom" && requested.name == "jdom2" -> "2.0.6.1"
                else -> null
            }
            if (patchedVersion != null) {
                useVersion(patchedVersion)
                because("Use a stable version without the build dependency advisories reviewed for this release")
            }
        }
    }
}

plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.4.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.21" apply false
}

allprojects {
    dependencies.components {
        // AGP resolves Lint in a detached configuration. Metadata rules cover
        // that classpath without replacing tasks or suppressing Lint checks.
        // Scope corrections to the exact tool dependencies audited here.
        withModule("com.android.tools:sdk-common") {
            if (id.version == "32.4.1") {
                allVariants {
                    withDependencies {
                        filter { it.group == "org.bouncycastle" && it.name.endsWith("-jdk18on") }
                            .forEach { it.version { require("1.85") } }
                    }
                }
            }
        }
        withModule("org.apache.commons:commons-compress") {
            if (id.version == "1.27.1") {
                allVariants {
                    withDependencies {
                        filter { it.group == "org.apache.commons" && it.name == "commons-lang3" }
                            .forEach { it.version { require("3.18.0") } }
                    }
                }
            }
        }
        withModule("org.apache.httpcomponents:httpmime") {
            if (id.version == "4.5.6") {
                allVariants {
                    withDependencies {
                        filter { it.group == "org.apache.httpcomponents" && it.name == "httpclient" }
                            .forEach { it.version { require("4.5.14") } }
                    }
                }
            }
        }
    }
}
