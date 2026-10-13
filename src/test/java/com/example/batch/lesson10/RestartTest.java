package com.example.batch.lesson10;

import com.example.batch.support.BatchIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.example.batch.jooq.Tables.PRODUCT;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 10 장. 실패한 Job 을 같은 파라미터로 다시 실행하면 "실패한 지점부터" 이어서 처리된다.
 */
class RestartTest extends BatchIntegrationTestSupport {

    private static final int TOTAL = 20;
    private static final int BROKEN_LINE = 13; // 13번째 상품에서 실패 -> chunk(5) 기준 3번째 chunk

    @Autowired
    private Job productImportJob;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        jobOperatorTestUtils.setJob(productImportJob);
    }

    @Test
    void 실패한_Job_을_같은_파라미터로_재시작하면_실패한_chunk_부터_이어서_처리한다() throws Exception {
        Path csv = tempDir.resolve("products.csv");
        writeProducts(csv, BROKEN_LINE);
        JobParameters params = new JobParametersBuilder()
                .addString("inputFile", csv.toUri().toString())
                .toJobParameters();

        // 1) 첫 실행: 13번째 상품의 음수 가격 때문에 실패. 앞의 chunk 2개(10건)는 이미 커밋됨
        JobExecution first = jobOperatorTestUtils.startJob(params);

        assertThat(first.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(dsl.fetchCount(PRODUCT)).isEqualTo(10);

        // 2) 파일을 고친 뒤 "같은 파라미터"로 다시 실행 -> 같은 JobInstance 의 두 번째 JobExecution
        writeProducts(csv, -1);
        JobExecution second = jobOperatorTestUtils.startJob(params);

        assertThat(second.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(second.getJobInstance().getInstanceId()).isEqualTo(first.getJobInstance().getInstanceId());

        // 3) 11번째 상품부터 읽었으므로 이번 실행의 read 는 10건이고, 중복 없이 전체 20건이 된다
        StepExecution secondStep = second.getStepExecutions().iterator().next();
        assertThat(secondStep.getReadCount()).isEqualTo(TOTAL - 10);
        assertThat(dsl.fetchCount(PRODUCT)).isEqualTo(TOTAL);
    }

    private static void writeProducts(Path csv, int brokenLine) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("code,name,price,stock");
        for (int i = 1; i <= TOTAL; i++) {
            long price = (i == brokenLine) ? -1 : i * 1_000L;
            lines.add("r-%03d,재시작 상품 %d,%d,%d".formatted(i, i, price, i));
        }
        Files.write(csv, lines, StandardCharsets.UTF_8);
    }
}
