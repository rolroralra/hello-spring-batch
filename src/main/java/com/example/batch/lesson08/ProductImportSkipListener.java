package com.example.batch.lesson08;

import com.example.batch.lesson05.Product;
import com.example.batch.lesson05.ProductCsvRow;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import static com.example.batch.jooq.Tables.PRODUCT_IMPORT_ERROR;

/**
 * Skip 된 아이템을 product_import_error 테이블에 기록한다.
 *
 * <ul>
 *   <li>onSkipInRead    : 파일 파싱 실패 (아이템 객체가 없으므로 원본 라인을 기록)</li>
 *   <li>onSkipInProcess : Processor 검증 실패</li>
 *   <li>onSkipInWrite   : DB 저장 실패 (예: UNIQUE 제약조건 위반)</li>
 * </ul>
 *
 * <p><b>주의</b>: SkipListener 는 chunk 트랜잭션 안에서 호출될 수 있다.
 * 특히 Writer 에서 DB 오류가 나면 PostgreSQL 은 해당 트랜잭션을 abort 상태로 만들기 때문에
 * 같은 트랜잭션으로 INSERT 하면 "current transaction is aborted" 오류가 나고,
 * chunk 가 롤백되면 기록도 함께 사라진다.
 * 그래서 에러 기록은 REQUIRES_NEW 로 <b>별도 트랜잭션</b>에서 커밋한다.
 */
public class ProductImportSkipListener implements SkipListener<ProductCsvRow, Product> {

    private static final Logger log = LoggerFactory.getLogger(ProductImportSkipListener.class);
    private static final int MAX_REASON_LENGTH = 1000;

    private final DSLContext dsl;
    private final TransactionTemplate requiresNewTx;

    public ProductImportSkipListener(DSLContext dsl, PlatformTransactionManager transactionManager) {
        this.dsl = dsl;
        this.requiresNewTx = new TransactionTemplate(transactionManager);
        this.requiresNewTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void onSkipInRead(Throwable t) {
        String item = (t instanceof FlatFileParseException e)
                ? "line " + e.getLineNumber() + ": " + e.getInput()
                : null;
        record("READ", item, t);
    }

    @Override
    public void onSkipInProcess(ProductCsvRow item, Throwable t) {
        record("PROCESS", String.valueOf(item), t);
    }

    @Override
    public void onSkipInWrite(Product item, Throwable t) {
        record("WRITE", String.valueOf(item), t);
    }

    private void record(String phase, String item, Throwable t) {
        String reason = abbreviate(t.getClass().getSimpleName() + ": " + rootMessage(t));
        log.warn("[SKIP:{}] item={} reason={}", phase, item, reason);

        requiresNewTx.executeWithoutResult(status -> dsl.insertInto(PRODUCT_IMPORT_ERROR)
                .set(PRODUCT_IMPORT_ERROR.PHASE, phase)
                .set(PRODUCT_IMPORT_ERROR.ITEM, item)
                .set(PRODUCT_IMPORT_ERROR.REASON, reason)
                .execute());
    }

    private static String rootMessage(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return String.valueOf(root.getMessage());
    }

    private static String abbreviate(String value) {
        return value.length() <= MAX_REASON_LENGTH ? value : value.substring(0, MAX_REASON_LENGTH);
    }
}
