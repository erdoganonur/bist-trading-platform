plugins {
    id("java")
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
    id("com.netflix.dgs.codegen") version "6.2.1"
}

group = "com.bisttrading"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
}

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // GraphQL Dependencies
    implementation("org.springframework.boot:spring-boot-starter-graphql")
    implementation("com.netflix.graphql.dgs:graphql-dgs-spring-boot-starter:8.7.1")
    implementation("com.netflix.graphql.dgs:graphql-dgs-extended-scalars:8.7.1")
    implementation("com.graphql-java:graphql-java-extended-scalars:21.0")

    // DataLoader for N+1 prevention
    implementation("com.graphql-java:java-dataloader:3.2.2")

    // WebSocket for subscriptions
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // JWT & Security
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.security:spring-security-oauth2-jose")

    // HTTP Client for service calls
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    // Database
    implementation("org.postgresql:postgresql")

    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // JSON Processing
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Validation
    implementation("jakarta.validation:jakarta.validation-api")

    // Monitoring
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Core modules dependency
    implementation(project(":platform-core:core-common"))
    implementation(project(":platform-core:core-security"))
    implementation(project(":platform-core:core-domain"))

    // Development
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.graphql:spring-graphql-test")
    testImplementation("com.netflix.graphql.dgs:graphql-dgs-spring-boot-starter-test:8.7.1")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.3")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// GraphQL Code Generation
tasks.named<com.netflix.graphql.dgs.codegen.gradle.GenerateJavaTask>("generateJava") {
    generateClient = false
    packageName = "com.netflix.dgs.codegen.generated"
    typeMapping = mutableMapOf(
        "Decimal" to "java.math.BigDecimal",
        "DateTime" to "java.time.OffsetDateTime",
        "TCKN" to "java.lang.String"
    )
}

// Ensure code generation runs before compilation
tasks.compileJava {
    dependsOn("generateJava")
}

sourceSets {
    main {
        java {
            srcDirs("${layout.buildDirectory.get()}/generated/sources/dgs-codegen/src/main/java")
        }
    }
}