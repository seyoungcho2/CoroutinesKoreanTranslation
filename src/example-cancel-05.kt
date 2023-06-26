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