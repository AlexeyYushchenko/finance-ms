plugins {
	java
	war
	id("org.springframework.boot") version "3.2.5"
	id("io.spring.dependency-management") version "1.1.4"
}

group = "ru.utlc"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
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
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.liquibase:liquibase-core")
	providedRuntime("org.springframework.boot:spring-boot-starter-tomcat")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation ("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("io.github.resilience4j:resilience4j-spring-boot2:1.7.0")
	implementation("io.github.resilience4j:resilience4j-circuitbreaker:1.7.0")
	implementation("io.github.resilience4j:resilience4j-retry:1.7.0")

//	implementation ("org.springframework.boot:spring-boot-starter-data-r2dbc")
//	implementation ("io.r2dbc:r2dbc-postgresql")
//	implementation ("io.r2dbc:r2dbc-testcontainers")

	developmentOnly("org.springframework.boot:spring-boot-docker-compose")
	runtimeOnly("org.postgresql:postgresql")
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.projectlombok:lombok:1.18.22")

	implementation("org.mapstruct:mapstruct:1.5.5.Final")
	annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.testcontainers:postgresql:1.19.8")
}

tasks.withType<JavaCompile> {
	options.compilerArgs = listOf("-Amapstruct.defaultComponentModel=spring")
	options.isIncremental = true
}

tasks.withType<Test> {
	useJUnitPlatform()
}
