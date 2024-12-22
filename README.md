# Point Service 동시성 제어 보고서

## 동시성 제어가 필요한 이유
이 애플리케이션에서 Point 자원은 Thread Safety 하지 못한 메모리로 관리되고 있습니다.

`UserPointTable`
```java
@Component
public class UserPointTable {

    private final Map<Long, UserPoint> table = new HashMap<>();

    public UserPoint selectById(Long id) {
        throttle(200);
        return table.getOrDefault(id, UserPoint.empty(id));
    }

    public UserPoint insertOrUpdate(long id, long amount) {
        throttle(300);
        UserPoint userPoint = new UserPoint(id, amount, System.currentTimeMillis());
        table.put(id, userPoint);
        return userPoint;
    }

    private void throttle(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep((long) (Math.random() * millis));
        } catch (InterruptedException ignored) {

        }
    }
}
```
특히나 `@Component`로 객체를 관리하기 때문에, 스프링 애플리케이션 전체에서 싱글톤으로 객체를 관리합니다. 따라서 모든 요청이 유일한 `UserPointTable` 인스턴스를 사용합니다.

이 경우, 지역 변수는 상관없지만 멤버 필드인 `private final Map<Long, UserPoint> table = new HashMap<>();`는 모든 클라이언트 요청이 자원을 공유합니다.

그렇기 때문에 다수의 클라이언트 요청이 들어오는 경우, `table` 자원에 대해 동시에 접근하는 위험이 발생합니다.
예를 들어, 특정 사용자가 포인트 충전 혹은 사용을 동시에 하는 경우 동시성 제어가 제대로 되지 않으면 어느 요청은 무시되는 상황이 발생합니다.

따라서 우리는 이러한 공유 자원에 대해 동시성을 제어할 필요가 있습니다.

## 동시성 제어 방법
결과적으로 저는 ConcurrentHashMap과 ReentrantLock을 사용하여 동시성을 제어하였습니다.

제가 고려했던 동시성 제어 방법을 알아보고, 채택 이유를 말씀드리겠습니다.
### 1. `synchronized`

`synchronized`는 자바에서 `모니터 락`을 사용하여 동시성 제어를 매우 간단하게 할 수 있도록 해주는 키워드입니다.

#### 사용 방법
 
```java
public synchronized UserPoint chargeUserPoint() { // -- (1)
    // 수행할 로직
}

public UserPoint chargeUserPoint() {             // -- (2)
    synchronized (this) {
        // 수행할 로직
    }
}
```
(1) 처럼 메서드에 바로 키워드를 붙일 수도 있고, 메서드 내부에서 블록형태로도 사용할 수 있습니다. 
일반적으로는 메서드에 붙이기보다 블럭으로 사용하는 것을 권장합니다. 
메서드에 붙이면, 임계영역이 아닌 지점까지도 lock을 할 수 있기 때문에 좀 더 유연하게 사용하기 위해서는 `synchronized block`을 사용하면 됩니다. 

자바 클래스와 객체에는 **모니터** 라는 것이 존재하는데요. `synchronized`는 이 **모니터**를 사용하여 락을 관리합니다.

메서드에 `synchronized`를 붙여주면 해당 메서드의 인스턴스에 있는 모니터로 락을 걸게 됩니다. `synchronized block`에서는 본인이 원하는 클래스, 객체를 소괄호안에 사용하면 됩니다. 이 때 `this`는 인스턴스 자신을 말합니다.

`synchronized`는 굉장히 편리한 기능입니다. 다만, 락 기능이 너무 부족하고 유연성, 확장성이 떨어집니다. 락 획득에 타임아웃을 둔다든가 하는 로직은 사용할 수 없습니다.


### 2. `ReentrantLock`
`ReentrantLock`은 자바에서 동시성 제어를 더 정교하게 처리할 수 있는 도구입니다. `synchronized`와 달리, `ReentrantLock`은 같은 스레드에서 중첩적으로 락을 획득할 수 있는 특징을 가지고 있으며, 유연성과 성능 면에서 장점이 있습니다.

#### 사용 방법
```java
private final ReentrantLock lock = new ReentrantLock();

public UserPoint insertOrUpdate(long id, long amount) {
    lock.lock(); // 락 획득
    try {
        // 임계영역
    } finally {
        lock.unlock(); // 락 해제
    }
}
```

`tryLock()`을 사용하여 timeout을 설정할 수도 있고, `Condition`을 사용하여 특정 조건에서 대기하거나 신호를 보낼 수도 있습니다.

### 3. `Concurrent Collections`
자바의 `java.util.concurrent` 패키지는 동시성 문제를 해결하기 위해 여러 유용한 컬렉션을 제공합니다. 이 패키지의 컬렉션들은 자체적으로 동시성을 지원하며, 스레드 안전한 방식으로 작동합니다.

`ConcurrentHashMap`은 이 패키지에서 제공하는 대표적인 클래스 중 하나로, 다수의 스레드가 동시에 읽고 쓰기를 수행할 수 있도록 설계되었습니다.

`ConcurrentHashMap`의 특징
- 세분화된 락(Segment-Based Locking):
`ConcurrentHashMap`은 내부적으로 버킷에 락을 세분화하여 특정 키에 대한 작업만 락을 걸도록 최적화되어 있습니다. 이는 전체 맵에 락을 걸지 않기 때문에 높은 동시성을 보장합니다.

- 락 없는 읽기:
대부분의 읽기 작업은 락을 필요로 하지 않으므로, 읽기 성능이 뛰어납니다.

- 병렬성 제어: 
내부적으로 병렬 수준(parallelism level)을 설정하여 얼마나 많은 쓰레드가 동시에 접근할 수 있는지 제어할 수 있습니다.

### 4. `Atomic Variables`
자바의 `java.util.concurrent.atomic` 패키지는 원자적 연산을 제공하여 동시성을 보장합니다. 이 패키지의 클래스들은 기본 데이터 타입(int, long 등)을 동시성 문제 없이 안전하게 관리할 수 있도록 설계되어 있습니다. 가장 일반적으로 사용되는 클래스는 `AtomicInteger`, `AtomicLong`, `AtomicReference` 등이 있습니다.

내부적으로 CAS(Compare-And-Swap) 연산을 사용하여 락 없이도 OS에서 데이터의 무결성을 유지합니다.

원래 `ConcurrentHashMap` + `AtomicBoolean`를 사용하려고 했으나, 이 방법은 `while`을 무한으로 돌며 `AtomicBoolean`을 체크해야 했기 때문에 비효율적이라고 판단했다.

### 5. `ConcurrentHashMap + ReentrantLock`
지금까지 알아본 동시성 제어 방법들과는 달리, Point 애플리케이션은 사용자 별로 다른 `Lock`을 가져야 합니다.

따라서 이번 과제에서는 해당 방법을 채택하였습니다. 

예를 들어 A 사용자과 B 사용자가 동시에 요청했을 때, B 사용자는 A 사용자의 락을 기다릴 필요가 없습니다. 이 경우를 해결하기 위해 저는 앞서 언급했듯이 `ConcurrentHashMap` + `ReentrantLock`을 사용하였습니다.
`ConcurrentHashMap`에 `userId`를 key로, `ReentrantLock` 객체를 value로 저장하여 사용자(userId)와 Lock 객체를 일대일로 매핑하였습니다.

#### 예시코드
```java
    private final ConcurrentHashMap<Long, ReentrantLock> userLock = new ConcurrentHashMap<>();

    public UserPoint chargeUserPoint(long userId, long amount, long chargeMillis) {
        ReentrantLock lock = userLock.computeIfAbsent(userId, key -> new ReentrantLock(true));

        lock.lock();

        try {
            UserPointValidator.validateChargeAmount(amount);

            UserPoint userPoint = userPointTable.selectById(userId);

            UserPointValidator.validateTotalPoints(userPoint, amount);

            pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, chargeMillis);

            return userPointTable.insertOrUpdate(userId, userPoint.point() + amount);
        } finally {
            lock.unlock();
        }
    }
```

위 예시처럼 서비스에 특정 사용자의 요청이 들어오면, `userId`를 통해 `ConcurrentHashMap`인 `userLock`에서 해당하는 `Lock`을 조회합니다.

> `userLock.computeIfAbsent`은 key에 해당하는 값이 있으면 그대로 반환, 없으면 `put` 하고 그 값을 반환한다.

동시에 들어온 동일한 사용자의 요청은 락을 가지지 못하고, 먼저 들어온 동일 사용자 요청에만 블로킹되어 락을 가질 때 까지 기다립니다.

락을 가진 사용자가 아닌 다른 사용자가 요청하는 경우 `userId`가 다르기 때문에 락이 걸리지 않은 또 다른 `Lock`으로 임계영역에 진입할 수 있게 됩니다.
