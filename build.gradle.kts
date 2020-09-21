plugins {
    kotlin("multiplatform") version KOTLIN_VERSION
    kotlin("plugin.serialization") version KOTLIN_VERSION
    `maven-publish`
}
group = "kouch"
version = "1.0-SNAPSHOT"

repositories {
    //webdav/geotools should be before jcenter https://github.com/akhikhl/gretty/issues/322
    maven("https://repo.osgeo.org/repository/release/")
    maven("https://repo.osgeo.org/repository/Geoserver-releases/")
    jcenter()
    mavenCentral()
    maven("https://repo.maven.apache.org/maven2")
    maven("https://repo.boundlessgeo.com/main")
    maven("http://repo.boundlessgeo.com/main")
    maven("http://download.java.net/maven/2")
    gradlePluginPortal()
    maven("https://kotlin.bintray.com/kotlinx")
    maven("http://dl.bintray.com/kotlin/kotlin-eap")
    maven("http://dl.bintray.com/kotlin/kotlin-eap-1.2")
    maven("https://www.jitpack.io")
    maven("http://maven.geo-solutions.it/")
    maven("https://mvnrepository.com/artifact/org.apache.commons/commons-math3")
    maven("https://jcenter.bintray.com/")
}
kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = JAVA_VERSION.toString()
        }
    }
    js {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                    webpackConfig.cssSupport.enabled = true
                }
            }
        }
    }
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }


    sourceSets {
        all {
            languageSettings.apply {
                progressiveMode = true
//                enableLanguageFeature("InlineClasses")
//                useExperimentalAnnotation("kotlin.Experimental")
//                useExperimentalAnnotation("kotlinx.serialization.ImplicitReflectionSerializer")
                useExperimentalAnnotation("kotlinx.serialization.InternalSerializationApi")
//                useExperimentalAnnotation("kotlin.RequiresOptIn")
//                useExperimentalAnnotation("kotlinx.serialization.UnstableDefault")
//                useExperimentalAnnotation("kotlin.ExperimentalUnsignedTypes")
//                useExperimentalAnnotation("kotlin.ExperimentalStdlibApi")
//                useExperimentalAnnotation("kotlin.time.ExperimentalTime")
            }
        }
        val commonMain by getting {
            dependencies {
                implementation(kotlin("reflect"))
                implementation("io.ktor:ktor-client-core:$KTOR_VERSION")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-common:$KOTLINX_COROUTINES_VERSION")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$SERIALIZATION_VERSION")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$KOTLINX_COROUTINES_VERSION")
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-cio:$KTOR_VERSION")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$SERIALIZATION_VERSION")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$KOTLINX_COROUTINES_VERSION")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
        val jsMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-js:$KTOR_VERSION")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$SERIALIZATION_VERSION")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$KOTLINX_COROUTINES_VERSION")
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
//        val nativeMain by getting
//        val nativeTest by getting
    }
}
