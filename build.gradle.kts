import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
	java
	checkstyle
	pmd
	jacoco
	id("org.springframework.boot") version "4.1.1"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "org.spring.divas"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-validation")

	implementation("org.hibernate.orm:hibernate-core")
	implementation("org.postgresql:postgresql")
	implementation("org.mapstruct:mapstruct:1.6.3")

	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")


	compileOnly("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testCompileOnly("org.projectlombok:lombok")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testAnnotationProcessor("org.projectlombok:lombok")
	testImplementation("com.h2database:h2")
}

tasks.named<BootJar>("bootJar") {
	archiveFileName.set("app.jar")
}

checkstyle {
	toolVersion = "13.7.0"
	configFile = file("checks.xml")
}

pmd {
	isConsoleOutput = true
}

jacoco {
	toolVersion = "0.8.15"
}

tasks.test {
	useJUnitPlatform()
	finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
	dependsOn(tasks.test)
	reports {
		xml.required.set(true)
		csv.required.set(true)
		html.required.set(true)
	}
}