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
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Type;
import java.util.Optional;
import java.util.stream.Stream;
import javax.lang.model.type.TypeKind;
import tech.picnic.errorprone.bugpatterns.util.SourceCode;

/**
 * A {@link BugChecker} that flags JUnit tests with {@link
 * org.junit.jupiter.params.provider.MethodSource} that can be written as a {@link
 * org.junit.jupiter.params.provider.ValueSource}.
 */
// XXX: Support rewriting when there are multiple sources defined for the `@MethodSource`, iff
// applicable.
// XXX: Don't remove factory methods that are used by another `@MethodSource`.
@AutoService(BugChecker.class)
@BugPattern(
    summary =
        "Prefer `@ValueSource` over a `@MethodSource` that contains only one type of arguments",
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

    return tryConstructValueSourceFix(parameterType, annotationTree, state)
        .map(fix -> describeMatch(tree, fix.build()))
        .orElse(Description.NO_MATCH);
  }

  private static Optional<SuggestedFix.Builder> tryConstructValueSourceFix(
      Type parameterType, AnnotationTree methodSourceAnnotation, VisitorState state) {
    Optional<String> factoryMethodName = extractSingleFactoryMethodName(methodSourceAnnotation);
    if (factoryMethodName.isEmpty()) {
      /* `@MethodSource` defines more than one source. */
      return Optional.empty();
    }
    MethodTree factoryMethod = findFactoryMethod(factoryMethodName.orElseThrow(), state);

    Optional<String> valueSourceAttributeValue =
        getReturnTree(factoryMethod)
            .map(ReturnTree::getExpression)
            .filter(MethodInvocationTree.class::isInstance)
            .map(MethodInvocationTree.class::cast)
            .filter(m -> STREAM_OF_ARGUMENTS.matches(m, state))
            .flatMap(m -> extractArgumentsFromStream(m, state));

    return valueSourceAttributeValue.map(
        attributeValue ->
            SuggestedFix.builder()
                .addImport("org.junit.jupiter.params.provider.ValueSource")
                .replace(
                    methodSourceAnnotation,
                    String.format(
                        "@ValueSource(%s = {%s})",
                        toValueSourceAttributeName(parameterType.toString()), attributeValue))
                .delete(factoryMethod));
  }

  private static Optional<ReturnTree> getReturnTree(MethodTree methodTree) {
    return methodTree.getBody().getStatements().stream()
        .filter(ReturnTree.class::isInstance)
        .findFirst()
        .map(ReturnTree.class::cast);
  }

  private static Optional<String> extractSingleFactoryMethodName(
      AnnotationTree methodSourceAnnotation) {
    ExpressionTree attributeExpression =
        ((AssignmentTree) Iterables.getOnlyElement(methodSourceAnnotation.getArguments()))
            .getExpression();
    Type attributeType = ASTHelpers.getType(attributeExpression);
    return attributeType.getKind() == TypeKind.ARRAY
        ? Optional.empty()
        : Optional.of(attributeType.stringValue());
  }

  private static MethodTree findFactoryMethod(String factoryMethodName, VisitorState state) {
    return state.findEnclosing(ClassTree.class).getMembers().stream()
        .filter(MethodTree.class::isInstance)
        .map(MethodTree.class::cast)
        .filter(method -> method.getName().contentEquals(factoryMethodName))
        .findFirst()
        .orElseThrow();
  }

  private static Optional<String> extractArgumentsFromStream(
      MethodInvocationTree tree, VisitorState state) {
    ImmutableList<String> arguments =
        tree.getArguments().stream()
            .filter(MethodInvocationTree.class::isInstance)
            .map(MethodInvocationTree.class::cast)
            .flatMap(mit -> mit.getArguments().stream())
            .filter(JUnitValueSource::isCompileTimeConstant)
            .map(argument -> SourceCode.treeToString(argument, state))
            .collect(toImmutableList());

    /* Not all values are compile-time constants. */
    if (arguments.size() != tree.getArguments().size()) {
      return Optional.empty();
    }
    return Optional.of(String.join(", ", arguments));
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
    return argument.getKind() == Kind.MEMBER_SELECT
        ? ((MemberSelectTree) argument).getIdentifier().contentEquals("class")
        : ASTHelpers.constValue(argument) != null;
  }
}
