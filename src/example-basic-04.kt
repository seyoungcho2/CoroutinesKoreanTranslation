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