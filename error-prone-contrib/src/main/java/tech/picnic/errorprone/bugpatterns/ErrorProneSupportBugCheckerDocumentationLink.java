package tech.picnic.errorprone.bugpatterns;

import static com.google.common.base.Verify.verify;
import static com.google.errorprone.BugPattern.LinkType.CUSTOM;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.BugPattern.StandardTags.LIKELY_ERROR;
import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.AT_LEAST_ONE;
import static com.google.errorprone.matchers.Matchers.annotations;
import static com.google.errorprone.matchers.Matchers.isType;
import static com.google.errorprone.matchers.Matchers.packageStartsWith;
import static tech.picnic.errorprone.bugpatterns.util.Documentation.BUG_PATTERNS_BASE_URL;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.AnnotationMatcherUtils;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.matchers.MultiMatcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree;
import javax.lang.model.element.Name;

/**
 * A {@link BugChecker} that flags {@link BugChecker} declarations inside {@code
 * tech.picnic.errorprone.*} packages that do not reference the Error Prone Support website.
 */
@AutoService(BugChecker.class)
@BugPattern(
    summary = "`Error Prone Support checks should reference their online documentation",
    link = BUG_PATTERNS_BASE_URL + "ErrorProneSupportBugCheckerDocumentationLink",
    linkType = CUSTOM,
    severity = SUGGESTION,
    tags = LIKELY_ERROR)
public final class ErrorProneSupportBugCheckerDocumentationLink extends BugChecker
    implements ClassTreeMatcher {
  private static final long serialVersionUID = 1L;
  private static final Matcher<ClassTree> IS_ERROR_PRONE_SUPPORT_CLASS =
      packageStartsWith("tech.picnic.errorprone");
  private static final MultiMatcher<ClassTree, AnnotationTree> HAS_BUG_PATTERN_ANNOTATION =
      annotations(AT_LEAST_ONE, isType(BugPattern.class.getName()));

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    if (ASTHelpers.findEnclosingNode(state.getPath(), ClassTree.class) != null) {
      /* This is a nested class, likely used within a test class. */
      return Description.NO_MATCH;
    }

    if (!IS_ERROR_PRONE_SUPPORT_CLASS.matches(tree, state)) {
      /*
       * Bug checkers defined elsewhere are unlikely to be documented on the Error Prone Support
       * website.
       */
      return Description.NO_MATCH;
    }

    ImmutableList<AnnotationTree> bugPatternAnnotations =
        HAS_BUG_PATTERN_ANNOTATION.multiMatchResult(tree, state).matchingNodes();
    if (bugPatternAnnotations.isEmpty()) {
      return Description.NO_MATCH;
    }

    AnnotationTree annotation = Iterables.getOnlyElement(bugPatternAnnotations);

    //    SuggestedFix.Builder x =
    //        SuggestedFixes.updateAnnotationArgumentValues(
    //            annotation, state, "linkType", ImmutableList.of("BugPattern.LinkType.CUSTOM"));

    SuggestedFix fix = validateArguments(annotation, tree.getSimpleName(), state);

    return fix.isEmpty() ? Description.NO_MATCH : describeMatch(tree, fix);
  }

  private static SuggestedFix validateArguments(
      AnnotationTree annotation, Name className, VisitorState state) {
    SuggestedFix.Builder suggestedFix = SuggestedFix.builder();

    ExpressionTree link = AnnotationMatcherUtils.getArgument(annotation, "link");
    if (link == null || !isDocumentationUrl(link, className)) {
      // XXX: Fix arg.
      suggestedFix.merge(
          SuggestedFixes.updateAnnotationArgumentValues(
              annotation, state, "link", ImmutableList.of("XXX")));
    }

    return suggestedFix.build();
  }

  private static boolean isDocumentationUrl(ExpressionTree link, Name className) {
    String value = ASTHelpers.constValue(link, String.class);
    if (!(BUG_PATTERNS_BASE_URL + className).contentEquals(value)) {
      return false;
    }

    if (link instanceof BinaryTree) {
      BinaryTree tree = (BinaryTree) link;
      verify(tree.getKind() == Tree.Kind.PLUS, "Unexpected binary operator");

      ASTHelpers.getSymbol(tree.getLeftOperand()).name.contentEquals("BUG_PATTERNS_BASE_URL");
    }

    return true;
  }
}
