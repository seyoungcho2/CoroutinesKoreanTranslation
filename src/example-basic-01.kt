import kotlinx.coroutines.*

fun main() = runBlocking { // this: CoroutineScope
    launch { // 새로운 coroutine을 실행하고 계속한다.
        delay(1000L) // 블로킹 하지 않고 1초를 지연시킨다. (기본 시간 단위 : ms)
        println("World!") // 지연 이후에 프린트한다.
    }
    println("Hello") // 이전 coroutine이 지연된 동안 main coroutine이 실행된다.
}