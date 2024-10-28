
import org.hidetake.gradle.swagger.generator.GenerateSwaggerCode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    java
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
    id("org.springdoc.openapi-gradle-plugin") version "1.9.0"
//    id("org.graalvm.buildtools.native") version "0.9.28"
    id("org.hidetake.swagger.generator") version "2.19.2"
    id("com.gorylenko.gradle-git-properties") version "2.4.2"

    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    kotlin("kapt") version "1.9.25"

    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
}

group = "plus.maa"
version = "2.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget = JvmTarget.JVM_21
    }
}

kapt {
    keepJavacAnnotationProcessors = true
}

repositories {
    maven(url = "https://maven.aliyun.com/repository/public/")
    maven(url = "https://maven.aliyun.com/repository/spring/")
    mavenCentral()
}

dependencies {
    val hutoolVersion = "5.8.32"
    val mapstructVersion = "1.5.5.Final"

    kapt("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("io.mockk:mockk:1.13.13")
//    testImplementation("org.springframework.boot:spring-boot-starter-test")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    // springdoc 相关依赖没有被自动管理，必须保留版本号，
    // springdoc-openapi-starter-webmvc-ui 升级到 2.3.0 以及以上版本会导致 therapi 不兼容
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")
    implementation("com.github.therapi:therapi-runtime-javadoc:0.15.0")
    kapt("com.github.therapi:therapi-runtime-javadoc-scribe:0.15.0")

    // kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")

    // kotlin-logging
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")

    // hutool 的邮箱工具类依赖
    implementation("com.sun.mail:javax.mail:1.6.2")
    implementation("cn.hutool:hutool-extra:$hutoolVersion")
    implementation("cn.hutool:hutool-jwt:$hutoolVersion")
    implementation("cn.hutool:hutool-dfa:$hutoolVersion")

    // mapstruct
    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    kapt("org.mapstruct:mapstruct-processor:$mapstructVersion")

    implementation("org.eclipse.jgit:org.eclipse.jgit:7.0.0.202409031743-r")
    implementation("org.eclipse.jgit:org.eclipse.jgit.ssh.apache.agent:7.0.0.202409031743-r")
    implementation("org.freemarker:freemarker:2.3.33")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation("com.github.erosb:everit-json-schema:1.14.4") {
        exclude("commons-logging", "commons-logging")
    }
    implementation("com.google.guava:guava:33.3.1-jre")
    implementation("org.aspectj:aspectjweaver:1.9.21")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    swaggerCodegen("org.openapitools:openapi-generator-cli:7.9.0")
}

val swaggerOutputDir = layout.buildDirectory.dir("docs")
val swaggerOutputName = "swagger.json"

openApi {
    apiDocsUrl = "http://localhost:8848/v3/api-docs"
    outputDir = swaggerOutputDir
    outputFileName = swaggerOutputName
    waitTimeInSeconds = 30
}

swaggerSources {
    val clientDir = layout.buildDirectory.dir("clients").get()
    val swaggerOutputFile = swaggerOutputDir.get().file(swaggerOutputName).asFile
    create("TsFetch") {
        setInputFile(swaggerOutputFile)
        code(
            closureOf<GenerateSwaggerCode> {
                language = "typescript-fetch"
                configFile = file("client-config/ts-fetch.json")
//            templateDir = file('client-config/typescript-fetch')
                rawOptions = listOf("-e", "mustache")
                outputDir = file(clientDir.dir("ts-fetch-client"))
            },
        )
    }
    create("CSharp") {
        setInputFile(swaggerOutputFile)
        code(
            closureOf<GenerateSwaggerCode> {
                language = "csharp"
                configFile = file("client-config/csharp-netcore.json")
                outputDir = file(clientDir.dir("csharp-client"))
//            rawOptions = listOf("--type-mappings", "binary=System.IO.Stream")
            },
        )
    }
    create("Cpp") {
        setInputFile(swaggerOutputFile)
        code(
            closureOf<GenerateSwaggerCode> {
                language = "cpp-restsdk"
                configFile = file("client-config/cpp.json")
                outputDir = file(clientDir.dir("cpp-client"))
            },
        )
    }
    create("Rust") {
        setInputFile(swaggerOutputFile)
        code(
            closureOf<GenerateSwaggerCode> {
                language = "rust"
                configFile = file("client-config/rust.json")
                outputDir = file(clientDir.dir("rust-client"))
            },
        )
    }
}

tasks {
    forkedSpringBootRun {
        doNotTrackState("See https://github.com/springdoc/springdoc-openapi-gradle-plugin/issues/102")
    }
    test {
        useJUnitPlatform()
    }
}

gitProperties {
    failOnNoGitDirectory = false
    keys = listOf("git.branch", "git.commit.id", "git.commit.id.abbrev", "git.commit.time")
}

ktlint {
    ignoreFailures = false

    reporters {
        reporter(ReporterType.PLAIN)
    }
}
