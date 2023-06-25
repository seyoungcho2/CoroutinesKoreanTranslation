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