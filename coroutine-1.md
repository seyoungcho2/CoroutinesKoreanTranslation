---
description: '원문 최종 수정 :  2023년 6월 16일'
---

# Coroutine 공유 상태와 동시성

> [원문](https://kotlinlang.org/docs/shared-mutable-state-and-concurrency.html)



Coroutine은 Dispatchers.Default와 같이 멀티 스레드를 관리하는 Dispatcher에 의해 병렬적으로 실행될 수 있다. 이는 병렬 실행 시 일어날 수 있는 일반적인 문제들을 모두 만들어낸다. 가장 주요한 문제는 변경 가능한 **공유 상태의 동기화**이다. Coroutine에서 이 문제에 대한 일부 해결 방식은 멀티 스레드 세계에서의 해결방식과 유사하지만, 다른 해결 방식들은 Coroutine에만 있다.



## Coroutine을 여러개 실행했을 때의 문제점

같은 동작을 수천번 하는 수백개의 Coroutine을 실행한다고 하자. 이후의 추가 비교를 위해 완료 시간을 측정한다 :

```kotlin
suspend fun massiveRun(action: suspend () -> Unit) {
    val n = 100  // number of coroutines to launch
    val k = 1000 // times an action is repeated by each coroutine
    val time = measureTimeMillis {
        coroutineScope { // scope for coroutines
            repeat(n) {
                launch {
                    repeat(k) { action() }
                }
            }
        }
    }
    println("Completed ${n * k} actions in $time ms")
}
```

복수의 스레드를 관리하는 `Dispatchers.Default` 를 사용해 공유 가변 변수를 증가시키는 간단한 동작으로 시작하자.

```kotlin
var counter = 0

fun main() = runBlocking {
    withContext(Dispatchers.Default) {
        massiveRun {
            counter++
        }
    }
    println("Counter = $counter")
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-sync-01.kt)에서 확인할 수 있습니다.

마지막에 무엇이 출력될까? 이것이 “Counter = 100000”을 출력할 가능성은 거의 없다. 100개의 Coroutine이 동기화 없이 여러 스레드에서 `counter`을 동시에 증가시키기 때문이다.

***

## Volatile은 동시성 문제를 해결하지 못한다

변수를 `volatile`로 만드는 것이 동시성 문제를 해결한다는 잘못된 인식이 있다. 시도해보자 :

```kotlin
@Volatile // in Kotlin `volatile` is an annotation 
var counter = 0

fun main() = runBlocking {
    withContext(Dispatchers.Default) {
        massiveRun {
            counter++
        }
    }
    println("Counter = $counter")
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-sync-02.kt)에서 확인할 수 있습니다.

이 코드는 느리게 동작하지만, 여전히 마지막에 “Counter = 100000”을 얻기 못한다. volatile 변수가 선형화(기술적인 용어로 “원자성”이라 한다)된 읽기와 쓰기를 제공하지만, 값을 증가시키는 것과 같은 더 큰 동작에 대해서는 원자성을 제공하지 않는다.



## Thread-safe한 데이터 구조

Threads과 Coroutines에 모두 작동하는 일반적인 해결 방법은 공유 상태에 수행되어야하는 모든 동작에 대한 필수적인 동기화를 제공하는 스레드 안전한(동기화된, 선형성, 원자성 이라고도 부름) 데이터 구조를 사용하는 것이다. 간단한 카운터에 대해서는 `incrementAndGet` 이라 불리는 원자적인 동작을 제공하는 `AtomicInteger` 클래스를 사용할 수 있다.

```kotlin
val counter = AtomicInteger()

fun main() = runBlocking {
    withContext(Dispatchers.Default) {
        massiveRun {
            counter.incrementAndGet()
        }
    }
    println("Counter = $counter")
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-sync-03.kt)에서 확인할 수 있습니다.

이는 이 특정한 문제에 대한 가장 빠른 해결 방법이다. 이는 일반적인 카운터, 컬렉션, 큐와 다른 표준적인 데이터 구조와 그들의 동작에서 작동한다. 하지만, 이는 이미 만들어진 스레드 안전한 구현이 없는 복잡한 상태나 복잡한 작업으로 쉽게 확장될 수 없다.

***

## 세밀하게 Thread 제한하기

스레드 제한은 특정한 공유된 상태로의 접근이 단일 스레드로 제한된 공유 상태 문제에 대한 접근 방식이다. 이는 일반적으로 모든 UI 상태가 단일 이벤트 디스패처/어플리케이션 스레드로 제한된 UI가 있는 어플리케이션에 사용된다. 단일 스레드를 관리하는 Context를 사용해 Coroutine을 적용하는 것은 쉽다.

```kotlin
val counterContext = newSingleThreadContext("CounterContext")
var counter = 0

fun main() = runBlocking {
    withContext(Dispatchers.Default) {
        massiveRun {
            // confine each increment to a single-threaded context
            withContext(counterContext) {
                counter++
            }
        }
    }
    println("Counter = $counter")
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-sync-04.kt)에서 확인할 수 있습니다.

이 코드는 세밀하게 스레드를 제한하기 때문에 아주 느리게 동작한다. 숫자를 증가시키는 각 동작은 멀티 스레드 디스패처인 `Dispatchers.Default` Context에서 `withContext(counterContext)` 블록을 사용해 단일 스레드 Context로 전환한다.

## 굵게 Thread 제한하기

실제로 스레드 제한은 큰 코드 덩어리에서 수행된다. 예를 들어, 큰 부분의 상태를 갱신하는 비즈니스 로직은 단일 스레드로 제한된다. 다음의 예제에서는 각 Coroutine을 단일 스레드 Context에서 실행되도록 함으로써 그렇게 한다.

```kotlin
val counterContext = newSingleThreadContext("CounterContext")
var counter = 0

fun main() = runBlocking {
    // confine everything to a single-threaded context
    withContext(counterContext) {
        massiveRun {
            counter++
        }
    }
    println("Counter = $counter")
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-sync-05.kt)에서 확인할 수 있습니다.

이제 더 빠르게 실행되고 옳은 결과를 생성한다.

***

## 상호 배제

문제에 대한 상호 배제 솔루션은 모든 모든 공유 상태에 대한 변경을 절대로 동시에 실행되지 않는 critical section으로 만들어 보호하는 것이다. 블로킹을 수행하기 위해서는 일반적으로 `synchronized`나 `ReentrantLock`을 사용한다. Coroutine의 대체제는 Mutex라 불린다. 이는 critical section을 구분하기 위한 `lock`과 `unlock` 함수를 가진다. 중요한 차이점은 `Mutext.lock()`이 일시중단 함수라는 것이다. 이는 Thread를 블록하지 않는다.

`mutex.lock(); try { ... } finally { mutex.unlock() }` 패턴을 나타내는 `withLock` 확장함수 또한 있다.

```kotlin
val mutex = Mutex()
var counter = 0

fun main() = runBlocking {
    withContext(Dispatchers.Default) {
        massiveRun {
            // protect each increment with lock
            mutex.withLock {
                counter++
            }
        }
    }
    println("Counter = $counter")
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-sync-06.kt)에서 확인할 수 있습니다.

이 예에서 Lock을 거는 것은 세밀해서 비용이 든다. 하지만, 이는 주기적으로 공유 상태를 수정해야 하지만 상태를 제한시킬 수 있는 자연 스레드가 없는 일부 상황에서 좋은 선택이 된다. 하지만,&#x20;

\


## Actors

actor는 Coroutine의 조합으로 만들어진 엔티티이다. actor의 상태는 제한되고 Coroutine 속에 캡슐화되며, Channel을 통해 다른 Coroutine과 통신한다. 간단한 actor는 함수로 작성될 수 있지만, 복잡한 상태를 가진 actor는 class로 작성되는 것이 적합하다.

간단한 actor Coroutine 빌더를 통해서 actor의 수신 Channel을  Scope에 편리하게 결합해서 메세지를 수신할수 있다. 또한 발신 Channel을 결과 Job 객체에 결합함으로써 actor에 대한 단일 참조를 통해 제어할 수 있다.

actor을 사용하는 첫 단계는 actor가 처리할 메세지의 class를 정의하는 것이다. Kotlin의 sealed class는 이 목적으로 아주 적합하다. 우리는 `sealed class`로 `CounterMsg`를 정의하고, `IncCounter` 메세지를  counter을 증가시키는데 사용하고 `GetCounter` 메세지를 값을 가져오는데 사용한다. 후자(GetCounter)는 응답을 보내는 것이 필요하다. 미래에 알려질(통신될) 단일 값을 나타내는 통신 원시값(primitive)인 CompletableDeffered이 이를 위해 사용된다.

```kotlin
// Message types for counterActor
sealed class CounterMsg
object IncCounter : CounterMsg() // one-way message to increment counter
class GetCounter(val response: CompletableDeferred<Int>) : CounterMsg() // a request with reply
```

그 다음 actor Coroutine 빌더를 사용해 actor을 실행시키는 함수를 정의한다.

```kotlin
// This function launches a new counter actor
fun CoroutineScope.counterActor() = actor<CounterMsg> {
    var counter = 0 // actor state
    for (msg in channel) { // iterate over incoming messages
        when (msg) {
            is IncCounter -> counter++
            is GetCounter -> msg.response.complete(counter)
        }
    }
}
```

전체 코드는 다음과 같다 :&#x20;

```kotlin
fun main() = runBlocking<Unit> {
    val counter = counterActor() // create the actor
    withContext(Dispatchers.Default) {
        massiveRun {
            counter.send(IncCounter)
        }
    }
    // send a message to get a counter value from an actor
    val response = CompletableDeferred<Int>()
    counter.send(GetCounter(response))
    println("Counter = ${response.await()}")
    counter.close() // shutdown the actor
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-sync-07.kt)에서 확인할 수 있습니다.

어떤 Context에서 actor가 실행되는지는 상관 없다(정확성을 위해). actor는 Coroutine이고 Coroutine은 순차적으로 실행된다. 따라서 특정한 Coroutine에 대한 상태의 제한은 공유된 가변 상태에 대한 해결책으로 동작한다. 실제로 actor는 그들의 비공개 상태를 수정할 수 있지만, 메세지를 통해서만 서로에게 영향을 줄 수 있다.(lock이 필요하지 않도록)

Actor는 로드 시 Lock보다 효율적이다. 항상 해야 할 작업이 있으며, 다른 Context로의 전환이 전혀 필요없기 때문이다.

> 📖  actor Coroutine 빌더는 produce Coroutine 빌더 두개라는 것을 명심하자. actor는 메세지를 수신하는 Channel과 연관되어 있고, producer는 메세지를 보내는 Channel과 연관되어 있다.
