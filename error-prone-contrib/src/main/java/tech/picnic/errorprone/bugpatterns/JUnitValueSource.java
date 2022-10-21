package tech.picnic.errorprone.bugpatterns;

import static com.google.auto.common.MoreStreams.toImmutableList;
import static com.google.errorprone.BugPattern.LinkType.CUSTOM;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.BugPattern.StandardTags.SIMPLIFICATION;
import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.AT_LEAST_ONE;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.annotations;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.isPrimitiveOrBoxedPrimitiveType;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.isType;
import static com.google.errorprone.matchers.Matchers.methodHasParameters;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static tech.picnic.errorprone.bugpatterns.util.Documentation.BUG_PATTERNS_BASE_URL;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;
import java.util.Optional;
import java.util.stream.Stream;
import tech.picnic.errorprone.bugpatterns.util.SourceCode;

/**
 * A {@link BugChecker} that flags JUnit tests with {@link
 * org.junit.jupiter.params.provider.MethodSource} that can be written as a {@link
 * org.junit.jupiter.params.provider.ValueSource}.
 */
@AutoService(BugChecker.class)
@BugPattern(
    summary = "Prefer `@ValueSource` over a `@MethodSource` that contains only a single argument",
    linkType = CUSTOM,
    link = BUG_PATTERNS_BASE_URL + "JUnitValueSource",
    severity = SUGGESTION,
    tags = SIMPLIFICATION)
public final class JUnitValueSource extends BugChecker implements MethodTreeMatcher {
  private static final long serialVersionUID = 1L;
  private static final Matcher<ExpressionTree> STREAM_OF_ARGUMENTS =
      staticMethod().onClass(Stream.class.getName()).named("of");
  private static final Matcher<MethodTree> VALUE_SOURCE_CANDIDATE =
      allOf(
          annotations(AT_LEAST_ONE, isType("org.junit.jupiter.params.provider.MethodSource")),
          methodHasParameters(
              anyOf(
                  isPrimitiveOrBoxedPrimitiveType(),
                  isSameType(String.class),
                  isSameType(state -> state.getSymtab().classType))));

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (!VALUE_SOURCE_CANDIDATE.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    Type parameterType = ASTHelpers.getType(Iterables.getOnlyElement(tree.getParameters()));
    AnnotationTree annotationTree =
        ASTHelpers.getAnnotationWithSimpleName(
            tree.getModifiers().getAnnotations(), "MethodSource");
    Optional<Fix> fix = tryConstructValueSourceFix(parameterType, annotationTree, state);

    return fix.isPresent() ? describeMatch(tree, fix.orElseThrow()) : Description.NO_MATCH;
  }

  // XXX: Multiple factories.
  private static Optional<Fix> tryConstructValueSourceFix(
      Type parameterType, AnnotationTree methodSourceAnnotation, VisitorState state) {
    String factoryMethodName = extractFactoryMethodName(methodSourceAnnotation);
    MethodTree factoryMethod = getFactoryMethod(factoryMethodName, state);

    Optional<String> valueSourceAttributeValue =
        getReturnTreeExpression(factoryMethod)
            .filter(MethodInvocationTree.class::isInstance)
            .map(MethodInvocationTree.class::cast)
            .filter(method -> STREAM_OF_ARGUMENTS.matches(method, state))
            .flatMap(m -> extractValueFromArgumentBody(state, m));

    return valueSourceAttributeValue.map(
        attributeValue ->
            SuggestedFix.builder()
                .addImport("org.junit.jupiter.params.provider.ValueSource")
                .replace(
                    methodSourceAnnotation,
                    String.format(
                        "@ValueSource(%s = {%s})",
                        toValueSourceAttributeName(parameterType.tsym.name.toString()),
                        attributeValue))
                .delete(factoryMethod)
                .build());
  }

  private static String extractFactoryMethodName(AnnotationTree methodSourceAnnotation) {
    ExpressionTree expression =
        ((AssignmentTree) Iterables.getOnlyElement(methodSourceAnnotation.getArguments()))
            .getExpression();
    return ASTHelpers.getType(expression).stringValue();
  }

  private static Optional<String> extractValueFromArgumentBody(
      VisitorState state, MethodInvocationTree m) {
    ImmutableList<String> args =
        m.getArguments().stream()
            .filter(MethodInvocationTree.class::isInstance)
            .map(MethodInvocationTree.class::cast)
            .flatMap(mit -> mit.getArguments().stream())
            .filter(JUnitValueSource::isCompileTimeConstant)
            .map(argument -> SourceCode.treeToString(argument, state))
            .collect(toImmutableList());

    if (args.size() != m.getArguments().size()) {
      return Optional.empty();
    }
    return Optional.of(String.join(", ", args));
  }

  private static Optional<ExpressionTree> getReturnTreeExpression(MethodTree methodTree) {
    return methodTree.getBody().getStatements().stream()
        .filter(ReturnTree.class::isInstance)
        .findFirst()
        .map(ReturnTree.class::cast)
        .map(ReturnTree::getExpression);
  }

  private static MethodTree getFactoryMethod(String factoryMethodName, VisitorState state) {
    return state.findEnclosing(ClassTree.class).getMembers().stream()
        .filter(MethodTree.class::isInstance)
        .map(MethodTree.class::cast)
        .filter(method -> method.getName().contentEquals(factoryMethodName))
        .findFirst()
        .orElseThrow();
  }

  private static String toValueSourceAttributeName(String type) {
    switch (type) {
      case "Class":
        return "classes";
      case "Character":
        return "chars";
      case "Integer":
        return "ints";
      default:
        return type.toLowerCase() + "s";
    }
  }

  private static boolean isCompileTimeConstant(ExpressionTree argument) {
    return argument.getKind() == Tree.Kind.MEMBER_SELECT
        ? ((MemberSelectTree) argument).getIdentifier().contentEquals("class")
        : ASTHelpers.constValue(argument) != null;
  }
}
