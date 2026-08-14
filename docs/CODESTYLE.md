## 代码风格指南

多数情况下我们遵循 [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)。

### [4.1.3](https://google.github.io/styleguide/javaguide.html#s4.1.3-braces-empty-blocks) 空 catch 块
```java
void method() {
  try {
    doSomething();
  } catch (Exception e) {}
}
```
允许，但不鼓励。若要忽略异常，应说明原因：

```java
void method() {
  try {
    doSomething();
  } catch (Exception e) {
    // 失败可忽略，因此不处理异常
  }
}
```

### [4.8.2.2](https://google.github.io/styleguide/javaguide.html#s4.8.2-variable-declarations) 在需要时再声明
我们将局部变量的原则延伸到成员变量。<br>
若能提升可读性，且字段在方法外无独立含义，应尽量靠近使用处声明。
示例：

```java
class MyClass {
  private int myField;
  private int myOtherField;
  private int myThirdField;
  private int myFourthField;
  private int myFifthField;
  private int mySixthField;
  private int mySeventhField;

  // ...

  private final Queue<String> processQueue = new LinkedList<>();

  void method3() {
    processQueue.add("Hello");
  }

  String method4() {
    return processQueue.poll();
  }
}
```
