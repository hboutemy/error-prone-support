package tech.picnic.errorprone.bugpatterns.util;

import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.predicates.TypePredicate;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;

/**
 * A collection of extra static factory methods to make the {@link Matcher} DSL read more fluently.
 */
public final class MoreMatchers {
  private MoreMatchers() {}

  private static TypePredicate hasAnnotation(String annotationClassName) {
    return (type, state) -> ASTHelpers.hasAnnotation(type.tsym, annotationClassName, state);
  }

  /**
   * Determines whether an expression has a meta annotation of the given class name. This includes
   * annotations inherited from superclasses due to @Inherited.
   *
   * @param <T> the type of the expression tree
   * @param annotationClass the binary class name of the annotation (e.g.
   *     "javax.annotation.Nullable", or "some.package.OuterClassName$InnerClassName")
   * @return a {@link Matcher} that matches expressions with the specified meta annotation
   */
  public static <T extends Tree> Matcher<T> hasMetaAnnotation(String annotationClass) {
    TypePredicate typePredicate = hasAnnotation(annotationClass);
    return (tree, state) -> {
      Symbol sym = ASTHelpers.getSymbol(tree);
      return sym != null && typePredicate.apply(sym.type, state);
    };
  }
}
