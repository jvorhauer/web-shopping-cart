import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktor_version: String by project

val versions = mapOf(
  "scala" to "2.13",
  "akka" to "2.6.20",
  "akka-http" to "10.2.10",
  "kotlin" to "1.7.20",
  "ktor" to ktor_version
)

plugins {
  application
  kotlin("jvm") version "1.7.20"
  id("io.ktor.plugin") version "2.1.2"
  id("org.jetbrains.kotlin.plugin.serialization") version "1.7.20"
}

group = "nl.miruvor"
version = "1.0.0"

application {
  mainClass.set("nl.miruvor.ApplicationKt")

  val isDevelopment: Boolean = project.ext.has("development")
  applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(platform("io.ktor:ktor-bom:${versions["ktor"]}"))
  implementation("io.ktor:ktor-server-core-jvm")
  implementation("io.ktor:ktor-server-double-receive-jvm")
  implementation("io.ktor:ktor-server-host-common-jvm")
  implementation("io.ktor:ktor-server-status-pages-jvm")
  implementation("io.ktor:ktor-server-call-logging-jvm")
  implementation("io.ktor:ktor-server-metrics-jvm")
  implementation("io.ktor:ktor-server-metrics-micrometer-jvm")
  implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.3")
  implementation("io.ktor:ktor-server-html-builder-jvm")
  implementation("io.ktor:ktor-server-content-negotiation-jvm")
  implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
  implementation("io.ktor:ktor-server-default-headers-jvm")
  implementation("io.ktor:ktor-server-auth-jvm")
  implementation("io.ktor:ktor-server-netty-jvm")
  implementation("io.ktor:ktor-server-request-validation")

  implementation("io.javalin:javalin:5.1.1")

  implementation(platform("com.typesafe.akka:akka-bom_${versions["scala"]}:2.6.20"))
  implementation("com.typesafe.akka:akka-persistence-typed_2.13")
  implementation("com.typesafe.akka:akka-serialization-jackson_2.13")
  implementation("org.scala-lang:scala-library:2.13.10")
  implementation("ch.qos.logback:logback-classic:1.2.11")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.4")
  implementation("org.jetbrains.kotlin:kotlin-reflect")

  implementation("ch.qos.logback:logback-classic:1.3.4")
  implementation("io.micrometer:micrometer-registry-prometheus:1.6.3")

  testImplementation(kotlin("test"))
  testImplementation("io.ktor:ktor-server-tests-jvm")
  testImplementation("com.typesafe.akka:akka-actor-testkit-typed_2.13")
  testImplementation("org.assertj:assertj-core:3.11.1")
  testImplementation("junit:junit:4.13.1")
  implementation(kotlin("stdlib-jdk8"))
}

tasks.test {
  useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  kotlinOptions.jvmTarget = "11"
  kotlinOptions.languageVersion = "1.8"
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
  jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
  jvmTarget = "1.8"
}
