package io.hhplus.tdd.point;

import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class PointServiceIntegrationTest {

    @Autowired
    private PointService pointService;

    @Autowired
    private UserPointTable userPointTable;

    private void executeConcurrency(int threads, Runnable task) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(threads);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            executor.execute(() -> {
                task.run();
                latch.countDown();
            });
        }
        latch.await();
        executor.shutdown();
    }

    private void executeParallel(int threads, int userCount, Consumer<Long> task) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(threads);
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            long userId = (i % userCount);
            executor.execute(() -> {
                task.accept(userId);
                latch.countDown();
            });
        }

        latch.await();
        executor.shutdown();
    }

    /**
     * 한 명의 유저가 포인트를 동시에 충전할 때, 각각의 요청이 독립적인 지 검증하는 테스트입니다.
     */
    @Test
    @DisplayName("한 명의 사용자가 동시에 여러 건의 포인트를 충전하는 경우 순차적으로 충전한다.")
    void pointShouldChargeConcurrently() throws InterruptedException {
        // given
        long userId = 1L;
        int nThreads = 10;
        long amount = 1_000L;

        executeConcurrency(nThreads, () -> pointService.chargeUserPoint(userId, amount));

        UserPoint userPoint = pointService.getUserPointByUserId(userId);

        // then
        assertThat(userPoint.point()).isEqualTo(nThreads * amount);

        // tearDown
        userPointTable.insertOrUpdate(userId, 0);
    }

    /**
     * 여러명의 유저가 서로 간섭받지 않고 각자 요청에 대해서 독립적인 지 검증하는 테스트입니다.
     */
    @Test
    @DisplayName("여러 명의 사용자가 동시에 포인트를 충전하는 경우 병렬적으로 충전한다.")
    void pointShouldChargeInParallel() throws InterruptedException {
        // given
        int userCount = 3;
        int threads = 30;
        long amount = 1_000L;

        executeParallel(threads, userCount, userId -> pointService.chargeUserPoint(userId, amount));

        // then
        for (int i = 0; i < userCount; i++) {
            UserPoint userPoint = pointService.getUserPointByUserId(i);
            assertThat(userPoint.point()).isEqualTo(amount * threads / userCount);

            // tearDown
            userPointTable.insertOrUpdate(i, 0);
        }
    }

    /**
     * 한 명의 유저가 포인트를 동시에 사용할 때, 각각의 요청이 독립적인 지 검증하는 테스트입니다.
     */
    @Test
    @DisplayName("한 명의 사용자가 동시에 여러 건의 포인트를 사용하는 경우 순차적으로 사용한다.")
    void pointShouldUseConcurrently() throws InterruptedException {
        // given
        long userId = 1L;
        int nThreads = 10;
        long amount = 1_000L;

        for (int i = 0; i < nThreads; i++) {
            pointService.chargeUserPoint(userId, amount);
        }

        // when
        executeConcurrency(nThreads, () -> pointService.usePoint(userId, amount));

        UserPoint userPoint = pointService.getUserPointByUserId(userId);

        // then
        assertThat(userPoint.point()).isZero();

        // tearDown
        userPointTable.insertOrUpdate(userId, 0);
    }

    /**
     * 여러명의 유저가 서로 간섭받지 않고 각자 사용 요청을 수행할 수 있는 지 검증하는 테스트입니다.
     */
    @Test
    @DisplayName("여러 명의 사용자가 동시에 포인트를 사용하는 경우 병렬적으로 사용한다.")
    void pointShouldUseInParallel() throws InterruptedException {
        // given
        int userCount = 3;
        int nThreads = 30;

        for (int i = 0; i < userCount; i++) {
            pointService.chargeUserPoint(i, 100_000L);
        }

        // when
        executeParallel(nThreads, userCount, userId -> pointService.usePoint(userId, 10_000L));

        // then
        for (int i = 0; i < userCount; i++) {
            UserPoint userPoint = pointService.getUserPointByUserId(i);
            assertThat(userPoint.point()).isZero();

            // tearDown
            userPointTable.insertOrUpdate(i, 0);
        }
    }
}
