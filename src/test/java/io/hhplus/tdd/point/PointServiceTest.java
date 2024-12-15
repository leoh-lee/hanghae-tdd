package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static io.hhplus.tdd.point.TransactionType.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @InjectMocks
    private PointService pointService;

    @Test
    @DisplayName("사용자 ID로 해당 사용자의 포인트를 조회한다.")
    void getUserPointByUserId() {
        // given
        long userId = 1L;
        int point = 10;
        when(userPointTable.selectById(userId)).thenReturn(new UserPoint(userId, point, System.currentTimeMillis()));
        // when
        UserPoint userPoint = pointService.getUserPointByUserId(userId);
        // then
        assertThat(userPoint.id()).isEqualTo(userId);
        assertThat(userPoint.point()).isEqualTo(point);
    }

    @Test
    @DisplayName("사용자 ID로 해당 사용자의 포인트 충전/이용 내역을 조회한다.")
    void getPointHistoryByUserId() {
        // given
        long userId = 1L;
        List<PointHistory> pointHistories = List.of(
                new PointHistory(1L, userId, 10000L, CHARGE, System.currentTimeMillis()),
                new PointHistory(2L, userId, 3000L, USE, System.currentTimeMillis())
        );
        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(pointHistories);

        // when
        List<PointHistory> findPointHistories = pointService.getPointHistoryByUserId(userId);

        // then
        assertThat(findPointHistories).extracting("id", "userId", "amount", "type")
                .containsExactlyInAnyOrder(
                        tuple(1L, userId, 10000L, CHARGE),
                        tuple(2L, userId, 3000L, USE)
                );
    }

    @Test
    @DisplayName("사용자 ID로 해당 사용자의 포인트를 충전한다.")
    void chargeUserPoint() {
        // given
        long userId = 1L;
        long amount = 10000L;
        when(userPointTable.insertOrUpdate(userId, amount)).thenReturn(new UserPoint(1L, 10000L, System.currentTimeMillis()));

        // when
        UserPoint userPoint = pointService.chargeUserPoint(userId, amount);

        // then
        assertThat(userPoint.id()).isEqualTo(userId);
        assertThat(userPoint.point()).isEqualTo(10000L);
    }

    @Test
    @DisplayName("포인트 사용 시 사용하려는 포인트보다 기존 포인트가 적으면 예외가 발생한다.")
    void usePoint_whenIsNotEnoughPoints_throwException() {
        // given
        long userId = 1L;
        when(userPointTable.selectById(userId)).thenReturn(new UserPoint(1L, 8000L, System.currentTimeMillis()));

        // when
        // then
        assertThatThrownBy(()->pointService.usePoint(userId, 10000L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("포인트가 부족하여 사용할 수 없습니다.");
    }

    @Test
    @DisplayName("사용자 ID와 사용할 포인트로 포인트를 사용한다.")
    void usePoint_success() {
        // given
        long userId = 1L;
        long originalPoint = 10000L;
        long usePoint = 5000L;
        long remainPoint = originalPoint - usePoint;
        when(userPointTable.selectById(userId)).thenReturn(new UserPoint(userId, originalPoint, System.currentTimeMillis()));
        when(userPointTable.insertOrUpdate(userId, remainPoint)).thenReturn(new UserPoint(userId, remainPoint, System.currentTimeMillis()));

        // when
        UserPoint userPoint = pointService.usePoint(userId, usePoint);

        // then
        assertThat(userPoint.id()).isEqualTo(userId);
        assertThat(userPoint.point()).isEqualTo(remainPoint);
    }

}
