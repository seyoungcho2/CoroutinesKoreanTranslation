---
description: '원문 최종 수정 :  2023년 6월 16일'
---

# Coroutines 가이드

&#x20;Kotlin은 다른 라이브러리들이 coroutines를 활용할 수 있도록 표준 라이브러리 상에서 최소한의 저수준 API들만을 제공한다.  비슷한 기능을 가진 다른 많은 언어들과 다르게, `async`와 `await`은 Kotlin의 키워드나 표준 라이브러리의 구성요소가 아니다. 또한 Kotlin의 일시 중단 함수는 다른 비동기 개념인 futures나 promises보다 안전하고 오류가 덜 발생할 수 있도록 추상화되어 있다.

&#x20;`kotlinx.coroutines` 패키지는 coroutines에 다양한 기능들을 제공하기 위해 JetBrains사에서 개발된 라이브러리이다. 여기에는 `launch`, `async` 등 이 가이드에서 다루는 Coroutines 사용을 위한 고수준 primitives가 포함된다.

&#x20;이 문서는 `kotlinx.coroutines`의 핵심 기능들에 대한 가이드이며, 다양한 주제들과 일련의 예시들로 구성되었다.

&#x20;coroutines를 사용하고 이 가이드의 예제를 학습하기 위해서는 [project README](https://github.com/Kotlin/kotlinx.coroutines/blob/master/README.md#using-in-your-projects)에 설명된 대로 `kotlinx-coroutines-core`에 대한 의존성을 추가해야 한다.



### 목차 <a href="#table-of-contents" id="table-of-contents"></a>

* Coroutines 기초
* [실습: Coroutine 및 Channel 소개](https://play.kotlinlang.org/hands-on/Introduction%20to%20Coroutines%20and%20Channels/01\_Introduction)
* 취소와 타임아웃
* 일시중단 함수 구성하기
* Coroutine Context와 Dispatcher
* 비동기 Flow
* Channels
* Coroutine 예외 처리
* Coroutine 공유 상태와 동시성
* Select expression (experimental)
* Tutorial: IntelliJ를 사용한 Coroutines 디버깅
* Tutorial: IntelliJ를 사용한 Kotlin Flow 디버깅



### 추가 참조 <a href="#additional-references" id="additional-references"></a>

* Guide to UI programming with coroutines
* Coroutines design document (KEEP)
* Full kotlinx.coroutines API reference
* Best practices for coroutines in Android
* Additional Android resources for Kotlin coroutines and flow

\
