import kotlinx.coroutines.*

fun main() = runBlocking {
    val job = launch { // 새로운 코루틴을 실행하고 그 Job에 대한 참조를 유지한다
        delay(1000L)
        println("World!")
    }
    println("Hello")
    job.join() // 자식 코루틴이 완료될 때까지 기다린다.
    println("Done")
}