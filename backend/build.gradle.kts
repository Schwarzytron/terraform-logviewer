plugins {
	java
	id("org.springframework.boot") version "3.5.6"
	id("io.spring.dependency-management") version "1.1.7"
	id("com.google.protobuf") version "0.9.4"
	id("java")
}

val grpcVersion = "1.59.0"
val protobufVersion = "3.25.1"
val protocVersion = protobufVersion

group = "ru.konkurst1.ekb"
version = "0.0.1-SNAPSHOT"
description = "terraform-logviewer"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")
	implementation("io.grpc:grpc-netty:$grpcVersion")
	implementation("io.grpc:grpc-protobuf:$grpcVersion")
	implementation("io.grpc:grpc-stub:$grpcVersion")
	implementation("io.grpc:grpc-services:$grpcVersion")
	implementation("com.google.protobuf:protobuf-java:$protobufVersion")
	implementation("com.google.protobuf:protobuf-java-util:$protobufVersion")
	compileOnly("org.projectlombok:lombok")
	compileOnly("org.apache.tomcat:annotations-api:6.0.53")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	developmentOnly("org.springframework.boot:spring-boot-docker-compose")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("com.h2database:h2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

protobuf {
	protoc {
		artifact = "com.google.protobuf:protoc:$protobufVersion"
	}
	plugins {
		create("grpc")  {
			artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion:linux-x86_64"
		}
	}
	generateProtoTasks {
		all().forEach { task ->
			task.plugins {
				create("grpc") {
					option("jvm")
				}
			}
		}
	}
}

sourceSets {
	main {
		java {
			srcDirs(
				"build/generated/source/proto/main/grpc",
				"build/generated/source/proto/main/java"
			)
		}
	}
}