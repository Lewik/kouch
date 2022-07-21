plugins {
    kotlin("multiplatform") version KOTLIN_VERSION
    kotlin("plugin.serialization") version KOTLIN_VERSION
    `maven-publish`
}
group = "kouch"
version = "0.0.53"

repositories {
    mavenCentral()
    maven("https://repo.maven.apache.org/maven2")
    maven("https://www.jitpack.io")
}
kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = JAVA_VERSION.toString()
        }
    }

    sourceSets {
        all {
            languageSettings.apply {
                progressiveMode = true
                optIn("kotlinx.serialization.InternalSerializationApi")
            }
        }
        val commonMain by getting {
            dependencies {
                implementation(kotlin("reflect"))
                implementation("io.ktor:ktor-client-core:$KTOR_VERSION")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$KOTLINX_COROUTINES_VERSION")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$SERIALIZATION_VERSION")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$SERIALIZATION_VERSION")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-cio:$KTOR_VERSION")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$KOTLINX_COROUTINES_VERSION")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$SERIALIZATION_VERSION")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$SERIALIZATION_VERSION")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
    }
}
tasks {
    withType<Test>() {
        enabled = false
        reports.html.required.set(false)
    }
}
