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

}
