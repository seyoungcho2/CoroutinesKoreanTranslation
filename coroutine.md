---
description: '원문 최종 수정 :  2023년 6월 16일'
---

# Coroutine 예외 처리

> [페이지 편집](coroutine.md)
>
> [원문](https://kotlinlang.org/docs/exception-handling.html)



이 섹션에서는 예외 처리와 예외 발생 시 취소에 대해 다룬다. 우리는 취소된 Coroutine이 일시중단 지점에서 CancellationException을 발생시키고 이것이 Coroutine의 동작원리에 의해서 무시되는 것을 알고 있다. 이 장에서는 취소 도중 예외가 발생되거나 같은 Coroutine에서 복수의 자식 Coroutine이 예외를 발생시킬 경우 어떤 일이 일어나는지 살펴볼 것이다.



## Exception 전파

Coroutine 빌더는 자동으로 예외를 전파([launch](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/launch.html)와 [actor](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/actor.html))하거나 사용자에게 예외를 노출([async](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/async.html)와 [produce](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/produce.html))한다. 이 빌더들이 다른 Coroutine의 자식이 아닌 root Coroutine을 만드는데 사용될 때, 전자(`launch`와 `actor`)는 Java의 `Thread.uncaughtExceptionHandler`와 비슷하게 앞의 빌더들은 예외를 잡히지 않은 예외로 다룬다. 반면 후자(`async`와 `produce`)는 `await`이나 `receive`를 통해 사용자가 마지막 예외를 소비하는지에 의존한다.(`produce`와 `receive`는 [Channels](channels.md) 섹션에서 다룬다)



이는 `GlobalScope`를 사용해 root Coroutine을 만드는 간단한 예제로 설명될 수 있다.

> 📖  GlobalScope는 사소하지 않은 역효과를 만들 수 있는 섬세하게 다뤄져야 하는 API이다. 모든 어플리케이션에 대해 root Coroutine을 만드는 것은 GlobalScope의 드문 적합한 사용 방법 중 하나이다. 따라서 @OptIn(DelicateCoroutinesApi::class)을 사용해 GlobalScope를 명시적으로 opt-in 시켜야 한다.

```kotlin
@OptIn(DelicateCoroutinesApi::class)
fun main() = runBlocking {
    val job = GlobalScope.launch { // root coroutine with launch
        println("Throwing exception from launch")
        throw IndexOutOfBoundsException() // Will be printed to the console by Thread.defaultUncaughtExceptionHandler
    }
    job.join()
    println("Joined failed job")
    val deferred = GlobalScope.async { // root coroutine with async
        println("Throwing exception from async")
        throw ArithmeticException() // Nothing is printed, relying on user to call await
    }
    try {
        deferred.await()
        println("Unreached")
    } catch (e: ArithmeticException) {
        println("Caught ArithmeticException")
    }
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-exceptions-01.kt)에서 확인할 수 있습니다.

이 코드의 출력은 다음과 같다(디버그 옵션을 켜놓았음) :

```
Throwing exception from launch
Exception in thread "DefaultDispatcher-worker-2 @coroutine#2" java.lang.IndexOutOfBoundsException
Joined failed job
Throwing exception from async
Caught ArithmeticException
```

***

## CoroutineExceptionHandler 사용해 전파된 예외 처리하기

잡히지 않은 예외를 콘솔에 출력하도록 기본 동작을 커스터마이징 할 수 있다. root Coroutine 상의 Context의 요소인 `CoroutineExceptionHandler`는, root Coroutine과 모든 자식 Coroutine들에 대해 커스텀한 예외 처리가 필요한 경우, 일반 catch 블록으로 사용될 수 있다. 이는 [Thread.uncaughtExceptionHandler](https://docs.oracle.com/javase/8/docs/api/java/lang/Thread.html#setUncaughtExceptionHandler\(java.lang.Thread.UncaughtExceptionHandler\))와 비슷하다. `CoroutineExceptionHandler` 을 사용해서 예외를 복구 하지는 못한다. Coroutine은 Handler가 호출되었을 때 이미 해당 Exception에 대한 처리를 완료했기 때문이다. 일반적으로 `CoroutineExceptionHandler`는 오류를 로깅하거나, 애러 메세지를 보여주거나, 어플리케이션을 종료하거나 다시 시작하기 위해 사용된다.

`CoroutineExceptionHandler`는 잡히지 않은 예외에 대해서만 실행된다 - 다른 어떠한 방식으로도 처리되지 않은 예외. 특히, 모든 자식 Coroutine들(다른 Job의 Context로 만들어진 Coroutines)은 그들의 예외를 부모 Coroutine에서 처리하도록 위임하는데, 그 부모 또한 부모에게 위임해서 root Coroutine까지 올라간다. 따라서 그들의 Context에 추가된 `CoroutineExceptionHandler`는 절대 사용되지 않는다. 추가적으로 async 빌더는 모든 예외를 잡아 Deferred 객체에 나타내므로, CoroutineExceptionHandler가 아무런 효과가 없음은 마찬가지이다.

> 📖  Supervision Scope 상에서 실행되는 Coroutine은 예외를 그들의 부모로 전파하지 않으며, 이 규칙으로부터 제외된다. 이 문서에서 이후 다룰 Supervision 섹션에서 더 자세히 알려줄 것이다.

```kotlin
val handler = CoroutineExceptionHandler { _, exception -> 
    println("CoroutineExceptionHandler got $exception") 
}
val job = GlobalScope.launch(handler) { // root coroutine, running in GlobalScope
    throw AssertionError()
}
val deferred = GlobalScope.async(handler) { // also root, but async instead of launch
    throw ArithmeticException() // Nothing will be printed, relying on user to call deferred.await()
}
joinAll(job, deferred)
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-exceptions-02.kt)에서 확인할 수 있습니다.

이 코드의 출력은 다음과 같다 :&#x20;

```
CoroutineExceptionHandler got java.lang.AssertionError
```

***

## Cancellation과 Exceptions

취소는 예외와 밀접히 연관되어 있다. Coroutine은 내부적으로 취소를 위해 `CancellationException`을 사용하며, 이 예외는 모든 Handler에서 무시된다. 따라서 이들은 `catch`블록으로부터 얻을 수 있는 추가적인 디버그 정보를 위해서만 사용되어야 한다. Coroutine이 Job.cancel을 사용해 취소될 경우 종료되지만, 부모 Coroutine의 실행을 취소하지는 않는다.&#x20;

```kotlin
val job = launch {
    val child = launch {
        try {
            delay(Long.MAX_VALUE)
        } finally {
            println("Child is cancelled")
        }
    }
    yield()
    println("Cancelling child")
    child.cancel()
    child.join()
    yield()
    println("Parent is not cancelled")
}
job.join()
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-exceptions-03.kt)에서 확인할 수 있습니다.

이 코드의 출력은 다음과 같다 :

```
Cancelling child
Child is cancelled
Parent is not cancelled
```

만약 Coroutine이 `CancellationException` 말고 다른 예외를 만난다면, 그 예외로 부모 Coroutine까지 취소한다. 이 동작은 재정의할 수 없으며, 구조화된 동시성을 위해 안정적인 Coroutine 계층구조를 제공하는데 사용된다. CoroutineExceptionHandler의 구현은 자식 Coroutine들을 위해 사용되지 않는다.

> 📖  이 예에서, CoroutineExceptionHandler는 언제나 GlobalScope에서 만들어진 Coroutine에 설치된다.  main 함수의 runBlocking Scope에서 실행된 Coroutine에 예외 처리기를 설치하는 것은 의미가 없다. 설치된 CoroutineExceptionHandler가 있더라도 main Coroutine은 자식 Coroutine들이 예외로 인해 완료되면 언제나 취소되기 때문이다.



예외는 모든 자식 Coroutine이 종료될 때만 부모에 의해 처리되며, 다음 예제에서 확인할 수 있다 :

```kotlin
val handler = CoroutineExceptionHandler { _, exception -> 
    println("CoroutineExceptionHandler got $exception") 
}
val job = GlobalScope.launch(handler) {
    launch { // the first child
        try {
            delay(Long.MAX_VALUE)
        } finally {
            withContext(NonCancellable) {
                println("Children are cancelled, but exception is not handled until all children terminate")
                delay(100)
                println("The first child finished its non cancellable block")
            }
        }
    }
    launch { // the second child
        delay(10)
        println("Second child throws an exception")
        throw ArithmeticException()
    }
}
job.join()
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-exceptions-04.kt)에서 확인할 수 있습니다.

이 코드의 출력은 다음과 같다 :

```
Second child throws an exception
Children are cancelled, but exception is not handled until all children terminate
The first child finished its non cancellable block
CoroutineExceptionHandler got java.lang.ArithmeticException
```



## Exceptions 합치기

만약 Coroutine의 복수의 자식들이 예외와 함께 실행에 실패한다면, 일반적인 규칙은 "첫번째 예외가 이긴다"이며, 따라서 첫 예외만 처리된다. 첫 예외 이후 생긴 모든 추가적인 예외들은 첫번째 예외에 suppressed로 붙여진다.

```kotlin
@OptIn(DelicateCoroutinesApi::class)
fun main() = runBlocking {
    val handler = CoroutineExceptionHandler { _, exception ->
        println("CoroutineExceptionHandler got $exception with suppressed ${exception.suppressed.contentToString()}")
    }
    val job = GlobalScope.launch(handler) {
        launch {
            try {
                delay(Long.MAX_VALUE) // it gets cancelled when another sibling fails with IOException
            } finally {
                throw ArithmeticException() // the second exception
            }
        }
        launch {
            delay(100)
            throw IOException() // the first exception
        }
        delay(Long.MAX_VALUE)
    }
    job.join()  
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-exceptions-05.kt)에서 확인할 수 있습니다.

> 📖 주의:  위 코드는 suppressed 예외를 지원하는 JDK7 버전 이상에서만 정상적으로 동작한다.

위 코드의 출력은 다음과 같다.

```
CoroutineExceptionHandler got java.io.IOException with suppressed [java.lang.ArithmeticException]
```

취소 예외는 투명하고 기본적으로 감싸진다.

```kotlin
val handler = CoroutineExceptionHandler { _, exception ->
    println("CoroutineExceptionHandler got $exception")
}
val job = GlobalScope.launch(handler) {
    val inner = launch { // all this stack of coroutines will get cancelled
        launch {
            launch {
                throw IOException() // the original exception
            }
        }
    }
    try {
        inner.join()
    } catch (e: CancellationException) {
        println("Rethrowing CancellationException with original cause")
        throw e // cancellation exception is rethrown, yet the original IOException gets to the handler  
    }
}
job.join()
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-exceptions-06.kt)에서 확인할 수 있습니다.

이 코드의 출력은 다음과 같다.

```
Rethrowing CancellationException with original cause
CoroutineExceptionHandler got java.io.IOException
```





## Supervision

이전에 공부한 것처럼, 취소는 Coroutine의 전체 계층을 통해 전파되는 양방향 관계를 가진다. 단방향 취소만이 필요한 경우를 살펴보자.

이러한 요구사항에 대한 좋은 예제는 Scope 내부에 Job이 선언된 UI 구성요소이다. 만약 UI의 자식의 작업이 실패되더라도, 언제나 모든 UI 구성요소를 취소(효과적으로 종료)하는 것은 필수적이지 않다. 하지만, UI 구성요소가 파괴되면(그리고 그 Job이 취소되면), 더이상 결과값이 필요 없기 때문에 모든 자식 Job을 취소하는 것은 필수적이다.&#x20;

다른 예시는 여러 자식 Job을 생성하고 이들의 실행이 감독되고 그들의 실패가 추적되어서 실패된 것들만 재시작 해야하는 서버 프로세스이다.&#x20;



### Supervision job

`SupervisorJob`이 이 복적을 위해 사용될 수 있다. 이는 취소가 아래 방향으로 전파되는 것만 제외하면 일반적인 `Job`과 비슷하다. 이는 다음 예제를 통해 설명될 수 있다.

```kotlin
val supervisor = SupervisorJob()
with(CoroutineScope(coroutineContext + supervisor)) {
    // launch the first child -- its exception is ignored for this example (don't do this in practice!)
    val firstChild = launch(CoroutineExceptionHandler { _, _ ->  }) {
        println("The first child is failing")
        throw AssertionError("The first child is cancelled")
    }
    // launch the second child
    val secondChild = launch {
        firstChild.join()
        // Cancellation of the first child is not propagated to the second child
        println("The first child is cancelled: ${firstChild.isCancelled}, but the second one is still active")
        try {
            delay(Long.MAX_VALUE)
        } finally {
            // But cancellation of the supervisor is propagated
            println("The second child is cancelled because the supervisor was cancelled")
        }
    }
    // wait until the first child fails & completes
    firstChild.join()
    println("Cancelling the supervisor")
    supervisor.cancel()
    secondChild.join()
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-supervision-01.kt)에서 확인할 수 있습니다.

이 코드의 출력은 다음과 같다 :

```
The first child is failing
The first child is cancelled: true, but the second one is still active
Cancelling the supervisor
The second child is cancelled because the supervisor was cancelled
```

### &#x20;Supervision Scope

특정 범위에 대한 동시성을 적용하기 위해 `coroutineScope` 대신 `supervisorScope`을 사용할 수 있다. 이는 취소를 한 방향으로만 전파하며, 그 자신이 실패했을 때만 자식 Coroutine들을 취소한다. 또한 coroutineScope 처럼 완료되기 전에 모든 자식들이 완료되는 것을 기다린다.

```kotlin
try {
    supervisorScope {
        val child = launch {
            try {
                println("The child is sleeping")
                delay(Long.MAX_VALUE)
            } finally {
                println("The child is cancelled")
            }
        }
        // Give our child a chance to execute and print using yield 
        yield()
        println("Throwing an exception from the scope")
        throw AssertionError()
    }
} catch(e: AssertionError) {
    println("Caught an assertion error")
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-supervision-02.kt)에서 확인할 수 있습니다.

이 코드의 출력은 다음과 같다 :

```
The child is sleeping
Throwing an exception from the scope
The child is cancelled
Caught an assertion error
```



#### **Supervise가 사용된  Coroutine에서의 예외**

Job과 SupervisorJob의 또다른 중요한 차이는 예외 처리이다. 모든 자식은 자신의 예외를 예외 처리 메커지니즘에 따라 직접 처리해야 한다. 이 다른점은 자식의 실패가 부모에게 전파되지 않는다는 점이다. 이는 `supervisorScope` 내부에서 직접 실행된 Coroutine은 root Coroutine과 비슷하게 그들의 Scope내부에 설치된 `CoroutineExceptionHandler`를 쓰는 것을 뜻한다.(자세한 것은 [CoroutineExceptionHandler](https://translatordev.com/66) 섹션을 참조) &#x20;

```kotlin
val handler = CoroutineExceptionHandler { _, exception -> 
    println("CoroutineExceptionHandler got $exception") 
}
supervisorScope {
    val child = launch(handler) {
        println("The child throws an exception")
        throw AssertionError()
    }
    println("The scope is completing")
}
println("The scope is completed")
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-supervision-03.kt)에서 확인할 수 있습니다.

이 코드의 출력은 다음과 같다 :

```
The scope is completing
The child throws an exception
CoroutineExceptionHandler got java.lang.AssertionError
The scope is completed
```

\


\
