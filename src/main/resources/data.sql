-- =====================================================================
-- 실습용 기본 데이터 (기동할 때마다 schema.sql 다음에 실행된다)
-- =====================================================================

-- ---------------------------------------------------------------------
-- 고객 20명. 대부분 BRONZE 로 시작하고, 일부는 일부러 실제 실적과 다른 등급을 넣어 둔다.
--  -> 06 장 customerGradeJob 을 돌리면 등급이 올라가거나(승급) 내려간다(강등).
-- ---------------------------------------------------------------------
INSERT INTO customer (name, email, grade)
VALUES ('김민준', 'minjun.kim@example.com', 'BRONZE'),
       ('이서연', 'seoyeon.lee@example.com', 'BRONZE'),
       ('박도윤', 'doyun.park@example.com', 'BRONZE'),
       ('최지우', 'jiwoo.choi@example.com', 'BRONZE'),
       ('정하준', 'hajun.jung@example.com', 'BRONZE'),
       ('강서윤', 'seoyun.kang@example.com', 'BRONZE'),
       ('조은우', 'eunwoo.cho@example.com', 'BRONZE'),
       ('윤지호', 'jiho.yoon@example.com', 'BRONZE'),
       ('장하은', 'haeun.jang@example.com', 'BRONZE'),
       ('임시우', 'siwoo.lim@example.com', 'BRONZE'),
       ('한수아', 'sua.han@example.com', 'BRONZE'),
       ('오예준', 'yejun.oh@example.com', 'BRONZE'),
       ('서지아', 'jia.seo@example.com', 'BRONZE'),
       ('신주원', 'juwon.shin@example.com', 'BRONZE'),
       ('권다은', 'daeun.kwon@example.com', 'BRONZE'),
       ('황건우', 'gunwoo.hwang@example.com', 'BRONZE'),
       ('안채원', 'chaewon.ahn@example.com', 'BRONZE'),
       ('송현우', 'hyunwoo.song@example.com', 'BRONZE'),
       ('류지안', 'jian.ryu@example.com', 'GOLD'),
       ('홍유나', 'yuna.hong@example.com', 'VIP');

-- ---------------------------------------------------------------------
-- 주문 300건 (2026-10-01 ~ 2026-10-07, 하루 40~45건)
--  * 고객별로 15건씩 (customer_id = i % 20 + 1)
--  * 1~3번 고객은 금액 4배, 4~8번 고객은 2배 -> 등급이 고르게 나뉘도록
--  * 10건 중 1건은 CANCELLED (집계/등급 산정에서 제외)
--  * 2026-10-08 에는 주문이 없다 -> 07 장에서 NO_ORDERS 흐름 실습용
-- 실행할 때마다 같은 데이터가 나오도록 random() 대신 산술식으로 만든다.
-- ---------------------------------------------------------------------
INSERT INTO orders (customer_id, amount, status, ordered_at)
SELECT (i % 20) + 1                                           AS customer_id,
       ((i * 7919) % 97 + 1) * 1000
           * CASE
                 WHEN (i % 20) + 1 <= 3 THEN 4
                 WHEN (i % 20) + 1 <= 8 THEN 2
                 ELSE 1
             END                                              AS amount,
       CASE WHEN i % 10 = 0 THEN 'CANCELLED' ELSE 'PAID' END  AS status,
       TIMESTAMP '2026-10-01 00:00:00'
           + (i % 7) * INTERVAL '1 day'
           + (i % 24) * INTERVAL '1 hour'
           + ((i * 13) % 60) * INTERVAL '1 minute'            AS ordered_at
FROM generate_series(1, 300) AS s(i);
