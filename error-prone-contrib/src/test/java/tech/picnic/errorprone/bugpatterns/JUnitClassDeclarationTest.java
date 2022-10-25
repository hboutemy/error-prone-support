package tech.picnic.errorprone.bugpatterns;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import com.google.errorprone.CompilationTestHelper;
import org.junit.jupiter.api.Test;

final class JUnitClassDeclarationTest {
  private final CompilationTestHelper compilationTestHelper =
      CompilationTestHelper.newInstance(JUnitClassDeclaration.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(JUnitClassDeclaration.class, getClass());

  @Test
  void identification() {
    compilationTestHelper
        .addSourceLines(
            "A.java",
            "import org.junit.jupiter.api.Test;",
            "",
            "// BUG: Diagnostic contains:",
            "class A {",
            "  @Test",
            "  void foo() {}",
            "}")
        .addSourceLines(
            "B.java",
            "import org.junit.jupiter.params.ParameterizedTest;",
            "",
            "// BUG: Diagnostic contains:",
            "class B {",
            "  @ParameterizedTest",
            "  void foo() {}",
            "}")
        .addSourceLines(
            "C.java",
            "import org.junit.jupiter.api.Test;",
            "",
            "// BUG: Diagnostic contains:",
            "public class C {",
            "  @Test",
            "  void foo() {}",
            "}")
        .addSourceLines(
            "D.java",
            "import org.junit.jupiter.api.Test;",
            "",
            "final class D {",
            "  @Test",
            "  void foo() {}",
            "}")
        .addSourceLines(
            "E.java",
            "import org.junit.jupiter.api.Test;",
            "import org.springframework.context.annotation.Configuration;",
            "",
            "@Configuration",
            "final class E {",
            "  @Test",
            "  void foo() {}",
            "}")
        .addSourceLines(
            "F.java",
            "import org.junit.jupiter.api.Nested;",
            "import org.junit.jupiter.api.Test;",
            "",
            "class F {",
            "  @Nested",
            "  // BUG: Diagnostic contains:",
            "  class A {",
            "    @Test",
            "    void foo() {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  void replacement() {
    refactoringTestHelper
        .addInputLines(
            "A.java",
            "import org.junit.jupiter.api.Test;",
            "",
            "public class A {",
            "  @Test",
            "  void foo() {}",
            "}")
        .addOutputLines(
            "A.java",
            "import org.junit.jupiter.api.Test;",
            "",
            "final class A {",
            "  @Test",
            "  void foo() {}",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }
}
