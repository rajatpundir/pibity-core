import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("org.springframework.boot") version "2.3.0.RELEASE"
  id("io.spring.dependency-management") version "1.0.9.RELEASE"
  id("com.diffplug.gradle.spotless") version "3.26.1"
  kotlin("jvm") version "1.3.72"
  kotlin("plugin.spring") version "1.3.72"
  kotlin("plugin.jpa") version "1.3.72"
}

group = "com.pibity"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-devtools")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  implementation("org.liquibase:liquibase-core")
  implementation("com.google.code.gson:gson:2.8.6")
  implementation("org.keycloak:keycloak-spring-security-adapter:11.0.2")
  implementation("org.keycloak:keycloak-admin-client:11.0.2")

  runtimeOnly("org.postgresql:postgresql")

  testImplementation("org.springframework.boot:spring-boot-starter-test") {
    exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
  }
  testImplementation("org.springframework.security:spring-security-test")
}

tasks.withType<Test> {
  useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
  kotlinOptions {
    freeCompilerArgs = listOf("-Xjsr305=strict")
    jvmTarget = "1.8"
  }
}

spotless {
  kotlin {
//        ktlint()
    licenseHeaderFile("pibity.kotlin.license")
  }
//  kotlinGradle {
//    ktlint()
//  }
}

configure<SourceSetContainer> {
  named("main") {
    java.srcDir("src/main/kotlin")
  }
}
