package com.example.batch.lesson08;

import com.example.batch.common.JobSummaryListener;
import com.example.batch.lesson05.Product;
import com.example.batch.lesson05.ProductCsvReaders;
import com.example.batch.lesson05.ProductCsvRow;
import com.example.batch.lesson05.ProductItemProcessor;
import com.example.batch.lesson05.ProductJooqItemWriter;
import org.jooq.DSLContext;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.DefaultJobParametersValidator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.batch.infrastructure.item.validator.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 08 장. 내결함성(Fault Tolerance) - 잘못된 데이터가 섞인 CSV 를 Skip / Retry 로 처리한다.
 *
 * <pre>
 * faultTolerantImportJob
 *  └─ faultTolerantImportStep (chunk = 5, faultTolerant)
 *       skip  : FlatFileParseException (파싱 실패), ValidationException (검증 실패),
 *               DuplicateKeyException (중복 상품 코드)       -> 최대 10건까지
 *       retry : TransientDataAccessException (일시적 DB 오류) -> 최대 3번
 *       skipListener : product_import_error 테이블에 기록
 * </pre>
 *
 * 실행: ./gradlew bootRun --args='--spring.batch.job.name=faultTolerantImportJob inputFile=classpath:input/products-with-errors.csv'
 */
@Configuration
public class FaultTolerantImportJobConfig {

    static final int CHUNK_SIZE = 5;
    static final int SKIP_LIMIT = 10;
    static final int RETRY_LIMIT = 3;
    static final String INPUT_FILE = "inputFile";

    @Bean
    public Job faultTolerantImportJob(JobRepository jobRepository,
                                      Step faultTolerantImportStep,
                                      JobSummaryListener jobSummaryListener) {
        return new JobBuilder("faultTolerantImportJob", jobRepository)
                .validator(new DefaultJobParametersValidator(new String[]{INPUT_FILE}, new String[]{}))
                .listener(jobSummaryListener)
                .start(faultTolerantImportStep)
                .build();
    }

    @Bean
    public Step faultTolerantImportStep(JobRepository jobRepository,
                                        PlatformTransactionManager transactionManager,
                                        FlatFileItemReader<ProductCsvRow> faultTolerantCsvReader,
                                        DSLContext dsl) {
        return new StepBuilder("faultTolerantImportStep", jobRepository)
                .<ProductCsvRow, Product>chunk(CHUNK_SIZE)
                .transactionManager(transactionManager)
                .reader(faultTolerantCsvReader)
                .processor(new ProductItemProcessor())
                .writer(new ProductJooqItemWriter(dsl))
                .faultTolerant()
                    .skip(FlatFileParseException.class, ValidationException.class, DuplicateKeyException.class)
                    .skipLimit(SKIP_LIMIT)
                    .retry(TransientDataAccessException.class)
                    .retryLimit(RETRY_LIMIT)
                    .skipListener(new ProductImportSkipListener(dsl, transactionManager))
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<ProductCsvRow> faultTolerantCsvReader(
            ResourceLoader resourceLoader,
            @Value("#{jobParameters['inputFile']}") String inputFile) {
        return ProductCsvReaders.create("faultTolerantCsvReader", resourceLoader.getResource(inputFile));
    }
}
