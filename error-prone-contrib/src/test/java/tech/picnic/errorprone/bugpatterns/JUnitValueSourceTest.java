package tech.picnic.errorprone.bugpatterns;

import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class JUnitValueSourceTest {
  private final CompilationTestHelper compilationTestHelper =
      CompilationTestHelper.newInstance(JUnitValueSource.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(JUnitValueSource.class, getClass());

  @MethodSource("identificationProvider")
  @ParameterizedTest
  void identification(String type, String name, String first, String second) {
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
            String.format("  void foo(%s %s) {", type, name),
            String.format("    assertThat(%s).isNotNull();", name),
            "  }",
            "",
            "  private static Stream<Arguments> fooProvider() {",
            String.format("    return Stream.of(arguments(%s), arguments(%s));", first, second),
            "  }",
            "}")
        .doTest();
  }

  private static Stream<Arguments> identificationProvider() {
    return Stream.of(
        arguments("Byte", "first", "(byte) 0", "(byte) 1"),
        arguments("byte", "first", "(byte) 0", "(byte) 1"),
        arguments("char", "character", "'a'", "'b'"),
        arguments("Character", "character", "'a'", "'b'"),
        arguments("Class<?>", "clazz", "String.class", "Integer.class"),
        arguments("Double", "number", "7.0d", "8.0d"),
        arguments("double", "number", "7.0d", "8.0d"),
        arguments("Float", "number", "9.0f", "10.0f"),
        arguments("float", "number", "9.0f", "10.0f"),
        arguments("int", "number", "3", "4"),
        arguments("Integer", "number", "3", "4"),
        arguments("Long", "number", "5L", "6"),
        arguments("long", "number", "5L", "6"),
        arguments("Short", "number", "1", "2"),
        arguments("short", "number", "1", "2"),
        arguments("String", "string", "\"foo\"", "\"bar\""));
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

  @MethodSource("replacementProvider")
  @ParameterizedTest
  void replacement(String type, String argument, String name, String first, String second) {
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
            String.format("  void foo(%s %s) {", type, name),
            String.format("    assertThat(%s).isNotNull();", name),
            "  }",
            "",
            "  private static Stream<Arguments> fooProvider() {",
            String.format("    return Stream.of(arguments(%s), arguments(%s));", first, second),
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
            String.format("  @ValueSource(%s = {%s, %s})", argument, first, second),
            String.format("  void foo(%s %s) {", type, name),
            String.format("    assertThat(%s).isNotNull();", name),
            "  }",
            "}")
        .doTest();
  }

  private static Stream<Arguments> replacementProvider() {
    return Stream.of(
        arguments("Byte", "bytes", "first", "(byte) 0", "(byte) 1"),
        arguments("byte", "bytes", "first", "(byte) 0", "(byte) 1"),
        arguments("char", "chars", "character", "'a'", "'b'"),
        arguments("Character", "chars", "character", "'a'", "'b'"),
        arguments("Class<?>", "classes", "clazz", "String.class", "Integer.class"),
        arguments("Double", "doubles", "number", "7.0d", "8.0d"),
        arguments("double", "doubles", "number", "7.0d", "8.0d"),
        arguments("Float", "floats", "number", "9.0f", "10.0f"),
        arguments("float", "floats", "number", "9.0f", "10.0f"),
        arguments("int", "ints", "number", "3", "4"),
        arguments("Integer", "ints", "number", "3", "4"),
        arguments("Long", "longs", "number", "5L", "6"),
        arguments("long", "longs", "number", "5L", "6"),
        arguments("Short", "shorts", "number", "1", "2"),
        arguments("short", "shorts", "number", "1", "2"),
        arguments("String", "strings", "string", "\"foo\"", "\"bar\""));
  }
}
