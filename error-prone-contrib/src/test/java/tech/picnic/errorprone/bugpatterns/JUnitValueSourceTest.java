package tech.picnic.errorprone.bugpatterns;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.jupiter.api.Test;

final class JUnitValueSourceTest {
  private final CompilationTestHelper compilationTestHelper =
      CompilationTestHelper.newInstance(JUnitValueSource.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(JUnitValueSource.class, getClass());

  @Test
  void identificationByteWrapper() {
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
            "  @MethodSource(\"fooProvider\")",
            "  // BUG: Diagnostic contains:",
            "  void foo(Byte first) {",
            "    assertThat(first).isNotNull();",
            "  }",
            "",
            "  private static Stream<Arguments> fooProvider() {",
            "    return Stream.of(arguments((byte) 0), arguments((byte) 1));",
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
            "  @MethodSource(\"fooProvider\")",
            "  void foo(int first, int second) {",
            "    assertThat(first).isNotNull();",
            "  }",
            "",
            "  private static Stream<Arguments> fooProvider() {",
            "    return Stream.of(",
            "      arguments(1, 2),",
            "      arguments(3, 4));",
            "  }",
            "}")
        .doTest();
  }

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
            "  @MethodSource(\"fooProvider\")",
            "  // BUG: Diagnostic contains:",
            "  void foo(char character) {",
            "    assertThat(character).isNotNull();",
            "  }",
            "",
            "  private static Stream<Arguments> fooProvider() {",
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
            "  @MethodSource(\"fooProvider\")",
            "  // BUG: Diagnostic contains:",
            "  void foo(Character character) {",
            "    assertThat(character).isNotNull();",
            "  }",
            "",
            "  private static Stream<Arguments> fooProvider() {",
            "    return Stream.of(arguments('a'), arguments('b'));",
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
            "  @MethodSource(\"fooProvider\")",
            "  // BUG: Diagnostic contains:",
            "  void foo(Class<?> clazz) {",
            "    assertThat(clazz).isNotNull();",
            "  }",
            "",
            "  private static Stream<Arguments> fooProvider() {",
            "    return Stream.of(arguments(String.class), arguments(Integer.class));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  void identificationDoubleWrapper() {
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
            "  @MethodSource(\"fooProvider\")",
            "  // BUG: Diagnostic contains:",
            "  void foo(Double number) {",
            "    assertThat(number).isNotNull();",
            "  }",
            "",
            "  private static Stream<Arguments> fooProvider() {",
            "    return Stream.of(arguments(7.0d), arguments(8.0d));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  void identificationDouble() {
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
            "  @MethodSource(\"fooProvider\")",
            "  // BUG: Diagnostic contains:",
            "  void foo(double number) {",
            "    assertThat(number).isNotNull();",
            "  }",
            "",
            "  private static Stream<Arguments> fooProvider() {",
            "    return Stream.of(arguments(7.0d), arguments(8.0d));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  void identificationFloatWrapper() {
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
            "  @MethodSource(\"fooProvider\")",
            "  // BUG: Diagnostic contains:",
            "  void foo(Float number) {",
            "    assertThat(number).isNotNull();",
            "  }",
            "",
            "  private static Stream<Arguments> fooProvider() {",
            "    return Stream.of(arguments(9.0f), arguments(10.0f));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  void identificationFloat() {
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
            "  @MethodSource(\"fooProvider\")",
            "  // BUG: Diagnostic contains:",
            "  void foo(float number) {",
            "    assertThat(number).isNotNull();",
            "  }",
            "",
            "  private static Stream<Arguments> fooProvider() {",
            "    return Stream.of(arguments(9.0f), arguments(10.0f));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  void identificationInt() {
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
            "  @MethodSource(\"fooProvider\")",
            "  // BUG: Diagnostic contains:",
            "  void foo(int number) {",
            "    assertThat(number).isNotNull();",
            "  }",
            "",
            "  private static Stream<Arguments> fooProvider() {",
            "    return Stream.of(arguments(3), arguments(4));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  void identificationInteger() {
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
            "  @MethodSource(\"fooProvider\")",
            "  // BUG: Diagnostic contains:",
            "  void foo(Integer number) {",
            "    assertThat(number).isNotNull();",
            "  }",
            "",
            "  private static Stream<Arguments> fooProvider() {",
            "    return Stream.of(arguments(3), arguments(4));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  void identificationLongWrapper() {
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
            "  @MethodSource(\"fooProvider\")",
            "  // BUG: Diagnostic contains:",
            "  void foo(Long number) {",
            "    assertThat(number).isNotNull();",
            "  }",
            "",
            "  private static Stream<Arguments> fooProvider() {",
            "    return Stream.of(arguments(5L), arguments(6));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  void identificationLong() {
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
            "  @MethodSource(\"fooProvider\")",
            "  // BUG: Diagnostic contains:",
            "  void foo(long number) {",
            "    assertThat(number).isNotNull();",
            "  }",
            "",
            "  private static Stream<Arguments> fooProvider() {",
            "    return Stream.of(arguments(5L), arguments(6));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  void identificationShortWrapper() {
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
            "  @MethodSource(\"fooProvider\")",
            "  // BUG: Diagnostic contains:",
            "  void foo(Short number) {",
            "    assertThat(number).isNotNull();",
            "  }",
            "",
            "  private static Stream<Arguments> fooProvider() {",
            "    return Stream.of(arguments(1), arguments(2));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  void identificationShort() {
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
            "  @MethodSource(\"fooProvider\")",
            "  // BUG: Diagnostic contains:",
            "  void foo(short number) {",
            "    assertThat(number).isNotNull();",
            "  }",
            "",
            "  private static Stream<Arguments> fooProvider() {",
            "    return Stream.of(arguments(1), arguments(2));",
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
            "  @MethodSource(\"fooProvider\")",
            "  // BUG: Diagnostic contains:",
            "  void foo(String string) {",
            "    assertThat(string).isNotNull();",
            "  }",
            "",
            "  private static Stream<Arguments> fooProvider() {",
            "    return Stream.of(arguments(\"foo\"), arguments(\"bar\"));",
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
            "  @MethodSource(\"fooProvider\")",
            "  void foo(int number) {",
            "    assertThat(number).isNotNull();",
            "  }",
            "",
            "  private static Stream<Arguments> fooProvider() {",
            "    return Stream.of(1, 2).map(Arguments::arguments);",
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
            "  @MethodSource(\"fooProvider\")",
            "  // BUG: Diagnostic contains:",
            "  void foo(int foo) {",
            "    assertThat(foo).isNotNull();",
            "  }",
            "",
            "  private static Stream<Arguments> fooProvider() {",
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
        .doTest();
  }
}
