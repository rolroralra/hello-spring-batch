package com.example.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HelloSpringBatchApplication {

    public static void main(String[] args) {
        // Job 실행 결과(성공/실패)를 프로세스 종료 코드로 그대로 전달한다. (스케줄러 연동 시 중요)
        System.exit(SpringApplication.exit(SpringApplication.run(HelloSpringBatchApplication.class, args)));
    }
}
