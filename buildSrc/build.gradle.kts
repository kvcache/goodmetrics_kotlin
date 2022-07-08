
repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("gradle-plugin"))
//    implementation("org.jlleitschuh.gradle:ktlint-gradle:10.2.1")
}

plugins {
    kotlin("jvm") version "1.7.10"
    `kotlin-dsl`
    idea
}
