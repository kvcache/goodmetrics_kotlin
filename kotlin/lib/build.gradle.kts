import com.google.protobuf.gradle.*

plugins {
    id("org.jetbrains.kotlin.jvm")

    id("com.google.protobuf") version "0.8.18"

    // Apply the java-library plugin for API and implementation separation.
    `java-library`
    `maven-publish`

    idea
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

val protoVersion = "3.19.3"
val grpcVersion = "1.43.2"
val grpcKtVersion = "1.2.1"
val coroutinesVersion = "1.6.0"

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("io.grpc:grpc-netty:$grpcVersion")
    implementation("io.grpc:grpc-kotlin-stub:$grpcKtVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("com.google.protobuf:protobuf-kotlin:$protoVersion")


    if (JavaVersion.current().isJava9Compatible) {
        // Workaround for @javax.annotation.Generated
        // see: https://github.com/grpc/grpc-java/issues/3633
        compileOnly("javax.annotation:javax.annotation-api:1.3.1")
    }

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

    protobuf(files("src/proto/metrics"))
}

kotlin {
    jvmToolchain {
        (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of("17"))
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn", "-Xopt-in=kotlin.time.ExperimentalTime")
    }
}

protobuf {
    // Configure the protoc executable
    protoc {
        // Download from repositories
        artifact = "com.google.protobuf:protoc:$protoVersion"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:$grpcKtVersion:jdk7@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpc")
                id("grpckt")
            }
            it.builtins {
                id("kotlin")
            }
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("client") {
            groupId = "io.goodmetrics"
            artifactId = "client"
            version = "0.1.0"

            println("components:")
            for (c in components) {
                println("  ${c.name}")
            }
            from(components["kotlin"])

            pom {
                name.set("Good metrics")
                description.set("A metrics recording library that is good")
                url.set("https://github.com/WarriorOfWire/goodmetrics_kotlin/blob/main/README.md")
                properties.set(mapOf(
                ))
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("warriorofwire")
                        name.set("Kenny")
                        email.set("3454741+WarriorOfWire@users.noreply.github.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com:WarriorOfWire/goodmetrics_kotlin.git")
                    developerConnection.set("scm:git:ssh://git@github.com:WarriorOfWire/goodmetrics_kotlin.git")
                    url.set("https://github.com/WarriorOfWire/goodmetrics_kotlin")
                }
            }
        }
    }
}
