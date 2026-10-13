# Hello Spring Batch

Spring Batch 를 처음 배우는 개발자를 위한 **단계별 실습 강의**입니다.
문서를 순서대로 읽으면서, 같은 저장소 안의 실습 코드를 직접 실행해 보는 방식으로 진행합니다.

- 모든 실습은 `./gradlew bootRun` 한 줄로 실행됩니다. (PostgreSQL 은 Docker Compose 로 자동 기동)
- 애플리케이션이 뜰 때마다 `schema.sql` / `data.sql` 이 실행되어 **항상 같은 데이터에서** 실습을 시작합니다.
- 모든 Job 에는 Testcontainers 기반 통합 테스트가 있습니다.

## 기술 스택

| 구분 | 버전 | 비고 |
|------|------|------|
| Java | 25 | Gradle Toolchain 이 없으면 자동으로 내려받음 |
| Spring Boot | 4.1.1 | |
| Spring Batch | 6.0.5 | Spring Boot 4.1.1 관리 버전 |
| jOOQ | 3.21.7 | 타입 안전한 SQL DSL, `schema.sql` 로부터 코드 생성 |
| PostgreSQL | 18.6 (`postgres:18.6-alpine`) | 2026-10 기준 최신 안정 버전 (19 는 beta) |
| Gradle | 9.7.1 (Kotlin DSL) | Wrapper 포함 |
| Testcontainers | 2.0.5 | 통합 테스트용 PostgreSQL |

## 사전 준비

- Docker (Docker Desktop / OrbStack 등) 실행 중일 것
- JDK 는 아무 버전이나 하나 있으면 됩니다 (Gradle 실행용). Java 25 는 Gradle 이 자동으로 준비합니다.

## 빠른 시작

```bash
# 1) 첫 번째 Job 실행 (PostgreSQL 컨테이너가 자동으로 뜬다)
./gradlew bootRun --args='--spring.batch.job.name=helloJob'

# 2) 테스트 전체 실행 (Testcontainers 가 별도의 PostgreSQL 을 띄운다)
./gradlew test
```

로그에 아래와 같이 나오면 성공입니다.

```
>>> [helloJob] 시작 - parameters={JobParameter{name='run.id', value=1, ...}}
Hello, Spring Batch!
현재 등록된 고객 수: 20명
<<< [helloJob] 종료 - status=COMPLETED, exitStatus=COMPLETED, elapsed=240ms
```

## 강의 목차

| # | 문서 | 배우는 내용 | 실습 Job |
|---|------|-------------|----------|
| 01 | [Spring Batch 소개](docs/01-spring-batch-introduction.md) | 배치란 무엇인가, Job/Step/JobRepository, 6.x 변경점 | - |
| 02 | [프로젝트 구성](docs/02-project-setup.md) | Gradle, Docker Compose, schema/data.sql, jOOQ 코드 생성 | - |
| 03 | [첫 번째 Job: Tasklet](docs/03-first-job-tasklet.md) | Job, Step, Tasklet, RunIdIncrementer | `helloJob` |
| 04 | [JobParameters 와 메타데이터](docs/04-job-parameters-and-metadata.md) | JobInstance / JobExecution, `@StepScope`, Validator, BATCH_* 테이블 | `parameterJob` |
| 05 | [Chunk 지향 처리](docs/05-chunk-oriented-processing.md) | Reader / Processor / Writer, CSV -> DB, filter | `productImportJob` |
| 06 | [jOOQ 로 DB -> DB 처리](docs/06-db-to-db-with-jooq.md) | jOOQ 페이징 Reader, batch update, 재시작 가능한 Reader | `customerGradeJob` |
| 07 | [여러 Step 과 Flow](docs/07-multi-step-and-flow.md) | 조건 분기(Decider), ExecutionContext 데이터 공유, 멱등성 | `dailySalesJob` |
| 08 | [내결함성: Skip / Retry](docs/08-fault-tolerance.md) | skip, retry, SkipListener, 트랜잭션 함정 | `faultTolerantImportJob` |
| 09 | [배치 테스트](docs/09-testing.md) | `@SpringBatchTest`, JobOperatorTestUtils, Testcontainers | - |
| 10 | [운영과 다음 단계](docs/10-operations-and-next-steps.md) | 재시작, 스케줄링, 병렬 처리, 운영 체크리스트 | - |

## 실습 Job 실행 명령 모음

```bash
./gradlew bootRun --args='--spring.batch.job.name=helloJob'
./gradlew bootRun --args='--spring.batch.job.name=parameterJob targetDate=2026-10-01,java.time.LocalDate'
./gradlew bootRun --args='--spring.batch.job.name=productImportJob inputFile=classpath:input/products.csv'
./gradlew bootRun --args='--spring.batch.job.name=customerGradeJob'
./gradlew bootRun --args='--spring.batch.job.name=dailySalesJob targetDate=2026-10-01,java.time.LocalDate'
./gradlew bootRun --args='--spring.batch.job.name=dailySalesJob targetDate=2026-10-08,java.time.LocalDate'
./gradlew bootRun --args='--spring.batch.job.name=faultTolerantImportJob inputFile=classpath:input/products-with-errors.csv'
```

## 프로젝트 구조

```
hello-spring-batch
├── build.gradle.kts                 # Spring Boot, jOOQ 코드 생성, JaCoCo 설정
├── compose.yaml                     # PostgreSQL 18.6
├── docs/                            # 강의 문서 (01 ~ 10)
└── src
    ├── main
    │   ├── java/com/example/batch
    │   │   ├── common/              # 공통: JobSummaryListener, JooqPagingItemReader
    │   │   ├── lesson03/            # helloJob
    │   │   ├── lesson04/            # parameterJob
    │   │   ├── lesson05/            # productImportJob
    │   │   ├── lesson06/            # customerGradeJob
    │   │   ├── lesson07/            # dailySalesJob
    │   │   └── lesson08/            # faultTolerantImportJob
    │   └── resources
    │       ├── application.yml
    │       ├── schema.sql           # 도메인 테이블 (매 기동 시 재생성 + jOOQ 코드 생성 입력)
    │       ├── data.sql             # 기본 데이터 (고객 20명, 주문 300건)
    │       └── input/               # CSV 입력 파일
    └── test/java/com/example/batch  # 단위 테스트 + Testcontainers 통합 테스트
```

## 실습 데이터

| 테이블 | 초기 데이터 | 사용하는 장 |
|--------|-------------|-------------|
| `customer` | 20명 (대부분 BRONZE, 19번 GOLD, 20번 VIP) | 03, 06 |
| `orders` | 300건, 2026-10-01 ~ 2026-10-07 (10건 중 1건 CANCELLED) | 04, 06, 07 |
| `product` | 비어 있음 (CSV 로 적재) | 05, 08 |
| `daily_sales_summary` | 비어 있음 (집계 결과) | 07 |
| `product_import_error` | 비어 있음 (Skip 기록) | 08 |

> `spring.sql.init.mode=always` 이므로 **애플리케이션을 띄울 때마다 도메인 테이블이 초기화**됩니다.
> 반면 Spring Batch 메타데이터 테이블(`BATCH_*`)은 계속 쌓입니다. 이 차이가 04장의 핵심 실습입니다.

DB 에 직접 접속해서 결과를 확인할 수도 있습니다.

```bash
docker exec -it hello-batch-postgres psql -U batch -d batch
# 또는 DB 클라이언트: jdbc:postgresql://localhost:5432/batch (batch / batch)
```

## 자주 쓰는 명령

```bash
docker compose up -d          # DB 만 먼저 띄우기
docker compose down -v        # DB 컨테이너와 볼륨 삭제 (메타데이터까지 완전 초기화)
./gradlew jooqCodegen         # schema.sql 을 바꾼 뒤 jOOQ 코드 다시 생성
./gradlew test                # 테스트 + 커버리지 리포트 (build/reports/jacoco/test/html)
```

## 문제 해결

| 증상 | 원인 / 해결 |
|------|-------------|
| `'docker version ...' failed with exit code 1` | Docker 데몬이 꺼져 있음. Docker Desktop 실행 |
| `JobInstanceAlreadyCompleteException` | 같은 파라미터로 이미 성공한 Job. 파라미터를 바꾸거나 `docker compose down -v` 로 메타데이터 초기화 (04장 참고) |
| 전달한 JobParameters 가 무시됨 | Spring Batch 6 에서는 incrementer 가 있는 Job 에 넘긴 파라미터가 무시된다 (03, 04장 참고) |
| 원하지 않는 Job 이 실행됨 | 기본값은 `helloJob` (`application.yml`). `--spring.batch.job.name=...` 으로 지정 |
| 5432 포트 충돌 | 로컬 PostgreSQL 종료 또는 `compose.yaml` 의 포트 변경 |
| jOOQ 클래스(`Tables.CUSTOMER` 등)를 못 찾음 | `./gradlew compileJava` 로 코드 생성. IDE 는 Gradle 프로젝트 다시 불러오기 |

---

[다음: 01. Spring Batch 소개 →](docs/01-spring-batch-introduction.md)
