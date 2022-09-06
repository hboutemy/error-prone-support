package tech.picnic.errorprone.bugpatterns.util;

import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.jvm.Target;

/**
 * A collection of Error Prone utility methods for dealing with the source code representation of
 * AST nodes.
 */
// XXX: Can we locate this code in a better place? Maybe contribute it upstream?
public final class SourceCode {
  // XXX: Proper name for this?
  private static final String TEXT_BLOCK_MARKER = "\"\"\"";

  private SourceCode() {}

  /**
   * Tells whether the targeted Java language level supports text blocks.
   *
   * @param state A {@link VisitorState} from which the targeted Java language level can be derived.
   * @return {@code true} iff text block expressions are supported.
   */
  // XXX: Add tests!
  public static boolean isTextBlockSupported(VisitorState state) {
    // XXX: String comparison is for JDK 11 compatibility. Is there a better way?
    return Target.instance(state.context).toString().compareTo("JDK1_15") >= 0;
    // return Target.instance(state.context).compareTo(Target.JDK1_15) >= 0;
  }

  /**
   * Tells whether the given expression is a text block.
   *
   * @param tree The AST node of interest.
   * @param state A {@link VisitorState} describing the context in which the given {@link
   *     ExpressionTree} is found.
   * @return {@code true} iff the given expression is a text block.
   */
  // XXX: Add tests!
  public static boolean isTextBlock(ExpressionTree tree, VisitorState state) {
    if (!(tree instanceof LiteralTree)) {
      return false;
    }

    LiteralTree literalTree = (LiteralTree) tree;
    if (literalTree.getKind() != Tree.Kind.STRING_LITERAL) {
      return false;
    }

    /* If the source code is unavailable then we assume that this literal is _not_ a text block. */
    String src = state.getSourceForNode(tree);
    return src != null && src.startsWith(TEXT_BLOCK_MARKER);
  }

  /**
   * Returns a string representation of the given {@link Tree}, preferring the original source code
   * (if available) over its prettified representation.
   *
   * @param tree The AST node of interest.
   * @param state A {@link VisitorState} describing the context in which the given {@link Tree} is
   *     found.
   * @return A non-{@code null} string.
   */
  public static String treeToString(Tree tree, VisitorState state) {
    String src = state.getSourceForNode(tree);
    return src != null ? src : tree.toString();
  }
}
