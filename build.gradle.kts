plugins {
	java
	war
	id("org.springframework.boot") version "3.3.2"
	id("io.spring.dependency-management") version "1.1.6"
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
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework:spring-r2dbc")

	implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.springframework.boot:spring-boot-starter-validation")

	implementation ("org.springframework.boot:spring-boot-starter-aop")
	// AspectJ Weaver dependency (optional, but recommended for better performance)
	implementation ("org.aspectj:aspectjweaver")

	implementation("org.liquibase:liquibase-core")

	implementation("io.github.resilience4j:resilience4j-spring-boot2:1.7.0")
	implementation("io.github.resilience4j:resilience4j-circuitbreaker:1.7.0")
	implementation("io.github.resilience4j:resilience4j-retry:1.7.0")

	runtimeOnly("org.postgresql:postgresql")
	runtimeOnly("org.postgresql:r2dbc-postgresql")

	providedRuntime("org.springframework.boot:spring-boot-starter-tomcat")

	developmentOnly("org.springframework.boot:spring-boot-docker-compose")

	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	implementation("org.mapstruct:mapstruct:1.5.5.Final")
	annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.testcontainers:postgresql:1.19.8")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile> {
	options.compilerArgs = listOf("-Amapstruct.defaultComponentModel=spring")
	options.isIncremental = true
}

tasks.withType<Test> {
	useJUnitPlatform()
}
