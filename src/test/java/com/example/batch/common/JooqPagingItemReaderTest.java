package com.example.batch.common;

import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.ExecutionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JooqPagingItemReaderTest {

    private static final List<Integer> SOURCE = IntStream.rangeClosed(1, 7).boxed().toList();

    /**
     * DB 대신 리스트를 offset/limit 으로 잘라서 돌려주는 가짜 쿼리. 호출 기록을 남긴다.
     */
    private final List<String> calls = new ArrayList<>();

    private final JooqPagingItemReader.PageQuery<Integer> fakeQuery = (offset, limit) -> {
        calls.add(offset + "/" + limit);
        return SOURCE.subList(Math.min(offset, SOURCE.size()), Math.min(offset + limit, SOURCE.size()));
    };

    @Test
    void 페이지_단위로_끝까지_읽고_null_을_반환한다() throws Exception {
        var reader = new JooqPagingItemReader<>("testReader", 3, fakeQuery);
        reader.open(new ExecutionContext());

        List<Integer> read = readAll(reader);
        reader.close();

        assertThat(read).containsExactlyElementsOf(SOURCE);
        assertThat(calls).containsExactly("0/3", "3/3", "6/3", "9/3");
    }

    @Test
    void ExecutionContext_에_저장된_위치부터_다시_읽는다() throws Exception {
        var first = new JooqPagingItemReader<>("testReader", 3, fakeQuery);
        var context = new ExecutionContext();
        first.open(context);
        first.read();
        first.read();
        first.read();
        first.read();
        first.update(context); // chunk 커밋 시점에 Step 이 호출하는 것과 같다
        first.close();

        var restarted = new JooqPagingItemReader<>("testReader", 3, fakeQuery);
        restarted.open(context);

        assertThat(readAll(restarted)).containsExactly(5, 6, 7);
    }

    @Test
    void pageSize_가_0_이하이면_예외() {
        assertThatThrownBy(() -> new JooqPagingItemReader<>("testReader", 0, fakeQuery))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static List<Integer> readAll(JooqPagingItemReader<Integer> reader) throws Exception {
        List<Integer> result = new ArrayList<>();
        Integer item;
        while ((item = reader.read()) != null) {
            result.add(item);
        }
        return result;
    }
}
