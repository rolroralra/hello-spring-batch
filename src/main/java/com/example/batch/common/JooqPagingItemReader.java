package com.example.batch.common;

import org.springframework.batch.infrastructure.item.data.AbstractPaginatedDataItemReader;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * jOOQ 쿼리로 페이지 단위 조회를 하는 ItemReader.
 *
 * <p>Spring Batch 는 jOOQ 전용 Reader 를 제공하지 않으므로,
 * {@link AbstractPaginatedDataItemReader} 를 상속해서 "페이지를 읽는 방법"만 구현한다.
 * 읽은 건수(read.count)는 부모 클래스가 ExecutionContext 에 저장해 주므로 재시작도 지원된다.
 * (재시작 위치로 이동하는 {@link #jumpToItem(int)} 은 직접 구현했다. 아래 설명 참고)
 *
 * <p>주의: OFFSET 기반 페이징이므로 쿼리에는 반드시 고유한 정렬 조건(ORDER BY id 등)이 있어야 하고,
 * 처리 도중 조회 조건(WHERE)에 해당하는 데이터가 바뀌면 누락/중복이 생길 수 있다.
 *
 * @param <T> 읽을 아이템 타입
 */
public class JooqPagingItemReader<T> extends AbstractPaginatedDataItemReader<T> {

    /**
     * 한 페이지를 조회하는 함수. (offset, limit) -> 결과 목록
     */
    @FunctionalInterface
    public interface PageQuery<T> {
        List<T> fetch(int offset, int limit);
    }

    private final PageQuery<T> pageQuery;

    public JooqPagingItemReader(String name, int pageSize, PageQuery<T> pageQuery) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize 는 1 이상이어야 합니다: " + pageSize);
        }
        this.pageQuery = Objects.requireNonNull(pageQuery, "pageQuery");
        setName(Objects.requireNonNull(name, "name"));
        setPageSize(pageSize);
    }

    @Override
    protected Iterator<T> doPageRead() {
        int offset = page * pageSize;
        return pageQuery.fetch(offset, pageSize).iterator();
    }

    /**
     * 재시작 시 이미 처리한 itemIndex 건을 건너뛴다.
     *
     * <p>부모 클래스의 기본 구현은 건너뛴 뒤 page 를 증가시키지 않아서
     * 같은 페이지를 한 번 더 읽는(중복 처리) 문제가 있어 직접 구현한다.
     * (JooqPagingItemReaderTest 의 재시작 테스트로 검증)
     */
    @Override
    protected void jumpToItem(int itemIndex) {
        page = itemIndex / pageSize;
        int skipInPage = itemIndex % pageSize;

        Iterator<T> currentPage = doPageRead();
        page++;
        for (int i = 0; i < skipInPage && currentPage.hasNext(); i++) {
            currentPage.next();
        }
        results = currentPage;
    }
}
