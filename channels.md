---
description: '원문 최종 수정 :  2023년 6월 20일'
---

# Channels

> [페이지 편집](channels.md)



Deffered 값은 두 코루틴 사이에 단일 값을 전달하는데 편리한 방법을 제공한다. Channel은 값의 스트림을 전달하는 방법을 제공한다.



## Channel 기초

`Channel`은 개념적으로 `BlockingQueue`와 매우 유사하다. 주요한 다른점은 블로킹 연산인 `put` 대신 일시중단 연산인 `send`를 가지고, 블로킹 연산인 `take` 대신 일시중단 연산인 `receive`를 가진다는 점이다.

```kotlin
val channel = Channel<Int>()
launch {
    // this might be heavy CPU-consuming computation or async logic, we'll just send five squares
    for (x in 1..5) channel.send(x * x)
}
// here we print five received integers:
repeat(5) { println(channel.receive()) }
println("Done!")
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-channel-01.kt)에서 확인할 수 있습니다.

코드의 출력은 다음과 같다

```
1
4
9
16
25
Done!
```



## Channel 닫기와 반복적으로 수신하기

Channel은 Queue와 다르게 더이상 다른 원소들이 오지 않는 다는 것을 알리기 위해 닫힐 수 있다. Channel로부터 값을 받는쪽에서는 일반적인 `for` 루프를 사용해서 원소를 편하게 받을 수 있다.

개념적으로 `close` 함수는 채널로 특별한 닫기 토큰을 보내는 것과 같다. 이 닫기 토큰을 받으면 반복이 멈춘다. 따라서 닫기 토큰을 받기 전에 보내진 모든 원소들이 수신되었음을 보장할 수 있다.

```kotlin
val channel = Channel<Int>()
launch {
    for (x in 1..5) channel.send(x * x)
    channel.close() // we're done sending
}
// here we print received values using `for` loop (until the channel is closed)
for (y in channel) println(y)
println("Done!")
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-channel-02.kt)에서 확인할 수 있습니다.

***

## Producer로 Channel 만들기

Coroutine이 원소들의 시퀀스를 생성하는 패턴은 매우 일반적이다. 이는 동시성 코드에서 자주 발견되는 **생산자-소비자 패턴**의 일부이다. 생산자를 채널을 파라미터로 받는 함수로 추상화 할 수 있지만, 이는 함수로부터 결과가 반환되어야 한다는 상식과 맞지 않는다.

생산자측에서는 `producer`라는 이름을 가진 편리한 코루틴 빌더를 통해 이를 간단하게 할 수 있고, 소비자측의 for 루프를 `consumeEach` 확장 함수를 사용해 대체할 수 있다.

```kotlin
fun CoroutineScope.produceSquares(): ReceiveChannel<Int> = produce {
    for (x in 1..5) send(x * x)
}

fun main() = runBlocking {
    val squares = produceSquares()
    squares.consumeEach { println(it) }
    println("Done!")
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-channel-03.kt)에서 확인할 수 있습니다.

***

## 파이프라인

파이프라인은 하나의 Coroutine이 값의 스트림을 생성하는 것을 뜻한다. 값의 스트림은 무한할 수도 있다.

```kotlin
fun CoroutineScope.produceNumbers() = produce<Int> {
    var x = 1
    while (true) send(x++) // infinite stream of integers starting from 1
}
```

그리고 다른 Coroutine이나 Coroutines 들이 그 스트림을 소비하고, 작업을 수행하고, 다른 결과를 생성한다. 아래의 예시에서 숫자들은 단순히 제곱된다.

```kotlin
fun CoroutineScope.square(numbers: ReceiveChannel<Int>): ReceiveChannel<Int> = produce {
    for (x in numbers) send(x * x)
}
```

메인 코드는 모든 파이프라인을 연결하기 시작한다.

```kotlin
val numbers = produceNumbers() // produces integers from 1 and on
val squares = square(numbers) // squares integers
repeat(5) {
    println(squares.receive()) // print first five
}
println("Done!") // we are done
coroutineContext.cancelChildren() // cancel children coroutines
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-channel-04.kt)에서 확인할 수 있습니다.

> 📖  Coroutine을 생성하는 모든 함수들은 [CoroutineScope](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-coroutine-scope/)의 확장함수로 정의되어 있다. 이를 통해 [구조화된 동시성](https://kotlinlang.org/docs/composing-suspending-functions.html#structured-concurrency-with-async)의 원칙에 의존하도록 해서 어플리케이션에 글로벌하게 남아있는 코루틴이 없도록 할 수 있다.



## 파이프라인으로 소수 만들기

Coroutine의 파이프라인을 사용해서 소수를 생성하는 예제를 통해 파이프라인을 극한으로 사용해보겠다. 숫자의 무한한 시퀀스로 시작해보자 :

```kotlin
fun CoroutineScope.numbersFrom(start: Int) = produce<Int> {
    var x = start
    while (true) send(x++) // infinite stream of integers from start
}
```

다음 파이프라인 단계에서는 들어오는 숫자의 스트림을 필터링해서, 주어진 소수로 나눌 수 있는 모든 숫자들을 제거한다.

```kotlin
fun CoroutineScope.filter(numbers: ReceiveChannel<Int>, prime: Int) = produce<Int> {
    for (x in numbers) if (x % prime != 0) send(x)
}
```

이제 숫자 스트림을 2에서 부터 시작하고, 현재 Channel에서 소수를 가져오고, 각 발견된 소수에 대해 새로운 파이프라인 단계를 실행하는 새로운 파이프라인을 구축한다.

```
numbersFrom(2) -> filter(2) -> filter(3) -> filter(5) -> filter(7) ...
```

다음 예제는 메인 스레드의 Context에서 모든 파이프라인을 실행해서, 첫 10개의 소수를 출력한다. Scope 내의 모든 Coroutine이 main 함수의 `runBlocking` Coroutine에서 실행되었으므로 시작한 코루틴들의 명시적인 리스트를 가지고 있을 필요가 없다. 처음 10개의 소수를 출력한 후, `cancelChildren` 확장 함수를 이용해서 모든 자식 Coroutine을 취소한다.

```kotlin
var cur = numbersFrom(2)
repeat(10) {
    val prime = cur.receive()
    println(prime)
    cur = filter(cur, prime)
}
coroutineContext.cancelChildren() // cancel all children to let main finish
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-channel-05.kt)에서 확인할 수 있습니다.

코드의 출력은 다음과 같다 :

```
2
3
5
7
11
13
17
19
23
29
```



표준 라이브러리의 `iterator` Coroutine 빌더를 사용해 같은 파이프라인을 빌드할 수 있다. `produce`를 `iterator`로 `send`를 `yield`로, `receive`를 `next`로, `ReceiveChannel`을 `iterator`로 바꾸고 Coroutine Scope을 제거하자. `runBlocking` 또한 필요 없어졌다. 하지만, 위에서 다룬 채널을 사용하는 파이프라인의 이점은 Dispatcher.Default Context 상에서 실행할 경우 복수의 CPU 코어를 사용할 수 있다는 점이다.

어쨌든, 이것은 소수를 찾는 매우 비현실적인 방법이다. 실제로 파이프라인은 다른 원격 서비스에 대한 비동기 호출과 같은 일시중단 호출이 포함되며, 이 파이프라인은 `sequence` / `iterator` 을 사용해서 만들어질 수 없다. 완전히 비동기적인 `produce`와 달리 임의의 일시 중단을 포함할 수 없기 때문이다.

\


## Fan-out

복수의 Coroutine은 같은 채널로부터 수신하면서, 그들간에 작업을 분산할 수 있다. 1초에 10개의 숫자를 주기적으로 정수를 생성하는 생산자 Coroutine으로 시작하자 :

```kotlin
fun CoroutineScope.produceNumbers() = produce<Int> {
    var x = 1 // start from 1
    while (true) {
        send(x++) // produce next
        delay(100) // wait 0.1s
    }
}
```

그러면 우리는 몇개의 프로세서 Coroutine을 가질 수 있다. 이 예에서 프로세서 Coroutine은 그들의 id와 받은 숫자를 출력한다.

```kotlin
fun CoroutineScope.launchProcessor(id: Int, channel: ReceiveChannel<Int>) = launch {
    for (msg in channel) {
        println("Processor #$id received $msg")
    }
}
```

이제 5개의 프로세서들을 실행하고 거의 1초간 동작하도록 하자. 어떤 일이 일어나는지 확인하자 :

```kotlin
val producer = produceNumbers()
repeat(5) { launchProcessor(it, producer) }
delay(950)
producer.cancel() // cancel producer coroutine and thus kill them all
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-channel-06.kt)에서 확인할 수 있습니다.

특정 정수를 수신하는 프로세서의 id값이 다를 수 있지만, 출력은 다음과 비슷할 것이다.

```
Processor #2 received 1
Processor #4 received 2
Processor #0 received 3
Processor #1 received 4
Processor #3 received 5
Processor #2 received 6
Processor #4 received 7
Processor #0 received 8
Processor #1 received 9
Processor #3 received 10
```

생산자 Coroutine을 취소하는 것은 생산자 Coroutine의 채널을 닫는다. 그러므로 실제로 프로세서 Coroutine이 수행하는 채널의 반복을 종료한다.

또한, `launchProcessor` 코드에서 Fan-out을 수행하기 위해 명시적으로 `for` 루프를 사용해서 채널에 대해 반복을 수행한 방법에 주목하자. 이 `for` 루프 패턴은 `consumeEach`와 다르게 복수의 Coroutine에 사용하기 완전히 안전하다. 만약 하나의 프로세서 Coroutine이 실패하면 다른 Coroutine이 채널에 대한 처리를 할 것이다. 반면에, `consumerEach`를 사용해 작성된 프로세서는 정상적으로 혹은 비정상적으로 완료될 때 언제나 해당 채널을 소비(취소)한다.



## Fan-in

복수의 Coroutine이 같은 채널에 값을 보낼 수 있다. 예를 들어, 문자열을 다루는 채널이 있다고 해보고, 일정한 시간마다 채널로 특정한 문자열을 반복적으로 보내는 일시중단 함수가 있다고 해보자.

```kotlin
suspend fun sendString(channel: SendChannel<String>, s: String, time: Long) {
    while (true) {
        delay(time)
        channel.send(s)
    }
}
```

이제, 문자열을 보내는 몇개의 Coroutine을 실행하면 어떤 일이 발생하는지 확인해보자(이 예에서는 메인 스레드의 Context에서 메인 Coroutine의 자식으로 Coroutine들을 실행한다) :

```kotlin
val channel = Channel<String>()
launch { sendString(channel, "foo", 200L) }
launch { sendString(channel, "BAR!", 500L) }
repeat(6) { // receive first six
    println(channel.receive())
}
coroutineContext.cancelChildren() // cancel all children to let main finish
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-channel-07.kt)에서 확인할 수 있습니다.

출력은 다음과 같다 :

```
foo
foo
BAR!
foo
foo
BAR!
```

\


## Buffered channels

지금까지 보여진 Channel에는 Buffer가 없다. Buffer되지 않은 채널은 발신자와 수신자가 서로 만날 때 값을 전송한다. 이는 랑데뷰라고도 불린다. 만약 send가 먼저 실행되면, receive가 실행될 때까지 일시 중단된다. 만약 receive가 먼저 실행되면, send가 실행될 때까지 일시 중단된다.

`Channel()` 팩토리 함수와 `produce` 빌더 모두 **Buffer 크기**를 정하기 위해 선택적으로 capacity 파라미터를 받는다. `BlockingQueue`와 비슷하게, Buffer은 지정된 `capacity`만큼의 용량을 두고 발신자가 일시 중단 전에 복수의 원소들을 보낼 수 있도록 하고, Buffer가 꽉 차면 중단한다.

다음 코드의 동작을 살펴보자 :&#x20;

```kotlin
val channel = Channel<Int>(4) // create buffered channel
val sender = launch { // launch sender coroutine
    repeat(10) {
        println("Sending $it") // print before sending each element
        channel.send(it) // will suspend when buffer is full
    }
}
// don't receive anything... just wait....
delay(1000)
sender.cancel() // cancel sender coroutine
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-channel-08.kt)에서 확인할 수 있습니다.

이는 capacity가 **4**인 Buffered Channel을 사용하므로 sending을 **다섯 번** 출력한다.

```
Sending 0
Sending 1
Sending 2
Sending 3
Sending 4
```

첫 4개의 원소는 buffer에 추가되고, 발신자는 5번째 것을 보내려고 할 때 일시 중단 한다.

\


\


***

## Channel은 평등하다

Channel로의 보내고 받는 작업은 복수의 Coroutine을 호출하는 순서에 대해 공정하다. Channel은 FIFO구조로 제공되며, 먼저 `receive`를 호출하는 Coroutine이 원소를 갖게 된다. 다음 예제에서 "ping"과 "pong"이라 불리는 두 Coroutine은 공유된 table Channel을 통해 "ball" 객체를 수신한다.

```kotlin
data class Ball(var hits: Int)

fun main() = runBlocking {
    val table = Channel<Ball>() // a shared table
    launch { player("ping", table) }
    launch { player("pong", table) }
    table.send(Ball(0)) // serve the ball
    delay(1000) // delay 1 second
    coroutineContext.cancelChildren() // game over, cancel them
}

suspend fun player(name: String, table: Channel<Ball>) {
    for (ball in table) { // receive the ball in a loop
        ball.hits++
        println("$name $ball")
        delay(300) // wait a bit
        table.send(ball) // send the ball back
    }
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-channel-09.kt)에서 확인할 수 있습니다.

"ping" Coroutine이 먼저 시작한다. 따라서 이 Coroutine이 ball을 먼저 수신한다. "ping" Coroutine이 ball을 table로 돌려보낸 후, 즉시 다시 receive를 시작하더라도 "pong" Coroutine이 이미 수신 대기 하고 있기 때문에 ball은 "pong" Coroutine이 받는다.

```
ping Ball(hits=1)
pong Ball(hits=2)
ping Ball(hits=3)
pong Ball(hits=4)
```

때때로 채널은 사용중인 실행기의 특성으로 인해 불공평해보이게 실행될 수 있다. 자세한 사항은 [이 이슈](https://github.com/Kotlin/kotlinx.coroutines/issues/111)에서 확인하자.&#x20;

\


## Ticker channels

Ticker Channel은 Channel에서 마지막으로 소비가 일어나고 일정 시간이 지난 이후에 `Unit`을 생성해내는 특별한 랑데뷰 채널이다. 이 자체로는 쓸모 없어 보일지 모르지만, 시간을 기반으로한 복잡한 `produce` 파이프라인 블록을 구축하거나 Windowing이나 시간에 의존적인 처리를 하는데 유용하다. Ticker Channel은 "on tick" 동작을 수행하기 위해 `select`될 수 있다.

이러한 Channel을 만들기 위해 `ticker` 라 불리는 팩토리 메서드를 사용한다. 더이상 원소를 받을 필요가 없음을 나타내기 위해서는 `ReceiveChannel.cancel` 메서드를 사용하면 된다.

실제로 어떻게 동작하는지 살펴보자 :

```kotlin
fun main() = runBlocking<Unit> {
    val tickerChannel = ticker(delayMillis = 100, initialDelayMillis = 0) // create ticker channel
    var nextElement = withTimeoutOrNull(1) { tickerChannel.receive() }
    println("Initial element is available immediately: $nextElement") // no initial delay

    nextElement = withTimeoutOrNull(50) { tickerChannel.receive() } // all subsequent elements have 100ms delay
    println("Next element is not ready in 50 ms: $nextElement")

    nextElement = withTimeoutOrNull(60) { tickerChannel.receive() }
    println("Next element is ready in 100 ms: $nextElement")

    // Emulate large consumption delays
    println("Consumer pauses for 150ms")
    delay(150)
    // Next element is available immediately
    nextElement = withTimeoutOrNull(1) { tickerChannel.receive() }
    println("Next element is available immediately after large consumer delay: $nextElement")
    // Note that the pause between `receive` calls is taken into account and next element arrives faster
    nextElement = withTimeoutOrNull(60) { tickerChannel.receive() } 
    println("Next element is ready in 50ms after consumer pause in 150ms: $nextElement")

    tickerChannel.cancel() // indicate that no more elements are needed
}
```

> 📌 전체 코드는 [이곳](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-channel-10.kt)에서 확인할 수 있습니다.

이는 다음 줄들을 출력한다 :&#x20;

```
Initial element is available immediately: kotlin.Unit
Next element is not ready in 50 ms: null
Next element is ready in 100 ms: kotlin.Unit
Consumer pauses for 150ms
Next element is available immediately after large consumer delay: kotlin.Unit
Next element is ready in 50ms after consumer pause in 150ms: kotlin.Unit
```

`ticker`는 소비자가 일시중지 하는 것을 알고, 기본 동작으로 일시중지가 발생하면 다음 원소가 생산되는 것을 지연시켜, 원소가 일정 비율로 생성되도록 유지한다.

선택적으로 `mode` 매개변수를 `TickerMode.FIXED_DELAY` 로 설정해서 두 원소 간에 일정한 지연이 발생하도록 할 수 있다.



\
