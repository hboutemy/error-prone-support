package tech.picnic.errorprone.refasterrules;

import static com.google.errorprone.refaster.ImportPolicy.STATIC_IMPORT_ALWAYS;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.reverseOrder;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.Streams;
import com.google.errorprone.refaster.Refaster;
import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import com.google.errorprone.refaster.annotation.MayOptionallyUse;
import com.google.errorprone.refaster.annotation.Placeholder;
import com.google.errorprone.refaster.annotation.UseImportPolicy;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import tech.picnic.errorprone.refaster.annotation.OnlineDocumentation;

/** Refaster rules related to expressions dealing with {@link Stream}s. */
@OnlineDocumentation
final class StreamRules {
  private StreamRules() {}

  /**
   * Prefer {@link Collectors#joining()} over {@link Collectors#joining(CharSequence)} with an empty
   * delimiter string.
   */
  static final class Joining {
    @BeforeTemplate
    Collector<CharSequence, ?, String> before() {
      return joining("");
    }

    @AfterTemplate
    @UseImportPolicy(STATIC_IMPORT_ALWAYS)
    Collector<CharSequence, ?, String> after() {
      return joining();
    }
  }

  /** Prefer {@link Stream#empty()} over less clear alternatives. */
  static final class EmptyStream<T> {
    @BeforeTemplate
    Stream<T> before() {
      return Stream.of();
    }

    @AfterTemplate
    Stream<T> after() {
      return Stream.empty();
    }
  }

  /** Prefer {@link Stream#ofNullable(Object)} over more contrived alternatives. */
  static final class StreamOfNullable<T> {
    @BeforeTemplate
    Stream<T> before(T object) {
      return Refaster.anyOf(
          Stream.of(object).filter(Objects::nonNull), Optional.ofNullable(object).stream());
    }

    @AfterTemplate
    Stream<T> after(T object) {
      return Stream.ofNullable(object);
    }
  }

  /**
   * Prefer {@link Arrays#stream(Object[])} over {@link Stream#of(Object[])}, as the former is
   * clearer.
   */
  static final class StreamOfArray<T> {
    @BeforeTemplate
    Stream<T> before(T[] array) {
      return Stream.of(array);
    }

    @AfterTemplate
    Stream<T> after(T[] array) {
      return Arrays.stream(array);
    }
  }

  /** Don't unnecessarily call {@link Streams#concat(Stream...)}. */
  static final class ConcatOneStream<T> {
    @BeforeTemplate
    Stream<T> before(Stream<T> stream) {
      return Streams.concat(stream);
    }

    @AfterTemplate
    Stream<T> after(Stream<T> stream) {
      return stream;
    }
  }

  /** Prefer {@link Stream#concat(Stream, Stream)} over the Guava alternative. */
  static final class ConcatTwoStreams<T> {
    @BeforeTemplate
    Stream<T> before(Stream<T> s1, Stream<T> s2) {
      return Streams.concat(s1, s2);
    }

    @AfterTemplate
    Stream<T> after(Stream<T> s1, Stream<T> s2) {
      return Stream.concat(s1, s2);
    }
  }

  /** Avoid unnecessary nesting of {@link Stream#filter(Predicate)} operations. */
  abstract static class FilterOuterStreamAfterFlatMap<T, S> {
    @Placeholder
    abstract Stream<S> toStreamFunction(@MayOptionallyUse T element);

    @BeforeTemplate
    Stream<S> before(Stream<T> stream, Predicate<? super S> predicate) {
      return stream.flatMap(v -> toStreamFunction(v).filter(predicate));
    }

    @AfterTemplate
    Stream<S> after(Stream<T> stream, Predicate<? super S> predicate) {
      return stream.flatMap(v -> toStreamFunction(v)).filter(predicate);
    }
  }

  /** Avoid unnecessary nesting of {@link Stream#map(Function)} operations. */
  abstract static class MapOuterStreamAfterFlatMap<T, S, R> {
    @Placeholder
    abstract Stream<S> toStreamFunction(@MayOptionallyUse T element);

    @BeforeTemplate
    Stream<R> before(Stream<T> stream, Function<? super S, ? extends R> function) {
      return stream.flatMap(v -> toStreamFunction(v).map(function));
    }

    @AfterTemplate
    Stream<R> after(Stream<T> stream, Function<? super S, ? extends R> function) {
      return stream.flatMap(v -> toStreamFunction(v)).map(function);
    }
  }

  /** Avoid unnecessary nesting of {@link Stream#flatMap(Function)} operations. */
  abstract static class FlatMapOuterStreamAfterFlatMap<T, S, R> {
    @Placeholder
    abstract Stream<S> toStreamFunction(@MayOptionallyUse T element);

    @BeforeTemplate
    Stream<R> before(
        Stream<T> stream, Function<? super S, ? extends Stream<? extends R>> function) {
      return stream.flatMap(v -> toStreamFunction(v).flatMap(function));
    }

    @AfterTemplate
    Stream<R> after(Stream<T> stream, Function<? super S, ? extends Stream<? extends R>> function) {
      return stream.flatMap(v -> toStreamFunction(v)).flatMap(function);
    }
  }

  /**
   * Where possible, clarify that a mapping operation will be applied only to a single stream
   * element.
   */
  // XXX: Consider whether to have a similar rule for `.findAny()`. For parallel streams it
  // wouldn't be quite the same....
  // XXX: This change is not equivalent for `null`-returning functions, as the original code throws
  // an NPE if the first element is `null`, while the latter yields an empty `Optional`.
  static final class StreamMapFirst<T, S> {
    @BeforeTemplate
    Optional<S> before(Stream<T> stream, Function<? super T, S> function) {
      return stream.map(function).findFirst();
    }

    @AfterTemplate
    Optional<S> after(Stream<T> stream, Function<? super T, S> function) {
      return stream.findFirst().map(function);
    }
  }

  /** In order to test whether a stream has any element, simply try to find one. */
  static final class StreamIsEmpty<T> {
    @BeforeTemplate
    boolean before(Stream<T> stream) {
      return Refaster.anyOf(
          stream.count() == 0,
          stream.count() <= 0,
          stream.count() < 1,
          stream.findFirst().isEmpty());
    }

    @AfterTemplate
    boolean after(Stream<T> stream) {
      return stream.findAny().isEmpty();
    }
  }

  /** In order to test whether a stream has any element, simply try to find one. */
  static final class StreamIsNotEmpty<T> {
    @BeforeTemplate
    boolean before(Stream<T> stream) {
      return Refaster.anyOf(
          stream.count() != 0,
          stream.count() > 0,
          stream.count() >= 1,
          stream.findFirst().isPresent());
    }

    @AfterTemplate
    boolean after(Stream<T> stream) {
      return stream.findAny().isPresent();
    }
  }

  static final class StreamMin<T> {
    @BeforeTemplate
    Optional<T> before(Stream<T> stream, Comparator<? super T> comparator) {
      return Refaster.anyOf(
          stream.max(comparator.reversed()), stream.sorted(comparator).findFirst());
    }

    @AfterTemplate
    Optional<T> after(Stream<T> stream, Comparator<? super T> comparator) {
      return stream.min(comparator);
    }
  }

  static final class StreamMinNaturalOrder<T extends Comparable<? super T>> {
    @BeforeTemplate
    Optional<T> before(Stream<T> stream) {
      return Refaster.anyOf(stream.max(reverseOrder()), stream.sorted().findFirst());
    }

    @AfterTemplate
    @UseImportPolicy(STATIC_IMPORT_ALWAYS)
    Optional<T> after(Stream<T> stream) {
      return stream.min(naturalOrder());
    }
  }

  static final class StreamMax<T> {
    @BeforeTemplate
    Optional<T> before(Stream<T> stream, Comparator<? super T> comparator) {
      return Refaster.anyOf(
          stream.min(comparator.reversed()), Streams.findLast(stream.sorted(comparator)));
    }

    @AfterTemplate
    Optional<T> after(Stream<T> stream, Comparator<? super T> comparator) {
      return stream.max(comparator);
    }
  }

  static final class StreamMaxNaturalOrder<T extends Comparable<? super T>> {
    @BeforeTemplate
    Optional<T> before(Stream<T> stream) {
      return Refaster.anyOf(stream.min(reverseOrder()), Streams.findLast(stream.sorted()));
    }

    @AfterTemplate
    @UseImportPolicy(STATIC_IMPORT_ALWAYS)
    Optional<T> after(Stream<T> stream) {
      return stream.max(naturalOrder());
    }
  }

  /** Prefer {@link Stream#noneMatch(Predicate)} over more contrived alternatives. */
  static final class StreamNoneMatch<T> {
    @BeforeTemplate
    boolean before(Stream<T> stream, Predicate<? super T> predicate) {
      return Refaster.anyOf(
          !stream.anyMatch(predicate),
          stream.allMatch(Refaster.anyOf(not(predicate), predicate.negate())),
          stream.filter(predicate).findAny().isEmpty());
    }

    @AfterTemplate
    boolean after(Stream<T> stream, Predicate<? super T> predicate) {
      return stream.noneMatch(predicate);
    }
  }

  abstract static class StreamNoneMatch2<T> {
    @Placeholder(allowsIdentity = true)
    abstract boolean test(@MayOptionallyUse T element);

    @BeforeTemplate
    boolean before(Stream<T> stream) {
      return stream.allMatch(e -> !test(e));
    }

    @AfterTemplate
    boolean after(Stream<T> stream) {
      return stream.noneMatch(e -> test(e));
    }
  }

  /** Prefer {@link Stream#anyMatch(Predicate)} over more contrived alternatives. */
  static final class StreamAnyMatch<T> {
    @BeforeTemplate
    boolean before(Stream<T> stream, Predicate<? super T> predicate) {
      return Refaster.anyOf(
          !stream.noneMatch(predicate), stream.filter(predicate).findAny().isPresent());
    }

    @AfterTemplate
    boolean after(Stream<T> stream, Predicate<? super T> predicate) {
      return stream.anyMatch(predicate);
    }
  }

  static final class StreamAllMatch<T> {
    @BeforeTemplate
    boolean before(Stream<T> stream, Predicate<? super T> predicate) {
      return stream.noneMatch(Refaster.anyOf(not(predicate), predicate.negate()));
    }

    @AfterTemplate
    boolean after(Stream<T> stream, Predicate<? super T> predicate) {
      return stream.allMatch(predicate);
    }
  }

  abstract static class StreamAllMatch2<T> {
    @Placeholder(allowsIdentity = true)
    abstract boolean test(@MayOptionallyUse T element);

    @BeforeTemplate
    boolean before(Stream<T> stream) {
      return stream.noneMatch(e -> !test(e));
    }

    @AfterTemplate
    boolean after(Stream<T> stream) {
      return stream.allMatch(e -> test(e));
    }
  }
}
