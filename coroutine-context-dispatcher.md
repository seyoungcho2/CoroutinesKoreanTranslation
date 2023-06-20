---
description: '원문 최종 수정 :  2023년 6월 16일'
---

# Coroutine Context와 Dispatcher

Coroutines는 언제나 Kotlin 표준 라이브러리에 정의된 [CoroutineContext](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/-coroutine-context/) 타입 값으로 표현되는 일부 Context 상에서 실행된다. Coroutine의 Context는 다양한 요소의 집합이다. 주요 요소는 이전 섹션에서 본 Coroutine의 [Job](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-job/index.html)과 이번 섹션에서 다룰 Dispatcher이다.



## Dispatchers와 Threads

Coroutine Context에는 해당 Coroutine의 실행에 사용되는 단일 스레드나 복수의 스레드를 결정하는 **Coroutine Dispatcher**([CoroutineDispatcher](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-coroutine-dispatcher/) 문서를 확인)가 포함 된다. Coroutine Dispatcher은 Coroutine의 실행될 사용될 스레드를 특정 스레드로 제한하거나 스레드풀에 분배하거나, 제한 없이 실행 되도록 할 수 있다.

`launch`나 `async` 같은 모든 Coroutine Builder들은 새로운 Coroutine을 위해 Dispatcher나 다른 Context 요소들을 명시적으로 지정하는데 사용할 수 있는 `CoroutineContext` 파라미터를 선택적으로 받을 수 있다.&#x20;

다음의 예를 실행해보자.

```kotlin
launch { // context of the parent, main runBlocking coroutine
    println("main runBlocking      : I'm working in thread ${Thread.currentThread().name}")
}
launch(Dispatchers.Unconfined) { // not confined -- will work with main thread
    println("Unconfined            : I'm working in thread ${Thread.currentThread().name}")
}
launch(Dispatchers.Default) { // will get dispatched to DefaultDispatcher 
    println("Default               : I'm working in thread ${Thread.currentThread().name}")
}
launch(newSingleThreadContext("MyOwnThread")) { // will get its own new thread
    println("newSingleThreadContext: I'm working in thread ${Thread.currentThread().name}")
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-context-01.kt)에서 확인할 수 있습니다.

이는 다음의 결과를 출력한다(순서가 다를 수 있음) :

```
Unconfined            : I'm working in thread main
Default               : I'm working in thread DefaultDispatcher-worker-1
newSingleThreadContext: I'm working in thread MyOwnThread
main runBlocking      : I'm working in thread main
```

`launch { ... }`가 파라미터 없이 사용된다면, 실행되는 [CoroutineScope](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-coroutine-scope/index.html)으로 부터 Context를 상속 받는다(Dispatcher도 같이). 이런 경우 main 함수의 `runBlocking` Coroutine으로부터 Context를 상속 받아 메인 스레드에서 실행되게 된다.&#x20;

`Dispatchers.Unconfined` 또한 메인 스레드에서 실행되는 특별한 Dispatcher이지만, 실제로는 나중에 설명될 다른 동작방식에 기인한다.

Default Dispatcher은 Scope내에서 다른 Dispatcher을 사용이 명시적으로 지정되지 않았을 때 사용된다. `Dispatchers.Default`로 표기되며, 스레드들이 공유하는 Background Pool을 사용한다.

`newSingleThreadContext`는 Coroutine이 실행되기 위한 새로운 단일 스레드를 생성한다. 전용 스레드는 매우 비싼 리소스이다. 실제 어플리케이션에서 더 이상 필요하지 않을 때 `close` 함수를 사용해 해제되어야 하며, 최상위 레벨의 변수에 저장하여 어플리케이션이 실행되는 동안 재사용 될 수 있도록 해야 한다.&#x20;

***

## Unconfined vs confined dispatcher

Coroutine Dispatcher인 `Dispatchers.Unconfined`는 처음 일시 중단 되기 전까지만 호출 스레드에서 Coroutine을 시작한다. 일시 중단 이후에는 실행된 일시 중단 함수에 의해 완전히 결정된 스레드 상에서 Coroutine을 재개한다. Unconfined Dispatcher은 특정한 스레드에서 수행되어야 하는 CPU 시간이나 공유되는 데이터(UI 데이터와 같은)를 업데이트 하지 않는 Coroutines에 적합하다.

반면에, Dispatcher은 기본적으로 바깥 `CoroutineScope`에 의해 상속 된다. 특히 `runBlocking` Coroutine에 대한 기본 Dispatcher는 호출 스레드로 제한되므로, 이를 상속하면 예측 가능한 FIFO 스케쥴링으로 이 스레드의 실행을 제한할 수 있다.

```kotlin
launch(Dispatchers.Unconfined) { // not confined -- will work with main thread
    println("Unconfined      : I'm working in thread ${Thread.currentThread().name}")
    delay(500)
    println("Unconfined      : After delay in thread ${Thread.currentThread().name}")
}
launch { // context of the parent, main runBlocking coroutine
    println("main runBlocking: I'm working in thread ${Thread.currentThread().name}")
    delay(1000)
    println("main runBlocking: After delay in thread ${Thread.currentThread().name}")
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-context-02.kt)에서 확인할 수 있습니다.

위는 다음의 결과를 만든다.

```
Unconfined      : I'm working in thread main
main runBlocking: I'm working in thread main
Unconfined      : After delay in thread kotlinx.coroutines.DefaultExecutor
main runBlocking: After delay in thread main
```

`runBlocking { ... }` 을 상속받은 Context를 가진 Coroutine은 메인 스레에서 실행되지만, Unconfined를 가진 Coroutine은 delay 함수가 사용하는 Default Executor Thread에서 재개된다.&#x20;

> 📖 Unconfined Dispatcher은 Coroutine의 일부 작업이 즉시 실행되어야 하기 때문에 나중에 수행하기 위해 Coroutine을 Dispatch 할 필요가 없거나, 원하지 않는 부수효과를 생성하는 특정한 경우에 도움될 수 있는 고급 메커니즘이다. 일반적인 코드에서 Unconfined Dispatcher은 사용되지 말아야 한다.



## Coroutines와 Threads 디버깅 하기

Coroutines는 하나의 스레드에서 일시 중단한 다음 다른 스레드에서 재개될 수 있다. 싱글 스레드를 가진 Dispatcher에서도 특별한 도구가 없으면 Coroutine이 언제, 어디서 무엇을 하는지 알기 어렵다.

### IDEA를 사용해 디버깅 하기

Kotlin Plugin인 Coroutine Debugger은 Intellij IDEA에서 Coroutines 디버깅을 간단하게 할 수 있도록 한다.

> 📍 디버깅은 `kotlinx-coroutines-core 1.3.8` 혹은 그 이후 버전에서부터만 동작한다.

**Debug** tool window는 **Coroutines** 탭을 포함한다. 이 탭에서 현재 실행 중이거나 일시 중단된 Coroutine 모두에 대한 정보를 확인할 수 있다. Coroutines 는 실행 중인 Dispatcher에 따라 그룹화된다.

<figure><img src="https://kotlinlang.org/docs/images/coroutine-idea-debugging-1.png" alt=""><figcaption></figcaption></figure>

Coroutine Debugger을 사용해서 이런 것들을 할 수 있다 :

* 각 Coroutine에 대한 상태를 확인할 수 있다.
* 실행 중이거나 일시 중단된 Coroutines에 대한 지역 변수나 캡처된 변수의 값들을 확인할 수 있다..
* Coroutine 생성 스택뿐만 아니라, Coroutine 내부의 콜스택에 대한 확인 할 수 있다. 스택은 일반적인 디버깅에서 손실되는 프레임을 포함한 전체 프레임들에 대한 변수의 값들을 포함한다.
* 각 Coroutine의 상태와 그 스택들을 포함하는 전체 리포트를 가져올 수 있다. 이것을 얻기 위해서는 **Coroutines** Tab을 마우스 우클릭 후, **Get Coroutines Dump**를 클릭하면 된다.

Coroutine 디버깅을 시작하기 위해서는, Breakpoint들을 설정하고 어플리케이션을 디버그 모드로 실행하면 된다.



Coroutine 디버깅에 대해 [튜토리얼](https://kotlinlang.org/docs/debug-coroutines-with-idea.html)에서 더 알아볼 수 있다.



### 로깅을 통해 디버깅 하기

Coroutine Debugger 없이 스레드를 사용하는 어플리케이션을 디버깅하는 방법은 각 로그 구문에 스레드 이름을 넣어, 로그 파일에 스레드 이름을 인쇄하는 것이다. 이 기능은 로깅 프레임워크들에서 보편적으로 지원한다. Coroutines를 사용할 때 스레드 이름만으로는  Context에 대한 많은 정보들을 제공하지 않으므로, `kotlinx.coroutines` 는 디버깅을 쉽게 하기 위한 많은 기능들을 포함한다.&#x20;

JVM 옵션에 `-Dkotlinx.coroutines.debug` 를 포함한 채로 다음 코드를 실행해보자.&#x20;

```kotlin
import kotlinx.coroutines.*

fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")

fun main() = runBlocking<Unit> {
    val a = async {
        log("I'm computing a piece of the answer")
        6
    }
    val b = async {
        log("I'm computing another piece of the answer")
        7
    }
    log("The answer is ${a.await() * b.await()}")    
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-context-03.kt)에서 확인할 수 있습니다.

이 코드에는 3 개의 Coroutine이 있다. `runBlocking` 속의 main Coroutine(#1)과 지연된 값 `a`(#2)와 `b`(#3)를 계산하는 두 Coroutine들이다. 이들은 모두 `runBlocking`의 Context에서 실행되며, 이들은 모두 Main Thread에서 실행되도록 제한된다. 이 코드의 출력은 다음과 같다 :

```
[main @coroutine#2] I'm computing a piece of the answer
[main @coroutine#3] I'm computing another piece of the answer
[main @coroutine#1] The answer is 42
```

`log` 함수는 대괄호들 내부의 스레드의 이름을 출력하며, 현재 실행중인 Coroutine에 추가된 식별자는 메인 스레드임을 알 수 있다. 이 식별자는 디버깅 모드가 켜져 있을 때 생성된 모든 Coroutine들에 연속적으로 할당된다.

> 📖 디버깅 모드는 JVM이 `-ea` 옵션이 포함된 채로 실행될 때 켜진다. 디버깅 기능들에 대한 추가적인 것들은 [DEBUG\_PROPERTY\_NAME](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-d-e-b-u-g\_-p-r-o-p-e-r-t-y\_-n-a-m-e.html) 프로퍼티에 대한 문서에서 확인할 수 있다.

\


## Thread 전환 하기

다음 코드를 JVM option에 `-Dkotlinx.coroutines.debug`를 넣어 실행시켜보자([debug](https://translatordev.com/29) 확인).&#x20;

```kotlin
newSingleThreadContext("Ctx1").use { ctx1 ->
    newSingleThreadContext("Ctx2").use { ctx2 ->
        runBlocking(ctx1) {
            log("Started in ctx1")
            withContext(ctx2) {
                log("Working in ctx2")
            }
            log("Back to ctx1")
        }
    }
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-context-04.kt)에서 확인할 수 있습니다.

이는 몇가지 새로운 기술들을 보여준다. 하나는 `runBlocking`을 명시적으로 구체화된 Context와 함께 사용하는 것이고, 다른 하나는 아래 출력 에서 볼 수 있듯이 `withContext` 함수를 같은 Coroutine에 계속 있으면서 Context를 바꾸는데 사용하는 것이다.

```
[Ctx1 @coroutine#1] Started in ctx1
[Ctx2 @coroutine#1] Working in ctx2
[Ctx1 @coroutine#1] Back to ctx1
```

또한 이 예제에서는 Kotlin 표준 라이브러리의 `use` 함수를 `newSingleThreadContext`에 의해 생성된 스레드들이 더이상 필요하지 않을 때 해제하는데 사용한다.



## Context 내부의 Job

Coroutines의 Job은 Context의 구성요소이고, `coroutineContext[Job]` 표현식으로부터 가져올 수 있다.&#x20;

```
fun main() = runBlocking<Unit> {
    println("My job is ${coroutineContext[Job]}")    
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-context-05.kt)에서 확인할 수 있습니다.



디버그 모드에서 출력은 다음과 같다.

```
My job is "coroutine#1":BlockingCoroutine{Active}@6d311334
```

CoroutineScope의 isActive는 `coroutineContext[Job]?.isActive == true` 를 편리하게 사용하기 위한 축약어임을 참고하자.

***

## Coroutine의 자식들

Coroutine이 다른 Coroutine의 CoroutineScope내에서 실행되면 Context를 CoroutineScope.coroutineContext를 통해 상속 받고 새로운 Coroutine의 Job은 부모 Coroutine의 Job의 자식이 된다. 부모 Coroutine이 취소되면, 자식 Coroutine들 또한 재귀적으로 취소된다.

하지만, 부모-자식 관계는 두가지 방법으로 명시적으로 재정의 될 수 있다.

* Coroutine을 실행할 때 두 개의 다른 Scope이 명시적으로 설정하는 경우(예를 들어, `GlobalScope.launch`), 부모 Scope으로부터 Job을 상속 받지 않는다.
* 새로운 Coroutine을 위해 다른 `Job`이 Context로 전달되는 경우(아래 예시와 같이) 부모 Scope의 `Job`을 재정의한다.



두 경우 모두 실행된 Coroutine은 실행된 Scope에 묶여있지 않고 독립적으로 동작한다.

```kotlin
// launch a coroutine to process some kind of incoming request
val request = launch {
    // it spawns two other jobs
    launch(Job()) { 
        println("job1: I run in my own Job and execute independently!")
        delay(1000)
        println("job1: I am not affected by cancellation of the request")
    }
    // and the other inherits the parent context
    launch {
        delay(100)
        println("job2: I am a child of the request coroutine")
        delay(1000)
        println("job2: I will not execute this line if my parent request is cancelled")
    }
}
delay(500)
request.cancel() // cancel processing of the request
println("main: Who has survived request cancellation?")
delay(1000) // delay the main thread for a second to see what happens
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-context-06.kt)에서 확인할 수 있습니다.



코드를 실행한 결과는 다음과 같다.

```
job1: I run in my own Job and execute independently!
job2: I am a child of the request coroutine
main: Who has survived request cancellation?
job1: I am not affected by cancellation of the request
```



## 부모의 책임

부모 Coroutine은 언제나 자식들이 완료될 때까지 기다린다. 부모는 모든 자식들의 실행을 명시적으로 추적하지 못하고, 그들이 모두 끝날 때까지 기다리기 위해 `Job.join`을 사용할 필요가 없다.

```kotlin
// launch a coroutine to process some kind of incoming request
val request = launch {
    repeat(3) { i -> // launch a few children jobs
        launch  {
            delay((i + 1) * 200L) // variable delay 200ms, 400ms, 600ms
            println("Coroutine $i is done")
        }
    }
    println("request: I'm done and I don't explicitly join my children that are still active")
}
request.join() // wait for completion of the request, including all its children
println("Now processing of the request is complete")
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-context-07.kt)에서 확인할 수 있습니다.

결과는 다음과 같다 :

```
request: I'm done and I don't explicitly join my children that are still active
Coroutine 0 is done
Coroutine 1 is done
Coroutine 2 is done
Now processing of the request is complete
```

\




## 디버깅을 위해 Coroutines에 이름 짓기

자동으로 설정된 ID들은 Coroutine들이 자주 로깅될 때 유용하며, 같은 Coroutine에서 오는 연관된 로그 기록들을 연관짓기만 하면 된다. 하지만, Coroutine이 특정한 요청이나 특정한 백그라운드 작업을 하는 경우 디버깅 목적을 위해 이름을 설정하는 것이 좋다. Context의 요소인 `CoroutineName`는 스레드의 이름과 같은 용도로 사용된다. 이는 디버깅 모드가 켜져 있을 때 Coroutine을 실행하는 스레드 이름에 포함된다.

다음 예시는 이 개념에 대해 보여준다 :&#x20;

```kotlin
log("Started main coroutine")
// run two background value computations
val v1 = async(CoroutineName("v1coroutine")) {
    delay(500)
    log("Computing v1")
    252
}
val v2 = async(CoroutineName("v2coroutine")) {
    delay(1000)
    log("Computing v2")
    6
}
log("The answer for v1 / v2 = ${v1.await() / v2.await()}")
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-context-08.kt)에서 확인할 수 있습니다.

JVM 옵션에 `-Dkotlinx.coroutines.debug` 를 넣은 상태로 생성한 결과는 다음과 유사하다 :&#x20;

```
[main @main#1] Started main coroutine
[main @v1coroutine#2] Computing v1
[main @v2coroutine#3] Computing v2
[main @main#1] The answer for v1 / v2 = 42
```

***

## Context 요소들 결합하기

종종 Coroutine Context에 복수의 요소들을 정의해야 할 수 있다. 우리는 이를 위해 `+` 연산자를 사용할 수 있다. 예를 들어, 명시적으로 Dispatcher을 지정함과 동시에 명시적으로 이름을 지정한 Coroutine을 실행해야 할 수 있다 :&#x20;

```kotlin
launch(Dispatchers.Default + CoroutineName("test")) {
    println("I'm working in thread ${Thread.currentThread().name}")
}
```

📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-context-09.kt)에서 확인할 수 있습니다.

JVM Option에 `-Dkotlinx.coroutines.debug` 을 추가한 코드의 결과값은 다음과 같다 :

```
I'm working in thread DefaultDispatcher-worker-1 @test#2
```

\
Coroutine Scope
---------------

Context, 자식 그리고 Job들에 대한 지식을 결합시켜보자. 어플리케이션이 Coroutine이 아닌 생명주기을 가진 객체를 가지고 있다고 가정해보자. 예를 들어, 안드로이드 어플리케이션을 만들고 안드로이드 Activity의 Context 상에서 데이터를 가져오고 업데이트 시키거나, 애니메이션을 실행하는 등의 비동기 작업들을 수행하기 위해서 다양한 Coroutine들을 실행시킬 수 있다. 이 Coroutine들은 Activity가 파괴될 때 메모리 누수를 방지하기 위해 취소되어야 한다. 물론 Context와 Job들을 직접 조작하여 Activity의 Coroutine의 생명주기를 결합시킬 수 있다. 하지만, `kotlinx.coroutines` 패키지는 `CoroutineScope`을 캡슐화하는 추상화를 제공한다.  &#x20;



우리는 Activity의 생명주기에 묶은 `CoroutineScope`의 인스턴스를 생성해 Coroutines의 생명주기를 관리한다. `CoroutineScope` 인스턴스는 `CoroutineScope()` 이나 `MainScope()` 같은 팩토리 함수들로 생성될 수 있다. 전자는 일반적인 목적의 Scope을 생성하며, 후자는 UI 어플리케이션을 위한 Scope을 생성하고 `Dispatchers.Main`을 기본 디스패쳐로 사용한다.

```kotlin
class Activity {
    private val mainScope = MainScope()

    fun destroy() {
        mainScope.cancel()
    }
    // to be continued ...
```

이제 정의된 `scope`을 사용해 `Activity`의 Scope 내에서 Coroutines를 실행시킬 수 있다. 데모를 위해 다른 시간의 delay를 가지는 10개의 Coroutine들을 생성하자:

```kotlin
// class Activity continues
    fun doSomething() {
        // launch ten coroutines for a demo, each working for a different time
        repeat(10) { i ->
            mainScope.launch {
                delay((i + 1) * 200L) // variable delay 200ms, 400ms, ... etc
                println("Coroutine $i is done")
            }
        }
    }
} // class Activity ends
```

main 함수에서 Activity를 생성하며, 테스트 함수인 `doSomething` 를 호출하고, Activity를 500ms 후에 파괴한다. 이는 `doSomething`에서 실행된 모든 Coroutine들을 취소한다. Activity가 파괴된 이후에는, 조금 더 기다려도 아무 메세지도 출력되지 않는 것을 확인 할 수 있다.

```kotlin
val activity = Activity()
activity.doSomething() // run test function
println("Launched coroutines")
delay(500L) // delay for half a second
println("Destroying activity!")
activity.destroy() // cancels all coroutines
delay(1000) // visually confirm that they don't work
```

📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-context-10.kt)에서 확인할 수 있습니다.

이 예시에 대한 출력은 다음과 같다 :

```
Launched coroutines
Coroutine 0 is done
Coroutine 1 is done
Destroying activity!
```

첫 두개의 Coroutine들만 메세지를 출력하고 나머지들은 `Activity.destory()`에서 `job.cancel()`이 한 번 호출되어 취소되는 것을 확인할 수 있다.

> 📖  안드로이드는 수명주기가 있는 모든 엔티티들에서 CoroutineScope에 대한 자사의 지원을 제공한다. [다음 문서](https://developer.android.com/topic/libraries/architecture/coroutines#lifecyclescope)를 참조하자.



### Thread-local 데이터

Thread-local 데이터를 Coroutine으로 전달하거나, Coroutine들간에 전달하는 기능이 있으면 편리하다. 하지만, Coroutine들은 특정 스레드에 묶여있지 않기 때문에 이를 직접하게 되면 보일러 플레이트를 만들 수 있다.

이를 해결하기 위해 `ThreadLocal`을 위한 `asContextElement` 확장 함수가 있다. 이는 Coroutine이 Context를 변경할 때마다 주어진 `ThreadLocal`의 값을 유지하고 복원하는 추가적인 Context 구성요소를 생성한다.

이는 직접 보면 쉽게 설명된다 :&#x20;

```kotlin
threadLocal.set("main")
println("Pre-main, current thread: ${Thread.currentThread()}, thread local value: '${threadLocal.get()}'")
val job = launch(Dispatchers.Default + threadLocal.asContextElement(value = "launch")) {
    println("Launch start, current thread: ${Thread.currentThread()}, thread local value: '${threadLocal.get()}'")
    yield()
    println("After yield, current thread: ${Thread.currentThread()}, thread local value: '${threadLocal.get()}'")
}
job.join()
println("Post-main, current thread: ${Thread.currentThread()}, thread local value: '${threadLocal.get()}'")
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-context-11.kt)에서 확인할 수 있습니다.

이 예에서 우리는 Dispatcher.Default를 사용하여 백그라운드 스레드풀에서 새로운 Coroutine을 실행한다. 따라서 스레드풀과 다른 스레드에서 동작하지만, 어떤 스레드에서 동작하던지 상관 없이 `threadLocal.asContextElement(value = "launch")`를 사용해 지정한 Thread-local 변수 값을 가지고 있다. 따라서 [디버그 옵션을 적용](https://kotlinlang.org/docs/coroutine-context-and-dispatchers.html#debugging-coroutines-and-threads)해 출력한 결과 값은 다음과 같다.

```
Pre-main, current thread: Thread[main @coroutine#1,5,main], thread local value: 'main'
Launch start, current thread: Thread[DefaultDispatcher-worker-1 @coroutine#2,5,main], thread local value: 'launch'
After yield, current thread: Thread[DefaultDispatcher-worker-2 @coroutine#2,5,main], thread local value: 'launch'
Post-main, current thread: Thread[main @coroutine#1,5,main], thread local value: 'main'
```



해당 Context 요소를 설정하는 것을 잊기 쉽다. 만약 Coroutine을 실행하는 스레드가 다르다면 스레드에 의해 액세스된 Thread-local 변수는 예측할 수 없는 값을 가질 수 있다. 이러한 상황을 방지하기 위해 `ensurePresent` 메서드를 사용하고 잘못된 사용이 있을 시 fail-fast 하는 것이 권장된다.&#x20;

`ThreadLocal`은 최고의 지원을 제공하며, `kotlinx.coroutines` 패키지의 모든 원시 요소들과 함께 사용할 수 있다. 그러나 이는 하나의 주요한 제한사항을 가진다 : 만약 Thread-local이 변경되면, 새로운 값은 코루틴을 호출한 곳에 전달되지 않고(Context 요소가 모든 `ThreadLocal` 객체로의 접근을 추적할 수 없기 때문에), 변경된 값은 다음 일시 중단시점에 손실된다. Coroutine 내의 Thread-local을 변경하기 위해서는 `withContext`를 사용하자. 더 자세한 것은 [asContextElement](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/as-context-element.html)를 참조.

또는 Thread-local 변수에 `class Counter(var i: Int)` 와 같은 변경 가능한 박스를 저장할 수 있다. 하지만, 이 경우  변경 가능한 박스의 변수값이 동시 접근되어 바뀌는 것에 대해 동기화할 모든 책임이 생긴다.

로깅 MDC와의 통합, transactional contexts, 혹은 데이터 전달을 위해 내부적으로 Thread-local을 사용하는 다른 라이브러리들과 같은 Thread-local 고급 사용법은 구현되어야 하는 interface를 설명 해놓은 [ThreadContextElement](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-thread-context-element/) 문서를 참고하면 된다.

***

\


\


***

\
