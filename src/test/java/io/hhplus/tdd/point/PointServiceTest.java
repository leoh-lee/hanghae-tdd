package io.hhplus.tdd.point;

import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private UserPointTable userPointTable;

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
}
