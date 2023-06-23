---
description: '원문 최종 수정 :  2023년 6월 16일'
---

# 취소와 타임아웃

> [페이지 편집](https://github.com/seyoungcho2/CoroutinesKoreanTranslation/edit/main/undefined.md)
>
> [원문](https://kotlinlang.org/docs/cancellation-and-timeouts.html)



이 섹션은 코루틴 Cancellation과 Timeout에 대해 다룹니다.

## Coroutine 실행 취소하기

긴 시간동안 실행되는 어플리케이션에서 백그라운드에서 실행되는 Coroutine에 대한 세밀한 제어가 필요할 수 있다. 예를 들어, 유저가 Coroutine을 실행시킨 페이지를 닫아 결과가 더 이상 필요하지 않아 작업이 취소되어도 되는 경우이다. `launch` 함수는 실행중인 코루틴을 취소하는 데 사용할 수 있는 `Job` 객체를 반환한다.

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    val job = launch {
        repeat(1000) { i ->
            println("job: I'm sleeping $i ...")
            delay(500L)
        }
    }
    delay(1300L) // 약간의 시간 동안 delay 한다.
    println("main: I'm tired of waiting!")
    job.cancel() // Job을 cancel한다.
    job.join() // Job의 실행이 완료될 때까지 기다린다. 
    println("main: Now I can quit.")    
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-cancel-01.kt)에서 확인할 수 있습니다.

이는 다음 결과를 출력한다.

```
job: I'm sleeping 0 ...
job: I'm sleeping 1 ...
job: I'm sleeping 2 ...
main: I'm tired of waiting!
main: Now I can quit.
```

main 함수가 `job.cancel`을 호출하면, Job이 취소되었기 때문에 다른 코루틴의 출력을 확인할 수 없다. Job의 확장 함수로 `cancel`과 `join` 호출을 결합한 `cancelAndJoin`도 있다.



## Coroutines 취소는 협력적이다 <a href="#cancellation-is-cooperative" id="cancellation-is-cooperative"></a>

Coroutine의 취소는 **협력적**이다. Coroutine 코드는 취소 가능하도록 협력해야 한다. `kotlinx.coroutines` 패키지의 모든 일시 중단 함수들은 **취소 가능**하다. 그들은 Coroutine이 취소되었는지 확인하고 취소되었을 경우 `CancellationException`을 발생시킨다. 만약 코루틴이 계산 작업 중이고 취소를 확인하지 않는다면, 다음의 예시처럼 취소될 수 없다.

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    val startTime = System.currentTimeMillis()
    val job = launch(Dispatchers.Default) {
        var nextPrintTime = startTime
        var i = 0
        while (i < 5) { // 계산 루프, CPU를 낭비한다
            // 1초에 두 번 메세지를 출력한다.
            if (System.currentTimeMillis() >= nextPrintTime) {
                println("job: I'm sleeping ${i++} ...")
                nextPrintTime += 500L
            }
        }
    }
    delay(1300L) // 약간의 시간 동안 delay 한다
    println("main: I'm tired of waiting!")
    job.cancelAndJoin() // Job을 취소하고 실행이 완료될 때까지 기다린다.
    println("main: Now I can quit.")    
}
```

> 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-cancel-02.kt)에서 확인할 수 있습니다.

코드를 실행하여 취소가 실행된 이후에도 다섯번의 반복 후에 Job이 완료될 때까지 "I'm sleeping"이 계속해서 프린트 되는 것을 보자.&#x20;

위와 같은 문제가 `CancellationException`을 catch 하고 다시 throw 하지 않는 경우에도 생긴다.

```
import kotlinx.coroutines.*

fun main() = runBlocking {
    val job = launch(Dispatchers.Default) {
        repeat(5) { i ->
            try {
                // 1초에 두 번 메세지를 출력한다.
                println("job: I'm sleeping $i ...")
                delay(500)
            } catch (e: Exception) {
                // 예외를 로깅한다.
                println(e)
            }
        }
    }
    delay(1300L) // 약간의 시간 동안 delay 한다.
    println("main: I'm tired of waiting!")
    job.cancelAndJoin() // Job을 취소하고 완료될 때까지 기다린다.
    println("main: Now I can quit.")    
}
```

> 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-cancel-03.kt)에서 확인할 수 있습니다.

`Exception`을 catch 하는 것은 안티 패턴이지만, 이 문제는 `CancellationException`을 다시 throw 하지 않는 `runCatching` 함수를 사용하는 경우와 같이 미묘한 경우에도 일어날 수 있다.



## Coroutine의 Computation 코드를 취소 가능하게 만들기

computation code를 취소 가능하게 만드는 두가지 접근 방식이 있다. 첫 째는 주기적으로 일시 중단 함수를 실행시켜서 취소되었는지 확인하도록 하는 것이다. 이 목적을 위해 좋은 방식인 `yield` 함수가 있다. 다른 하나는 명시적으로 취소 상태를 확인하도록 하는 것이다. 후자의 접근 방식을 시도해보도록 하자.

이전 예시의 `while (i < 5)`를 `while (isActive)`로 변경한 후 다시 실행 시켜보도록 하자.

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    val startTime = System.currentTimeMillis()
    val job = launch(Dispatchers.Default) {
        var nextPrintTime = startTime
        var i = 0
        while (isActive) { // 취소 가능한 computation loop
            // 1초에 두 번 메세지를 출력한다.
            if (System.currentTimeMillis() >= nextPrintTime) {
                println("job: I'm sleeping ${i++} ...")
                nextPrintTime += 500L
            }
        }
    }
    delay(1300L) // 약간의 시간 동안 delay 한다
    println("main: I'm tired of waiting!")
    job.cancelAndJoin() // Job을 취소하고 실행이 완료될 때까지 기다린다.
    println("main: Now I can quit.")    
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-cancel-04.kt)에서 확인할 수 있습니다.

이제 이 loop가 취소되는 것을 볼 수 있다. `isActive`는 `CoroutineScope` 객체를 통해 Coroutine 내부에서 사용할 수 있는 확장 프로퍼티이다.



## finally 사용해 리소스 닫기

취소 가능한 일시 중단 함수는 취소 시에 `CancellationException`을 throw하며, 이는 일반적인 방식으로 처리할 수 있다. 예를 들어 `try { ... }` 와 `finally { ... }` 구문이나, Kotlin의 `use` 함수는 Coroutine이 취소될 때 정상적으로 종료 작업을 수행한다.

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    val job = launch {
        try {
            repeat(1000) { i ->
                println("job: I'm sleeping $i ...")
                delay(500L)
            }
        } finally {
            println("job: I'm running finally")
        }
    }
    delay(1300L) // 잠시 기다리기
    println("main: I'm tired of waiting!")
    job.cancelAndJoin() // Job을 취소하고 완료될 때까지 기다리기
    println("main: Now I can quit.")    
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-cancel-05.kt)에서 확인할 수 있습니다.

join과 cancelAndJoin 모두 종료 작업이 완료될 때까지 기다린다. 따라서 위의 예시는 다음의 결과를 생성한다.

```
job: I'm sleeping 0 ...
job: I'm sleeping 1 ...
job: I'm sleeping 2 ...
main: I'm tired of waiting!
job: I'm running finally
main: Now I can quit.
```



## 실행 취소가 불가능한 블록 실행하기

이전 예제에서 `finally` 블록 내부에서 일시 중단 함수를 사용하려고 하면, 이 코드를 실행 중인 코루틴이 취소되기 때문에 `CancellationException`이 발생한다. 잘 작동하는 리소스를 닫는 작업들(파일 닫기, Job 취소하기 또는 모든 종류의 통신 채널을 닫기)은 보통 Blocking이 발생하지 않는 작업들이며, 보통 어떠한 일시 중단 함수들도 포함하고 있지 않기 때문에 문제가 되지 않는다. 하지만, 드물게 취소된 Coroutine에 대해 일시 중단을 해야 하는 경우 다음과 같이 `withContext`함수에 `NonCancellable` Context를 전달하여 사용하는 `withContext(NonCancellable) { ... }` 를 사용할 수 있다.

```kotlin
val job = launch {
    try {
        repeat(1000) { i ->
            println("job: I'm sleeping $i ...")
            delay(500L)
        }
    } finally {
        withContext(NonCancellable) {
            println("job: I'm running finally")
            delay(1000L)
            println("job: And I've just delayed for 1 sec because I'm non-cancellable")
        }
    }
}
delay(1300L) // delay a bit
println("main: I'm tired of waiting!")
job.cancelAndJoin() // cancels the job and waits for its completion
println("main: Now I can quit.")
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-cancel-06.kt)에서 확인할 수 있습니다.



## Timeout

Coroutine의 실행을 취소하는 가장 명백하고 실용적인 이유는 실행 시간이 Timeout으로 설정한 시간을 넘어섰기 때문이다. 해당 `Job`에 대한 참조를 만들고 새로운 별도의 Coroutine을 실행해서 일정 시간 이후에 참조된 Job을 취소하는 과정을 거칠 수 있지만, 이러한 동작을 수행하는 `withTimeout`가 이미 만들어져 있다. 다음 예를 보자:

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    withTimeout(1300L) {
        repeat(1000) { i ->
            println("I'm sleeping $i ...")
            delay(500L)
        }
    }
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-cancel-07.kt)에서 확인할 수 있습니다.

위 코드는 다음을 출력한다.

```
I'm sleeping 0 ...
I'm sleeping 1 ...
I'm sleeping 2 ...
Exception in thread "main" kotlinx.coroutines.TimeoutCancellationException: Timed out waiting for 1300 ms
```

`withTimeout`에 의해 throw되는 `TimeoutCancellationException`은 `CancellationException`의 서브클래스이다. 우리는 이전에 이러한 스택 추적값이 인쇄된 것을 보지 못했다. 이는 CancellationException이 Coroutine이 완료되기 위한 일반적인 원인으로 간주되기 때문이다. 하지만, 위 예에서는 `withTimeout`을 `main` 함수 내부에서 사용했다.

취소는 단순한 Exception이기 때문에, 모든 리소스들은 일반적인 방식으로 닫힌다. 만약 시간 초과를 일으키는 동작들이나, `withTimeout`과 비슷하지만 시간 초과가 일어난다면 `null`이 return되는 `withTimeoutOrNull` 함수를 사용해야 한다면, `try {...} catch (e: TimeoutCancellationException) {...}` 블록으로 코드를 감싸는 방식을 사용할 수 있다:

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    val result = withTimeoutOrNull(1300L) {
        repeat(1000) { i ->
            println("I'm sleeping $i ...")
            delay(500L)
        }
        "Done" // will get cancelled before it produces this result
    }
    println("Result is $result")
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-cancel-08.kt)에서 확인할 수 있습니다.

코드를 실행할 때 더이상 예외가 발생하지 않는 것을 확인할 수 있다.

```
I'm sleeping 0 ...
I'm sleeping 1 ...
I'm sleeping 2 ...
Result is null
```



## 비동기 Timeout과 리소스

`withTimeout`으로 발생되는 Timeout 이벤트는 현재 실행중인 블록의 코드와 비동기적으로 일어나며 언제든지 일어날 수 있다, 심지어 Timeout 블록에서 return이 일어나기 직전에서도 일어날 수 있다.\*1 만약 블록 외부에서 닫거나 해제되어야 하는 일부 리소스를 블록 내부에서 열거나 획득해야 하는 경우 이 점을 염두에 두어야 한다.

닫을 수 있는 리소스를 `Resource` 클래스를 사용하여 모방해보자. 이 클래스는 인스턴스화 될 때 `acquired` 의 숫자를 증가시키고 `close` 함수를 통해 이 숫자를 감소시킴으로써, 얼마나 많은 수의 인스턴스가 생성되었는지를 추적한다. 이제 withTimeout 마지막에 Resource를 생성하는 많은 Coroutine을 생성하자. 약간의 지연(delay)를 추가함으로써 `withTimeout` 블록이 이미 끝났을 때 실행되도록 만들어 리소스 누수가 일어나도록 한다.

```kotlin
import kotlinx.coroutines.*

var acquired = 0

class Resource {
    init { acquired++ } // 리소스를 획득한다.
    fun close() { acquired-- } // 리소스를 해제한다.
}

fun main() {
    runBlocking {
        repeat(10_000) { // 10만개의 Coroutine을 실행한다.
            launch { 
                val resource = withTimeout(60) { // Timeout 기준시간을 60ms로 설정한다.
                    delay(50) // 50ms 동안 delay한다.
                    Resource() // 리소스를 획득하고 withTimeout 블록의 return 값으로 리소스를 반환한다.
                }
                resource.close() // 리소스를 해제한다.
            }
        }
    }
    // runBlocking 바깥은 모든 Coroutine 들이 완료된 다음 실행횐다.
    println(acquired) // 획득되고 해제되지 않은 리소스들의 개수를 출력한다.
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-cancel-09.kt)에서 확인할 수 있습니다.

위 코드를 실행하면, 컴퓨터의 타이밍에 따라 다를 수 있지만 항상 0을 프린트 하지는 않는 것을 볼 수 있다. 0이 아닌 값을 확인하기 위해서는 이 예제에서 시간 초과 시간을 조정해야 할 수 있다.

> 📌 이 예제에서 1만개의 Coroutine으로 acquired counter을 증가 시키고 감소 시키는 것은, `runblocking` 에 의해 같은 Thread에서 실행되기 때문에 완전히 Safe하다. 이와 관련된 추가적인 설명은 Coroutine Context에 대해 다루는 Chapter에서 설명할 것이다.

이러한 문제를 해결하기 위해서는 리소스를 withTimeout 블록에서 반환하는 대신 리소스에 대한 참조를 변수에 저장하는 방법을 사용할 수 있다.

```
import kotlinx.coroutines.*

var acquired = 0

class Resource {
    init { acquired++ } // Acquire the resource
    fun close() { acquired-- } // Release the resource
}

fun main() {
    runBlocking {
        repeat(10_000) { // 1만개의 Coroutine을 실행한다.
            launch { 
                var resource: Resource? = null // 아직 획득되지 않았다.
                try {
                    withTimeout(60) { // Timeout 기준시간을 60 ms로 설정한다.
                        delay(50) // 50ms 동안 delay 한다.
                        resource = Resource() // 리소스를 획득하였으면 저장한다.      
                    }
                    // 여기에서 리소스에 대한 추가적인 작업을 할 수 있다.
                } finally {  
                    resource?.close() // 리소스를 얻었으면 해제한다.
                }
            }
        }
    }
    // Outside of runBlocking all coroutines have completed
    println(acquired) // Print the number of resources still acquired
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-cancel-10.kt)에서 확인할 수 있습니다.

이 예시는 언제나 0을 출력한다. 리소스가 누수되지 않는다.
