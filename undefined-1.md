---
description: '원문 최종 수정 :  2023년 6월 16일'
---

# 일시중단 함수 구성하기

> [페이지 편집](https://github.com/seyoungcho2/CoroutinesKoreanTranslation/edit/main/undefined-1.md)
>
> [원문](https://kotlinlang.org/docs/composing-suspending-functions.html)



이 섹션은 일시 중단 함수를 구성하기 위한 다양한 접근 방식을 다룬다.

## 기본적인 순차 처리

일종의 원격 서비스 호출이나 계산 같은 두 유용한 일시 중단 함수들이 서로 다른 위치에 정의되어 있다고 가정해보자. 이들은 유용한척 하지만 실제로는 이 예제의 목적을 위해 1초간 delay가 일어난다.

```kotlin
suspend fun doSomethingUsefulOne(): Int {
    delay(1000L) // 여기서 유용한 작업을 실행한다고 가정한다.
    return 13
}

suspend fun doSomethingUsefulTwo(): Int {
    delay(1000L) // 여기서도 유용한 작업을 실행한다고 가정한다.
    return 29
}
```

먼저 `doSomethingUsefulOne`을 호출하고 `doSomethingUsefulTwo`을 호출한 다음 결과의 합계를 계산해야 하는 경우 이들을 **순차적**으로 실행되도록 하기 위해서 어떤 것을 해야할까? 이런 작업은 첫 째 함수의 결과를 사용해 둘 째 함수를 호출해야 하는지 혹은 어떻게 호출 할지를 결정해야 할 때 사용된다.

일반적인 코드와 같이 Coroutine 코드는 기본적으로 순차적이기 때문에, 일반적인 순차 호출을 사용한다. 다음 예제는 두 일시 중단 함수들을 실행하는데 걸리는 총 시간을 측정하여 보여준다.

```kotlin
val time = measureTimeMillis {
    val one = doSomethingUsefulOne()
    val two = doSomethingUsefulTwo()
    println("The answer is ${one + two}")
}
println("Completed in $time ms")
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-compose-01.kt)에서 확인할 수 있습니다.

이 코드는 다음의 결과를 출력한다.

```
The answer is 42
Completed in 2017 ms
```



## async를 사용한 동시성

만약 `doSomethingUsefulOne`과 `doSomethingUsefulTwo`의 실행 사이에 종속성이 없고, 이 둘을 **동시**에 실행함으로써 응답을 더 빨리 얻고 싶다면 어떻게 해야할까? 여기에서 async가 사용될 수 있다.

개념적으로 `async`는 `launch`와 같다. `async`는 다른 스레드들과 동시에 동작하는 별도의 경량 Thread인 Coroutine을 시작한다. 다른 점은 `launch`는 결과값을 전달하지 않는 Job을 return 하지만, `async`는 나중에 결과값을 반환할 것을 약속하는 경량이고 스레드 블로킹을 하지 않는 Future인 `Deferred`를 반환한다는 점이다. Deferred에 대해 `.await()` 함수를 사용해 결과값을 얻을 수 있지만, `Deferred` 또한 `Job`이라 필요할 때 취소될 수 있다.

```
val time = measureTimeMillis {
    val one = async { doSomethingUsefulOne() }
    val two = async { doSomethingUsefulTwo() }
    println("The answer is ${one.await() + two.await()}")
}
println("Completed in $time ms")
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-compose-02.kt)에서 확인할 수 있습니다.

위 코드는 다음의 결과를 출력한다.

```
The answer is 42
Completed in 1017 ms
```

두 Coroutine들이 동시에 실행되기 때문에 두 배 정도 빠른 것을 볼 수 있다. Coroutines의 동시성은 언제나 명시적이다.



## async lazy하게 시작하기

선택적으로 첫 파라미터 값을 `CoroutineStart.LAZY` 로 설정함으로써 `async`를 lazy하게 만들 수 있다. 이 모드에서는 Coroutine의 결과값이 `await`에 의해 필요해지거나, `Job`의 start 함수가 실행될 때 시작된다. 다음 예를 실행해보자:

```kotlin
val time = measureTimeMillis {
    val one = async(start = CoroutineStart.LAZY) { doSomethingUsefulOne() }
    val two = async(start = CoroutineStart.LAZY) { doSomethingUsefulTwo() }
    // some computation
    one.start() // 첫 째를 start
    two.start() // 둘 째를 start
    println("The answer is ${one.await() + two.await()}")
}
println("Completed in $time ms")
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-compose-03.kt)에서 확인할 수 있습니다.



이는 다음의 결과를 생성한다.

```
The answer is 42
Completed in 1017 ms
```

여기에는 두 개의 Coroutine이 정의되어 있지만 이전 예제와 같이 실행되지 않으며, 프로그래머에게 `start`를 사용하여 언제 시작할 것인지에 대한 제어 권한이 주어진다. 먼저 one을 실행한 다음 two를 시작하며, 각 Coroutine들이 끝날 때까지 기다린다.

`await`은 Coroutine을 시작하고 완료를 기다리도록 하기 때문에, 개별 Coroutine들에서 `start`를 호출하지 않고 `println` 함수 내부에서 `await`을 호출하면 순차 처리가 된다. 이는 지연 처리를 위한 의도된 유즈케이스가 아니다. `async(start = CoroutineStart.LAZY)`는 값의 연산을 위한 계산이 일시 중단 함수를 포함할 때 표준 `lazy` 함수를 대체한다.



## 비동기 스타일 함수

구조적인 동시성에서 벗어나기 위해 `GlobalScope`를 참조하는 `async` Coroutine Builder을 사용하여 `doSomethingUsefulOne` 및 `doSomethingUsefulTwo`을 실행하는 **비동기** 스타일의 함수를 정의할 수 있다. 이러한 함수들의 이름은 "...Async"를 접미사를 가지도록 하여, 함수들이 비동기 계산을 시작하기만 하고 결괏값을 얻기 위해 Deferred 값을 사용해야 한다는 것을 강조한다.

> 📖  GlobalScope는 사소하지 않은 역효과를 일으킬 수 있는 섬세하게 다뤄야 하는 API이다. 그 중 하나는 아래에서 설명될 것이며, 명시적으로 `GlobalScope`를 `@OptIn(DelicateCoroutinesApi::class)`과 함께 사용되도록 해야 한다.

```kotlin
// somethingUsefulOneAsync 의 반환 타입은 Deferred<Int> 이다.
@OptIn(DelicateCoroutinesApi::class)
fun somethingUsefulOneAsync() = GlobalScope.async {
    doSomethingUsefulOne()
}

// somethingUsefulTwoAsync 의 반환 타입은 Deferred<Int>
@OptIn(DelicateCoroutinesApi::class)
fun somethingUsefulTwoAsync() = GlobalScope.async {
    doSomethingUsefulTwo()
}
```

이러한 `xxxAsync` 함수들은 일시중단 함수가 아니라는 점에 주목하자. 이 함수들은 어디에서든지 사용될 수 있다. 하지만, 코드를 호출 할 때 이 함수들을 사용하면 이들의 동작은 언제나 비동기적(이곳에서는 동시성을 의미) 실행을 포함한다. 다음의 예는 그들이 Coroutine 바깥에서 어떻게 사용되는지에 대해 보여준다:

```kotlin
// 이 예제에서 main 함수 뒤에 runBlocking이 없는 것에 주목하자.
fun main() {
    val time = measureTimeMillis {
        // 우리는 Coroutine 바깥에서 비동기 작업을 시작 할 수 있다. 
        val one = somethingUsefulOneAsync()
        val two = somethingUsefulTwoAsync()
        // 하지만 결과를 기다리는 것은 일시중단이나 블로킹 중 하나를 포함해야 한다.
        // 여기서 우리는 `runBlocking { ... }` 을 사용해 메인 스레드를 블록시키고 결과값이 오기를 기다린다.
        runBlocking {
            println("The answer is ${one.await() + two.await()}")
        }
    }
    println("Completed in $time ms")
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-compose-04.kt)에서 확인할 수 있습니다.

> 📖 async 함수를 사용하는 프로그래밍 스타일은 다른 언어들에서 많이 사용되기 때문에 이곳에서 설명을 위해 제공된다. Kotlin Coroutines에서 이러한 스타일을 사용하는 것은 아래에서 설명되는 이유로 **강하게 권장되지 않는다**.&#x20;

코드 상의 `val one = somethingUsefulOneAsync()` 행과 `one.await()` 표현식 사이에 약간의 논리 오류가 발생해, 프로그램이 예외를 발생시켜 프로그램에 의해 수행되던 작업이 중단되면 어떻게 되는지 생각해보자. 일반적으로 전역 오류 처리기는 이 예외를 잡아 개발자들을 위해 오류를 로깅하고 보고할 수 있지만, 그렇지 않으면 프로그램은 다른 작업을 계속할 수 있다. 하지만, 시작한 작업이 중단되었음에도 백그라운드에서 `somethingUsefulOneAsync`가 계속해서 실행중이다. 아래 세션에서 다루는 것처럼 이러한 문제는 구조적인 동시성을 적용한 경우에는 발생하지 않는다.



## 구조화된 동시성과 async

`async`를 사용한 동시 실행 예제를 사용하여 `doSomethingUsefulOne`과 `doSomethingUsefulTwo`를 동시에 실행하고 그들의 실행 결과를 합쳐서 반환하는 함수를 추출해보자. `async` Coroutine Builder가 CoroutineScope의 확장 함수로 정의되어 있기 때문에 이를 Scope내에 포함해야 하며, 이것이 `coroutineScope` 함수가 제공하는 기능이다.

```
suspend fun concurrentSum(): Int = coroutineScope {
    val one = async { doSomethingUsefulOne() }
    val two = async { doSomethingUsefulTwo() }
    one.await() + two.await()
}
```

이렇게 하면 `concurrentSum` 함수 내부에서 문제가 생겨서 예외가 발생 되었을 때, Scope 내부에서 실행된 모든 Coroutine들이 취소된다.

```kotlin
val time = measureTimeMillis {
    println("The answer is ${concurrentSum()}")
}
println("Completed in $time ms")
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-compose-05.kt)에서 확인할 수 있습니다.

위 `main` 함수의 출력에서 보여지듯이, 두 작업들은 동시 실행된다.

```
The answer is 42
Completed in 1017 ms
```

취소는 언제나 Coroutines의 계층 구조를 통해 전파된다.

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking<Unit> {
    try {
        failedConcurrentSum()
    } catch(e: ArithmeticException) {
        println("Computation failed with ArithmeticException")
    }
}

suspend fun failedConcurrentSum(): Int = coroutineScope {
    val one = async<Int> { 
        try {
            delay(Long.MAX_VALUE) // Emulates very long computation
            42
        } finally {
            println("First child was cancelled")
        }
    }
    val two = async<Int> { 
        println("Second child throws an exception")
        throw ArithmeticException()
    }
    one.await() + two.await()
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-compose-06.kt)에서 확인할 수 있습니다.

자식들 중 하나(위에서는 `two`라는 변수로 명명됨)가 취소로 인해 실패하면 첫 `async` 함수와 await을 수행중인 부모가 모두 취소되는 방식에 유의하자

```
Second child throws an exception
First child was cancelled
Computation failed with ArithmeticException
```
