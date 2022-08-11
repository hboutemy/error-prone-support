package tech.picnic.errorprone.bugpatterns;

import static com.google.auto.common.MoreStreams.toImmutableList;
import static com.google.errorprone.BugPattern.LinkType.NONE;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.BugPattern.StandardTags.SIMPLIFICATION;
import static com.sun.source.tree.Tree.Kind.MEMBER_SELECT;
import static com.sun.source.tree.Tree.Kind.METHOD;
import static com.sun.source.tree.Tree.Kind.METHOD_INVOCATION;
import static com.sun.source.tree.Tree.Kind.RETURN;
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
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import java.util.Optional;
import tech.picnic.errorprone.bugpatterns.util.SourceCode;

/**
 * A {@link BugChecker} which flags JUnit tests with {@link
 * org.junit.jupiter.params.provider.MethodSource} that can be written as a {@link
 * org.junit.jupiter.params.provider.ValueSource}.
 */
@AutoService(BugChecker.class)
@BugPattern(
    summary = "JUnit parameter provider can likely be improved",
    linkType = NONE,
    severity = SUGGESTION,
    tags = SIMPLIFICATION)
public final class JUnitValueSource extends BugChecker implements MethodTreeMatcher {
  private static final long serialVersionUID = 1L;
  private static final ImmutableMap<String, String> VALID_PARAMETER_TYPES =
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
  private static final Matcher<AnnotationTree> METHOD_SOURCE =
      Matchers.isType("org.junit.jupiter.params.provider.MethodSource");

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    AnnotationTree methodSourceAnnotation =
        ASTHelpers.getAnnotations(tree).stream()
            .filter(annotation -> METHOD_SOURCE.matches(annotation, state))
            .findFirst()
            .orElse(null);
    if (methodSourceAnnotation == null) {
      return Description.NO_MATCH;
    }

    String parameterType = hasValidParameters(tree).orElse(null);
    if (parameterType == null) {
      return Description.NO_MATCH;
    }

    String valueSource = getValueSource(parameterType, methodSourceAnnotation, state).orElse(null);
    if (valueSource == null) {
      return Description.NO_MATCH;
    }

    return describeMatch(
        tree,
        SuggestedFix.builder()
            .addImport("org.junit.jupiter.params.provider.ValueSource")
            .replace(methodSourceAnnotation, valueSource)
            .delete(getMethodSourceMethod(methodSourceAnnotation, state).orElse(null))
            .build());
  }

  private static Optional<String> hasValidParameters(MethodTree tree) {
    if (tree.getParameters().isEmpty() || tree.getParameters().size() > 1) {
      return Optional.empty();
    }

    return Optional.ofNullable(ASTHelpers.getType(tree.getParameters().get(0)))
        .map(type -> VALID_PARAMETER_TYPES.get(type.tsym.getQualifiedName().toString()));
  }

  private static Optional<String> getValueSource(
      String parameterType, AnnotationTree methodSourceAnnotation, VisitorState state) {
    MethodTree methodSourceTree = getMethodSourceMethod(methodSourceAnnotation, state).orElse(null);
    if (methodSourceTree == null) {
      return Optional.empty();
    }

    ReturnTree returnTree = getReturnTree(methodSourceTree).orElse(null);
    if (returnTree == null) {
      return Optional.empty();
    }

    MethodInvocationTree invocationTree = ((MethodInvocationTree) returnTree.getExpression());
    if (!isSimpleStream(invocationTree)) {
      return Optional.empty();
    }

    ImmutableList<String> arguments =
        invocationTree.getArguments().stream()
            .filter(argument -> argument.getKind() == METHOD_INVOCATION)
            .map(MethodInvocationTree.class::cast)
            .map(invocation -> collectValues(invocation, state))
            .collect(toImmutableList());

    return Optional.of(
        String.format("@ValueSource(%s = {%s})", parameterType, String.join(", ", arguments)));
  }

  private static Optional<ReturnTree> getReturnTree(MethodTree methodSourceTree) {
    return methodSourceTree.getBody().getStatements().stream()
        .filter(statement -> statement.getKind() == RETURN)
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

    String methodSourceMethodName =
        Optional.ofNullable(AnnotationMatcherUtils.getArgument(annotation, "value"))
            .filter(expression -> expression.getKind() == Tree.Kind.STRING_LITERAL)
            .map(literal -> ((LiteralTree) literal).getValue().toString())
            .orElse(null);
    if (methodSourceMethodName == null) {
      return Optional.empty();
    }

    return classTree.getMembers().stream()
        .filter(member -> member.getKind() == METHOD)
        .filter(method -> ((MethodTree) method).getName().contentEquals(methodSourceMethodName))
        .findFirst()
        .map(MethodTree.class::cast);
  }

  private static String collectValues(MethodInvocationTree tree, VisitorState state) {
    return tree.getArguments().stream()
        .map(expression -> SourceCode.treeToString(expression, state))
        .collect(joining(", "));
  }
}
