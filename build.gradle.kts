import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import groovy.lang.Closure

plugins {
    id("com.github.johnrengelman.shadow") version "2.0.4"
    id("me.champeau.gradle.jmh") version "0.4.7"
    // 0.12*rc* drops requirement on native git, which might be useful
    id("com.palantir.git-version") version "0.12.0-rc2"
    java
    distribution
}

dependencies {
    compile(files("$projectDir/tools/repack/asm-re-7.0.jar"))
    testCompile("org.apache.commons:commons-text:1.4")
    testCompile("com.google.code.gson:gson:2.8.5")
    testCompile("com.google.guava:guava:26.0-jre")
    testCompile("org.junit.jupiter:junit-jupiter-api:5.3.1")
    testCompile("org.junit.jupiter:junit-jupiter-params:5.3.1")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:5.3.1")
    jmh("com.google.guava:guava:26.0-jre")
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

tasks {
    register("versionTxt") {
        doLast {
            File(resDir, "version.txt").writeText(extendedVersion())
        }
        outputs.upToDateWhen({ false })
    }
}

tasks.withType<ProcessResources> {
    dependsOn("versionTxt")
}

tasks.getByName("build").dependsOn("shadowJar")
tasks.getByName("distZip").dependsOn("shadowJar")

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
    gradleVersion = "5.0"
    distributionType = Wrapper.DistributionType.ALL
}
