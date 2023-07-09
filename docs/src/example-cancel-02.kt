import kotlinx.coroutines.*

fun main() = runBlocking {
    val startTime = System.currentTimeMillis()
    val job = launch(Dispatchers.Default) {
        var nextPrintTime = startTime
        var i = 0
        while (i < 5) { // 계산 루프, CPU를 낭비한다
            // 1초에 두 번 메세지를 출력한다.
            if (System.currentTimeMillis() >= nextPrintTime) {
                println("job: I'm sleeping ${i++} ...")
                nextPrintTime += 500L
            }
        }
    }
    delay(1300L) // 약간의 시간 동안 delay 한다
    println("main: I'm tired of waiting!")
    job.cancelAndJoin() // Job을 취소하고 실행이 완료될 때까지 기다린다.
    println("main: Now I can quit.")
}