package tech.picnic.errorprone.bugpatterns;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import com.google.errorprone.CompilationTestHelper;
import org.junit.jupiter.api.Test;

final class ErrorProneSupportBugCheckerDocumentationLinkTest {
  private final CompilationTestHelper compilationTestHelper =
      CompilationTestHelper.newInstance(
          ErrorProneSupportBugCheckerDocumentationLink.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(
          ErrorProneSupportBugCheckerDocumentationLink.class, getClass());

  @Test
  void identification() {
    compilationTestHelper
        .addSourceLines(
            "A.java",
            "import com.google.errorprone.BugPattern;",
            "",
            "@BugPattern(summary = \"Class in default package\", severity = BugPattern.SeverityLevel.ERROR)",
            "class A {}")
        .addSourceLines(
            "com/example/B.java",
            "package com.example;",
            "",
            "import com.google.errorprone.BugPattern;",
            "",
            "@BugPattern(summary = \"Class in custom package\", severity = BugPattern.SeverityLevel.ERROR)",
            "class B {}")
        .addSourceLines(
            "tech/picnic/errorprone/subpackage/C.java",
            "package tech.picnic.errorprone.subpackage;",
            "",
            "import com.google.errorprone.BugPattern;",
            "import tech.picnic.errorprone.bugpatterns.util.Documentation;",
            "",
            "@BugPattern(",
            "    summary = \"Error Prone Support class in subpackge with proper link\",",
            "    link = Documentation.BUG_PATTERNS_BASE_URL + \"C\",",
            "    linkType = BugPattern.LinkType.CUSTOM,",
            "    severity = BugPattern.SeverityLevel.ERROR)",
            "class C {}")
        .addSourceLines(
            "tech/picnic/errorprone/D.java",
            "package tech.picnic.errorprone;",
            "",
            "import static com.google.errorprone.BugPattern.LinkType.CUSTOM;",
            "import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;",
            "import static tech.picnic.errorprone.bugpatterns.util.Documentation.BUG_PATTERNS_BASE_URL;",
            "",
            "import com.google.errorprone.BugPattern;",
            "",
            "@BugPattern(",
            "    summary = \"Error Prone Support class with proper link and static imports\",",
            "    link = BUG_PATTERNS_BASE_URL + \"D\",",
            "    linkType = CUSTOM,",
            "    severity = ERROR)",
            "class D {}")
        .doTest();
  }

  @Test
  void replacement() {
    refactoringTestHelper
        .addInputLines(
            "A.java",
            "import static org.assertj.core.api.Assertions.assertThat;",
            "",
            "class A {",
            "  void m() {",
            "    assertThat(1).isEqualTo(null);",
            "    assertThat(\"foo\").isEqualTo(null);",
            "  }",
            "}")
        .addOutputLines(
            "A.java",
            "import static org.assertj.core.api.Assertions.assertThat;",
            "",
            "class A {",
            "  void m() {",
            "    assertThat(1).isNull();",
            "    assertThat(\"foo\").isNull();",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }
}
