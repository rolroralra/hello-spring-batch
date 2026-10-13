package com.example.batch.lesson07;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;

/**
 * 앞 Step 이 Job ExecutionContext 에 올려준 집계 결과를 받아서 리포트 파일을 만든다.
 *
 * <p>{@code #{jobExecutionContext['...']}} 로 Job ExecutionContext 값을 주입받는다.
 */
@Component
@StepScope
public class DailySalesReportTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(DailySalesReportTasklet.class);

    private final LocalDate targetDate;
    private final int orderCount;
    private final long totalAmount;
    private final Path reportDir;

    public DailySalesReportTasklet(@Value("#{jobParameters['targetDate']}") LocalDate targetDate,
                                   @Value("#{jobExecutionContext['orderCount']}") int orderCount,
                                   @Value("#{jobExecutionContext['totalAmount']}") long totalAmount,
                                   @Value("${batch.report-dir}") Path reportDir) {
        this.targetDate = targetDate;
        this.orderCount = orderCount;
        this.totalAmount = totalAmount;
        this.reportDir = reportDir;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws IOException {
        Files.createDirectories(reportDir);
        Path reportFile = reportDir.resolve("daily-sales-" + targetDate + ".txt");

        String content = """
                ===== 일별 매출 리포트 =====
                날짜     : %s
                주문 건수 : %d건
                매출 합계 : %s원
                """.formatted(targetDate, orderCount, NumberFormat.getInstance(Locale.KOREA).format(totalAmount));

        Files.writeString(reportFile, content, StandardCharsets.UTF_8);
        log.info("리포트 생성: {}", reportFile.toAbsolutePath());
        return RepeatStatus.FINISHED;
    }
}
