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
            "  private static final char CONST_CHAR = 'c';",
            "",
            "  @ParameterizedTest",
            "  @MethodSource(\"fooTestCases\")",
            "  // BUG: Diagnostic contains:",
            "  void bar(char character) {",
            "    assertThat(character).isNotNull();",
            "  }",
            "",
            "  private static Stream<Arguments> fooTestCases() {",
            "    return Stream.of(arguments('a'), arguments('b'), arguments(CONST_CHAR));",
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
            "    return Stream.of(",
            "        arguments(String.class), arguments(Integer.class), arguments(java.lang.Double.class));",
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
            "    return Stream.of(arguments(1, 2), arguments(3, 4));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  void identificationNoRuntimeParameters() {
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
            "  void foo(int foo) {",
            "    assertThat(foo).isNotNull();",
            "  }",
            "",
            "  private static Stream<Arguments> fooTestCases() {",
            "    int second = 1 + 2;",
            "    return Stream.of(arguments(1), arguments(second));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  void identificationDontFlagForMultipleFactories() {
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
            "  @MethodSource({\"fooTestCases\", \"barTestCases\"})",
            "  void foo(int i) {",
            "    assertThat(i).isNotNull();",
            "  }",
            "",
            "  private static Stream<Arguments> fooTestCases() {",
            "    return Stream.of(arguments(1));",
            "  }",
            "",
            "  private static Stream<Arguments> barTestCases() {",
            "    return Stream.of(arguments(1));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  void identificationOnlyConstantValues() {
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
            "  private static final int MAGIC = 42;",
            "",
            "  @ParameterizedTest",
            "  @MethodSource(\"fooTestCases\")",
            "  // BUG: Diagnostic contains:",
            "  void foo(int foo) {",
            "    assertThat(foo).isNotNull();",
            "  }",
            "",
            "  private static Stream<Arguments> fooTestCases() {",
            "    return Stream.of(arguments(MAGIC), arguments(2));",
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
            "  private static final int MAGIC_NUMBER = 42;",
            "",
            "  @ParameterizedTest",
            "  @MethodSource(\"fooTestCases\")",
            "  void foo(int foo) {",
            "    assertThat(foo).isNotNull();",
            "  }",
            "",
            "  private static Stream<Arguments> fooTestCases() {",
            "    return Stream.of(arguments(1), arguments(2), arguments(MAGIC_NUMBER));",
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
            "  private static final int MAGIC_NUMBER = 42;",
            "",
            "  @ParameterizedTest",
            "  @ValueSource(ints = {1, 2, MAGIC_NUMBER})",
            "  void foo(int foo) {",
            "    assertThat(foo).isNotNull();",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }
}
