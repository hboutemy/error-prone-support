package tech.picnic.errorprone.bugpatterns;

import static com.google.auto.common.MoreStreams.toImmutableList;
import static com.google.errorprone.BugPattern.LinkType.NONE;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.BugPattern.StandardTags.SIMPLIFICATION;
import static com.google.errorprone.matchers.Matchers.isType;
import static com.sun.source.tree.Tree.Kind.MEMBER_SELECT;
import static java.util.stream.Collectors.joining;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.AnnotationMatcherUtils;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree.Kind;
import java.util.Optional;
import tech.picnic.errorprone.bugpatterns.util.SourceCode;

/**
 * A {@link BugChecker} which flags JUnit tests with {@link
 * org.junit.jupiter.params.provider.MethodSource} that can be written as a {@link
 * org.junit.jupiter.params.provider.ValueSource}.
 */
@AutoService(BugChecker.class)
@BugPattern(
    summary =
        "Prefer `@ValueSource` over a `@MethodSource` with arguments containing a single argument",
    linkType = NONE,
    severity = SUGGESTION,
    tags = SIMPLIFICATION)
public final class JUnitValueSource extends BugChecker implements MethodTreeMatcher {
  private static final long serialVersionUID = 1L;
  private static final ImmutableMap<String, String> METHOD_SOURCE_PARAMETER_TYPES =
      ImmutableMap.<String, String>builder()
          .put("boolean", "booleans")
          .put("byte", "bytes")
          .put("char", "chars")
          .put("double", "doubles")
          .put("float", "floats")
          .put("int", "ints")
          .put("java.lang.Boolean", "booleans")
          .put("java.lang.Byte", "bytes")
          .put("java.lang.Character", "chars")
          .put("java.lang.Class", "classes")
          .put("java.lang.Double", "doubles")
          .put("java.lang.Float", "floats")
          .put("java.lang.Integer", "ints")
          .put("java.lang.Long", "longs")
          .put("java.lang.Short", "shorts")
          .put("java.lang.String", "strings")
          .put("long", "longs")
          .put("short", "shorts")
          .build();
  private static final Matcher<AnnotationTree> METHOD_SOURCE_ANNOTATION =
      isType("org.junit.jupiter.params.provider.MethodSource");

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    return ASTHelpers.getAnnotations(tree).stream()
        .filter(annotation -> METHOD_SOURCE_ANNOTATION.matches(annotation, state))
        .findFirst()
        .flatMap(annotation -> tryValueSourceFix(tree, annotation, state))
        .orElse(Description.NO_MATCH);
  }

  private Optional<Description> tryValueSourceFix(
      MethodTree tree, AnnotationTree annotation, VisitorState state) {
    return getParameterType(tree)
        .flatMap(type -> tryConstructValueSourceFix(tree, type, annotation, state));
  }

  private static Optional<String> getParameterType(MethodTree tree) {
    if (tree.getParameters().isEmpty() || tree.getParameters().size() > 1) {
      return Optional.empty();
    }

    return Optional.ofNullable(ASTHelpers.getType(tree.getParameters().get(0)))
        .map(type -> METHOD_SOURCE_PARAMETER_TYPES.get(type.tsym.getQualifiedName().toString()));
  }

  private Optional<Description> tryConstructValueSourceFix(
      MethodTree methodTree,
      String parameterType,
      AnnotationTree methodSourceAnnotation,
      VisitorState state) {
    Optional<MethodInvocationTree> methodInvocationTree =
        getMethodSourceMethod(methodSourceAnnotation, state)
            .flatMap(JUnitValueSource::getReturnTree)
            .filter(MethodInvocationTree.class::isInstance)
            .map(MethodInvocationTree.class::cast)
            .filter(rt -> !isSimpleStream(rt));

    if (methodInvocationTree.isEmpty()) {
      return Optional.empty();
    }

    ImmutableList<String> arguments =
        methodInvocationTree.orElseThrow().getArguments().stream()
            .filter(MethodInvocationTree.class::isInstance)
            .map(MethodInvocationTree.class::cast)
            .map(invocation -> collectValues(invocation, state))
            .collect(toImmutableList());

    return Optional.of(
        describeMatch(
            methodTree,
            SuggestedFix.builder()
                .addImport("org.junit.jupiter.params.provider.ValueSource")
                .replace(
                    methodSourceAnnotation,
                    String.format(
                        "@ValueSource(%s = {%s})", parameterType, String.join(", ", arguments)))
                .delete(getMethodSourceMethod(methodSourceAnnotation, state).orElse(null))
                .build()));
  }

  private static Optional<ReturnTree> getReturnTree(MethodTree methodTree) {
    return methodTree.getBody().getStatements().stream()
        .filter(ReturnTree.class::isInstance)
        .findFirst()
        .map(ReturnTree.class::cast);
  }

  private static boolean isSimpleStream(MethodInvocationTree invocationTree) {
    ExpressionTree methodSelect = invocationTree.getMethodSelect();
    return methodSelect.getKind() == MEMBER_SELECT
        && ((MemberSelectTree) methodSelect).getIdentifier().contentEquals("of");
  }

  private static Optional<MethodTree> getMethodSourceMethod(
      AnnotationTree annotation, VisitorState state) {
    ClassTree classTree = state.findEnclosing(ClassTree.class);
    if (classTree == null) {
      return Optional.empty();
    }

    Optional<String> methodSourceMethodName =
        Optional.ofNullable(AnnotationMatcherUtils.getArgument(annotation, "value"))
            .filter(expression -> expression.getKind() == Kind.STRING_LITERAL)
            .map(literal -> ((LiteralTree) literal).getValue().toString());

    return classTree.getMembers().stream()
        .filter(MethodTree.class::isInstance)
        .map(MethodTree.class::cast)
        .filter(method -> method.getName().contentEquals(methodSourceMethodName.orElseThrow()))
        .findFirst();
  }

  private static String collectValues(MethodInvocationTree tree, VisitorState state) {
    return tree.getArguments().stream()
        .map(expression -> SourceCode.treeToString(expression, state))
        .collect(joining(", "));
  }
}
