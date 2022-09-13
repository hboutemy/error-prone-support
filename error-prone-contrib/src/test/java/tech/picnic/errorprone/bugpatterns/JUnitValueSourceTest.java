package tech.picnic.errorprone.bugpatterns;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import com.google.errorprone.CompilationTestHelper;
import org.junit.jupiter.api.Test;

final class JUnitValueSourceTest {
  private final CompilationTestHelper compilationTestHelper =
      CompilationTestHelper.newInstance(JUnitValueSource.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(JUnitValueSource.class, getClass());

  @Test
  void identificationChar() {
    compilationTestHelper
        .addSourceLines(
            "A.java",
            "import static org.assertj.core.api.Assertions.assertThat;",
            "import static org.junit.jupiter.params.provider.Arguments.arguments;",
            "",
            "import java.util.stream.Stream;",
            "import org.junit.jupiter.params.ParameterizedTest;",
            "import org.junit.jupiter.params.provider.Arguments;",
            "import org.junit.jupiter.params.provider.MethodSource;",
            "",
            "class A {",
            "  @ParameterizedTest",
            "  @MethodSource(\"fooTestCases\")",
            "  // BUG: Diagnostic contains:",
            "  void bar(char character) {",
            "    assertThat(character).isNotNull();",
            "  }",
            "",
            "  private static Stream<Arguments> fooTestCases() {",
            "    return Stream.of(arguments('a'), arguments('b'));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  void identificationCharacter() {
    compilationTestHelper
        .addSourceLines(
            "A.java",
            "import static org.assertj.core.api.Assertions.assertThat;",
            "import static org.junit.jupiter.params.provider.Arguments.arguments;",
            "",
            "import java.util.stream.Stream;",
            "import org.junit.jupiter.params.ParameterizedTest;",
            "import org.junit.jupiter.params.provider.Arguments;",
            "import org.junit.jupiter.params.provider.MethodSource;",
            "",
            "class A {",
            "  @ParameterizedTest",
            "  @MethodSource(\"fooTestCases\")",
            "  // BUG: Diagnostic contains:",
            "  void foo(Character character) {",
            "    assertThat(character).isNotNull();",
            "  }",
            "",
            "  private static Stream<Arguments> fooTestCases() {",
            "    return Stream.of(arguments(Character.valueOf('a')), arguments(Character.valueOf('b')));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  void identificationString() {
    compilationTestHelper
        .addSourceLines(
            "A.java",
            "import static org.assertj.core.api.Assertions.assertThat;",
            "import static org.junit.jupiter.params.provider.Arguments.arguments;",
            "",
            "import java.util.stream.Stream;",
            "import org.junit.jupiter.params.ParameterizedTest;",
            "import org.junit.jupiter.params.provider.Arguments;",
            "import org.junit.jupiter.params.provider.MethodSource;",
            "",
            "class A {",
            "  @ParameterizedTest",
            "  @MethodSource(\"fooTestCases\")",
            "  // BUG: Diagnostic contains:",
            "  void foo(String string) {",
            "    assertThat(string).isNotNull();",
            "  }",
            "",
            "  private static Stream<Arguments> fooTestCases() {",
            "    return Stream.of(arguments(\"foo\"), arguments(\"bar\"));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  void identificationClass() {
    compilationTestHelper
        .addSourceLines(
            "A.java",
            "import static org.assertj.core.api.Assertions.assertThat;",
            "import static org.junit.jupiter.params.provider.Arguments.arguments;",
            "",
            "import java.util.stream.Stream;",
            "import org.junit.jupiter.params.ParameterizedTest;",
            "import org.junit.jupiter.params.provider.Arguments;",
            "import org.junit.jupiter.params.provider.MethodSource;",
            "",
            "class A {",
            "  @ParameterizedTest",
            "  @MethodSource(\"fooTestCases\")",
            "  // BUG: Diagnostic contains:",
            "  void foo(Class<?> clazz) {",
            "    assertThat(clazz).isNotNull();",
            "  }",
            "",
            "  private static Stream<Arguments> fooTestCases() {",
            "    return Stream.of(arguments(String.class), arguments(Integer.class));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  void identificationStreamChain() {
    compilationTestHelper
        .addSourceLines(
            "A.java",
            "import static org.assertj.core.api.Assertions.assertThat;",
            "",
            "import java.util.stream.Stream;",
            "import org.junit.jupiter.params.ParameterizedTest;",
            "import org.junit.jupiter.params.provider.Arguments;",
            "import org.junit.jupiter.params.provider.MethodSource;",
            "",
            "class A {",
            "  @ParameterizedTest",
            "  @MethodSource(\"fooTestCases\")",
            "  void foo(int number) {",
            "    assertThat(number).isNotNull();",
            "  }",
            "",
            "  private static Stream<Arguments> fooTestCases() {",
            "    return Stream.of(1, 2).map(Arguments::arguments);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  void identificationDontFlagMultipleParameters() {
    compilationTestHelper
        .addSourceLines(
            "A.java",
            "import static org.assertj.core.api.Assertions.assertThat;",
            "import static org.junit.jupiter.params.provider.Arguments.arguments;",
            "",
            "import java.util.stream.Stream;",
            "import org.junit.jupiter.params.ParameterizedTest;",
            "import org.junit.jupiter.params.provider.Arguments;",
            "import org.junit.jupiter.params.provider.MethodSource;",
            "",
            "class A {",
            "  @ParameterizedTest",
            "  @MethodSource(\"fooTestCases\")",
            "  void foo(int first, int second) {",
            "    assertThat(first).isNotNull();",
            "  }",
            "",
            "  private static Stream<Arguments> fooTestCases() {",
            "    return Stream.of(",
            "      arguments(1, 2),",
            "      arguments(3, 4));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  void replacement() {
    refactoringTestHelper
        .addInputLines(
            "A.java",
            "import static org.assertj.core.api.Assertions.assertThat;",
            "import static org.junit.jupiter.params.provider.Arguments.arguments;",
            "",
            "import java.util.stream.Stream;",
            "import org.junit.jupiter.params.ParameterizedTest;",
            "import org.junit.jupiter.params.provider.Arguments;",
            "import org.junit.jupiter.params.provider.MethodSource;",
            "",
            "class A {",
            "  @ParameterizedTest",
            "  @MethodSource(\"fooTestCases\")",
            "  void foo(int foo) {",
            "    assertThat(foo).isNotNull();",
            "  }",
            "",
            "  private static Stream<Arguments> fooTestCases() {",
            "    return Stream.of(arguments(1), arguments(2));",
            "  }",
            "}")
        .addOutputLines(
            "A.java",
            "import static org.assertj.core.api.Assertions.assertThat;",
            "import static org.junit.jupiter.params.provider.Arguments.arguments;",
            "",
            "import java.util.stream.Stream;",
            "import org.junit.jupiter.params.ParameterizedTest;",
            "import org.junit.jupiter.params.provider.Arguments;",
            "import org.junit.jupiter.params.provider.MethodSource;",
            "import org.junit.jupiter.params.provider.ValueSource;",
            "",
            "class A {",
            "  @ParameterizedTest",
            "  @ValueSource(ints = {1, 2})",
            "  void foo(int foo) {",
            "    assertThat(foo).isNotNull();",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }
}
