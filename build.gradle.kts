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
	// Spring Boot Starters
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework:spring-r2dbc")
	implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-aop")

	// AspectJ (only if needed)
	implementation("org.aspectj:aspectjweaver")

	// Liquibase (consider switching to starter if simpler configuration needed)
	implementation("org.liquibase:liquibase-core")
	implementation("org.postgresql:postgresql")

	// Resilience4j (ensure compatibility with Spring Boot 3.x)
	implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0") // Updated version
	implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.2.0")
	implementation("io.github.resilience4j:resilience4j-retry:2.2.0")

	// R2DBC PostgreSQL
	runtimeOnly("org.postgresql:r2dbc-postgresql")

	// Tomcat (for WAR deployment)
	providedRuntime("org.springframework.boot:spring-boot-starter-tomcat")

	// Development tools
	developmentOnly("org.springframework.boot:spring-boot-docker-compose")

	// R2DBC SPI
	implementation("io.r2dbc:r2dbc-spi:0.9.0.RELEASE")

	// Lombok
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.projectlombok:lombok:1.18.22")

	// MapStruct
	implementation("org.mapstruct:mapstruct:1.5.5.Final")
	annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")

	// Testcontainers
	testImplementation("org.testcontainers:postgresql:1.19.8")
	testImplementation("org.testcontainers:r2dbc:1.19.8") // Updated version
	testImplementation("org.testcontainers:testcontainers:1.19.8") // Consistent version

	// Testing frameworks
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.testcontainers:junit-jupiter:1.19.8") // Use consistent Testcontainers version
}

tasks.withType<JavaCompile> {
	options.compilerArgs = listOf("-Amapstruct.defaultComponentModel=spring")
	options.isIncremental = true
}

tasks.withType<Test> {
	useJUnitPlatform()
}
