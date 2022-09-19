package tech.picnic.errorprone.bugpatterns;

import static com.google.auto.common.MoreStreams.toImmutableList;
import static com.google.errorprone.BugPattern.LinkType.NONE;
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
import static java.util.stream.Collectors.joining;

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
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import tech.picnic.errorprone.bugpatterns.util.SourceCode;

/**
 * A {@link BugChecker} which flags JUnit tests with {@link
 * org.junit.jupiter.params.provider.MethodSource} that can be written as a {@link
 * org.junit.jupiter.params.provider.ValueSource}.
 */
@AutoService(BugChecker.class)
@BugPattern(
    summary = "Prefer `@ValueSource` over a `@MethodSource` that contains only a single argument",
    linkType = NONE,
    severity = SUGGESTION,
    tags = SIMPLIFICATION)
public final class JUnitValueSource extends BugChecker implements MethodTreeMatcher {
  private static final long serialVersionUID = 1L;

  // XXX: Add something about the argument types?
  private static final Matcher<MethodInvocationTree> STREAM_OF_ARGUMENTS =
      allOf(staticMethod().onClass(Stream.class.getName()).named("of"));
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

    Type type = ASTHelpers.getType(Iterables.getOnlyElement(tree.getParameters()));
    AnnotationTree annotationTree =
        ASTHelpers.getAnnotationWithSimpleName(
            tree.getModifiers().getAnnotations(), "MethodSource");
    return tryConstructValueSourceFix(tree, type, annotationTree, state);
  }

  private Description tryConstructValueSourceFix(
      MethodTree methodTree,
      Type parameterType,
      AnnotationTree methodSourceAnnotation,
      VisitorState state) {
    String factoryMethodName =
        ((JCAssign) Iterables.getOnlyElement(methodSourceAnnotation.getArguments()))
            .rhs.type.stringValue();
    MethodTree factoryMethod = getFactoryMethod(factoryMethodName, state);

    Optional<MethodInvocationTree> methodInvocationTree =
        getReturnTreeExpression(factoryMethod)
            .filter(MethodInvocationTree.class::isInstance)
            .map(MethodInvocationTree.class::cast)
            .filter(m -> STREAM_OF_ARGUMENTS.matches(m, state));

    if (methodInvocationTree.isEmpty()) {
      return Description.NO_MATCH;
    }

    if (!methodInvocationTree.orElseThrow().getArguments().stream()
        .filter(MethodInvocationTree.class::isInstance)
        .map(MethodInvocationTree.class::cast)
        .allMatch(argumentsMethod -> allArgumentsAreConstant(argumentsMethod.getArguments()))) {
      return Description.NO_MATCH;
    }

    ImmutableList<String> arguments =
        methodInvocationTree.orElseThrow().getArguments().stream()
            .filter(MethodInvocationTree.class::isInstance)
            .map(MethodInvocationTree.class::cast)
            .map(invocation -> collectValuesFromArgumentsMethod(invocation, state))
            .collect(toImmutableList());

    return describeMatch(
        methodTree,
        SuggestedFix.builder()
            .addImport("org.junit.jupiter.params.provider.ValueSource")
            .replace(
                methodSourceAnnotation,
                String.format(
                    "@ValueSource(%s = {%s})",
                    getAnnotationParameterName(parameterType.tsym.name.toString()),
                    String.join(", ", arguments)))
            .delete(factoryMethod)
            .build());
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

  private static String getAnnotationParameterName(String typeName) {
    switch (typeName) {
      case "Class":
        return "classes";
      case "Character":
        return "chars";
      case "Integer":
        return "ints";
      default:
        return typeName.toLowerCase() + "s";
    }
  }

  private static boolean allArgumentsAreConstant(List<? extends ExpressionTree> arguments) {
    return arguments.stream()
        .allMatch(
            tree -> {
              // XXX: Class literals don't have a constant value, but CAN be used in annotations
              if (tree.getKind() == Tree.Kind.MEMBER_SELECT) {
                return ((MemberSelectTree) tree).getIdentifier().contentEquals("class");
              }
              return ASTHelpers.constValue(tree) != null;
            });
  }

  private static String collectValuesFromArgumentsMethod(
      MethodInvocationTree tree, VisitorState state) {
    return tree.getArguments().stream()
        .map(expression -> SourceCode.treeToString(expression, state))
        .collect(joining(", "));
  }
}
