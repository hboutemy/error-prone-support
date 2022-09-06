package tech.picnic.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.LinkType.NONE;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.BugPattern.StandardTags.STYLE;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.sun.tools.javac.util.Position.NOPOS;
import static java.util.stream.Collectors.joining;

import com.google.auto.service.AutoService;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import com.google.googlejavaformat.java.ImportOrderer;
import com.google.googlejavaformat.java.JavaFormatterOptions.Style;
import com.google.googlejavaformat.java.RemoveUnusedImports;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import java.util.List;
import java.util.Optional;
import tech.picnic.errorprone.bugpatterns.util.SourceCode;

/**
 * A {@link BugChecker} which flags improperly formatted Error Prone test code.
 *
 * <p>All test code should be formatted in accordance with Google Java Format's {@link Formatter}
 * output, and imports should be ordered according to the {@link Style#GOOGLE Google} style.
 *
 * <p>This checker inspects inline code passed to {@code
 * com.google.errorprone.CompilationTestHelper} and {@code
 * com.google.errorprone.BugCheckerRefactoringTestHelper}. It requires that this code is properly
 * formatted and that its imports are organized. Only code that represents the expected output of a
 * refactoring operation is allowed to have unused imports, as most {@link BugChecker}s do not (and
 * are not able to) remove imports that become obsolete as a result of applying their suggested
 * fix(es).
 */
// XXX: The check does not flag well-formatted text blocks with insufficient indentation. Cover this
// using an generic check.
// XXX: GJF guesses the line separator to be used by inspecting the source. When using text blocks
// this may cause the current unconditional use of `\n` not to be sufficient when building on
// Windows; TBD.
@AutoService(BugChecker.class)
@BugPattern(
    summary =
        "Test code should follow the Google Java style (and when targeting JDK 15+ be "
            + "specified using a single text block)",
    linkType = NONE,
    severity = SUGGESTION,
    tags = STYLE)
public final class ErrorProneTestHelperSourceFormat extends BugChecker
    implements MethodInvocationTreeMatcher {
  private static final long serialVersionUID = 1L;
  private static final String FLAG_AVOID_TEXT_BLOCKS =
      "ErrorProneTestHelperSourceFormat:AvoidTextBlocks";
  private static final String FLAG_IGNORE_MALFORMED_CODE =
      "ErrorProneTestHelperSourceFormat:IgnoreMalformedCode";
  private static final Formatter FORMATTER = new Formatter();
  private static final Matcher<ExpressionTree> INPUT_SOURCE_ACCEPTING_METHOD =
      anyOf(
          instanceMethod()
              .onDescendantOf("com.google.errorprone.CompilationTestHelper")
              .named("addSourceLines"),
          instanceMethod()
              .onDescendantOf("com.google.errorprone.BugCheckerRefactoringTestHelper")
              .named("addInputLines"));
  private static final Matcher<ExpressionTree> OUTPUT_SOURCE_ACCEPTING_METHOD =
      instanceMethod()
          .onDescendantOf("com.google.errorprone.BugCheckerRefactoringTestHelper.ExpectOutput")
          .named("addOutputLines");
  // XXX: Proper name for this?
  private static final String TEXT_BLOCK_MARKER = "\"\"\"";
  private static final String DEFAULT_TEXT_BLOCK_INDENTATION = " ".repeat(12);

  private final boolean avoidTextBlocks;
  private final boolean ignoreMalformedCode;

  /** Instantiates the default {@link ErrorProneTestHelperSourceFormat}. */
  public ErrorProneTestHelperSourceFormat() {
    this(ErrorProneFlags.empty());
  }

  /**
   * Instantiates a customized {@link ErrorProneTestHelperSourceFormat}.
   *
   * @param flags Any provided command line flags.
   */
  public ErrorProneTestHelperSourceFormat(ErrorProneFlags flags) {
    avoidTextBlocks = flags.getBoolean(FLAG_AVOID_TEXT_BLOCKS).orElse(Boolean.FALSE);
    ignoreMalformedCode = flags.getBoolean(FLAG_IGNORE_MALFORMED_CODE).orElse(Boolean.FALSE);
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    boolean isOutputSource = OUTPUT_SOURCE_ACCEPTING_METHOD.matches(tree, state);
    if (!isOutputSource && !INPUT_SOURCE_ACCEPTING_METHOD.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    List<? extends ExpressionTree> sourceLines =
        tree.getArguments().subList(1, tree.getArguments().size());
    if (sourceLines.isEmpty()) {
      return buildDescription(tree).setMessage("No source code provided").build();
    }

    /* Attempt to format the source code only if it fully consists of constant expressions. */
    return getConstantSourceCode(sourceLines)
        .map(source -> flagFormattingIssues(sourceLines, source, isOutputSource, state))
        .orElse(Description.NO_MATCH);
  }

  private Description flagFormattingIssues(
      List<? extends ExpressionTree> sourceLines,
      String source,
      boolean retainUnusedImports,
      VisitorState state) {
    MethodInvocationTree methodInvocation = (MethodInvocationTree) state.getPath().getLeaf();

    String formatted;
    try {
      String gjfResult = formatSourceCode(source, retainUnusedImports);
      formatted = canUseTextBlocks(state) ? gjfResult : gjfResult.stripTrailing();
    } catch (FormatterException e) {
      return ignoreMalformedCode
          ? Description.NO_MATCH
          : buildDescription(methodInvocation)
              .setMessage(String.format("Source code is malformed: %s", e.getMessage()))
              .build();
    }

    boolean isFormatted = source.equals(formatted);
    boolean hasStringLiteralMismatch = shouldUpdateStringLiteralFormat(sourceLines, state);

    if (isFormatted && !hasStringLiteralMismatch) {
      return Description.NO_MATCH;
    }

    int startPos = ASTHelpers.getStartPosition(sourceLines.get(0));
    int endPos = state.getEndPosition(sourceLines.get(sourceLines.size() - 1));
    boolean hasNewlineMismatch =
        !isFormatted && source.stripTrailing().equals(formatted.stripTrailing());

    /*
     * The source code is not properly formatted and/or not specified using a single text block.
     * Report the more salient of the violations, and suggest a fix if sufficient source information
     * is available.
     */
    return buildDescription(methodInvocation)
        .setMessage(
            isFormatted || (hasNewlineMismatch && hasStringLiteralMismatch)
                ? String.format(
                    "Test code should %sbe specified using a single text block",
                    avoidTextBlocks ? "not " : "")
                : String.format(
                    "Test code should follow the Google Java style%s",
                    hasNewlineMismatch ? " (pay attention to trailing newlines)" : ""))
        .addFix(
            (startPos == NOPOS || endPos == NOPOS)
                ? SuggestedFix.emptyFix()
                : SuggestedFix.replace(
                    startPos,
                    endPos,
                    canUseTextBlocks(state)
                        ? toTextBlockExpression(methodInvocation, formatted, state)
                        : toLineEnumeration(formatted, state)))
        .build();
  }

  private boolean shouldUpdateStringLiteralFormat(
      List<? extends ExpressionTree> sourceLines, VisitorState state) {
    return canUseTextBlocks(state)
        ? sourceLines.size() > 1 || !SourceCode.isTextBlock(sourceLines.get(0), state)
        : sourceLines.stream().anyMatch(tree -> SourceCode.isTextBlock(tree, state));
  }

  private boolean canUseTextBlocks(VisitorState state) {
    return !avoidTextBlocks && SourceCode.isTextBlockSupported(state);
  }

  private static String toTextBlockExpression(
      MethodInvocationTree tree, String source, VisitorState state) {
    String indentation = suggestTextBlockIndentation(tree, state);

    // XXX: Verify trailing """ on new line.
    return TEXT_BLOCK_MARKER
        + '\n'
        + indentation
        + source
            .replace("\n", '\n' + indentation)
            .replace("\\", "\\\\")
            .replace(TEXT_BLOCK_MARKER, "\"\"\\\"")
        + TEXT_BLOCK_MARKER;
  }

  private static String toLineEnumeration(String source, VisitorState state) {
    return Splitter.on('\n')
        .splitToStream(source)
        .map(state::getConstantExpression)
        .collect(joining(", "));
  }

  private static String suggestTextBlockIndentation(
      MethodInvocationTree target, VisitorState state) {
    CharSequence sourceCode = state.getSourceCode();
    if (sourceCode == null) {
      return DEFAULT_TEXT_BLOCK_INDENTATION;
    }

    String source = sourceCode.toString();
    return getIndentation(target.getArguments().get(1), source)
        .or(() -> getIndentation(target.getArguments().get(0), source))
        .or(() -> getIndentation(target.getMethodSelect(), source))
        .orElse(DEFAULT_TEXT_BLOCK_INDENTATION);
  }

  private static Optional<String> getIndentation(Tree tree, String source) {
    int startPos = ASTHelpers.getStartPosition(tree);
    if (startPos == NOPOS) {
      return Optional.empty();
    }

    int finalNewLine = source.lastIndexOf('\n', startPos);
    if (finalNewLine < 0) {
      return Optional.empty();
    }

    return Optional.of(source.substring(finalNewLine + 1, startPos))
        .filter(CharMatcher.whitespace()::matchesAllOf);
  }

  private static String formatSourceCode(String source, boolean retainUnusedImports)
      throws FormatterException {
    String withReorderedImports = ImportOrderer.reorderImports(source, Style.GOOGLE);
    String withOptionallyRemovedImports =
        retainUnusedImports
            ? withReorderedImports
            : RemoveUnusedImports.removeUnusedImports(withReorderedImports);
    return FORMATTER.formatSource(withOptionallyRemovedImports);
  }

  private static Optional<String> getConstantSourceCode(
      List<? extends ExpressionTree> sourceLines) {
    StringBuilder source = new StringBuilder();

    for (ExpressionTree sourceLine : sourceLines) {
      if (source.length() > 0) {
        source.append('\n');
      }

      Object value = ASTHelpers.constValue(sourceLine);
      if (value == null) {
        return Optional.empty();
      }

      source.append(value);
    }

    return Optional.of(source.toString());
  }
}
