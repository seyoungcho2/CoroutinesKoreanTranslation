---
layout: editorial
---

# Kotlin Coroutines 공식 문서 번역을 시작하며

Kotlin Coroutines는 Kotlin을 위한 강력한 비동기 솔루션이다. 안드로이드 실무에서는 한동안 높은 점유율을 자랑한 RxJava를 Coroutines가 대체하고 있으며, 새로 시작하는 프로젝트들은 모두 Coroutines를 사용하고 있다. 그 이유는 Coroutines의 성능과 간결성, 가독성에 있다. Coroutines는 기존 스레드 모델들과 다른 경량 스레드(Light Weight Thread)라는 개념을 도입하여 불필요한 Thread Blocking을 방지할 수 있도록 하였으며, 직관적인 키워드를 통해 가독성을 높였다.

&#x20;이러한 장점으로 많은 개발자들이 실무에서 Coroutines를 사용하기 시작했지만, 공부를 위한 자료가 많이 부족하며, 많은 배경 지식들을 요구하기 때문에 접근하기 위한 허들이 높다. 나 또한 한글로 된 제대로 된 자료가 존재하지 않는 상황에서 각종 영어로된 문서, 영상, 책 등을 찾아 모르는 부분을 해결하였고, 영어에 익숙한 나조차 코루틴을 이해하기 위해 몇 달 이상의 시간이 걸렸다. \
&#x20;나는 이것이 문제라고 생각해 많은 한국인 개발자들이 코루틴에 접근하기 쉽도록 하기 위해 책과 강의 공식문서 그리고 Google IO 등을 보면서 정리한 것을 [18개의 글](http://kotlinworld.com/139)로 만들었고, 이에 대한 반응이 매우 좋았다. 하지만 이 글들은 내가 나름대로 주니어 개발자부터 시니어 개발자까지 이해하기 편하게 정리한 것일 뿐 Coroutines에 대한 모든 것을 다루지는 않는다.&#x20;

이에 따라 이번에는 Coroutines 에 대한 깊은 부분까지 다루기 위해 공식 문서의 번역이라는 새로운 시도를 하고자 한다. 이번 번역 작업을 통해 번역의 완성도와 가독성을 모두 잡은 공식 문서 가이드가 나올 수 있었으면 바라는 마음으로 블로그를 열어 번역을 시작한다.&#x20;

&#x20;부디 이 번역 작업이 많은 분들께 도움이 되었으면 좋겠다. 아래에서 바로 번역을 시작한다.



개발자 조세영&#x20;



### 원문 정보

원문 링크 : [https://kotlinlang.org/docs/coroutines-guide.html](https://kotlinlang.org/docs/coroutines-guide.html)

> 이 번역은 [번역 기여 가이드라인](https://kotlinlang.org/docs/contribute.html#translate-documentation-to-other-languages)에 따라 번역되었습니다.

### **번역자 정보**

[GitHub](https://github.com/seyoungcho2)

[Google Dev Library](https://devlibrary.withgoogle.com/authors/seyoungcho2)

[LinkedIn](https://www.linkedin.com/in/seyoungcho/)

Email : seyoungcho2@gmail.com
