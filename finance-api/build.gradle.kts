plugins {
    // Gives us java sources + the 'api' configuration
    id("java-library")

    // Optional: keep the Spring BOM support. Fine to remove if you don’t use it here
    id("io.spring.dependency-management") version "1.1.6"

    // Lets us publish to Maven local / remote
    id("maven-publish")
}

group = "ru.utlc.finance"
version = "1.0.0"          // bump when you change the public contract

java {
    withSourcesJar()   // registers sourcesJar
    withJavadocJar()   // registers javadocJar
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    api("jakarta.validation:jakarta.validation-api:3.0.2")
}

publishing {
    publications {
        create<MavenPublication>("financeApi") {
            // Automatically includes:
            //  - partner-api-1.0.0.jar
            //  - partner-api-1.0.0-sources.jar
            //  - partner-api-1.0.0-javadoc.jar
            from(components["java"])

            pom {
                name.set("Finance API")
                description.set("Shared DTOs for the Finance microservice")
                scm {
                    connection.set("scm:git:git://github.com/YourOrg/finance-ms.git")
                    developerConnection.set("scm:git:ssh://github.com/YourOrg/finance-ms.git")
                    url.set("https://github.com/YourOrg/finance-ms")
                }
            }
        }
    }
    repositories {
        mavenLocal()
    }
}
