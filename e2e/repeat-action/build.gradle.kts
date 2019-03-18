plugins {
    id("java")
}

dependencies {
}

tasks.withType<Jar> {
    manifest {
        attributes(mapOf(
            "Main-Class" to "demo.Main"
        ))
    }
}

repositories {
    mavenCentral()
}

tasks.withType<Wrapper> {
    gradleVersion = "5.2.1"
    distributionType = Wrapper.DistributionType.ALL
}
