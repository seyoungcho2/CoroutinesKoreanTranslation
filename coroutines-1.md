---
description: '원문 최종 수정 :  2023년 6월 16일'
---

# Coroutines 기초

이 섹션에서는 Coroutine의 기본 개념에 대해 다룹니다.



## 당신의 첫 번째 coroutine

Coroutine은 일시정지 연산을 위한 인스턴스이다. 이것은 코드의 나머지 부분들과 동시에 실행되는 코드 블록을 가진다는 점에서 스레드와 개념적으로 비슷하다. 하지만, 코루틴은 특정한 스레드에 종속되어 실행되지 않으며, 하나의 스레드에서 일시정지(suspend) 되었다가 다른 스레드에서 재개(resume)될 수 있다.

Coroutines는 경량 스레드로 생각될 수 있지만, 실제 사용을 스레드와 다르게 만드는 여러 다른 점들이 있다.&#x20;

다음 코드를 실행하여 동작하는 첫 Coroutine을 만들어 보자.

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking { // this: CoroutineScope
    launch { // 새로운 coroutine을 실행하고 계속한다.
        delay(1000L) // 블로킹 하지 않고 1초를 지연시킨다. (기본 시간 단위 : ms)
        println("World!") // 지연 이후에 프린트한다.
    }
    println("Hello") // 이전 coroutine이 지연된 동안 main coroutine이 실행된다.
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-basic-01.kt)에서 볼 수 있다.

위 코드를 실행하면 다음의 결과를 볼 수 있다.

```
Hello
World!
```

이 코드가 무엇을 하는지 분석해 보자.

[launch](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/launch.html)는 Coroutine Builder이다. launch는 독립적으로 동작하는 새로운 Coroutine을 나머지 코드와 동시에 실행되도록 한다. 이로 인해 `Hello`가 처음에 출력된다.

[delay](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/delay.html)는 특별한 일시중단 함수이다. delay는 Coroutine을 특정한 시간 동안 일시중단한다. Coroutine을 일시중단 하는 것은 Coroutine이 실행 중인 스레드를 블록 하지 않으며, 다른 Coroutine 이 해당 스레드에서 자신들의 코드를 실행할 수 있도록 한다.

[runBlocking](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/run-blocking.html) 또한 Coroutine Builder이다. runBlocking은 Coroutine 세계에 속하지 않은 일반적인 `fun main()` 과 `runBlocking { ... }` 중괄호 내부의 Coroutine 코드를 연결시켜 주는 역할을 한다. 이는 IDE 내에서 `runBlocking` 시작 중괄호 '{' 바로 다음에 오는 `this: CoroutineScope` 힌트로 강조표시된다.

`launch`는 [Coroutine Scope](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-coroutine-scope/) 내에서만 실행될 수 있기 때문에, 만약 이 코드에서 `runBlocking`을 제거하거나 작성하는 것을 잊는다면, [launch](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/launch.html) 호출 시에 애러가 발생할 것이다.&#x20;

```
Unresolved reference: launch
```

`runBlocking`이라는 이름은 `runBlocking { ... }` 내부의 모든 Coroutine의 실행이 완료될 때까지 이를 실행하는 스레드(위의 상황에서는 main thread)가 호출 시간 동안 블록된다는 뜻이다. 스레드는 비싼 자원이고 스레드를 블록 하는 것은 비효율적이고 바람직하지 않기 때문에, 어플리케이션의 최상위 수준에서 `runBlocking`이 이런 식으로 사용되는 것을 종종 볼 수 있지만 실제 코드에서는 거의 보지 못한다.



### 구조화된 동시성

Coroutine은 새로운 Coroutine의 수명을 제한하는 특정한 CoroutineScope 내에서만 실행될 수 있다는 원칙인 **구조화된 동시성**의 원칙을 따른다. 위의 예시에서는 runBlocking이 해당 Scope을 만들며, 그것이 이전 예시가 `World!` 가 프린트 될 때까지 1초를 기다린 후 종료되는 이유이다.

실제 어플리케이션에서는 당신은 많은 수의 Coroutine들을 실행할 것이다. 구조화된 동시성은 Coroutine들이 손실되거나 누수를 일으키지 않도록 한다. 바깥 Scope은 자식 Coroutine들이 모두 완료될 때까지 완료되지 못한다. 또한 구조화된 동시성은 코드 상의 애러들이 적절히 보고되고 손실되지 않도록 보장한다.



## 함수 추출해 리펙토링하기 <a href="#extract-function-refactoring" id="extract-function-refactoring"></a>

`launch { ... }` 내부의 코드를 분리된 함수로 추출해보자. 이 코드에서 "함수 추출" 을 수행한다면, `suspend` modifier을 가진 새로운 함수를 가지게 된다. 이것은 당신의 첫 일시중단 함수(suspending function)가 된다. 일시중단 함수는 다른 보통의 함수들과 마찬가지로 Coroutines 내부에서 사용될 수 있지만, 추가 기능은 다른 일시 중단 함수(이 예시에서 `delay`와 같은)를 사용하여 코루틴 실행을 일시 중단 할 수 있다는 것이다.

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking { // this: CoroutineScope
    launch { doWorld() }
    println("Hello")
}

// 이것은 당신의 첫 일시 중단 함수이다.
suspend fun doWorld() {
    delay(1000L)
    println("World!")
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-basic-02.kt)에서 확인할 수 있습니다.



## Scope Builder

다른 builder들에서 제공하는 Coroutine Scope 외에도, [coroutineScope](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/coroutine-scope.html) builder를 사용하여 고유한 scope을 선언할 수 있다.&#x20;

`coroutineScope`는 자식 Coroutine 들의 실행이 모두 완료될 때까지 종료되지 않는 Coroutine Scope을 생성한다.

`runBlocking`과 `coroutineScope` builder는 그들의 body\*1 와 children\*2들이 모두 완료될 때까지 대기한다는 점에서 비슷해보일지 모른다. 이 둘의 주요한 차이는 `runBlocking`은 대기를 위해 현재 Thread를 block시키는 반면, `coroutineScope`는 다른 작업이 수행될 수 있도록 작업 중이던 Thread의 자원 사용을 해제(release)한다는 점이다. 이러한 차이점 때문에 `runBlocking`은 일반 함수이고, `coroutineScope`은 일시 중단 함수이다.

`coroutineScope`은 어떠한 일시 중단 함수 내부에서나 사용될 수 있다. 예를 들어, `Hello`와 `World`를 동시 출력을 `suspend fun doWorld()` 함수로 이동할 수 있다.&#x20;

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    doWorld()
}

suspend fun doWorld() = coroutineScope {  // this: CoroutineScope
    launch {
        delay(1000L)
        println("World!")
    }
    println("Hello")
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-basic-01.kt)에서 확인할 수 있습니다.

이 코드 또한 다음과 같이 출력된다.

```
Hello
World!
```



## Scope Builder와 동시성

`coroutineScope` builder은 일시 중단 함수 내부에서 복수의 동시 작업을 수행하기 위해 사용될 수 있다. `doWorld` 일시 중단 함수 내부에서 두 개의 동시에 수행되는 코루틴을 실행해보도록 하자.

```kotlin
import kotlinx.coroutines.*

// doWorld와 "Done"을 순서대로 실행합니다.
fun main() = runBlocking {
    doWorld()
    println("Done")
}

// 두 섹션들을 모두 동시적으로 실행합니다
suspend fun doWorld() = coroutineScope { // this: CoroutineScope
    launch {
        delay(2000L)
        println("World 2")
    }
    launch {
        delay(1000L)
        println("World 1")
    }
    println("Hello")
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-basic-04.kt)에서 확인할 수 있습니다.

`launch { ... }` 블록 내부의 두 코드들은 동시적으로 실행되어, 시작 후 1초가 지난 다음 `World 1`이 먼저 출력되고, 시작 후 2초가 지난 후에 `World 2`가 출력된다. `doWorld`의 `coroutineScope`은 이 둘 모두가 완료된 후에 종료되며, 그 후에야 `doWorld`가 반환되고 `Done` 문자열이 출력된다.

```
Hello
World 1
World 2
Done
```



## Job 명시적으로 사용하기

`launch` Coroutine builder는 실행된 Coroutine을 처리하고 완료를 명시적으로 기다리도록 하는데 사용할 수 있는 `Job` 객체를 반환한다. 예를 들어, 자식 코루틴이 완료될 때까지 기다린 다음 "Done" 문자열을 출력하도록 할 수 있다.

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    val job = launch { // 새로운 코루틴을 실행하고 그 Job에 대한 참조를 유지한다 
        delay(1000L)
        println("World!")
    }
    println("Hello")
    job.join() // 자식 코루틴이 완료될 때까지 기다린다.
    println("Done")     
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-basic-05.kt)에서 확인할 수 있습니다.

이 코드는 다음 결과를 만든다.

```
Hello
World!
Done
```



## Coroutines는 경량(light-weight) 이다

Coroutines는 JVM의 Thread들보다 덜 리소스 집약적이다. Thread를 사용할 때 JVM의 가용 메모리를 소진시키지는 코드는 Coroutine을 사용하여 리소스의 제한치에 도달하지 않도록 표현될 수 있다. 예를 들어, 다음의 코드는 각각이 5초간 기다린 후 마침표('.)를 출력하는 50,000개의 별개의 Coroutine을 실행하면서도 매우 적은 메모리만을 사용한다:

```
import kotlinx.coroutines.*

fun main() = runBlocking {
    repeat(50_000) { // 많은 수의 코루틴을 실행한다.
        launch {
            delay(5000L)
            print(".")
        }
    }
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-basic-06.kt)에서 확인할 수 있습니다.

만약 위와 같은 프로그램을 Thread들을 이용하여 작성한다면(runBlocking을 제거하고 launch를 thread로 대체하고, delay를 Thread.sleep으로 대체), 많은 메모리를 사용하게 된다. 실행되는 OS, JDK 버전, 설정에 따라 동시에 실행 중인 스레드가 너무 많지 않도록 out-of-memory error을 발생시키거나 스레드를 느리게 시작할 것이다.









