import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableMultiset.toImmutableMultiset;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.ImmutableSortedMultiset.toImmutableSortedMultiset;
import static com.google.common.collect.ImmutableSortedSet.toImmutableSortedSet;
import static com.google.common.collect.Sets.toImmutableEnumSet;
import static com.google.common.collect.Streams.stream;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.reverseOrder;
import static java.util.Map.Entry.comparingByKey;
import static java.util.Map.Entry.comparingByValue;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.BoundType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMultiset;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import com.google.common.primitives.Ints;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

final class RefasterCheckPositiveCases {
  /**
   * These types and statically imported methods may be fully replaced by some of the Refaster
   * templates under test. Refaster does not remove the associated imports, while Google Java
   * Formatter does. This listing ensures that any imports present in the input file are also
   * present in the output file.
   */
  private static final ImmutableSet<?> ELIDED_TYPES_AND_STATIC_IMPORTS =
      ImmutableSet.of(
          AbstractMap.class,
          Arrays.class,
          Ints.class,
          Iterators.class,
          Joiner.class,
          Lists.class,
          MoreObjects.class,
          Preconditions.class,
          Streams.class,
          (Runnable) () -> identity(),
          (Runnable) () -> joining());

  static final class AssortedTemplates {
    int testCheckIndex() {
      return Objects.checkIndex(0, 1);
    }

    ImmutableMap<Integer, String> testStreamOfMapEntriesImmutableMap() {
      // XXX: If `Integer.valueOf(n)` is replaced with `n` this doesn't work, even though it should.
      // Looks like a @Placeholder limitation. Try to track down and fix.
      return Stream.of(1, 2, 3).collect(toImmutableMap(n -> Integer.valueOf(n), n -> n.toString()));
    }

    ImmutableSet<ImmutableMap<Integer, Integer>> testIterableToMap() {
      return ImmutableSet.of(
          Maps.toMap(ImmutableList.of(1), n -> n * 2),
          Maps.toMap(ImmutableList.of(2)::iterator, Integer::valueOf),
          Maps.toMap(ImmutableList.of(3).iterator(), n -> n.intValue()));
    }

    ImmutableSet<ImmutableMap<Integer, Integer>> testIterableUniqueIndex() {
      return ImmutableSet.of(
          Maps.uniqueIndex(ImmutableList.of(1), n -> n * 2),
          Maps.uniqueIndex(ImmutableList.of(2)::iterator, Integer::valueOf),
          Maps.uniqueIndex(ImmutableList.of(3).iterator(), n -> n.intValue()));
    }

    ImmutableSet<ImmutableMap<Integer, Integer>> testSetToImmutableMap() {
      return ImmutableSet.of(
          Maps.toMap(ImmutableSet.of(1), n -> n + 2),
          Maps.toMap(ImmutableSet.of(2), Integer::valueOf));
    }

    ImmutableSet<BoundType> testStreamToImmutableEnumSet() {
      return Stream.of(BoundType.OPEN).collect(toImmutableEnumSet());
    }

    ImmutableMap<String, Integer> testTransformMapValueToImmutableMap() {
      return ImmutableMap.copyOf(
          Maps.transformValues(ImmutableMap.of("foo", 1L), v -> Math.toIntExact(v)));
    }

    ImmutableSet<String> testIteratorGetNextOrDefault() {
      return ImmutableSet.of(
          Iterators.getNext(ImmutableList.of("a").iterator(), "foo"),
          Iterators.getNext(ImmutableList.of("b").iterator(), "bar"),
          Iterators.getNext(ImmutableList.of("c").iterator(), "baz"));
    }
  }

  static final class BigDecimalTemplates {
    ImmutableSet<BigDecimal> testBigDecimalZero() {
      return ImmutableSet.of(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    ImmutableSet<BigDecimal> testBigDecimalOne() {
      return ImmutableSet.of(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
    }

    ImmutableSet<BigDecimal> testBigDecimalTen() {
      return ImmutableSet.of(BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN);
    }

    ImmutableSet<BigDecimal> testBigDecimalFactoryMethod() {
      return ImmutableSet.of(BigDecimal.valueOf(0), BigDecimal.valueOf(0L));
    }
  }

  static final class CollectionTemplates {
    ImmutableSet<Boolean> testCollectionIsEmpty() {
      return ImmutableSet.of(
          ImmutableSet.of(1).isEmpty(),
          ImmutableSet.of(2).isEmpty(),
          ImmutableSet.of(3).isEmpty(),
          !ImmutableSet.of(4).isEmpty(),
          !ImmutableSet.of(5).isEmpty(),
          !ImmutableSet.of(6).isEmpty());
    }

    ImmutableSet<Boolean> testCollectionAddAllFromCollection() {
      return ImmutableSet.of(
          new ArrayList<>().addAll(new HashSet<>()),
          Iterables.addAll(new ArrayList<>(), new HashSet<>()::iterator));
    }

    ArrayList<String> testNewArrayListFromCollection() {
      return new ArrayList<>(ImmutableList.of("foo"));
    }

    Stream<Integer> testImmutableCollectionAsListToStream() {
      return ImmutableSet.of(1).stream();
    }

    ImmutableList<Integer> testImmutableCollectionAsList() {
      return ImmutableSet.of(1).asList();
    }
  }

  static final class ComparatorTemplates {
    ImmutableSet<Comparator<String>> testNaturalOrderComparator() {
      return ImmutableSet.of(naturalOrder(), naturalOrder());
    }

    ImmutableSet<Comparator<String>> testNaturalOrderComparatorFallback() {
      return ImmutableSet.of(
          Comparator.<String>naturalOrder().thenComparing(naturalOrder()),
          Comparator.<String>naturalOrder().thenComparing(naturalOrder()));
    }

    Comparator<String> testReverseOrder() {
      return reverseOrder();
    }
  }

  static final class EqualityTemplates {
    ImmutableSet<Boolean> testPrimitiveOrReferenceEquality() {
      return ImmutableSet.of(
          true == false,
          (byte) 0 == (byte) 1,
          (short) 0 == (short) 1,
          0 == 1,
          0L == 1L,
          0F == 1F,
          0.0 == 1.0,
          Objects.equals(Boolean.TRUE, Boolean.FALSE),
          Objects.equals(Byte.valueOf((byte) 0), Byte.valueOf((byte) 1)),
          Objects.equals(Short.valueOf((short) 0), Short.valueOf((short) 1)),
          Objects.equals(Integer.valueOf(0), Integer.valueOf(1)),
          Objects.equals(Long.valueOf(0L), Long.valueOf(1L)),
          Objects.equals(Float.valueOf(0F), Float.valueOf(1F)),
          Objects.equals(Double.valueOf(0.0), Double.valueOf(1.0)),
          RoundingMode.UP == RoundingMode.DOWN,
          RoundingMode.UP == RoundingMode.DOWN,
          true != false,
          (byte) 0 != (byte) 1,
          (short) 0 != (short) 1,
          0 != 1,
          0L != 1L,
          0F != 1F,
          0.0 != 1.0,
          !Objects.equals(Boolean.TRUE, Boolean.FALSE),
          !Objects.equals(Byte.valueOf((byte) 0), Byte.valueOf((byte) 1)),
          !Objects.equals(Short.valueOf((short) 0), Short.valueOf((short) 1)),
          !Objects.equals(Integer.valueOf(0), Integer.valueOf(1)),
          !Objects.equals(Long.valueOf(0L), Long.valueOf(1L)),
          !Objects.equals(Float.valueOf(0F), Float.valueOf(1F)),
          !Objects.equals(Double.valueOf(0.0), Double.valueOf(1.0)),
          RoundingMode.UP != RoundingMode.DOWN,
          RoundingMode.UP != RoundingMode.DOWN);
    }

    boolean testEqualsPredicate() {
      // XXX: When boxing is involved this rule seems to break. Example:
      // Stream.of(1).anyMatch(e -> Integer.MIN_VALUE.equals(e));
      return Stream.of("foo").anyMatch("bar"::equals);
    }

    boolean testDoubleNegation() {
      return true;
    }

    ImmutableSet<Boolean> testNegation() {
      return ImmutableSet.of(
          true != false,
          true != false,
          (byte) 3 != (byte) 4,
          (short) 3 != (short) 4,
          3 != 4,
          3L != 4L,
          3F != 4F,
          3.0 != 4.0,
          BoundType.OPEN != BoundType.CLOSED);
    }

    ImmutableSet<Boolean> testIndirectDoubleNegation() {
      return ImmutableSet.of(
          true == false,
          true == false,
          (byte) 3 == (byte) 4,
          (short) 3 == (short) 4,
          3 == 4,
          3L == 4L,
          3F == 4F,
          3.0 == 4.0,
          BoundType.OPEN == BoundType.CLOSED);
    }
  }

  static final class ImmutableListTemplates {
    ImmutableList.Builder<String> testImmutableListBuilder() {
      return ImmutableList.builder();
    }

    ImmutableSet<ImmutableList<Integer>> testEmptyImmutableList() {
      return ImmutableSet.of(ImmutableList.of(), ImmutableList.of());
    }

    ImmutableSet<ImmutableList<Integer>> testIterableToImmutableList() {
      return ImmutableSet.of(
          ImmutableList.copyOf(ImmutableList.of(1)),
          ImmutableList.copyOf(ImmutableList.of(2)::iterator),
          ImmutableList.copyOf(ImmutableList.of(3).iterator()),
          ImmutableList.copyOf(ImmutableList.of(4)),
          ImmutableList.copyOf(ImmutableList.of(5)::iterator),
          ImmutableList.copyOf(ImmutableList.of(6).iterator()),
          ImmutableList.copyOf(new Integer[] {7}),
          ImmutableList.copyOf(new Integer[] {8}),
          ImmutableList.copyOf(new Integer[] {9}));
    }

    ImmutableList<Integer> testImmutableListAsList() {
      return ImmutableList.of(1, 2, 3);
    }

    ImmutableSet<ImmutableList<Integer>> testImmutableListSortedCopyOf() {
      return ImmutableSet.of(
          ImmutableList.sortedCopyOf(ImmutableSet.of(1)),
          ImmutableList.sortedCopyOf(ImmutableSet.of(2)),
          ImmutableList.sortedCopyOf(ImmutableSet.of(3)::iterator));
    }

    ImmutableSet<ImmutableList<String>> testImmutableListSortedCopyOfWithCustomComparator() {
      return ImmutableSet.of(
          ImmutableList.sortedCopyOf(Comparator.comparing(String::length), ImmutableSet.of("foo")),
          ImmutableList.sortedCopyOf(
              Comparator.comparing(String::isEmpty), ImmutableSet.of("bar")::iterator));
    }
  }

  static final class ImmutableMapTemplates {
    ImmutableMap.Builder<String, Integer> testImmutableMapBuilder() {
      return ImmutableMap.builder();
    }

    ImmutableMap<String, Integer> testEmptyImmutableMap() {
      return ImmutableMap.of();
    }

    ImmutableMap<String, Integer> testPairToImmutableMap() {
      return ImmutableMap.of("foo", 1);
    }

    ImmutableSet<ImmutableMap<String, Integer>> testEntryToImmutableMap() {
      return ImmutableSet.of(
          ImmutableMap.of(Map.entry("foo", 1).getKey(), Map.entry("foo", 1).getValue()),
          ImmutableMap.of(Map.entry("foo", 1).getKey(), Map.entry("foo", 1).getValue()));
    }

    ImmutableSet<ImmutableMap<String, Integer>> testIterableToImmutableMap() {
      return ImmutableSet.of(
          ImmutableMap.copyOf(ImmutableMap.of("foo", 1).entrySet()),
          ImmutableMap.copyOf(ImmutableMap.of("foo", 1).entrySet()),
          ImmutableMap.copyOf(Iterables.cycle(Map.entry("foo", 1))));
    }

    ImmutableMap<String, Integer> testImmutableMapCopyOfImmutableMap() {
      return ImmutableMap.of("foo", 1);
    }
  }

  static final class ImmutableSetTemplates {
    ImmutableSet.Builder<String> testImmutableSetBuilder() {
      return ImmutableSet.builder();
    }

    ImmutableSet<ImmutableSet<Integer>> testEmptyImmutableList() {
      return ImmutableSet.of(ImmutableSet.of(), ImmutableSet.of());
    }

    ImmutableSet<ImmutableSet<Integer>> testIterableToImmutableSet() {
      return ImmutableSet.of(
          ImmutableSet.copyOf(ImmutableList.of(1)),
          ImmutableSet.copyOf(ImmutableList.of(2)::iterator),
          ImmutableSet.copyOf(ImmutableList.of(3).iterator()),
          ImmutableSet.copyOf(ImmutableSet.of(4)),
          ImmutableSet.copyOf(ImmutableSet.of(5)::iterator),
          ImmutableSet.copyOf(ImmutableSet.of(6).iterator()),
          ImmutableSet.copyOf(new Integer[] {7}),
          ImmutableSet.copyOf(new Integer[] {8}),
          ImmutableSet.copyOf(new Integer[] {9}));
    }

    ImmutableSet<Integer> testImmutableSetCopyOfImmutableSet() {
      return ImmutableSet.of(1, 2);
    }

    ImmutableSet<Integer> testImmutableSetCopyOfSetView() {
      return Sets.difference(ImmutableSet.of(1), ImmutableSet.of(2)).immutableCopy();
    }
  }

  static final class ImmutableSortedSetTemplates {
    ImmutableSortedSet.Builder<String> testImmutableSortedSetBuilder() {
      return ImmutableSortedSet.orderedBy(Comparator.comparingInt(String::length));
    }

    ImmutableSortedSet.Builder<String> testImmutableSortedSetNaturalOrderBuilder() {
      return ImmutableSortedSet.naturalOrder();
    }

    ImmutableSortedSet.Builder<String> testImmutableSortedSetReverseOrderBuilder() {
      return ImmutableSortedSet.reverseOrder();
    }

    ImmutableSet<ImmutableSortedSet<Integer>> testEmptyImmutableList() {
      return ImmutableSet.of(ImmutableSortedSet.of(), ImmutableSortedSet.of());
    }

    ImmutableSet<ImmutableSortedSet<Integer>> testIterableToImmutableSortedSet() {
      // XXX: The first element is not rewritten (`naturalOrder()` isn't dropped). WHY!?
      return ImmutableSet.of(
          ImmutableSortedSet.copyOf(naturalOrder(), ImmutableList.of(1)),
          ImmutableSortedSet.copyOf(ImmutableList.of(2)),
          ImmutableSortedSet.copyOf(ImmutableList.of(3)::iterator),
          ImmutableSortedSet.copyOf(ImmutableList.of(4).iterator()),
          ImmutableSortedSet.copyOf(ImmutableSet.of(5)),
          ImmutableSortedSet.copyOf(ImmutableSet.of(6)::iterator),
          ImmutableSortedSet.copyOf(ImmutableSet.of(7).iterator()),
          ImmutableSortedSet.copyOf(new Integer[] {8}),
          ImmutableSortedSet.copyOf(new Integer[] {9}),
          ImmutableSortedSet.copyOf(new Integer[] {10}));
    }
  }

  static final class MapEntryTemplates {
    ImmutableSet<Map.Entry<String, Integer>> testMapEntry() {
      return ImmutableSet.of(Map.entry("foo", 1), Map.entry("bar", 2));
    }

    ImmutableSet<Comparator<Map.Entry<Integer, String>>> testMapEntryComparingByKey() {
      return ImmutableSet.of(comparingByKey(), comparingByKey());
    }

    Comparator<Map.Entry<Integer, String>> testMapEntryComparingByKeyWithCustomComparator() {
      return comparingByKey(Comparator.comparingInt(i -> i * 2));
    }

    ImmutableSet<Comparator<Map.Entry<Integer, String>>> testMapEntryComparingByValue() {
      return ImmutableSet.of(comparingByValue(), comparingByValue());
    }

    Comparator<Map.Entry<Integer, String>> testMapEntryComparingByValueWithCustomComparator() {
      return comparingByValue(Comparator.comparingInt(String::length));
    }
  }

  static final class NullTemplates {
    String testRequireNonNullElse() {
      return Objects.requireNonNullElse("foo", "bar");
    }

    long testIsNullFunction() {
      return Stream.of("foo").filter(Objects::isNull).count();
    }

    long testNonNullFunction() {
      return Stream.of("foo").filter(Objects::nonNull).count();
    }
  }

  static final class OptionalTemplates {
    Optional<String> testOptionalOfNullable() {
      return Optional.ofNullable(toString());
    }

    ImmutableSet<Boolean> testOptionalIsEmpty() {
      return ImmutableSet.of(Optional.empty().isEmpty(), Optional.of("foo").isEmpty());
    }

    ImmutableSet<Boolean> testOptionalIsPresent() {
      return ImmutableSet.of(Optional.empty().isPresent(), Optional.of("foo").isPresent());
    }

    Stream<Object> testOptionalToStream() {
      return Stream.concat(Optional.empty().stream(), Optional.of("foo").stream());
    }

    ImmutableSet<Optional<Integer>> testOptionalFirstCollectionElement() {
      return ImmutableSet.of(
          ImmutableSet.of(1).stream().findFirst(), ImmutableList.of(2).stream().findFirst());
    }

    Optional<String> testOptionalFirstIteratorElement() {
      return stream(ImmutableSet.of("foo").iterator()).findFirst();
    }

    ImmutableSet<Optional<String>> testTernaryOperatorOptionalPositiveFiltering() {
      return ImmutableSet.of(
          /* Or Optional.ofNullable (can't auto-infer). */ Optional.of("foo")
              .filter(v -> v.length() > 5),
          /* Or Optional.ofNullable (can't auto-infer). */ Optional.of("bar")
              .filter(v -> !v.contains("baz")));
    }

    ImmutableSet<Optional<String>> testTernaryOperatorOptionalNegativeFiltering() {
      return ImmutableSet.of(
          /* Or Optional.ofNullable (can't auto-infer). */ Optional.of("foo")
              .filter(v -> v.length() <= 5),
          /* Or Optional.ofNullable (can't auto-infer). */ Optional.of("bar")
              .filter(v -> v.contains("baz")));
    }

    ImmutableSet<Boolean> testMapOptionalToBoolean() {
      return ImmutableSet.of(
          Optional.of("foo").filter(String::isEmpty).isPresent(),
          Optional.of("bar").filter(s -> s.isEmpty()).isPresent());
    }

    Stream<Object> testFlatmapOptionalToStream() {
      return Stream.concat(
          Stream.of(Optional.empty()).flatMap(Optional::stream),
          Stream.of(Optional.of("foo")).flatMap(Optional::stream));
    }

    Stream<String> testMapToOptionalGet(Map<Integer, Optional<String>> map) {
      return Stream.of(1).flatMap(n -> map.get(n).stream());
    }

    Optional<Integer> testFilterOuterOptionalAfterFlatMap() {
      return Optional.of("foo").flatMap(v -> Optional.of(v.length())).filter(len -> len > 0);
    }

    Optional<Integer> testMapOuterOptionalAfterFlatMap() {
      return Optional.of("foo").flatMap(v -> Optional.of(v.length())).map(len -> len * 0);
    }

    Optional<Integer> testFlatMapOuterOptionalAfterFlatMap() {
      return Optional.of("foo").flatMap(v -> Optional.of(v.length())).flatMap(Optional::of);
    }
  }

  static final class PrimitiveTemplates {
    ImmutableSet<Boolean> testLessThan() {
      return ImmutableSet.of(
          (byte) 3 < (byte) 4, (short) 3 < (short) 4, 3 < 4, 3L < 4L, 3F < 4F, 3.0 < 4.0);
    }

    ImmutableSet<Boolean> testLessThanOrEqualTo() {
      return ImmutableSet.of(
          (byte) 3 <= (byte) 4, (short) 3 <= (short) 4, 3 <= 4, 3L <= 4L, 3F <= 4F, 3.0 <= 4.0);
    }

    ImmutableSet<Boolean> testGreaterThan() {
      return ImmutableSet.of(
          (byte) 3 > (byte) 4, (short) 3 > (short) 4, 3 > 4, 3L > 4L, 3F > 4F, 3.0 > 4.0);
    }

    ImmutableSet<Boolean> testGreaterThanOrEqualTo() {
      return ImmutableSet.of(
          (byte) 3 >= (byte) 4, (short) 3 >= (short) 4, 3 >= 4, 3L >= 4L, 3F >= 4F, 3.0 >= 4.0);
    }

    int testLongToIntExact() {
      return Math.toIntExact(Long.MAX_VALUE);
    }
  }

  static final class StreamTemplates {
    ImmutableSet<Stream<String>> testStreamOfNullable() {
      return ImmutableSet.of(Stream.ofNullable("a"), Stream.ofNullable("b"));
    }

    Stream<Integer> testConcatOneStream() {
      return Stream.of(1);
    }

    Stream<Integer> testConcatTwoStreams() {
      return Stream.concat(Stream.of(1), Stream.of(2));
    }

    Stream<Integer> testFilterOuterStreamAfterFlatMap() {
      return Stream.of("foo").flatMap(v -> Stream.of(v.length())).filter(len -> len > 0);
    }

    Stream<Integer> testMapOuterStreamAfterFlatMap() {
      return Stream.of("foo").flatMap(v -> Stream.of(v.length())).map(len -> len * 0);
    }

    Stream<Integer> testFlatMapOuterStreamAfterFlatMap() {
      return Stream.of("foo").flatMap(v -> Stream.of(v.length())).flatMap(Stream::of);
    }

    ImmutableSet<ImmutableList<Integer>> testStreamToImmutableList() {
      return ImmutableSet.of(
          Stream.of(1).collect(toImmutableList()), Stream.of(2).collect(toImmutableList()));
    }

    ImmutableSet<ImmutableSet<Integer>> testStreamToImmutableSet() {
      return ImmutableSet.of(
          Stream.of(1).collect(toImmutableSet()),
          Stream.of(2).collect(toImmutableSet()),
          Stream.of(3).collect(toImmutableSet()));
    }

    ImmutableSet<ImmutableSortedSet<Integer>> testStreamToImmutableSortedSet() {
      return ImmutableSet.of(
          Stream.of(1).collect(toImmutableSortedSet(naturalOrder())),
          Stream.of(2).collect(toImmutableSortedSet(naturalOrder())));
    }

    ImmutableSet<ImmutableMultiset<Integer>> testStreamToImmutableMultiset() {
      return ImmutableSet.of(
          Stream.of(1).collect(toImmutableMultiset()), Stream.of(2).collect(toImmutableMultiset()));
    }

    ImmutableSet<ImmutableSortedMultiset<Integer>> testStreamToImmutableSortedMultiset() {
      return ImmutableSet.of(
          Stream.of(1).collect(toImmutableSortedMultiset(naturalOrder())),
          Stream.of(2).collect(toImmutableSortedMultiset(naturalOrder())));
    }
  }

  static final class StringTemplates {
    ImmutableSet<Boolean> testStringIsEmpty() {
      return ImmutableSet.of(
          "foo".isEmpty(),
          "bar".isEmpty(),
          "baz".isEmpty(),
          !"foo".isEmpty(),
          !"bar".isEmpty(),
          !"baz".isEmpty());
    }

    ImmutableSet<Boolean> testStringIsNullOrEmpty() {
      return ImmutableSet.of(
          Strings.isNullOrEmpty(getClass().getName()),
          !Strings.isNullOrEmpty(getClass().getName()));
    }

    ImmutableSet<Optional<String>> testOptionalNonEmptyString() {
      return ImmutableSet.of(
          Optional.ofNullable(toString()).filter(s -> !s.isEmpty()),
          Optional.ofNullable(toString()).filter(s -> !s.isEmpty()));
    }

    ImmutableSet<String> testJoinStrings() {
      return ImmutableSet.of(
          String.join("a", new String[] {"foo", "bar"}),
          String.join("b", new CharSequence[] {"baz", "quux"}),
          String.join("c", new String[] {"foo", "bar"}),
          String.join("d", new CharSequence[] {"baz", "quux"}),
          String.join("e", ImmutableList.of("foo", "bar")),
          String.join("f", Iterables.cycle(ImmutableList.of("foo", "bar"))),
          String.join("g", ImmutableList.of("baz", "quux")));
    }
  }

  static final class TimeTemplates {
    ImmutableSet<Instant> testEpochInstant() {
      return ImmutableSet.of(Instant.EPOCH, Instant.EPOCH, Instant.EPOCH, Instant.EPOCH);
    }

    Instant testClockInstant() {
      return Clock.systemUTC().instant();
    }

    ZoneId testUtcConstant() {
      return ZoneOffset.UTC;
    }

    ImmutableSet<Boolean> testInstantIsBefore() {
      return ImmutableSet.of(Instant.MIN.isBefore(Instant.MAX), !Instant.MIN.isBefore(Instant.MAX));
    }

    ImmutableSet<Boolean> testInstantIsAfter() {
      return ImmutableSet.of(Instant.MIN.isAfter(Instant.MAX), !Instant.MIN.isAfter(Instant.MAX));
    }

    ImmutableSet<Boolean> testChronoLocalDateIsBefore() {
      return ImmutableSet.of(
          LocalDate.MIN.isBefore(LocalDate.MAX), !LocalDate.MIN.isBefore(LocalDate.MAX));
    }

    ImmutableSet<Boolean> testChronoLocalDateIsAfter() {
      return ImmutableSet.of(
          LocalDate.MIN.isAfter(LocalDate.MAX), !LocalDate.MIN.isAfter(LocalDate.MAX));
    }

    ImmutableSet<Boolean> testChronoLocalDateTimeIsBefore() {
      return ImmutableSet.of(
          LocalDateTime.MIN.isBefore(LocalDateTime.MAX),
          !LocalDateTime.MIN.isBefore(LocalDateTime.MAX));
    }

    ImmutableSet<Boolean> testChronoLocalDateTimeIsAfter() {
      return ImmutableSet.of(
          LocalDateTime.MIN.isAfter(LocalDateTime.MAX),
          !LocalDateTime.MIN.isAfter(LocalDateTime.MAX));
    }

    ImmutableSet<Boolean> testChronoZonedDateTimeIsBefore() {
      return ImmutableSet.of(
          ZonedDateTime.now().isBefore(ZonedDateTime.now()),
          !ZonedDateTime.now().isBefore(ZonedDateTime.now()));
    }

    ImmutableSet<Boolean> testChronoZonedDateTimeIsAfter() {
      return ImmutableSet.of(
          ZonedDateTime.now().isAfter(ZonedDateTime.now()),
          !ZonedDateTime.now().isAfter(ZonedDateTime.now()));
    }

    ImmutableSet<Boolean> testOffsetDateTimeIsAfter() {
      return ImmutableSet.of(
          OffsetDateTime.MIN.isAfter(OffsetDateTime.MAX),
          !OffsetDateTime.MIN.isAfter(OffsetDateTime.MAX));
    }

    ImmutableSet<Boolean> testOffsetDateTimeIsBefore() {
      return ImmutableSet.of(
          OffsetDateTime.MIN.isBefore(OffsetDateTime.MAX),
          !OffsetDateTime.MIN.isBefore(OffsetDateTime.MAX));
    }

    Duration testDurationBetweenInstants() {
      return Duration.between(Instant.MIN, Instant.MAX);
    }

    Duration testDurationBetweenOffsetDateTimes() {
      return Duration.between(OffsetDateTime.MIN, OffsetDateTime.MAX)
          .plus(Duration.between(OffsetDateTime.MIN, OffsetDateTime.MAX));
    }
  }
}
