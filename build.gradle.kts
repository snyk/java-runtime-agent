import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import groovy.lang.Closure

plugins {
    id("com.github.johnrengelman.shadow") version "2.0.4"
    id("me.champeau.gradle.jmh") version "0.4.8"
    // 0.12*rc* drops requirement on native git, which might be useful
    id("com.palantir.git-version") version "0.12.0-rc2"
    id("com.github.ben-manes.versions") version "0.21.0"
    java
    distribution
}

dependencies {
    compile(files("$projectDir/tools/repack/asm-re-7.1.jar"))
    testCompile("org.apache.commons:commons-text:1.6")
    testCompile("com.google.code.gson:gson:2.8.5")
    testCompile("com.google.guava:guava:27.1-jre")
    testCompile("org.mockito:mockito-core:2.28.2")
    testCompile("com.github.tomakehurst:wiremock:2.23.2")
    testCompile("org.junit.jupiter:junit-jupiter-api:5.4.2")
    testCompile("org.junit.jupiter:junit-jupiter-params:5.4.2")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:5.4.2")
    jmh("com.google.guava:guava:27.1-jre")
}

fun extendedVersion(): String {
    // https://github.com/palantir/gradle-git-version/issues/105#issuecomment-426228393
    val found = (extensions.extraProperties.get("gitVersion") as? Closure<*>)?.call() ?: "dirty"
    var ver = "git:" + found
    val travis_build = System.getenv("TRAVIS_BUILD_NUMBER")
    if (null != travis_build && !travis_build.isEmpty()) {
        ver += " travis:$travis_build"
    }

    return ver
}

val resDir = File("$projectDir/src/main/resources")
assert(resDir.isDirectory() || resDir.mkdirs())
val distributionDir = File("$projectDir/build/distributions")
assert(distributionDir.isDirectory() || distributionDir.mkdirs())

tasks {
    register("versionTxt") {
        doLast {
            File(resDir, "version.txt").writeText(extendedVersion())
            File(distributionDir, "version.txt").writeText(extendedVersion())
        }
        outputs.upToDateWhen({ false })
    }
}

tasks.withType<ProcessResources> {
    dependsOn("versionTxt")
}

tasks.getByName("build").dependsOn("shadowJar")
tasks.getByName("distZip").dependsOn("shadowJar")
tasks.getByName("distZip").dependsOn("versionTxt")

tasks.withType<Jar> {
    baseName = "without-deps"

    manifest {
        attributes(mapOf(
            "Extended-Version-Info" to extendedVersion(),
            "Premain-Class" to "io.snyk.agent.jvm.EntryPoint",
            "Can-Retransform-Classes" to true
        ))
    }
}

tasks.withType<ShadowJar> {
    mergeServiceFiles()

    baseName = "snyk-java-runtime-agent"
    classifier = ""
    version = ""
}

// It would be great to use shadowDistZip here, but it doesn't want to play nice.
// It wants us to be an application, and hence it wants us to ship shell scripts.
// It seems easier to pick up src/main/dist, and ram b/l/agent.jar in afterwards.
distributions {
    getByName("main") {
        baseName = "snyk-java-runtime-agent"
        contents {
            from ("build/libs") {
                include("snyk-java-runtime-agent.jar")
            }
            from("src/main/resources") {
                include("version.txt")
            }
            from("./") {
                include("LICENSE")
            }
        }
    }
}

tasks.named<Zip>("distZip") {
    // This prevents it from placing the version number, which we're not using,
    // in the archive name, and as the prefix for every path in the archive.
    archiveName = "snyk-java-runtime-agent.zip"
}

tasks.withType<Test> {
    useJUnitPlatform()
}

jmh {
    // severely limit the time benchmarks take to run
    fork = 1
    iterations = 1
    warmupIterations = 1
}

repositories {
    mavenCentral()
    jcenter()
}

tasks.withType<Wrapper> {
    gradleVersion = "5.4.1"
    distributionType = Wrapper.DistributionType.ALL
}

// https://github.com/ben-manes/gradle-versions-plugin#revisions
tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
    resolutionStrategy {
        componentSelection {
            all {
                val rejected = listOf("alpha", "beta", "rc", "cr", "m", "preview", "b", "ea").any { qualifier ->
                    candidate.version.matches(Regex("(?i).*[.-]$qualifier[.\\d-+]*"))
                }
                if (rejected) {
                    reject("Release candidate")
                }
            }
        }
    }
    // optional parameters
    checkForGradleUpdate = true
    outputFormatter = "json"
    outputDir = "build/dependencyUpdates"
    reportfileName = "report"
}
