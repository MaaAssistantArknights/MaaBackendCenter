import org.hidetake.gradle.swagger.generator.GenerateSwaggerCode
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    id("org.springframework.boot") version "3.2.2"
    id("io.spring.dependency-management") version "1.1.4"
    id("org.springdoc.openapi-gradle-plugin") version "1.8.0"
//    id("org.graalvm.buildtools.native") version "0.9.28"
    id("org.hidetake.swagger.generator") version "2.19.2"
    id("com.gorylenko.gradle-git-properties") version "2.4.1"

    kotlin("jvm") version "1.9.22"
    kotlin("plugin.spring") version "1.9.22"
    kotlin("kapt") version "1.9.22"
}

group = "plus.maa"
version = "2.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

repositories {
    maven(url = "https://maven.aliyun.com/repository/public/")
    maven(url = "https://maven.aliyun.com/repository/spring/")
    mavenCentral()
}


dependencies {
    val hutoolVersion = "5.8.26"
    val mapstructVersion = "1.5.5.Final"

    kapt("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // springdoc相关依赖没有被自动管理，必须保留版本号
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
    implementation("com.github.therapi:therapi-runtime-javadoc:0.15.0")
    kapt("com.github.therapi:therapi-runtime-javadoc-scribe:0.15.0")

    // kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // kotlin-logging
    implementation("io.github.oshai:kotlin-logging-jvm:6.0.3")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // hutool 的邮箱工具类依赖
    implementation("com.sun.mail:javax.mail:1.6.2")
    implementation("cn.hutool:hutool-extra:$hutoolVersion")
    implementation("cn.hutool:hutool-jwt:$hutoolVersion")
    implementation("cn.hutool:hutool-dfa:$hutoolVersion")

    // mapstruct
    implementation("org.mapstruct:mapstruct:${mapstructVersion}")
    kapt("org.mapstruct:mapstruct-processor:${mapstructVersion}")

    implementation("org.eclipse.jgit:org.eclipse.jgit:6.8.0.202311291450-r")
    implementation("org.eclipse.jgit:org.eclipse.jgit.ssh.apache.agent:6.8.0.202311291450-r")
    implementation("org.freemarker:freemarker:2.3.32")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation("com.github.erosb:everit-json-schema:1.14.4")
    implementation("com.google.guava:guava:32.1.3-jre")
    implementation("org.aspectj:aspectjweaver:1.9.21")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    swaggerCodegen("org.openapitools:openapi-generator-cli:7.2.0")

}

kapt {
    keepJavacAnnotationProcessors = true
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}


val swaggerOutputDir = layout.buildDirectory.dir("docs")
val swaggerOutputName = "swagger.json"


openApi {
    apiDocsUrl.set("http://localhost:8848/v3/api-docs")
    outputDir.set(file(swaggerOutputDir))
    outputFileName.set(swaggerOutputName)
    waitTimeInSeconds.set(30)
}

swaggerSources {
    val clientDir = layout.buildDirectory.dir("clients").get()
    val swaggerOutputFile = swaggerOutputDir.get().file(swaggerOutputName)
    create("TsFetch") {
        setInputFile(file(swaggerOutputFile))
        code(closureOf<GenerateSwaggerCode> {
            language = "typescript-fetch"
            configFile = file("client-config/ts-fetch.json")
//            templateDir = file('client-config/typescript-fetch')
            rawOptions = listOf("-e", "mustache")
            outputDir = file(clientDir.dir("ts-fetch-client"))
        })
    }
    create("CSharp") {
        setInputFile(file(swaggerOutputFile))
        code(closureOf<GenerateSwaggerCode> {
            language = "csharp"
            configFile = file("client-config/csharp-netcore.json")
            outputDir = file(clientDir.dir("csharp-client"))
//            rawOptions = listOf("--type-mappings", "binary=System.IO.Stream")
        })
    }
    create("Cpp") {
        setInputFile(file(swaggerOutputFile))
        code(closureOf<GenerateSwaggerCode> {
            language = "cpp-restsdk"
            configFile = file("client-config/cpp.json")
            outputDir = file(clientDir.dir("cpp-client"))
        })
    }
    create("Rust") {
        setInputFile(file(swaggerOutputFile))
        code(closureOf<GenerateSwaggerCode> {
            language = "rust"
            configFile = file("client-config/rust.json")
            outputDir = file(clientDir.dir("rust-client"))
        })
    }
}


gitProperties {
    failOnNoGitDirectory = false
    keys = listOf("git.branch", "git.commit.id", "git.commit.id.abbrev", "git.commit.time")
}
