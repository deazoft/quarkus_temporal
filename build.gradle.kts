plugins {
    java
    alias(libs.plugins.quarkus)
}

group = "com.addi"
version = "1.0.0-SNAPSHOT"

dependencies {
    // Quarkus BOM
    implementation(enforcedPlatform(libs.quarkus.bom))
    implementation(platform(libs.testcontainers.bom))

    // Quarkus Core
    implementation(libs.bundles.quarkus.core)

    // Reactive PostgreSQL
    implementation(libs.bundles.quarkus.database)

    // Kafka
    implementation(libs.bundles.quarkus.kafka)

    // OpenTelemetry (Tracing-First per ADR145)
    implementation(libs.bundles.quarkus.observability)

    // Temporal.io (Quarkiverse extension)
    implementation(libs.bundles.quarkus.temporal)

    // Utilities
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)

    // Testing
    testImplementation(libs.bundles.testing)
    testImplementation(libs.bundles.testcontainers)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf(
        "-parameters",
        "-Amapstruct.defaultComponentModel=cdi"
    ))
}

tasks.withType<Test> {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
    useJUnitPlatform()
}

// Native build configuration (Mandrel per ADR145)
tasks.named("quarkusBuild") {
    // Enable native build with: ./gradlew build -Dquarkus.native.enabled=true
}
