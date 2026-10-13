package com.example.batch.lesson05;

import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.core.io.Resource;

/**
 * 상품 CSV 를 읽는 FlatFileItemReader 를 만든다. (05 장, 08 장에서 공통 사용)
 */
public final class ProductCsvReaders {

    private static final String[] COLUMNS = {"code", "name", "price", "stock"};

    private ProductCsvReaders() {
    }

    public static FlatFileItemReader<ProductCsvRow> create(String name, Resource resource) {
        return new FlatFileItemReaderBuilder<ProductCsvRow>()
                .name(name)                 // ExecutionContext 에 읽은 위치를 저장할 때 쓰는 키
                .resource(resource)
                .encoding("UTF-8")
                .linesToSkip(1)             // 헤더 한 줄 건너뛰기
                .delimited()
                .delimiter(",")
                .names(COLUMNS)
                .targetType(ProductCsvRow.class) // record 이면 생성자 바인딩을 사용한다.
                .build();
    }
}
