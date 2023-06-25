import kotlinx.coroutines.*

fun main() = runBlocking {
    repeat(50_000) { // 많은 수의 코루틴을 실행한다.
        launch {
            delay(5000L)
            print(".")
        }
    }
}