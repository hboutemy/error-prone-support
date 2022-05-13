package tech.picnic.errorprone.bugpatterns;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Ignore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public final class EmptyMethodCheckTest {
  private final CompilationTestHelper compilationTestHelper =
      CompilationTestHelper.newInstance(EmptyMethodCheck.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(EmptyMethodCheck.class, getClass());

  @Test
  void identification() {
    compilationTestHelper
        .addSourceLines(
            "A.java",
            "class A {",
            "  Object m1() {",
            "    return null;",
            "  }",
            "",
            "  void m2() {",
            "    System.out.println(42);",
            "  }",
            "",
            "  // BUG: Diagnostic contains:",
            "  void m3() {}",
            "}")
        .doTest();
  }

  @Test
  void replacement() {
    refactoringTestHelper
        .addInputLines(
            "in/A.java",
            "final class A {",
            "  void instanceMethod() {}",
            "",
            "  static void staticMethod() {}",
            "}")
        .addOutputLines("out/A.java", "final class A {}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  @Disabled("TODO: Implement this")
  void override() {
    compilationTestHelper
        .addSourceLines(
            "A.java",
            "interface A {", //
            "  void example();",
            "}")
        .addSourceLines(
            "B.java",
            "class B implements A {", //
            "  @Override",
            "  public void example() {}",
            "}")
        .doTest();
  }
}
