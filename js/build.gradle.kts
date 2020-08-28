plugins {
    kotlin("js") version "1.4.0"
}
group = "net.tunedal"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    maven {
        url = uri("https://dl.bintray.com/kotlin/kotlinx")
    }
}
dependencies {
    testImplementation(kotlin("test-js"))
    implementation("org.jetbrains.kotlinx:kotlinx-html-js:0.7.2")
    implementation(project(":library"))
}
kotlin {
    js {
        moduleName = "tbx-converter"
        browser {
            binaries.executable()
            webpackTask {
                cssSupport.enabled = true
                outputFileName = "$moduleName.js"
            }
            runTask {
                cssSupport.enabled = true
                outputFileName = "$moduleName.js"
            }
            testTask {
                useKarma {
                    useChromiumHeadless()
                    webpackConfig.cssSupport.enabled = true
                }
            }
        }
    }
}
