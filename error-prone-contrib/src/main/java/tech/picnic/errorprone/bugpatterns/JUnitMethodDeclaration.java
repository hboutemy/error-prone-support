package tech.picnic.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.LinkType.CUSTOM;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.BugPattern.StandardTags.SIMPLIFICATION;
import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.AT_LEAST_ONE;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.annotations;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.enclosingClass;
import static com.google.errorprone.matchers.Matchers.hasModifier;
import static com.google.errorprone.matchers.Matchers.isType;
import static java.util.function.Predicate.not;
import static tech.picnic.errorprone.bugpatterns.util.ConflictDetection.findMethodRenameBlocker;
import static tech.picnic.errorprone.bugpatterns.util.Documentation.BUG_PATTERNS_BASE_URL;
import static tech.picnic.errorprone.bugpatterns.util.JUnit.SETUP_OR_TEARDOWN_METHOD;
import static tech.picnic.errorprone.bugpatterns.util.JUnit.TEST_METHOD;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodTree;
import java.util.Optional;
import javax.lang.model.element.Modifier;

/** A {@link BugChecker} that flags non-canonical JUnit method declarations. */
// XXX: Consider introducing a class-level check that enforces that test classes:
// 1. Are named `*Test` or `Abstract*TestCase`.
// 2. If not `abstract`, are package-private and don't have public methods and subclasses.
// 3. Only have private fields.
// XXX: If implemented, the current logic could flag only `private` JUnit methods.
@AutoService(BugChecker.class)
@BugPattern(
    summary = "JUnit method declaration can likely be improved",
    link = BUG_PATTERNS_BASE_URL + "JUnitMethodDeclaration",
    linkType = CUSTOM,
    severity = SUGGESTION,
    tags = SIMPLIFICATION)
public final class JUnitMethodDeclaration extends BugChecker implements MethodTreeMatcher {
  private static final long serialVersionUID = 1L;
  private static final String TEST_PREFIX = "test";
  private static final ImmutableSet<Modifier> ILLEGAL_MODIFIERS =
      ImmutableSet.of(Modifier.PRIVATE, Modifier.PROTECTED, Modifier.PUBLIC);
  private static final Matcher<MethodTree> HAS_UNMODIFIABLE_SIGNATURE =
      anyOf(
          annotations(AT_LEAST_ONE, isType("java.lang.Override")),
          allOf(
              Matchers.not(hasModifier(Modifier.FINAL)),
              Matchers.not(hasModifier(Modifier.PRIVATE)),
              enclosingClass(hasModifier(Modifier.ABSTRACT))));

  /** Instantiates a new {@link JUnitMethodDeclaration} instance. */
  public JUnitMethodDeclaration() {}

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (HAS_UNMODIFIABLE_SIGNATURE.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    boolean isTestMethod = TEST_METHOD.matches(tree, state);
    if (!isTestMethod && !SETUP_OR_TEARDOWN_METHOD.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
    SuggestedFixes.removeModifiers(tree.getModifiers(), state, ILLEGAL_MODIFIERS)
        .ifPresent(fixBuilder::merge);

    if (isTestMethod) {
      suggestTestMethodRenameIfApplicable(tree, fixBuilder, state);
    }

    return fixBuilder.isEmpty() ? Description.NO_MATCH : describeMatch(tree, fixBuilder.build());
  }

  private void suggestTestMethodRenameIfApplicable(
      MethodTree tree, SuggestedFix.Builder fixBuilder, VisitorState state) {
    tryCanonicalizeMethodName(tree)
        .ifPresent(
            newName ->
                findMethodRenameBlocker(newName, state)
                    .ifPresentOrElse(
                        blocker -> reportMethodRenameBlocker(tree, blocker, state),
                        () -> fixBuilder.merge(SuggestedFixes.renameMethod(tree, newName, state))));
  }

  private void reportMethodRenameBlocker(MethodTree tree, String reason, VisitorState state) {
    state.reportMatch(
        buildDescription(tree)
            .setMessage(
                String.format(
                    "This method's name should not redundantly start with `%s` (but note that %s)",
                    TEST_PREFIX, reason))
            .build());
  }

  private static Optional<String> tryCanonicalizeMethodName(MethodTree tree) {
    return Optional.of(ASTHelpers.getSymbol(tree).getQualifiedName().toString())
        .filter(name -> name.startsWith(TEST_PREFIX))
        .map(name -> name.substring(TEST_PREFIX.length()))
        .filter(not(String::isEmpty))
        .map(name -> Character.toLowerCase(name.charAt(0)) + name.substring(1))
        .filter(name -> !Character.isDigit(name.charAt(0)));
  }
}
