---
description: '원문 최종 수정 :  2023년 6월 16일'
---

# Tutorial: IntelliJ를 사용한 Kotlin Flow 디버깅

이 튜토리얼은 IntelliJ IDEA를 사용해 Kotlin Flow를 생성하고 디버깅 하는 방법에 대해 설명한다.

이 튜토리얼에서는 독자들이 Coroutine 개념에 대한 사전 지식이 있다고 가정한다.

## Kotlin Flow 생성하기

느린 방출기와 느린 수집기를 가진 Kotlin `flow`를 생성한다:

1\. Intellij IDEA에서 Kotlin 프로젝트를 연다. 만약 프로젝트가 없다면 하나를 새로 만든다.

2\. `kotlinx.coroutines` 라이브러리를 Gradle 프로젝트에서 사용하기 위해서 다음 종속성을 `build.gradle(.kts)`에 추가한다.

```
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
}
```

```
dependencies {
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4'
}
```

다른 빌드 시스템을 위해서는 [kotlinx.coroutines README](https://github.com/Kotlin/kotlinx.coroutines#using-in-your-projects) 의 지침을 참고하라.

3\. `src/main/kotlin` 속의 `Main.kt` 파일을 연다.

&#x20; `src` 디렉토리에는 Kotlin 소스 파일과 리소스가 포함되어 있다. `Main.kt` 파일에서는 `Hello World!` 를 출력하는 샘플 코드가 들어 있다.

4\. 세 개 숫자를 반환하는 `simple()` 함수를 생성한다.

* `delay()` 함수를 사용해 CPU 리소스를 소모하는 블로킹 코드를 모방한다. 이는 스레드를 블로킹하지 않고 Coroutine을 100ms 동안 일시중단 한다.
* `for` 루프에서 `emit()`함수를 사용해 값들을 생성한다.

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.system.*

fun simple(): Flow<Int> = flow {
    for (i in 1..3) {
        delay(100)
        emit(i)
    }
}
```

5\. `main()` 함수에서 코드를 바꾼다.

* `runBlocking()` 블록을 사용해 Coroutine을 감싼다.
* 방출된 값들을 `collect()` 함수를 사용해 수집한다.
* `delay()` 함수를 사용해 CPU 리소스를 소모하는 블로킹 코드를 모방한다. 이는 스레드를 블로킹하지 않고 Coroutine을 300ms 동안 일시중단 한다.
* Flow로부터 수집된 값을 `println()` 함수를 사용해 출력한다.

```kotlin
fun main() = runBlocking {
    simple()
        .collect { value ->
            delay(300)
            println(value)
        }
}
```

6\. Build Project를 눌러서 코드를 빌드한다.

<figure><img src="https://kotlinlang.org/docs/images/flow-build-project.png" alt=""><figcaption></figcaption></figure>

## Coroutine 디버그하기

1\. `emit()` 함수가 호출되는 곳에 브레이크포인트를 설정한다:

<figure><img src="https://kotlinlang.org/docs/images/flow-breakpoint.png" alt=""><figcaption></figcaption></figure>

2\. 화면 상단의 Run 구성 옆의 **Debug**를 클릭해서 코드를 디버그모드에서 실행한다.&#x20;

<figure><img src="https://kotlinlang.org/docs/images/flow-debug-project.png" alt=""><figcaption></figcaption></figure>

**디버그** 툴 윈도우는 다음과 같이 나타난다:

* **Frames** 탭은 콜스택을 포함한다.
* **Variable** 탭은 현재 Context 속의 변수를 포함한다. 이는 Flow가 첫 값을 방출한다는 것을 보여준다.
* **Coroutines** 탭은 실행중이거나 일시중단된 Coroutine에 대한 정보를 포함한다.&#x20;

<figure><img src="https://kotlinlang.org/docs/images/flow-debug-1.png" alt=""><figcaption></figcaption></figure>

3\. 디버그 툴 윈도우의 **Resume Program**을 눌러서 Debugger 세션을 재개한다. 프로그램은 같은 브레이크포인트에서 중단된다.

<figure><img src="https://kotlinlang.org/docs/images/flow-resume-debug.png" alt=""><figcaption></figcaption></figure>

이제 Flow는 두번째 값을 방출한다.

<figure><img src="https://kotlinlang.org/docs/images/flow-debug-2.png" alt=""><figcaption></figcaption></figure>

### Optimized-out variables

만약 디버거에서 suspend 함수를 사용한다면, 변수 이름 뒤에 "was optimized out" 문자가 붙은 것을 볼 수 있을 것이다.&#x20;

<figure><img src="https://kotlinlang.org/docs/images/variable-optimised-out.png" alt=""><figcaption></figcaption></figure>

이 문자는 변수가 더이상 메모리 상에 없다는 것을 뜻한다. 이 변수에 대한 값을 볼 수 없기 때문에 optimized 변수들을 포함한 코드를 디버그 하는 것은 어렵다. 컴파일러 옵션에 `-Xdebug`를 추가함으로써 이러한 동작을 비활성화 할 수 있다.

> ⚠ 이 플래그를 제품에 절대로 사용하지 마세요. `-Xdebug`는 메모리 누수를 일으킬 수 있습니다.





## 동시에 실행되는 Coroutine 추가하기

1\. `src/main/kotlin` 속의 `Main.kt` 파일을 연다.

2\. 코드에서 방출기와 수집기가 동시에 실행되도록 개선한다.

* `buffer()` 함수 호출을 추가해 방출기와 수집기가 동시에 실행되도록 한다. `buffer()`은 방출된 값을 저장하고 Flow 수집기를 분리된 Coroutine에서 실행한다.

```kotlin
fun main() = runBlocking<Unit> {
    simple()
        .buffer()
        .collect { value ->
            delay(300)
            println(value)
        }
}
```

3\. **Build Project**를 클릭해 코드를 빌드한다.



## 두 개의 Coroutine을 가진 Kotlin Flow를 디버깅하기

1\. println(value)에 새로운 브레이크포인트를 설정한다.

2\. 화면 상단의 Run 옆에 있는 **Debug**를 클릭해서 디버그 모드로 코드를 실행한다.

<figure><img src="https://kotlinlang.org/docs/images/flow-debug-3.png" alt=""><figcaption></figcaption></figure>

디버그 툴 윈도우는 다음과 같이 나타난다.

**Coroutines** 탭에는, 동시에 실행중이 두개의 Coroutine을 볼 수 있다. Flow 수집기와 방출기는 buffer() 함수 때문에 분리된 Coroutine에서 실행된다. buffer() 함수는 flow에서 방출된 값들을 버퍼에 저장한다. 방출기 Coroutine은 **RUNNING** 상태를 가지고, 수집기 Coroutine은 **SUSPENDED** 상태를 가진다.

3\. 디버그 툴 윈도우의 **Resume Program**을 눌러서 디버거 세션을 재개한다.

<figure><img src="https://kotlinlang.org/docs/images/flow-debug-4.png" alt=""><figcaption></figcaption></figure>

이제 수집기 Coroutine은 **RUNNING** 상태를 가지는 반면 방출기 Coroutine은 **SUSPENDED** 상태를 가진다.

각 Coroutine을 더욱 깊게 파고들어 코드를 디버깅 할 수 있다.
