---
description: '원문 최종 수정 :  2023년 6월 16일'
---

# Tutorial: IntelliJ를 사용한 Coroutines 디버깅

> [페이지 편집](https://github.com/seyoungcho2/CoroutinesKoreanTranslation/edit/main/tutorial-intellij-coroutines.md)
>
> [원문](https://kotlinlang.org/docs/debug-coroutines-with-idea.html)



이 튜토리얼은 IntelliJ IDEA를 사용해 Kotlin Coroutine을 만들고 디버깅하는 방법을 설명한다.

이 튜토리얼에서는 독자들이 Coroutines 개념에 대한 사전 지식이 있다고 가정한다.

## Coroutine 생성하기

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

다른 빌드 시스템을 위해서는 [kotlinx.coroutines README](https://github.com/Kotlin/kotlinx.coroutines#using-in-your-projects) 의 지침을 참조하라.

3\. `src/main/kotlin` 속의 `Main.kt` 파일을 연다.

&#x20; `src` 디렉토리에는 Kotlin 소스 파일과 리소스가 포함되어 있다. `Main.kt` 파일에서는 `Hello World!` 를 출력하는 샘플 코드가 들어 있다.

4\. `main()`함수 속의 코드를 수정한다 :

* Coroutine을 감싸기 위해 `runBlocking()` 블록을 사용한다.
* `async()` 함수를 사용해 Deferred 값인 a와 b를 생성하는 Coroutine을 생성한다.
* `await()` 함수를 사용해 연산 결과가 나올 때까지 기다린다.
* `println()` 함수를 사용해 연산 상태와 곱셈 결과를 출력한다.

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking<Unit> {
    val a = async {
        println("I'm computing part of the answer")
        6
    }
    val b = async {
        println("I'm computing another part of the answer")
        7
    }
    println("The answer is ${a.await() * b.await()}")
}
```

5\. **Build Project**를 클릭해서 코드를 빌드한다.

<figure><img src="https://kotlinlang.org/docs/images/flow-build-project.png" alt=""><figcaption></figcaption></figure>

## Coroutine 디버그 하기

1\. `println()` 함수 호출부들에 브레이크 포인트를 설정한다:

<figure><img src="https://kotlinlang.org/docs/images/coroutine-breakpoint.png" alt=""><figcaption></figcaption></figure>



2\. 화면 상단의 **Run** 옆에 있는 **Debug**를 클릭해서 디버그 모드로 코드를 실행한다.

<figure><img src="https://kotlinlang.org/docs/images/flow-debug-project.png" alt=""><figcaption></figcaption></figure>

디버그 툴 윈도우는 다음과 같이 나타난다:

* **Frames** 탭은 콜스택을 포함한다.
* **Variable** 탭은 현재 Context 속의 변수를 포함한다.
* **Coroutines** 탭은 실행중이거나 일시중단된 Coroutine에 대한 정보를 포함한다. 이는 3개의 Coroutine이 있음을 보여준다. 첫 째는 **RUNNING** 상태를 가진다, 그리고 다른 두개는 **CREATED** 상태를 가진다.

<figure><img src="https://kotlinlang.org/docs/images/coroutine-debug-1.png" alt=""><figcaption></figcaption></figure>

3\. 디버그 툴 윈도우의 **Resume Program**을 눌러서 Debugger 세션을 재개한다.

<figure><img src="https://kotlinlang.org/docs/images/coroutine-debug-2.png" alt=""><figcaption></figcaption></figure>

이제 **Coroutines** 탭은 다음을 보여준다:

* 첫 Coroutine은 **SUSPENDED** 상태를 가진다 - 이는 값을 기다리고 이에 따라 곱셈 연산을 수행할 수 있다.
* 두번째 Coroutine은 a 값을 계산한다 - 이는 **RUNNING** 상태를 가진다.
* 셋째 Coroutine은 **CREATED** 상태를 가지고 b의 값을 계산하지 않는다.

4\. 디버그 툴 윈도우의 **Resume Program**을 눌러서 Debugger 세션을 재개한다.

<figure><img src="https://kotlinlang.org/docs/images/coroutine-debug-3.png" alt=""><figcaption></figcaption></figure>

이제 Coroutine 탭은 다음을 보여준다:

* 첫 Coroutine은 **SUSPENDED** 상태를 가진다 - 이는 값을 기다리고 이에 따라 곱셈 연산을 수행할 수 있다.
* 두번째 Coroutine은 값을 계산했고 사라졌다.
* 셋째 Coroutine은 b의 값을 계산한다 - 이는 **RUNNING** 상태를 가진다.

IntelliJ IDEA 디버거를 사용하면, 코드를 디버그 하기 위해 각 Coroutine을 깊게 팔 수 있다.

## Optimized-out variables

만약 디버거에서 suspend 함수를 사용한다면, 변수 이름 뒤에 "was optimized out" 문자가 붙은 것을 볼 수 있을 것이다.&#x20;

<figure><img src="https://kotlinlang.org/docs/images/variable-optimised-out.png" alt=""><figcaption></figcaption></figure>

이 문자는 변수가 더이상 메모리 상에 없다는 것을 뜻한다. 이 변수에 대한 값을 볼 수 없기 때문에 optimized 변수들을 포함한 코드를 디버그 하는 것은 어렵다. 컴파일러 옵션에 `-Xdebug`를 추가함으로써 이러한 동작을 비활성화 할 수 있다.

> ⚠ 이 플래그를 제품에 절대로 사용하지 마세요. `-Xdebug`는 메모리 누수를 일으킬 수 있습니다.
