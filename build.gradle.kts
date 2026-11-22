import org.springframework.boot.gradle.tasks.bundling.BootJar
import com.google.protobuf.gradle.id

plugins {
	java
	checkstyle
	id("org.springframework.boot") version "4.1.1"
	id("io.spring.dependency-management") version "1.1.7"
	id("com.google.protobuf") version "0.9.6"
}

group = "org.spring.divas"
version = "0.0.1-SNAPSHOT"

val resilience4jVersion = "2.4.0"
val mapstructVersion = "1.6.3"
val wiremockSpringBootVersion = "4.4.2"
val protobufVersion = "4.36.2"
val grpcVersion = "1.84.1"
val logstashLogbackEncoderVersion = "9.0"
val checkstyleVersion = "13.7.0"

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
	implementation("org.springframework.boot:spring-boot-restclient")
	implementation("org.springframework.boot:spring-boot-starter-aspectj")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
	implementation("org.springframework.boot:spring-boot-starter-grpc-client")
	implementation("io.grpc:grpc-protobuf")
	implementation("io.grpc:grpc-stub")
	implementation("com.google.protobuf:protobuf-java:$protobufVersion")
	implementation("net.logstash.logback:logstash-logback-encoder:$logstashLogbackEncoderVersion")
	implementation("io.github.resilience4j:resilience4j-spring-boot4:$resilience4jVersion")
	implementation("org.hibernate.orm:hibernate-core")
	implementation("org.postgresql:postgresql")
	implementation("org.mapstruct:mapstruct:$mapstructVersion")

	compileOnly("org.projectlombok:lombok")
	compileOnly("jakarta.annotation:jakarta.annotation-api")

	runtimeOnly("io.micrometer:micrometer-registry-prometheus")

	annotationProcessor("org.projectlombok:lombok")
	annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")

	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.wiremock.integrations:wiremock-spring-boot:$wiremockSpringBootVersion")
	testImplementation("com.h2database:h2")

	testCompileOnly("org.projectlombok:lombok")

	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	testAnnotationProcessor("org.projectlombok:lombok")
}

tasks.named<BootJar>("bootJar") {
	archiveFileName.set("app.jar")
}

checkstyle {
	toolVersion = checkstyleVersion
	configFile = file("checks.xml")
	maxWarnings = 0
}

tasks.named<Checkstyle>("checkstyleMain") {
	source = fileTree("src/main/java")
}

protobuf {
	protoc {
		artifact = "com.google.protobuf:protoc:$protobufVersion"
	}
	plugins {
		id("grpc") {
			artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
		}
	}
	generateProtoTasks {
		all().forEach { task ->
			task.plugins {
			}
		}
	}
}

tasks.test {
	useJUnitPlatform()
}
