---
description: '원문 최종 수정 :  2023년 6월 16일'
---

# 비동기 Flow

> [페이지 편집](flow.md)
>
> [원문](https://kotlinlang.org/docs/flow.html)



일시 중단 함수들은 비동기적으로 단일 값을 반환한다. 그렇다면 어떻게 비동기적으로 계산된 복수의 값들을 반환할 수 있을까? 여기에서 바로 Kotlin의 Flows가 등장한다.



## 복수의 값들 표현하기

Kotlin에서 복수의 값들은 [collections](https://kotlinlang.org/docs/reference/collections-overview.html)를 사용해 표현될 수 있다. 예를 들어 3개의 숫자를 가진 `List`를 반환하는 `simple` 함수를 가지고, `forEach`를 사용해 그들을 모두 프린트할 수 있다.

```kotlin
fun simple(): List<Int> = listOf(1, 2, 3)
 
fun main() {
    simple().forEach { value -> println(value) } 
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-01.kt)에서 확인할 수 있습니다.

이 코드의 출력은 다음과 같다 :&#x20;

```
1
2
3
```



### Sequences

만약 CPU 리소스를 사용하면서 블로킹을 하는 코드(각 연산은 100ms의 시간이 소요된다)로 숫자에 대한 연산을 한다면,  `Sequence`를 사용해 숫자를 나타낼 수 있다.

```kotlin
fun simple(): Sequence<Int> = sequence { // sequence builder
    for (i in 1..3) {
        Thread.sleep(100) // pretend we are computing it
        yield(i) // yield next value
    }
}

fun main() {
    simple().forEach { value -> println(value) } 
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-02.kt)에서 확인할 수 있습니다.

이 코드의 결과값은 위와 같지만, 각 숫자들을 프린트하기 전 100ms을 대기한다.



### 일시중단 함수들

그러나 이러한 연산은 코드를 실행하는 메인 스레드를 블로킹한다. 만약 이 값들이 비동기 코드에 의해 계산된다면, 스레드를 블로킹 시키지 않고 수행되고 결과값을 리스트로 반환할 수 있도록 `simple` 함수를 `suspend` 수정자로 표시할 수 있다.

```
suspend fun simple(): List<Int> {
    delay(1000) // pretend we are doing something asynchronous here
    return listOf(1, 2, 3)
}

fun main() = runBlocking<Unit> {
    simple().forEach { value -> println(value) } 
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-03.kt)에서 확인할 수 있습니다.

이 코드는 1초간 기다린 후 숫자들을 프린트한다.



### Flows

결과 타입으로 `List<Int>`를 사용하면 한 번에 모든 값을 반환해야만 한다. 동기적으로 계산된 값을 `Sequence<Int>`를 사용해 나타냈던 것처럼, 비동기적으로 계산되는 값들을 스트림으로 나타내기 위해서는 `Flow<Int>` 타입을 사용할 수 있다 :

```kotlin
fun simple(): Flow<Int> = flow { // flow builder
    for (i in 1..3) {
        delay(100) // pretend we are doing something useful here
        emit(i) // emit next value
    }
}

fun main() = runBlocking<Unit> {
    // Launch a concurrent coroutine to check if the main thread is blocked
    launch {
        for (k in 1..3) {
            println("I'm not blocked $k")
            delay(100)
        }
    }
    // Collect the flow
    simple().collect { value -> println(value) } 
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-04.kt)에서 확인할 수 있습니다.

이 코드는 각 숫자들을 프린트하기 전, 메인 스레드를 블로킹 하지 않고 100ms 동안 대기한다. 이는 "I'm not blocked"를 100ms 마다 프린트하는 메인 스레드에서 실행되는 별도의 Coroutine을 통해 확인된다 :&#x20;

```
I'm not blocked 1
1
I'm not blocked 2
2
I'm not blocked 3
3
```

이전 예제들에서의 코드들과 Flow는 다음의 차이점들이 있다는 것을 확인하자 :

* `Flow`의 빌더 함수는 `flow`이다.
* `flow { ... }` 블록 내부의 코드들은 일시 중단 될 수 있다.
* `simple` 함수는 더이상 `suspend` 수정자로 표시되어 있지 않다.
* `emit` 함수를 사용해 flow에서 값들이 **방출**된다.
* `collect` 함수를 사용해 flow로부터 값들을 **수집**한다.

> 📖  `simple` 함수의 `flow { ... }` 블록 내부에서 delay를 `Thread.sleep`으로 교체하는 경우 메인 스레드가 블록되는 것을 볼 수 있다.&#x20;



## Flows는 차갑다

Flow는 Sequence와 비슷한 **차가운** Stream이다. `flow` 빌더 내부의 코드는 flow가 collect되기 전까지 실행되지 않는다. 이는 다음의 예에서 확실히 나타난다.

```kotlin
fun simple(): Flow<Int> = flow { 
    println("Flow started")
    for (i in 1..3) {
        delay(100)
        emit(i)
    }
}

fun main() = runBlocking<Unit> {
    println("Calling simple function...")
    val flow = simple()
    println("Calling collect...")
    flow.collect { value -> println(value) } 
    println("Calling collect again...")
    flow.collect { value -> println(value) } 
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-05.kt)에서 확인할 수 있습니다.

이는 다음과 같은 출력을 가진다 :&#x20;

```
Calling simple function...
Calling collect...
Flow started
1
2
3
Calling collect again...
Flow started
1
2
3
```

이것이 flow를 반환하는 simple 함수가 suspend 수정자로 표시되지 않은 이유이다. simple() 함수 호출 그 자체는 곧바로 반환되며 어떤 것도 기다리지 않는다. flow는 collect가 될때마다 새로 시작되며, 이것이 collect를 다시 호출할 때마다 "Flow started"가 표시되는 이유이다.



## Flow 취소 기초

Flow는 Coroutines의 기본적인 협력적인 취소를 따른다. 일반적으로, 취소 가능한 일시중단 함수(`delay` 같은)에서 Flow가 일시중단될 때 Flow로부터 값을 수집하는 것이 취소될 수 있다. 다음의 예는 Flow가 `withTimeoutOrNull` 블록에서 실행될 때, Flow가 시간 초과에 따라 어떻게 취소되고 코드 실행이 중지되는지 보여준다 :

```kotlin
fun simple(): Flow<Int> = flow { 
    for (i in 1..3) {
        delay(100)          
        println("Emitting $i")
        emit(i)
    }
}

fun main() = runBlocking<Unit> {
    withTimeoutOrNull(250) { // Timeout after 250ms 
        simple().collect { value -> println(value) } 
    }
    println("Done")
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-06.kt)에서 확인할 수 있습니다.

`simple` 함수의 flow에서 2개의 숫자만 방출되고, 다음과 같은 출력을 만드는 것에 주목하자 :

```
Emitting 1
1
Emitting 2
2
Done
```

> 📌 자세한 사항을 알고 싶으면 [Flow cancellation checks](https://kotlinlang.org/docs/flow.html#flow-cancellation-checks) 섹션을 확인하자.

***

## Flow 빌더

이전 예제들의 `flow { ... }` 빌더는 가장 기본적인 빌더이다. Flow를 선언할 수 있는 다른 빌더들도 있다.

* [flowOf](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/flow-of.html) 빌더는 정해진 값의 세트를 방출하는 Flow를 정의한다.
* 다양한 Collection들과 Sequence들은 `.asFlow()` 확장 함수를 사용해 Flow로 변환될 수 있다.

```
// 정수 범위를 flow로 변환한다.
(1..3).asFlow().collect { value -> println(value) }
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-07.kt)에서 확인할 수 있습니다.



## Flow 중간 연산자

Flow들은 Collections, Sequence와 같이 연산자를 이용해 변환될 수 있다. 중간 연산자는 업스트림 Flow에 적용되어 다운스트림 Flow를 반환한다. 이러한 연산자들은 Flow만큼 차갑다. 이러한 연산자를 호출하는 것은 그 자체로 일시 중단 함수가 아니다. 이는 빠르게 작동해 새롭게 변환된 Flow를 반환한다.

기본 연산자들은 [map](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/map.html) 혹은 [filter](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/filter.html)와 같은 친숙한 이름을 가지고 있다. 이러한 연산자들과 Sequence들의 중요한 차이점은 이 연산자들 내부의 코드 블록에서는 일시 중단 함수를 호출 할 수 있다는 점이다.

예를 들어 요청을 수행하는 것이 오래 걸리는 작업이고 일시 중단 기능으로 구현되어 있는 경우에도, 요청들을 받는 Flow를 `map` 연산자를 사용해 결과에 매핑할 수 있다.

```kotlin
suspend fun performRequest(request: Int): String {
    delay(1000) // imitate long-running asynchronous work
    return "response $request"
}

fun main() = runBlocking<Unit> {
    (1..3).asFlow() // a flow of requests
        .map { request -> performRequest(request) }
        .collect { response -> println(response) }
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-08.kt)에서 확인할 수 있습니다.

이는 다음과 같이 3줄의 결과를 만들어 내며, 각 줄은 이전 줄로부터 1초 후에 나타난다.

```
response 1
response 2
response 3
```



### Transform 연산자

Flow의 변환 연산자들 중에서 가장 일반적인 것은 `transform`이다. 이는 `map`이나 `filter`와 같은 간단한 변환을 모방하거나 복잡한 변환들을 구현하는데 사용할 수 있다. `transform` 연산자를 사용하면 임의의 횟수 만큼 값을 `emit` 할 수 있다.

예를 들어, `transform`을 사용하면 오래걸리는 비동기 요청을 하기 전에 문자열을 방출(emit)하고 그 응답을 기다릴 수 있다.

```
(1..3).asFlow() // a flow of requests
    .transform { request ->
        emit("Making request $request") 
        emit(performRequest(request)) 
    }
    .collect { response -> println(response) }
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-09.kt)에서 확인할 수 있습니다.

이 코드의 결과는 다음과 같다:

```
Making request 1
response 1
Making request 2
response 2
Making request 3
response 3
```



### 크기 한정 연산자

`take`과 같은 크기 한정 중간 연산자들은 해당 임계치에 도달했을 때 flow의 실행을 취소한다. Coroutines의 취소는 언제나 Exception을 throw하여 수행되므로, `try { ... } finally { ... }` 같은 모든 리소스 관리를 위한 기능들은 취소에서 정상적으로 작동한다.

```kotlin
fun numbers(): Flow<Int> = flow {
    try {                          
        emit(1)
        emit(2) 
        println("This line will not execute")
        emit(3)    
    } finally {
        println("Finally in numbers")
    }
}

fun main() = runBlocking<Unit> {
    numbers() 
        .take(2) // take only the first two
        .collect { value -> println(value) }
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-10.kt)에서 확인할 수 있습니다.

`numbers()` 함수 내부의 `flow { ... }` body의 실행이 두 번째 숫자를 emit하고 멈추는 것을 이 코드의 결과에서 확실하게 볼 수 있다:

```
1
2
Finally in numbers
```

## Flow 터미널 연산자

Flow의 터미널 연산자는 flow를 수집을 시작하는 **일시정지 함수**이다. [collect](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/collect.html) 연산자는 가장 기본 연산자이지만, 사용을 더 쉽게 만드는 다른 터미널 연산자들도 있다.

* 다양한 Collection으로의 변환을 수행하는 [toList](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/to-list.html) 와 [toSet](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/to-set.html) 같은 연산자.
* 첫 값만 가져오기 위한 [first](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/first.html) 연산자와 하나의 값만 방출되는 것을 확인하는 [single](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/single.html) 연산자.
* flow를 값으로 줄이는 [reduce](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/reduce.html)나 [fold](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/fold.html)를 연산자.

예를 들어 :

```kotlin
val sum = (1..5).asFlow()
    .map { it * it } // squares of numbers from 1 to 5                           
    .reduce { a, b -> a + b } // sum them (terminal operator)
println(sum)
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-11.kt)에서 확인할 수 있습니다.

이는 하나의 숫자를 출력한다 :

```
55
```



## Flow는 순차적이다

여러 Flow들에서 작동하는 특수한 연산자를 사용하지 않는 한 각 개별 Flow의 컬렉션은 순차적으로 동작한다. 컬렉션은 터미널 연산자를 호출하는 Coroutine에서 직접 동작한다. 기본적으로 어떠한 새로운 Coroutines도 실행되지 않는다. 방출된 각 값들은 중간 연산자들에 의해 업스트림에서 다운스트림으로 처리된 후 터미널 연산자에게 전달된다.



정수 중 짝수를 필터링 한 후 문자열에 매핑하는 다음 예제를 참조하자 :&#x20;

```kotlin
(1..5).asFlow()
    .filter {
        println("Filter $it")
        it % 2 == 0              
    }              
    .map { 
        println("Map $it")
        "string $it"
    }.collect { 
        println("Collect $it")
    }
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-12.kt)에서 확인할 수 있습니다.

이는 다음 결과를 생성한다.

```
Filter 1
Filter 2
Map 2
Collect string 2
Filter 3
Filter 4
Map 4
Collect string 4
Filter 5
```



## Flow Context

Flow의 수집은 언제나 Coroutine을 호출하는 Context상에서 일어난다. 예를 들어 만약 `simple`이라 불리는 Flow가 있다면, 다음의 코드의 `simple` flow는 구체적인 구현과 상관없이 코드 작성자가 지정한 Context상에서 실행된다 :&#x20;

```kotlin
withContext(context) {
    simple().collect { value ->
        println(value) // run in the specified context
    }
}
```

Flow의 이러한 성질은 **컨텍스트 보존(context preservation)**이라 불린다.&#x20;

따라서 기본적으로 `flow { ... }` 빌더 내부의 코드는 해당 Flow의 collector가 제공하는 Context 상에서 실행된다. 예를 들어, `simple` 함수의 구현이 호출되는 스레드를 출력하고 3개의 숫자들을 방출한다고 해보자 :

```kotlin
fun simple(): Flow<Int> = flow {
    log("Started simple flow")
    for (i in 1..3) {
        emit(i)
    }
}  

fun main() = runBlocking<Unit> {
    simple().collect { value -> log("Collected $value") } 
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-13.kt)에서 확인할 수 있습니다.

이 코드를 실행하면 다음과 같은 출력이 나온다.

```
[main @coroutine#1] Started simple flow
[main @coroutine#1] Collected 1
[main @coroutine#1] Collected 2
[main @coroutine#1] Collected 3
```

`simple().collect`가 메인 스레드에서 호출되므로, `simple` flow의 body 또한 메인 스레드에서 호출된다. 이것은 실행 Context를 신경 쓰지 않고 호출자를 차단하지 않도록 하는 비동기 코드 혹은 빠르게 실행되는 코드에 대한 완벽한 기본값이다.



### withContext를 사용할 때 일반적으로 겪을 수 있는 함정

하지만 오래 걸리는 CPU를 사용하는 코드는 [Dispatchers.Default](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-dispatchers/-default.html) Context에서 실행되어야 할 수 있고, UI를 업데이트하는 코드는 [Dispatchers.Main](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-dispatchers/-main.html) Context에서 실행되어야 할 수 있다.

일반적으로 withContext는 Kotlin Coroutines를 사용하는 코드의 Context 변경하는데 사용되지만, `flow { ... }` 빌더의 코드는 컨텍스트 보존 특성을 준수해야해서 다른 컨텍스트에서 방출(emit)하는 것은 허용되지 않는다.

다음 코드를 실행해보자 :&#x20;

```kotlin
fun simple(): Flow<Int> = flow {
    // The WRONG way to change context for CPU-consuming code in flow builder
    kotlinx.coroutines.withContext(Dispatchers.Default) {
        for (i in 1..3) {
            Thread.sleep(100) // pretend we are computing it in CPU-consuming way
            emit(i) // emit next value
        }
    }
}

fun main() = runBlocking<Unit> {
    simple().collect { value -> println(value) } 
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-14.kt)에서 확인할 수 있습니다.

이 코드는 다음의 Exception을 생성한다.

```
Exception in thread "main" java.lang.IllegalStateException: Flow invariant is violated:
		Flow was collected in [CoroutineId(1), "coroutine#1":BlockingCoroutine{Active}@5511c7f8, BlockingEventLoop@2eac3323],
		but emission happened in [CoroutineId(1), "coroutine#1":DispatchedCoroutine{Active}@2dae0000, Dispatchers.Default].
		Please refer to 'flow' documentation or use 'flowOn' instead
	at ...
```



### flowOn 연산자

이 Exception은 Flow에서 값 방출을 위한 Context를 변경하는데 사용할 수 있는 `flowOn` 함수를 가리킨다. Flow의 Context를 변경하는 올바른 방법은 아래 예제에 나와있다. 또한 이는 해당 스레드들의 이름을 인쇄하여 이것이 어떻게 작동하는지를 보여준다.\`

```kotlin
fun simple(): Flow<Int> = flow {
    for (i in 1..3) {
        Thread.sleep(100) // pretend we are computing it in CPU-consuming way
        log("Emitting $i")
        emit(i) // emit next value
    }
}.flowOn(Dispatchers.Default) // RIGHT way to change context for CPU-consuming code in flow builder

fun main() = runBlocking<Unit> {
    simple().collect { value ->
        log("Collected $value") 
    } 
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-15.kt)에서 확인할 수 있습니다.

메인 스레드에서 수집이 일어날 때, `flow { ... }` 가 백그라운드 스레드에서 동작 방식하는 방식에 대해 주목하자 :&#x20;

여기서 관찰해야 하는 또 다른 사항은 `flowOn` 연산자가 Flow의 기본적인 순차처리 특성을 변경했다는 점이다. 현재 수집은 하나의 Coroutine("coroutine#1")에서 발생하고, 수집 Coroutine과 동시에 다른 스레드에서 실행중인 Coroutine("coroutine#2")에서 방출이 일어난다. `flowOn` 연산자는 Context에서 CoroutineDispatcher을 변경해야 할 때 업스트림 Flow를 위한 다른 코루틴을 생성한다.



## Buffering

다른 Coroutine 속의 Flow의 다른 부분들을 실행하는 것은, Flow를 수집하는데 걸리는 전체 시간의 관점에서 유용할 수 있다. 특히 오래 걸리는 비동기 작업이 포함된 경우에 유용하다. 예를 들어, `simple` Flow의 방출이 하나의 값을 방출하는데 100ms 이 걸릴 정도로 느리고 수집 또한 수집된 값을 처리하는데 300ms이 걸릴 정도로 느린 경우를 생각해보자. 세 개의 숫자를 방출하는 Flow에서 이러한 숫자들을 수집하는데 얼마나 많은 시간이 걸리는지 살펴보자.

```kotlin
fun simple(): Flow<Int> = flow {
    for (i in 1..3) {
        delay(100) // pretend we are asynchronously waiting 100 ms
        emit(i) // emit next value
    }
}

fun main() = runBlocking<Unit> { 
    val time = measureTimeMillis {
        simple().collect { value -> 
            delay(300) // pretend we are processing it for 300 ms
            println(value) 
        } 
    }   
    println("Collected in $time ms")
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-16.kt)에서 확인할 수 있습니다.

이는 전체 수집 작업이 1200ms(각각이 400ms 걸리는 세개의 숫자들) 정도 걸리고, 다음과 같은 결과를 생성한다.

```
1
2
3
Collected in 1220 ms
```

`buffer` 연산자를 Flow에 사용해, `simple` Flow의 방출 코드가 수집 코드와 순차적으로 실행되도록 하는 대신 동시에 실행되도록 할 수 있다.&#x20;

```kotlin
val time = measureTimeMillis {
    simple()
        .buffer() // buffer emissions, don't wait
        .collect { value -> 
            delay(300) // pretend we are processing it for 300 ms
            println(value) 
        } 
}   
println("Collected in $time ms")
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-17.kt)에서 확인할 수 있습니다.

첫 숫자를 위해 100ms만을 기다리고 다른 값들을 처리하는데 각각 300ms의 시간이 걸리는 효율적인 처리 파이프라인을 만들어, 같은 숫자들을 더 빠르게 생성한다. 이런 방식으로 실행하는데 1000ms 정도의 시간이 걸린다.

```
1
2
3
Collected in 1071 ms
```

> 📖  `flowOn` 연산자는 `CoroutineDispatcher`을 변경해야 할 때 동일한 buffering 메커니즘을 사용한다. 하지만 여기서는 실행 Context를 변경하지 않고 명시적으로 buffering을 요청한다.

### &#x20;Conflation

flow가 연산 혹은 연산의 상태 갱신에 대한 일부 결과를 나타내는 경우 각 값을 처리할 필요가 없이 최신값만을 처리하면 된다. 이러한 경우, 수집자가 너무 느리게 값들을 처리하는 경우 중간 발행 값들을 건너 뛰기 위해 [conflate](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/conflate.html) 연산자를 사용할 수 있다. 이전 예제 위에 만들어보자 :&#x20;

```kotlin
val time = measureTimeMillis {
    simple()
        .conflate() // conflate emissions, don't process each one
        .collect { value -> 
            delay(300) // pretend we are processing it for 300 ms
            println(value) 
        } 
}   
println("Collected in $time ms")
```

📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-18.kt)에서 확인할 수 있습니다.

첫 숫자가 처리되는 동안 두번째, 세번째 숫자가 이미 생성되어 두번째 숫자가 **합쳐져(conflated)** 가장 최근에 발행된 세번째 숫자가 수집기에 전달된 것을 확인할 수 있다.

```
1
3
Collected in 758 ms
```



### 최신 값 처리하기

결합(Conflation)은 방출기와 수집기 양쪽이 모두 느린 경우에 처리를 빠르게 하기 위해 사용할 수 있는 방법이다. 결합은 방출된 값들을 삭제하여 처리를 빠르게 한다. 다른 방법은 느린 수집기의 실행을 취소하고 새로운 값이 발행될 때마다 다시 시작하는 것이다. 필수 로직인 `xxx` 연산자와 동일한 연산을 수행하지만, 새로운 값이 발행되면 이전 코드를 취소하는 `xxxLatest` 연산자 집합이 있다. 이전 예제에서 [conflate](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/conflate.html)를 [collectLatest](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/collect-latest.html)로 변경해보자 :&#x20;

```kotlin
val time = measureTimeMillis {
    simple()
        .collectLatest { value -> // cancel & restart on the latest value
            println("Collecting $value") 
            delay(300) // pretend we are processing it for 300 ms
            println("Done $value") 
        } 
}   
println("Collected in $time ms")
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-19.kt)에서 확인할 수 있습니다.

`collectLatest`의 body가 300ms의 시간이 걸리는 반면 새로운 값은 100ms 마다 발행되기 때문에, 블록이 모든 값들에 대해 실행되지만 마지막 값에 대해서만 완료되는 것을 확인할 수 있다 :&#x20;

```
Collecting 1
Collecting 2
Collecting 3
Done 3
Collected in 741 ms
```

\
여러 Flow 하나로 합치기
---------------

복수의 Flow를 합치는 다양한 방법이 있다.



### Zip

Kotlin 표준 라이브러리 상의 [Sequence.zip](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.sequences/zip.html) 확장 함수처럼, Flow는 두 개의 Flow의 값을 결합하는 [zip](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/zip.html) 연산자를 가지고 있다.

```kotlin
val nums = (1..3).asFlow() // numbers 1..3
val strs = flowOf("one", "two", "three") // strings 
nums.zip(strs) { a, b -> "$a -> $b" } // compose a single string
    .collect { println(it) } // collect and print
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-20.kt)에서 확인할 수 있습니다.

이 예제는 다음을 출력한다.

```
1 -> one
2 -> two
3 -> three
```



### Combine

Flow가 가장 최신의 값 혹은 연산을 표시할 때([conflation](https://translatordev.com/46)에 관한 관련된 섹션 참조), 해당 Flow의 가장 최신 값에 의존적인 연산의 수행을 필요로 하거나 업스트림이 새로운 값을 방출 할 때 다시 연산하도록 해야할 수 있다. 해당 연산을 수행하는 연산자의 집합을 [combine](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/combine.html)이라 부른다.

예를 들어, 이전 예제에서 숫자들이 300ms 마다 업데이트 되지만 문자열이 400ms마다 업데이트 되는 경우, 그들을 [zip](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/zip.html) 연산자를 사용해 zip연산을 수행하면 결과가 400ms마다 출력되기는 하지만 동일한 결과가 생성된다.

> 📖  이 예제에서는 [onEach](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/on-each.html) 중간 연산자를 사용해 각 요소들에 대해 지연을 주도록 하고, 샘플 Flow를 방출하는 코드를 선언적이고 짧게 만든다.

```kotlin
val nums = (1..3).asFlow().onEach { delay(300) } // numbers 1..3 every 300 ms
val strs = flowOf("one", "two", "three").onEach { delay(400) } // strings every 400 ms
val startTime = System.currentTimeMillis() // remember the start time 
nums.zip(strs) { a, b -> "$a -> $b" } // compose a single string with "zip"
    .collect { value -> // collect and print 
        println("$value at ${System.currentTimeMillis() - startTime} ms from start") 
    }
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-21.kt)에서 확인할 수 있습니다.



하지만, `zip` 대신에 `combine`을 사용해보면 :

```kotlin
val nums = (1..3).asFlow().onEach { delay(300) } // numbers 1..3 every 300 ms
val strs = flowOf("one", "two", "three").onEach { delay(400) } // strings every 400 ms          
val startTime = System.currentTimeMillis() // remember the start time 
nums.combine(strs) { a, b -> "$a -> $b" } // compose a single string with "combine"
    .collect { value -> // collect and print 
        println("$value at ${System.currentTimeMillis() - startTime} ms from start")
    }
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-22.kt)에서 확인할 수 있습니다.

`nums` 또는 `strs` Flow의 각 방출에 따라 줄이 인쇄되는 상당히 다른 결과를 얻을 수 있다

```
1 -> one at 452 ms from start
2 -> one at 651 ms from start
2 -> two at 854 ms from start
3 -> two at 952 ms from start
3 -> three at 1256 ms from start
```

## Flow를 Flatten하기

Flow는 비동기적으로 수신된 값의 시퀀스를 나타내고, 각 값이 다른 값들의 시퀀스에 대한 요청을 하기 매우 쉽다. 예를 들어, 두개의 문자열을 500ms 차이로 반환하는 다음의 함수가 있다고 해보자 :

```kotlin
fun requestFlow(i: Int): Flow<String> = flow {
    emit("$i: First")
    delay(500) // wait 500 ms
    emit("$i: Second")
}
```

만약 3개의 정수를 방출하는 flow가 있고, 다음과 같이 각각이 `requestFlow`를 호출한다고 해보자.

```kotlin
(1..3).asFlow().map { requestFlow(it) }
```

이는 추후 처리를 위해 단일 Flow로 **Flatten**해야 하는 flow의 flow(`Flow<Flow<String>>`)가 된다. Collection과 Sequence는 이런 상황을 위해 `flatten`과 `flatMap` 연산자가 있다. 하지만, Flow의 비동기 환경 때문에 Flow는 flattening을 위한 다른 **방법**이 필요하며, Flow에 대한 flattening 연산자의 집합이 존재한다.



### flatMapConcat

[flatMapConcat](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/flat-map-concat.html)과 [flattenConcat](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/flatten-concat.html)은 flow의 flow에 대한 연결을 제공한다. 이들은 해당 시퀀스 연산자들과 가장 직접적인 유사체이다. 이들은 다음 예제처럼 새로운 값을 수집하기 전에 안쪽의 Flow의 처리가 완료되기를 기다린다.

```kotlin
val startTime = System.currentTimeMillis() // remember the start time 
(1..3).asFlow().onEach { delay(100) } // emit a number every 100 ms 
    .flatMapConcat { requestFlow(it) }                                                                           
    .collect { value -> // collect and print 
        println("$value at ${System.currentTimeMillis() - startTime} ms from start") 
    }
```

📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-23.kt)에서 확인할 수 있습니다.

`flatMapConcat`의 순차적인 성질은 출력에서 명확하게 드러난다 :&#x20;

```
1: First at 121 ms from start
1: Second at 622 ms from start
2: First at 727 ms from start
2: Second at 1227 ms from start
3: First at 1328 ms from start
3: Second at 1829 ms from start
```



### flatMapMerge

다른 flattening 연산 방식은 수집되는 값을 모두 동시적으로 수집한 후, 수집된 값들을 하나의 Flow로 만들어 값이 최대한 빠르게 방출될 수 있도록 하는 것이다. 이는 [flatMapMerge](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/flat-map-merge.html), [flattenMerge](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/flatten-merge.html) 연산자에 의해 구현된다. 이 둘 모두 선택적으로 `concurrency` 파라미터를 받아 동시에 수집되는 Flows의 수를 제한할 수 있도록 한다(이 값은 기본값이 [DEFAULT\_CONCURRENCY](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-d-e-f-a-u-l-t\_-c-o-n-c-u-r-r-e-n-c-y.html)  로 설정된다).

```kotlin
val startTime = System.currentTimeMillis() // remember the start time 
(1..3).asFlow().onEach { delay(100) } // a number every 100 ms 
    .flatMapMerge { requestFlow(it) }                                                                           
    .collect { value -> // collect and print 
        println("$value at ${System.currentTimeMillis() - startTime} ms from start") 
    }
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-24.kt)에서 확인할 수 있습니다.

flatMapMerge의 동시적인 성질은 명확하게 드러난다 :

```
1: First at 136 ms from start
2: First at 231 ms from start
3: First at 333 ms from start
1: Second at 639 ms from start
2: Second at 732 ms from start
3: Second at 833 ms from start
```

> 📖  `flatMapMerge`는 내부의 코드 블록(이 예제에서는 `{ requestFlow(it) }`)을 순차적으로 호출하지만 결과 flow를 동시적으로 수집한다. 이는 `map { requestFlow(it) }` 를 먼저 호출하고 `flattenMerge`를 순차적으로 호출하는 것과 같다.



### flatMapLatest

[최신 값 처리하기](flow.md#undefined-3) 섹션에서 설명한 [collectLatest](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/collect-latest.html) 연산자와 비슷하게, 새로운 flow의 Collection이 방출되면 이전 flow의 Collection의 처리가 취소되는 "최신(Latest)" flattening 방식이 있다. 이는 [flatMapLatest](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/flat-map-latest.html) 연산자에 의해 구현된다.  &#x20;

```kotlin
val startTime = System.currentTimeMillis() // remember the start time 
(1..3).asFlow().onEach { delay(100) } // a number every 100 ms 
    .flatMapLatest { requestFlow(it) }                                                                           
    .collect { value -> // collect and print 
        println("$value at ${System.currentTimeMillis() - startTime} ms from start") 
    }
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-25.kt)에서 확인할 수 있습니다.

이 예제의 출력은 flatMapLatest가 어떻게 동작하는지에 대한 좋은 설명이 된다.

```
1: First at 142 ms from start
2: First at 322 ms from start
3: First at 425 ms from start
3: Second at 931 ms from start
```

> 📖  새로운 값이 수집되었을 때, `flatMapLatest`는 블록(이 예제에서는 `{ requestFlow(it) }`) 내부의 모든 코드를 취소한다. 이는 이 예제에서는 `requestFlow`가 일시중단되지 않고 취소되지 않도록 빠르게 호출되기 때문에 아무런 변화를 만들어내지 못한다. 하지만 `requestFlow` 내부에 `delay`와 같은 일시중단 함수가 있다면 달라진 결과가 보일 것이다.



## Flow 예외 처리

Flow 수집은 방출하는 곳 혹은 연산자 안의 코드가 예외를 발생시키는 경우 예외와 함께 완료될 수 있다. 예외들을 처리할 수 있는 몇가지 방법이 있다.

### 수집기에서의 try와 catch

수집기는 예외를 처리하기 위해  Kotlin의 `try/catch` 블록을 사용할 수 있다 :

```kotlin
fun simple(): Flow<Int> = flow {
    for (i in 1..3) {
        println("Emitting $i")
        emit(i) // emit next value
    }
}
​
fun main() = runBlocking<Unit> {
    try {
        simple().collect { value ->         
            println(value)
            check(value <= 1) { "Collected $value" }
        }
    } catch (e: Throwable) {
        println("Caught $e")
    } 
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-26.kt)에서 확인할 수 있습니다.

이 코드는 `collect` 터미널 연산자 안에서 성공적으로 예외를 잡아내며, 그 뒤로 더이상 값이 방출되지 않는다 :

```
Emitting 1
1
Emitting 2
2
Caught java.lang.IllegalStateException: Collected 2
```



### 모든 것이 잡힌다

이전 예제는 방출기나 중간 혹은 터미널 연산자 안에서의 예외들을 모두 잡아낸다. 예를 들어, 방출된 값들이 문자열로 매핑되도록 코드를 바꿔도 코드는 예외를 발생시킨다.&#x20;

```kotlin
fun simple(): Flow<String> = 
    flow {
        for (i in 1..3) {
            println("Emitting $i")
            emit(i) // emit next value
        }
    }
    .map { value ->
        check(value <= 1) { "Crashed on $value" }                 
        "string $value"
    }

fun main() = runBlocking<Unit> {
    try {
        simple().collect { value -> println(value) }
    } catch (e: Throwable) {
        println("Caught $e")
    } 
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-27.kt)에서 확인할 수 있습니다.

예외는 여전히 잡혀서 수집을 중단시킨다.

```
Emitting 1
string 1
Emitting 2
Caught java.lang.IllegalStateException: Crashed on 2
```

***



***

## 예외 투명성(Exception Transparency)

그러면 어떻게 방출기의 코드가 예외 처리 동작을 캡슐화 할 수 있을까?

Flow는 예외에 투명해야 하고, `try/catch` 블록 내부에서 `flow { ... }` 빌더의 값을 방출하는 것은 예외 투명성을 위반하는 것이다. 이렇게 하면 예외를 발생시키는 수집기 이전 예제와 같이 언제나 `try/catch`를 사용해 예외를 잡아낼 수 있다.

방출기는 `catch` 연산자를 사용해 예외 투명성을 유지시키고 예외 처리를 캡슐화 할 수 있다. `catch` 연산자의 body는 예외를 분석하고, 잡은 예외에 따라 다른 방식으로 대응할 수 있다.

* 예외는 `throw`를 사용해 다시 throw 될 수 있다.
* `catch`의 body에서 `emit`을 사용해 값을 방출함으로써, 예외를 방출로 바꿀 수 있다.
* 예외는 무시되거나, 로깅되거나, 다른 코드로 처리될 수 있다.

예를 들어, 예외를 잡는 부분에서 텍스트를 방출하도록 해보자 :&#x20;

```kotlin
simple()
    .catch { e -> emit("Caught $e") } // emit on exception
    .collect { value -> println(value) }
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-28.kt)에서 확인할 수 있습니다.

### &#x20;투명한 catch

예외 투명도를 존중하는 [catch](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/catch.html) 중간 연산자는 업스트림 예외만을 잡아낸다(`catch` 윗 부분의 연산자들에서의 예외만을 잡아내고 아래 부분의 예외는 잡아내지 않는다). 만약 `collect { ... }` 내부의 블록(`catch` 아래 부분의 코드)이 예외를 발생시키면 예외를 잡아내지 않는다.

```kotlin
fun simple(): Flow<Int> = flow {
    for (i in 1..3) {
        println("Emitting $i")
        emit(i)
    }
}

fun main() = runBlocking<Unit> {
    simple()
        .catch { e -> println("Caught $e") } // does not catch downstream exceptions
        .collect { value ->
            check(value <= 1) { "Collected $value" }                 
            println(value) 
        }
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-29.kt)에서 확인할 수 있습니다.



`catch` 연산자가 있음에도 "Caught ..." 메세지가 출력되지 않는다.

```
Emitting 1
1
Emitting 2
Exception in thread "main" java.lang.IllegalStateException: Collected 2
	at ...
```



### 선언적으로 잡기&#x20;

`collect` 연산자의 body를 `onEach` 내부로 이동하고 `catch` 연산자를 그 이후에 위치 시킴으로써, `catch` 연산자의 선언적인 성질을 모든 예외들을 처리하기 위한 욕구와 결합할 수 있다. `collect()`를 파라미터 없이 사용함으로써 Flow의 수집을 발생시킬 수 있다.

```kotlin
simple()
    .onEach { value ->
        check(value <= 1) { "Collected $value" }                 
        println(value) 
    }
    .catch { e -> println("Caught $e") }
    .collect()
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-30.kt)에서 확인할 수 있습니다.



이제 "Caught ..." 메세지가 출력되는 것을 확인할 수 있고, 명시적으로 `try/catch` 블록을 사용하지 않고도 모든 예외들을 잡아낼 수 있다.

```
Emitting 1
1
Emitting 2
Caught java.lang.IllegalStateException: Collected 2
```



## Flow 수집 완료 처리하기

flow 수집이 완료되면(정상적으로 혹은 예외가 발생되어서), 완료에 따른 동작을 실행해야 할 수 있다. 이미 알 수도 있듯이, 이는 명령적인 방식 혹은 선언적인 방식 두가지 방식으로 실행될 수 있다.

### &#x20;명령적인 finally 블록

`try`/`catch`에 더해서, 수집기는 `collect` 동작이 완료됨에 따라 동작을 실행하는 `finally` 블록을 사용할 수 있다.

```kotlin
fun simple(): Flow<Int> = (1..3).asFlow()

fun main() = runBlocking<Unit> {
    try {
        simple().collect { value -> println(value) }
    } finally {
        println("Done")
    }
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-31.kt)에서 확인할 수 있습니다.



이 코드는 `simple` flow에 의해 생성되는 3개의 숫자를 프린트하고 마지막에 "Done" 문자열을 출력한다 :&#x20;

```
1
2
3
Done
```



### 선언적인 처리

선언적으로 접근하면, flow는 flow의 수집이 완료되었을 때 실행되는 onCompletion 중간 연산자가 있다.

이전 예제를 `onCompletion` 연산자를 사용해 다시 작성할 수 있고, 이는 같은 결과를 출력한다.

```kotlin
simple()
    .onCompletion { println("Done") }
    .collect { value -> println(value) }
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-32.kt)에서 확인할 수 있습니다.

onCompletion의 중요한 이점은 수집 작업이 정상적으로 혹은 예외적으로 완료되었는지를 확인하는데 사용할 수 있는 람다식의 nullable한 `Throwable` 파라미터이다. 다음 예제에서 `simple` flow는 1을 방출한 다음 예외를 발생시킨다 :

```kotlin
fun simple(): Flow<Int> = flow {
    emit(1)
    throw RuntimeException()
}

fun main() = runBlocking<Unit> {
    simple()
        .onCompletion { cause -> if (cause != null) println("Flow completed exceptionally") }
        .catch { cause -> println("Caught exception") }
        .collect { value -> println(value) }
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-33.kt)에서 확인할 수 있습니다.

예측 했듯이, 이는 다음 결과를 출력한다 :&#x20;

```
1
Flow completed exceptionally
Caught exception
```

`catch`와는 다르게 `onCompletion` 연산자는 예외를 처리하지 않는다. 위의 예제 코드에서 확인할 수 있듯이 예외는 여전히 다운스트림으로 흐른다.  이는 이후의 `onCompletion` 연산자로 전달되며, `catch` 연산자를 사용해 처리될 수 있다.



### 성공적인 완료

`catch` 연산자와 또 다른 점은 `onCompletion`은 모든 예외를 볼 수 있고, 업스트림 flow가 취소나 실패 없이 성공적으로 완료되었을 때 `null` 예외를 수신한다는 것이다.

```kotlin
fun simple(): Flow<Int> = (1..3).asFlow()

fun main() = runBlocking<Unit> {
    simple()
        .onCompletion { cause -> println("Flow completed with $cause") }
        .collect { value ->
            check(value <= 1) { "Collected $value" }                 
            println(value) 
        }
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-34.kt)에서 확인할 수 있습니다.



다운스트림 예외로 인해 Flow가 중단되었기 때문에, 완료의 원인이 null이 아닌 것을 확인할 수 있다 :

```
1
Flow completed with java.lang.IllegalStateException: Collected 2
Exception in thread "main" java.lang.IllegalStateException: Collected 2
```



## 명령적으로 다루기 vs 선언적으로 다루기

이제 우리는 어떻게 Flow를 수집하고, 명령적인 방식과 선언적인 방식으로 완료와 예외를 처리하는 방법을 안다. 자연적으로 어떤 접근 방식이 선호되고 왜 그런지에 대한 의문이 생길 것이다. 이에 대해 라이브러리적인 관점에서 특정한 접근 방식만을 옹호하지 않는다. 두 접근 방식 모두 유효하며, 선호도와 코스 스타일에 따라 선택되어야 한다.



## Flow 실행하기

일부 소스에서 오는 비동기 이벤트를 표현하기 위해 flow를 사용하기 쉽다. 이런 경우, 들어오는 이벤트에 대한 반응을 코드로 등록하고 이후의 작업을 계속해서 수행하도록 하는 `addEventListener` 함수와 비슷한 역할을 하는 것이 필요하다. 이 역할을 `onEach` 연산자가 해줄 수 있다. 그러나, `onEach`는 중간 연산자이다. Flow를 수집하기 위해서는 터미널 연산자 또한 필요하다. 그렇지 않으면 `onEach`만을 호출하는 것만으로는 효과가 없다.



만약 `onEach` 이후에 `collect` 터미널 연산자를 사용하면, 이후의 코드는 Flow가 수집될 때까지 기다릴 것이다 :

```kotlin
// Imitate a flow of events
fun events(): Flow<Int> = (1..3).asFlow().onEach { delay(100) }

fun main() = runBlocking<Unit> {
    events()
        .onEach { event -> println("Event: $event") }
        .collect() // <--- Collecting the flow waits
    println("Done")
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-35.kt)에서 확인할 수 있습니다.

확인할 수 있듯이 다음과 같이 출력된다 :

```
Event: 1
Event: 2
Event: 3
Done
```

`launchIn` 터미널 연산자가 여기서 편리하게 사용될 수 있다. `collect`를 `launchIn`으로 대체함으로써 Flow의 수집을 별도의 Coroutine에서 실행할 수 있으므로, 이후의 코드들이 즉시 계속해서 실행될 수 있다.



```
fun main() = runBlocking<Unit> {
    events()
        .onEach { event -> println("Event: $event") }
        .launchIn(this) // <--- Launching the flow in a separate coroutine
    println("Done")
}
```

📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-36.kt)에서 확인할 수 있습니다.

이는 다음과 같이 출력된다 :

```
Done
Event: 1
Event: 2
Event: 3
```

`launchIn`에서 필요로 하는 `CoroutineScope` 파라미터는 CoroutineScope을 특정해 Flow가 실행되면 어떤 Coroutine이 수집을 할지를 결정하도록 한다. 위의 예제에서 이 Scope는 `runBlocking` Coroutine 빌더로부터 와서, flow가 실행되는 동안 runBlocking Scope가 자식 코루틴이 완료될 때까지 기다리도록 하고 main 함수를 반환하는 것을 방지해서 예제가 종료되지 않도록 한다.

실제 어플리케이션들에서는 한정된 생애를 가진 엔티티로부터 Scope를 가져온다 엔터티의 생애가 종료되는 순간 해당 Scope는 취소되며, 해당 Flow의 수집은 중단된다. 이러한 방식으로 `onEach { ... }.launchIn(scope)` 쌍은 `addEventListener`과 같이 동작한다. 하지만, 취소와 구조화된 동시성이 `removeEventListener` 함수에 해당하는 역할을 대신 수행해주기 때문에 필요 없다.

`launchIn` 또한 전체 Scope을 `cancel`하거나 `join`하지 않고 해당 Flow를 수집하는 Coroutine만을 `cancel`하기 위해 사용할 수 있는 `Job`을 반환한다는 점을 명심하자.



### Flow 취소 체크

편의를 위해 `flow` 빌더는 추가적으로 방출된 각 값에 대한 취소 동작을 하기 위한 [ensureActive](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/ensure-active.html) 체크를 수행한다. 이는 `flow { ... }` 에서 루프를 돌면서 바쁘게 방출되는 것이 취소 가능하다는 것을 뜻한다:

```kotlin
fun foo(): Flow<Int> = flow { 
    for (i in 1..5) {
        println("Emitting $i") 
        emit(i) 
    }
}

fun main() = runBlocking<Unit> {
    foo().collect { value -> 
        if (value == 3) cancel()  
        println(value)
    } 
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-37.kt)에서 확인할 수 있습니다.

숫자를 3까지만 소모하고 4를 방출한 다음에 CancellationException이 발생한다 :

```
Emitting 1
1
Emitting 2
2
Emitting 3
3
Emitting 4
Exception in thread "main" kotlinx.coroutines.JobCancellationException: BlockingCoroutine was cancelled; job="coroutine#1":BlockingCoroutine{Cancelled}@6d7b4f4c
```

하지만, 다른 대부분의 Flow 연산자들은 성능상의 이유로 추가적인 취소 체크를 하지 않는다. 예를 들어, 만약 `IntRange.asFlow` 확장 함수를 같은 바쁜 루프를 작성하기 위해 사용하고 아무 곳에서도 일시 중단 하지 않는다면, 취소를 위한 체크는 일어나지 않는다.

```kotlin
fun main() = runBlocking<Unit> {
    (1..5).asFlow().collect { value -> 
        if (value == 3) cancel()  
        println(value)
    } 
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-38.kt)에서 확인할 수 있습니다.

1부터 5까지의 모든 숫자들이 수집되고, `runBlocking`이 반환되기 전에만 취소가 감지된다.

```
1
2
3
4
5
Exception in thread "main" kotlinx.coroutines.JobCancellationException: BlockingCoroutine was cancelled; job="coroutine#1":BlockingCoroutine{Cancelled}@3327bd23
```



**바쁜 Flow를 취소 가능하게 만들기**

Coroutine에 바쁜 루프가 존재한다면 명시적으로 취소를 체크해야 한다. `.onEach { currentCoroutineContext().ensureActive() }` 를 추가할 수도 있지만 `cancellable` 연산자가 해당 역할을 수행하기 위해 이미 정의되어 있다 :

```kotlin
fun main() = runBlocking<Unit> {
    (1..5).asFlow().cancellable().collect { value -> 
        if (value == 3) cancel()  
        println(value)
    } 
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-flow-39.kt)에서 확인할 수 있습니다.

`cancellable` 연산자를 사용하면 1부터 3까지의 숫자들만 수집된다 :&#x20;

```
1
2
3
Exception in thread "main" kotlinx.coroutines.JobCancellationException: BlockingCoroutine was cancelled; job="coroutine#1":BlockingCoroutine{Cancelled}@5ec0a365
```

## Flow와 Reactive Stream

[리액티브 스트림](https://www.reactive-streams.org/)이나 Rxjava나 Project Reactor 같은 리액티브 프레임웍에 익숙한 사람들은 Flow를 설계 하는게 아주 익숙할 것이다.

실제로, Flow의 설계는 리액티브 스트림과 그에 대한 다양한 구현체들에 영감을 받았다. 하지만, Flow의 주요 목표는 가능한 단순하게 디자인을 하는 것이며, Kotlin의 일시중단 친화적이고 구조적인 동시성을 존중하는 것이다. 이러한 목표를 이루는 것은 리액티브 선지자과 그들의 엄청난 작업들이 없으면 불가능할 것이다. 이에 대한 완전한 이야기는 [Reactive Streams and Kotlin Flows](https://medium.com/@elizarov/reactive-streams-and-kotlin-flows-bfd12772cda4) 기사에서 읽을 수 있다.

개념적으로는 다르지만, Flow는 리액티브 스트림이며 Flow는 리액티브(사양과 TCK에 대해 호환되는) 발행자 또는 그 반대로 변환될 수 있다. 이러한 변환기는 기본적으로 `kotlinx.coroutines` 패키지에 의해 제공되며, 다른 리액티브 모듈에 대한 변환기는 해당 리액티브 모듈에서 찾을 수 있다(리액티브 스트림을 위한 `kotlinx-coroutines-reactive`, Project Reactor을 위한 `kotlinx-coroutines-reactor` 과 RxJava2/RxJava3를 위한 `kotlinx-coroutines-rx2`/`kotlinx-coroutines-rx3`).
