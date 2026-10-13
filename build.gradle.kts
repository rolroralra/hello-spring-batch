plugins {
    java
    jacoco
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    // jOOQ 공식 코드 생성 플러그인 (런타임 jOOQ 버전과 맞춘다: Spring Boot 4.1.1 -> jOOQ 3.21.7)
    id("org.jooq.jooq-codegen-gradle") version "3.21.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

val jooqVersion: String = dependencyManagement.importedProperties["jooq.version"] ?: "3.21.7"

dependencies {
    // Spring Batch + JDBC JobRepository (메타데이터 테이블을 PostgreSQL 에 저장)
    implementation("org.springframework.boot:spring-boot-starter-batch-jdbc")
    // jOOQ (DataSource / DSLContext 자동 구성)
    implementation("org.springframework.boot:spring-boot-starter-jooq")

    runtimeOnly("org.postgresql:postgresql")

    // ./gradlew bootRun 시 compose.yaml 의 PostgreSQL 을 자동으로 띄우고 연결 정보를 주입
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    // schema.sql 을 파싱해서 jOOQ 코드를 생성 (DB 없이 코드 생성 가능)
    jooqCodegen("org.jooq:jooq-meta-extensions:$jooqVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-batch-jdbc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-jooq-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

jooq {
    configuration {
        generator {
            database {
                name = "org.jooq.meta.extensions.ddl.DDLDatabase"
                properties {
                    property {
                        key = "scripts"
                        value = "src/main/resources/schema.sql"
                    }
                    property {
                        key = "sort"
                        value = "semantic"
                    }
                    // schema.sql 의 테이블은 스키마 없이(public) 사용한다.
                    property {
                        key = "unqualifiedSchema"
                        value = "none"
                    }
                    // PostgreSQL 처럼 따옴표 없는 식별자를 소문자로 취급
                    property {
                        key = "defaultNameCase"
                        value = "lower"
                    }
                }
            }
            generate {
                isRecords = true
                isPojos = false
                isJavaTimeTypes = true
            }
            target {
                packageName = "com.example.batch.jooq"
                directory = "build/generated-sources/jooq"
            }
        }
    }
}

sourceSets {
    main {
        java {
            srcDir("build/generated-sources/jooq")
        }
    }
}

tasks.named("compileJava") {
    dependsOn(tasks.named("jooqCodegen"))
}

tasks.named<org.jooq.codegen.gradle.CodegenTask>("jooqCodegen") {
    inputs.file("src/main/resources/schema.sql")
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

// 테스트 커버리지 리포트: build/reports/jacoco/test/html/index.html
tasks.jacocoTestReport {
    reports {
        xml.required = true
        html.required = true
    }
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) { exclude("com/example/batch/jooq/**") } // jOOQ 생성 코드 제외
        })
    )
}
