package com.example.batch.lesson05;

import com.example.batch.common.JobSummaryListener;
import org.jooq.DSLContext;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.DefaultJobParametersValidator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 05 장. Chunk 지향 처리 - CSV 파일을 읽어 product 테이블에 적재한다.
 *
 * <pre>
 * productImportJob
 *  └─ productImportStep (chunk = 5)
 *       Reader    : FlatFileItemReader   (input/products.csv)
 *       Processor : ProductItemProcessor (정제 + 필터)
 *       Writer    : ProductJooqItemWriter (jOOQ multi-row insert)
 * </pre>
 *
 * <p>입력 파일(inputFile)이 곧 JobInstance 의 식별자다.
 * 같은 파일은 한 번만 적재되고, 실패하면 같은 파라미터로 다시 실행해 실패 지점부터 재시작한다.
 * (그래서 RunIdIncrementer 를 쓰지 않는다. Spring Batch 6 에서는 incrementer 가 있으면 전달한 파라미터가 무시된다)
 *
 * 실행: ./gradlew bootRun --args='--spring.batch.job.name=productImportJob inputFile=classpath:input/products.csv'
 */
@Configuration
public class ProductImportJobConfig {

    static final int CHUNK_SIZE = 5;
    static final String INPUT_FILE = "inputFile";

    @Bean
    public Job productImportJob(JobRepository jobRepository,
                                Step productImportStep,
                                JobSummaryListener jobSummaryListener) {
        return new JobBuilder("productImportJob", jobRepository)
                .validator(new DefaultJobParametersValidator(new String[]{INPUT_FILE}, new String[]{}))
                .listener(jobSummaryListener)
                .start(productImportStep)
                .build();
    }

    @Bean
    public Step productImportStep(JobRepository jobRepository,
                                  PlatformTransactionManager transactionManager,
                                  FlatFileItemReader<ProductCsvRow> productCsvReader,
                                  DSLContext dsl) {
        return new StepBuilder("productImportStep", jobRepository)
                // <입력 타입, 출력 타입>chunk(커밋 단위)
                .<ProductCsvRow, Product>chunk(CHUNK_SIZE)
                .transactionManager(transactionManager)
                .reader(productCsvReader)
                .processor(new ProductItemProcessor())
                .writer(new ProductJooqItemWriter(dsl))
                .build();
    }

    /**
     * inputFile 은 Spring Resource 경로다. (예: classpath:input/products.csv, file:/tmp/products.csv)
     */
    @Bean
    @StepScope
    public FlatFileItemReader<ProductCsvRow> productCsvReader(
            ResourceLoader resourceLoader,
            @Value("#{jobParameters['inputFile']}") String inputFile) {
        return ProductCsvReaders.create("productCsvReader", resourceLoader.getResource(inputFile));
    }
}
