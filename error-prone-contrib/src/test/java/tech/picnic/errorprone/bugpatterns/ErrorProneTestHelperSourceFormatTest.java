package tech.picnic.errorprone.bugpatterns;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import com.google.errorprone.CompilationTestHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

final class ErrorProneTestHelperSourceFormatTest {
  // XXX: Add tests cases for `ErrorProneTestHelperSourceFormat:IgnoreMalformedCode`.
  private final CompilationTestHelper compilationTestHelper =
      CompilationTestHelper.newInstance(ErrorProneTestHelperSourceFormat.class, getClass());
  private final CompilationTestHelper avoidTextBlocksCompilationTestHelper =
      CompilationTestHelper.newInstance(ErrorProneTestHelperSourceFormat.class, getClass())
          .setArgs("-XepOpt:ErrorProneTestHelperSourceFormat:AvoidTextBlocks=true");
  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(
          ErrorProneTestHelperSourceFormat.class, getClass());
  private final BugCheckerRefactoringTestHelper avoidTextBlocksRefactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(
              ErrorProneTestHelperSourceFormat.class, getClass())
          .setArgs("-XepOpt:ErrorProneTestHelperSourceFormat:AvoidTextBlocks=true");

  // XXX: Consider reducing the `@DisabledForJreRange(max = JRE.JAVA_14)` test scope by moving the
  // text blocks to smaller test methods.

  @DisabledForJreRange(max = JRE.JAVA_14)
  @Test
  void identification() {
    compilationTestHelper
        .addSourceLines(
            "A.java",
            """
            import com.google.errorprone.BugCheckerRefactoringTestHelper;
            import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
            import com.google.errorprone.CompilationTestHelper;
            import tech.picnic.errorprone.bugpatterns.EmptyMethod;

            class A {
              private final CompilationTestHelper compilationTestHelper =
                  CompilationTestHelper.newInstance(EmptyMethod.class, getClass());
              private final BugCheckerRefactoringTestHelper refactoringTestHelper =
                  BugCheckerRefactoringTestHelper.newInstance(EmptyMethod.class, getClass());

              void m() {
                compilationTestHelper
                    // BUG: Diagnostic contains: No source code provided
                    .addSourceLines("A.java")
                    // BUG: Diagnostic contains: Source code is malformed:
                    .addSourceLines("B.java", "class B {")
                    // BUG: Diagnostic contains: Test code should be specified using a single text block
                    .addSourceLines("C.java", "class C {}")
                    // Malformed code, but not compile-time constant, so not flagged.
                    .addSourceLines("D.java", "class D {" + getClass())
                    // BUG: Diagnostic contains: Test code should follow the Google Java style
                    .addSourceLines("E.java", "class E { }")
                    // Well-formed code, so not flagged.
                    .addSourceLines("F.java", ""\"
                           class F {}
                           ""\")
                    // BUG: Diagnostic contains: Test code should follow the Google Java style (pay attention to
                    // trailing newlines)
                    .addSourceLines("G.java", ""\"
                           class G {}""\")
                    .doTest();

                refactoringTestHelper
                    // BUG: Diagnostic contains: Test code should follow the Google Java style
                    .addInputLines("in/A.java", "class A { }")
                    // BUG: Diagnostic contains: Test code should follow the Google Java style
                    .addOutputLines("out/A.java", "class A { }")
                    // BUG: Diagnostic contains: Test code should follow the Google Java style
                    .addInputLines(
                        "in/B.java",
                        ""\"
                        import java.util.Map;

                        class B {}
                        ""\")
                    // Unused import, but in an output file, so not flagged.
                    .addOutputLines(
                        "out/B.java",
                        ""\"
                        import java.util.Map;

                        class B {}
                        ""\")
                    .doTest(TestMode.TEXT_MATCH);
              }
            }
            """)
        .doTest();
  }

  @DisabledForJreRange(max = JRE.JAVA_14)
  @Test
  void identificationAvoidTextBlocks() {
    avoidTextBlocksCompilationTestHelper
        .addSourceLines(
            "A.java",
            """
            import com.google.errorprone.BugCheckerRefactoringTestHelper;
            import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
            import com.google.errorprone.CompilationTestHelper;
            import tech.picnic.errorprone.bugpatterns.EmptyMethod;

            class A {
              private final CompilationTestHelper compilationTestHelper =
                  CompilationTestHelper.newInstance(EmptyMethod.class, getClass());
              private final BugCheckerRefactoringTestHelper refactoringTestHelper =
                  BugCheckerRefactoringTestHelper.newInstance(EmptyMethod.class, getClass());

              void m() {
                compilationTestHelper
                    // BUG: Diagnostic contains: No source code provided
                    .addSourceLines("A.java")
                    // BUG: Diagnostic contains: Source code is malformed:
                    .addSourceLines("B.java", "class B {")
                    // Well-formed code, so not flagged.
                    .addSourceLines("C.java", "class C {}")
                    // Malformed code, but not compile-time constant, so not flagged.
                    .addSourceLines("D.java", "class D {" + getClass())
                    // BUG: Diagnostic contains: Test code should follow the Google Java style
                    .addSourceLines("E.java", "class E { }")
                    // BUG: Diagnostic contains: Test code should not be specified using a single text block
                    .addSourceLines("F.java", ""\"
                    class F {}
                    ""\")
                    // BUG: Diagnostic contains: Test code should follow the Google Java style (pay attention to
                    // trailing newlines)
                    .addSourceLines("G.java", "class G {}", "")
                    .doTest();

                refactoringTestHelper
                    // BUG: Diagnostic contains: Test code should follow the Google Java style
                    .addInputLines("in/A.java", "class A { }")
                    // BUG: Diagnostic contains: Test code should follow the Google Java style
                    .addOutputLines("out/A.java", "class A { }")
                    // BUG: Diagnostic contains: Test code should follow the Google Java style
                    .addInputLines("in/B.java", "import java.util.Map;", "", "class B {}")
                    // Unused import, but in an output file, so not flagged.
                    .addOutputLines("out/B.java", "import java.util.Map;", "", "class B {}")
                    .doTest(TestMode.TEXT_MATCH);
              }
            }
            """)
        .doTest();
  }

  // XXX: Add `replacement` test.
  @DisabledForJreRange(max = JRE.JAVA_14)
  @Test
  void replacement() {
    /*
     * Verifies that import sorting and code formatting is performed unconditionally, while unused
     * imports are removed unless part of a `BugCheckerRefactoringTestHelper` expected output file.
     */
    refactoringTestHelper
        .addInputLines(
            "in/A.java",
            """
            import com.google.errorprone.BugCheckerRefactoringTestHelper;
            import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
            import com.google.errorprone.CompilationTestHelper;
            import tech.picnic.errorprone.bugpatterns.EmptyMethod;

            class A {
              private final CompilationTestHelper compilationTestHelper =
                  CompilationTestHelper.newInstance(EmptyMethod.class, getClass());
              private final BugCheckerRefactoringTestHelper refactoringTestHelper =
                  BugCheckerRefactoringTestHelper.newInstance(EmptyMethod.class, getClass());

              void m() {
                compilationTestHelper
                    .addSourceLines(
                        "A.java",
                        ""\"
                        import java.util.Map;
                        import java.util.Collection;
                        import java.util.List;

                        interface A extends List<A>, Map<A,A> { }""\")
                    .doTest();

                refactoringTestHelper
                    .addInputLines(
                        "in/A.java",
                        ""\"
                        import java.util.Map;
                        import java.util.Collection;
                        import java.util.List;

                        interface A extends List<A>, Map<A,A> { }""\")
                    .addOutputLines(
                        "out/A.java",
                        ""\"
                        import java.util.Map;
                        import java.util.Collection;
                        import java.util.List;

                        interface A extends List<A>, Map<A,A> { }""\")
                    .doTest(TestMode.TEXT_MATCH);
              }
            }
            """)
        .addOutputLines(
            "out/A.java",
            """
            import com.google.errorprone.BugCheckerRefactoringTestHelper;
            import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
            import com.google.errorprone.CompilationTestHelper;
            import tech.picnic.errorprone.bugpatterns.EmptyMethod;

            class A {
              private final CompilationTestHelper compilationTestHelper =
                  CompilationTestHelper.newInstance(EmptyMethod.class, getClass());
              private final BugCheckerRefactoringTestHelper refactoringTestHelper =
                  BugCheckerRefactoringTestHelper.newInstance(EmptyMethod.class, getClass());

              void m() {
                compilationTestHelper
                    .addSourceLines(
                        "A.java",
                        ""\"
                        import java.util.List;
                        import java.util.Map;

                        interface A extends List<A>, Map<A, A> {}
                        ""\")
                    .doTest();

                refactoringTestHelper
                    .addInputLines(
                        "in/A.java",
                        ""\"
                        import java.util.List;
                        import java.util.Map;

                        interface A extends List<A>, Map<A, A> {}
                        ""\")
                    .addOutputLines(
                        "out/A.java",
                        ""\"
                        import java.util.Collection;
                        import java.util.List;
                        import java.util.Map;

                        interface A extends List<A>, Map<A, A> {}
                        ""\")
                    .doTest(TestMode.TEXT_MATCH);
              }
            }
            """)
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  void replacementAvoidTextBlocks() {
    /*
     * Verifies that import sorting and code formatting is performed unconditionally, while unused
     * imports are removed unless part of a `BugCheckerRefactoringTestHelper` expected output file.
     */
    avoidTextBlocksRefactoringTestHelper
        .addInputLines(
            "in/A.java",
            """
            import com.google.errorprone.BugCheckerRefactoringTestHelper;
            import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
            import com.google.errorprone.CompilationTestHelper;
            import tech.picnic.errorprone.bugpatterns.EmptyMethod;

            class A {
              private final CompilationTestHelper compilationTestHelper =
                  CompilationTestHelper.newInstance(EmptyMethod.class, getClass());
              private final BugCheckerRefactoringTestHelper refactoringTestHelper =
                  BugCheckerRefactoringTestHelper.newInstance(EmptyMethod.class, getClass());

              void m() {
                compilationTestHelper
                    .addSourceLines(
                        "A.java",
                        "import java.util.Map;",
                        "import java.util.Collection;",
                        "import java.util.List;",
                        "",
                        "interface A extends List<A>, Map<A,A> { }")
                    .doTest();

                refactoringTestHelper
                    .addInputLines(
                        "in/A.java",
                        "import java.util.Map;",
                        "import java.util.Collection;",
                        "import java.util.List;",
                        "",
                        "interface A extends List<A>, Map<A,A> { }")
                    .addOutputLines(
                        "out/A.java",
                        "import java.util.Map;",
                        "import java.util.Collection;",
                        "import java.util.List;",
                        "",
                        "interface A extends List<A>, Map<A,A> { }")
                    .doTest(TestMode.TEXT_MATCH);
              }
            }
            """)
        .addOutputLines(
            "out/A.java",
            """
            import com.google.errorprone.BugCheckerRefactoringTestHelper;
            import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
            import com.google.errorprone.CompilationTestHelper;
            import tech.picnic.errorprone.bugpatterns.EmptyMethod;

            class A {
              private final CompilationTestHelper compilationTestHelper =
                  CompilationTestHelper.newInstance(EmptyMethod.class, getClass());
              private final BugCheckerRefactoringTestHelper refactoringTestHelper =
                  BugCheckerRefactoringTestHelper.newInstance(EmptyMethod.class, getClass());

              void m() {
                compilationTestHelper
                    .addSourceLines(
                        "A.java",
                        "import java.util.List;",
                        "import java.util.Map;",
                        "",
                        "interface A extends List<A>, Map<A, A> {}")
                    .doTest();

                refactoringTestHelper
                    .addInputLines(
                        "in/A.java",
                        "import java.util.List;",
                        "import java.util.Map;",
                        "",
                        "interface A extends List<A>, Map<A, A> {}")
                    .addOutputLines(
                        "out/A.java",
                        "import java.util.Collection;",
                        "import java.util.List;",
                        "import java.util.Map;",
                        "",
                        "interface A extends List<A>, Map<A, A> {}")
                    .doTest(TestMode.TEXT_MATCH);
              }
            }
            """)
        .doTest(TestMode.TEXT_MATCH);
  }
}
