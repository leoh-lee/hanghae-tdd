package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    private final ConcurrentHashMap<Long, ReentrantLock> userLock = new ConcurrentHashMap<>();

    public UserPoint getUserPointByUserId(long userId) {
        return userPointTable.selectById(userId);
    }

    public List<PointHistory> getPointHistoriesByUserId(long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }

    public UserPoint chargeUserPoint(long userId, long amount) {
        ReentrantLock lock = userLock.computeIfAbsent(userId, key -> new ReentrantLock(true));

        lock.lock();

        try {
            UserPointValidator.validateChargeAmount(amount);

            UserPoint userPoint = userPointTable.selectById(userId);

            UserPointValidator.validateTotalPoints(userPoint.point(), amount);

            pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());

            return userPointTable.insertOrUpdate(userId, userPoint.point() + amount);
        } finally {
            lock.unlock();
        }
    }

    public UserPoint usePoint(long userId, long amount) {
        ReentrantLock lock = userLock.computeIfAbsent(userId, key -> new ReentrantLock(true));

        lock.lock();

        try {
            UserPoint userPoint = userPointTable.selectById(userId);

            UserPointValidator.isNotEnoughPoints(userPoint.point(), amount);

            pointHistoryTable.insert(userId, amount, TransactionType.USE, System.currentTimeMillis());

            return userPointTable.insertOrUpdate(userId, userPoint.point() - amount);
        } finally {
            lock.unlock();
        }
    }
}
