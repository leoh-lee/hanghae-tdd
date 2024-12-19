package io.hhplus.tdd.point;

public class UserPointValidator {
    public static final long MAX_AMOUNT = 100_000L;
    public static final long MIN_AMOUNT = 1_000L;
    public static final long AMOUNT_UNIT = 100L;
    public static final long MAX_TOTAL_POINTS = 1_000_000L;

    public static void validateChargeAmount(long amount) {
        if (amount > MAX_AMOUNT) {
            throw new IllegalArgumentException("포인트는 한 번에 최대 " + MAX_AMOUNT + "까지 충전할 수 있습니다.");
        }
        if (amount < MIN_AMOUNT) {
            throw new IllegalArgumentException("포인트는 최소 " + MIN_AMOUNT + "부터 충전할 수 있습니다.");
        }
        if (amount % AMOUNT_UNIT != 0) {
            throw new IllegalArgumentException("포인트는 " + AMOUNT_UNIT + " 단위로 충전할 수 있습니다.");
        }
    }

    public static void validateTotalPoints(long originalPoint, long amount) {
        if (originalPoint + amount > MAX_TOTAL_POINTS) {
            throw new IllegalStateException("최대 포인트는 " + MAX_TOTAL_POINTS + "입니다.");
        }
    }

    public static void isNotEnoughPoints(long originalPoint, long usePoint) {
        if (originalPoint < usePoint) {
            throw new IllegalStateException("포인트가 부족하여 사용할 수 없습니다.");
        }
    }
}
