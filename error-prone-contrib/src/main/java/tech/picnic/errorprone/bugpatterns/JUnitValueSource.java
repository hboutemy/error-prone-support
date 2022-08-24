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
import static com.google.errorprone.util.MoreAnnotations.getAnnotationValue;
import static com.sun.source.tree.Tree.Kind.MEMBER_SELECT;
import static java.util.stream.Collectors.joining;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.MoreAnnotations;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
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

  private static final String METHOD_SOURCE_ANNOTATION_FQCN =
      "org.junit.jupiter.params.provider.MethodSource";

  // XXX: Rename this, because we now already match on the correct parameter types.
  private static final Matcher<MethodTree> IS_METHOD_SOURCE_WITH_ONE_PARAMETER =
      allOf(
          annotations(AT_LEAST_ONE, isType("org.junit.jupiter.params.provider.MethodSource")),
          methodHasParameters(
              anyOf(
                  isPrimitiveOrBoxedPrimitiveType(),
                  isSameType(String.class),
                  // XXX:There should be a nicer way to do this.
                  (variableTree, state) ->
                      ASTHelpers.getType(variableTree)
                          .baseType()
                          .tsym
                          .toString()
                          .equals("java.lang.Class"))));

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (IS_METHOD_SOURCE_WITH_ONE_PARAMETER.matches(tree, state)) {
      Type type = ASTHelpers.getType(Iterables.getOnlyElement(tree.getParameters()));
      // XXX: Find a nice way to get the specific `AnnotationTree` without specifying the index.
      // This also shows we should add a test where the annotations are in a different order ;).
      return tryConstructValueSourceFix(tree, type, ASTHelpers.getAnnotations(tree).get(1), state);
    }
    return Description.NO_MATCH;
  }

  private Description tryConstructValueSourceFix(
      MethodTree methodTree,
      Type parameterType,
      AnnotationTree methodSourceAnnotation,
      VisitorState state) {
    String methodSourceValue = getMethodSourceAnnotationValue(ASTHelpers.getSymbol(methodTree));
    MethodTree factoryMethod = getFactoryMethod(methodSourceValue, state);

    Optional<MethodInvocationTree> methodInvocationTree =
        getReturnTree(factoryMethod)
            .map(ReturnTree::getExpression)
            .filter(MethodInvocationTree.class::isInstance)
            .map(MethodInvocationTree.class::cast)
            .filter(JUnitValueSource::isSimpleStream);

    if (methodInvocationTree.isEmpty()) {
      return null;
    }

    ImmutableList<String> arguments =
        methodInvocationTree.orElseThrow().getArguments().stream()
            .filter(MethodInvocationTree.class::isInstance)
            .map(MethodInvocationTree.class::cast)
            .map(invocation -> collectValues(invocation, state))
            .collect(toImmutableList());

    return describeMatch(
        methodTree,
        SuggestedFix.builder()
            .addImport("org.junit.jupiter.params.provider.ValueSource")
            .replace(
                methodSourceAnnotation,
                String.format(
                    "@ValueSource(%s = {%s})",
                    METHOD_SOURCE_PARAMETER_TYPES.get(parameterType.toString()),
                    String.join(", ", arguments)))
            .delete(factoryMethod)
            .build());
  }

  private static String getMethodSourceAnnotationValue(Symbol symbol) {
    return symbol.getRawAttributes().stream()
        .filter(a -> a.type.tsym.getQualifiedName().contentEquals(METHOD_SOURCE_ANNOTATION_FQCN))
        .findAny()
        .flatMap(a -> getAnnotationValue(a, "value"))
        .stream()
        .flatMap(MoreAnnotations::asStrings)
        .findFirst()
        .orElseThrow();
  }

  private static Optional<ReturnTree> getReturnTree(MethodTree methodTree) {
    return methodTree.getBody().getStatements().stream()
        .filter(ReturnTree.class::isInstance)
        .findFirst()
        .map(ReturnTree.class::cast);
  }

  // XXX: Didn't check this one. What is a `simpleStream`? I think we can also make this a
  // `Matcher`.
  private static boolean isSimpleStream(MethodInvocationTree invocationTree) {
    ExpressionTree methodSelect = invocationTree.getMethodSelect();
    return methodSelect.getKind() == MEMBER_SELECT
        && ((MemberSelectTree) methodSelect).getIdentifier().contentEquals("of");
  }

  private static MethodTree getFactoryMethod(String factoryMethodName, VisitorState state) {
    return state.findEnclosing(ClassTree.class).getMembers().stream()
        .filter(MethodTree.class::isInstance)
        .map(MethodTree.class::cast)
        .filter(method -> method.getName().contentEquals(factoryMethodName))
        .findFirst()
        .orElseThrow();
  }

  // XXX: Improve the method name, what are we _really_ collecting.
  private static String collectValues(MethodInvocationTree tree, VisitorState state) {
    return tree.getArguments().stream()
        .map(expression -> SourceCode.treeToString(expression, state))
        .collect(joining(", "));
  }
}
