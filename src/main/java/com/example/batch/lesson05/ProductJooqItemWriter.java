package com.example.batch.lesson05;

import org.jooq.DSLContext;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

import static com.example.batch.jooq.Tables.PRODUCT;

/**
 * jOOQ 로 chunk 단위 multi-row INSERT 를 수행하는 Writer.
 *
 * <pre>
 * INSERT INTO product (code, name, price, stock)
 * VALUES (?, ?, ?, ?), (?, ?, ?, ?), ...
 * </pre>
 *
 * jOOQ 는 Spring 의 트랜잭션에 참여하므로 chunk 트랜잭션이 롤백되면 이 INSERT 도 함께 롤백된다.
 */
public class ProductJooqItemWriter implements ItemWriter<Product> {

    private final DSLContext dsl;

    public ProductJooqItemWriter(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public void write(Chunk<? extends Product> chunk) {
        if (chunk.isEmpty()) {
            return;
        }
        var insert = dsl.insertInto(PRODUCT, PRODUCT.CODE, PRODUCT.NAME, PRODUCT.PRICE, PRODUCT.STOCK);
        for (Product product : chunk) {
            insert = insert.values(product.code(), product.name(), product.price(), product.stock());
        }
        insert.execute();
    }
}
