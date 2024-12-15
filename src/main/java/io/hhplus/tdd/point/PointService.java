package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    public UserPoint getUserPointByUserId(long id) {
        return userPointTable.selectById(id);
    }

    public List<PointHistory> getPointHistoryByUserId(long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }

    public UserPoint chargeUserPoint(long userId, long amount) {
        return userPointTable.insertOrUpdate(userId, amount);
    }

    public UserPoint usePoint(long userId, long amount) {
        UserPoint userPoint = userPointTable.selectById(userId);

        if (userPoint.isNotEnoughPoints(amount)) {
            throw new IllegalStateException("포인트가 부족하여 사용할 수 없습니다.");
        }

        return userPointTable.insertOrUpdate(userId, userPoint.point() - amount);
    }
}
