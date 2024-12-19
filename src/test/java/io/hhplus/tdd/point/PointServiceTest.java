package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static io.hhplus.tdd.point.TransactionType.*;
import static io.hhplus.tdd.point.UserPointValidator.*;
import static io.hhplus.tdd.point.UserPointValidator.AMOUNT_UNIT;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    private static final long USER_ID = 1L;

    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @InjectMocks
    private PointService pointService;

    /**
     * 사용자의 포인트 조회의 성공 케이스를 테스트합니다.
     */
    @Test
    @DisplayName("사용자 ID로 해당 사용자의 포인트를 조회한다.")
    void shouldRetrieveUserPointByUserId() {
        // given
        int point = 10;
        when(userPointTable.selectById(USER_ID)).thenReturn(new UserPoint(USER_ID, point, System.currentTimeMillis()));

        // when
        UserPoint userPoint = pointService.getUserPointByUserId(USER_ID);

        // then
        assertThat(userPoint.id()).isEqualTo(USER_ID);
        assertThat(userPoint.point()).isEqualTo(point);
    }

    /**
     * 사용자의 포인트 충전/이용 내역 조회 성공 케이스를 테스트합니다.
     */
    @Test
    @DisplayName("사용자 ID로 해당 사용자의 포인트 충전/이용 내역을 조회한다.")
    void shouldRetrievePointHistoriesByUserId() {
        // given
        List<PointHistory> pointHistories = List.of(
                new PointHistory(1L, USER_ID, 10000L, CHARGE, System.currentTimeMillis()),
                new PointHistory(2L, USER_ID, 3000L, USE, System.currentTimeMillis())
        );
        when(pointHistoryTable.selectAllByUserId(USER_ID)).thenReturn(pointHistories);

        // when
        List<PointHistory> findPointHistories = pointService.getPointHistoriesByUserId(USER_ID);

        // then
        assertThat(findPointHistories).extracting("id", "userId", "amount", "type")
                .containsExactlyInAnyOrder(
                        tuple(1L, USER_ID, 10000L, CHARGE),
                        tuple(2L, USER_ID, 3000L, USE)
                );
    }

    /**
     * 사용자의 포인트 충전 성공 케이스를 테스트합니다.
     */
    @Test
    @DisplayName("사용자 ID로 해당 사용자의 포인트를 충전한다.")
    void shouldChargeUserPointSuccessfully() {
        // given
        long originalPoint = 50000L;
        long chargePoint = 10000L;
        long chargeMillis = System.currentTimeMillis();

        when(userPointTable.selectById(USER_ID)).thenReturn(new UserPoint(USER_ID, originalPoint, chargeMillis));
        when(userPointTable.insertOrUpdate(USER_ID, originalPoint + chargePoint)).thenReturn(new UserPoint(USER_ID, originalPoint + chargePoint, chargeMillis));

        // when
        UserPoint userPoint = pointService.chargeUserPoint(USER_ID, chargePoint);

        // then
        assertThat(userPoint.id()).isEqualTo(USER_ID);
        assertThat(userPoint.point()).isEqualTo(originalPoint + chargePoint);

        verify(pointHistoryTable, times(1)).insert(eq(USER_ID), eq(chargePoint), eq(CHARGE), anyLong());
    }

    /**
     * 포인트 충전 시 한 번에 충전할 수 있는 최대 포인트를 검증하는 테스트입니다.
     */
    @Test
    @DisplayName("포인트 충전은 한 번에 최대 " + MAX_AMOUNT + " 까지 충전할 수 있다.")
    void shouldThrowExceptionWhenChargingPointsExceedingMaxLimit() {
        // given
        long chargePoint = 100_001L;
        long chargeMillis = System.currentTimeMillis();

        // when // then
        assertThatThrownBy(() -> pointService.chargeUserPoint(USER_ID, chargePoint))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("포인트는 한 번에 최대 %d까지 충전할 수 있습니다.", MAX_AMOUNT);
    }

    /**
     * 사용자의 포인트 총 잔고에 대한 한계를 검증하는 테스트입니다.
     */
    @Test
    @DisplayName("포인트의 잔고는 최대 " + MAX_TOTAL_POINTS + " 까지 충전 할 수 있다.")
    void shouldThrowExceptionWhenChargingPointsExceedingTotalLimit() {
        // given
        long chargePoint = 1_000L;
        long currentTimeMillis = System.currentTimeMillis();

        when(userPointTable.selectById(USER_ID)).thenReturn(new UserPoint(USER_ID,999_001L, currentTimeMillis));

        // when // then
        assertThatThrownBy(() -> pointService.chargeUserPoint(USER_ID, chargePoint))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("최대 포인트는 %d입니다.", MAX_TOTAL_POINTS);
    }

    /**
     * 사용자의 포인트 최소 충전 금액에 대해 검증하는 테스트입니다.
     */
    @Test
    @DisplayName("포인트는 최소 " + MIN_AMOUNT + " 부터 충전할 수 있다.")
    void shouldThrowExceptionWhenChargingPointsBelowMinLimit() {
        // given
        long chargePoint = 999L;
        long currentTimeMillis = System.currentTimeMillis();
        // when // then
        assertThatThrownBy(() -> pointService.chargeUserPoint(USER_ID, chargePoint))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("포인트는 최소 %d부터 충전할 수 있습니다.", MIN_AMOUNT);
    }

    /**
     * 사용자가 충전 할 수 있는 포인트 단위에 대해 검증하는 테스트입니다.
     */
    @ParameterizedTest
    @ValueSource(longs = {1201L, 1010L, 1310L, 1111L, 8801L})
    @DisplayName("포인트는 " + AMOUNT_UNIT +" 단위로 충전할 수 있다.")
    void shouldThrowExceptionWhenChargingPointsNotInAllowedUnits(long wrongChargePoint) {
        // given
        long currentTimeMillis = System.currentTimeMillis();
        // when // then
        assertThatThrownBy(() -> pointService.chargeUserPoint(USER_ID, wrongChargePoint))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("포인트는 %d 단위로 충전할 수 있습니다.", AMOUNT_UNIT);
    }

    /**
     * 포인트 잔액 부족 상황에 발생하는 예외에 대해 검증하는 테스트입니다.
     */
    @Test
    @DisplayName("포인트 사용 시 사용하려는 포인트보다 기존 포인트가 적으면 예외가 발생한다.")
    void shouldThrowExceptionWhenUsingPointsExceedingBalance() {
        // given
        when(userPointTable.selectById(USER_ID)).thenReturn(new UserPoint(1L, 8000L, System.currentTimeMillis()));

        // when
        // then
        assertThatThrownBy(()->pointService.usePoint(USER_ID, 10000L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("포인트가 부족하여 사용할 수 없습니다.");
    }

    /**
     * 포인트 잔액 부족 상황에 발생하는 예외에 대해 검증하는 테스트입니다.
     */
    @Test
    @DisplayName("사용자 ID와 사용할 포인트로 포인트를 사용한다.")
    void shouldUseUserPointSuccessfully() {
        // given
        long originalPoint = 10000L;
        long usePoint = 5000L;
        long remainPoint = originalPoint - usePoint;
        long useMillis = System.currentTimeMillis();

        when(userPointTable.selectById(USER_ID)).thenReturn(new UserPoint(USER_ID, originalPoint, System.currentTimeMillis()));
        when(userPointTable.insertOrUpdate(USER_ID, remainPoint)).thenReturn(new UserPoint(USER_ID, remainPoint, useMillis));

        // when
        UserPoint userPoint = pointService.usePoint(USER_ID, usePoint);

        // then
        assertThat(userPoint.id()).isEqualTo(USER_ID);
        assertThat(userPoint.point()).isEqualTo(remainPoint);
        verify(pointHistoryTable, times(1)).insert(eq(USER_ID), eq(usePoint), eq(USE), anyLong());
    }

}
