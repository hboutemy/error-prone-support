package tech.picnic.errorprone.refastertemplates;

import static com.google.common.collect.MoreCollectors.toOptional;
import static com.google.errorprone.refaster.ImportPolicy.STATIC_IMPORT_ALWAYS;
import static java.util.function.Function.identity;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.MoreCollectors;
import com.google.errorprone.refaster.Refaster;
import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import com.google.errorprone.refaster.annotation.MayOptionallyUse;
import com.google.errorprone.refaster.annotation.NotMatches;
import com.google.errorprone.refaster.annotation.Placeholder;
import com.google.errorprone.refaster.annotation.UseImportPolicy;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.publisher.PublisherProbe;
import reactor.util.function.Tuple2;
import tech.picnic.errorprone.refaster.util.ThrowsCheckedException;

/** Refaster templates related to Reactor expressions and statements. */
final class ReactorTemplates {
  private ReactorTemplates() {}

  /**
   * Prefer {@link Mono#fromSupplier(Supplier)} over {@link Mono#fromCallable(Callable)} where
   * feasible.
   */
  static final class MonoFromSupplier<T> {
    @BeforeTemplate
    Mono<T> before(@NotMatches(ThrowsCheckedException.class) Callable<? extends T> supplier) {
      return Mono.fromCallable(supplier);
    }

    @AfterTemplate
    Mono<T> after(Supplier<? extends T> supplier) {
      return Mono.fromSupplier(supplier);
    }
  }

  /** Prefer {@link Mono#justOrEmpty(Optional)} over more verbose alternatives. */
  // XXX: If `optional` is a constant and effectively-final expression then the `Mono.defer` can be
  // dropped. Should look into Refaster support for identifying this.
  static final class MonoFromOptional<T> {
    @BeforeTemplate
    @SuppressWarnings(
        "MonoFromSupplier" /* `optional` may match a checked exception-throwing expression. */)
    Mono<T> before(Optional<T> optional) {
      return Refaster.anyOf(
          Mono.fromCallable(() -> optional.orElse(null)),
          Mono.fromSupplier(() -> optional.orElse(null)));
    }

    @AfterTemplate
    Mono<T> after(Optional<T> optional) {
      return Mono.defer(() -> Mono.justOrEmpty(optional));
    }
  }

  /** Don't unnecessarily defer {@link Mono#error(Throwable)}. */
  static final class MonoDeferredError<T> {
    @BeforeTemplate
    Mono<T> before(Throwable throwable) {
      return Mono.defer(() -> Mono.error(throwable));
    }

    @AfterTemplate
    Mono<T> after(Throwable throwable) {
      return Mono.error(() -> throwable);
    }
  }

  /** Don't unnecessarily defer {@link Flux#error(Throwable)}. */
  static final class FluxDeferredError<T> {
    @BeforeTemplate
    Flux<T> before(Throwable throwable) {
      return Flux.defer(() -> Flux.error(throwable));
    }

    @AfterTemplate
    Flux<T> after(Throwable throwable) {
      return Flux.error(() -> throwable);
    }
  }

  /**
   * Don't unnecessarily pass {@link Mono#error(Supplier)} a method reference or lambda expression.
   */
  // XXX: Drop this rule once the more general rule `AssortedTemplates#SupplierAsSupplier` works
  // reliably.
  static final class MonoErrorSupplier<T, E extends Throwable> {
    @BeforeTemplate
    Mono<T> before(Supplier<E> supplier) {
      return Mono.error(() -> supplier.get());
    }

    @AfterTemplate
    Mono<T> after(Supplier<E> supplier) {
      return Mono.error(supplier);
    }
  }

  /**
   * Don't unnecessarily pass {@link Flux#error(Supplier)} a method reference or lambda expression.
   */
  // XXX: Drop this rule once the more general rule `AssortedTemplates#SupplierAsSupplier` works
  // reliably.
  static final class FluxErrorSupplier<T, E extends Throwable> {
    @BeforeTemplate
    Flux<T> before(Supplier<E> supplier) {
      return Flux.error(() -> supplier.get());
    }

    @AfterTemplate
    Flux<T> after(Supplier<E> supplier) {
      return Flux.error(supplier);
    }
  }

  /** Prefer {@link Mono#thenReturn(Object)} over more verbose alternatives. */
  static final class MonoThenReturn<T, S> {
    @BeforeTemplate
    Mono<S> before(Mono<T> mono, S object) {
      return Refaster.anyOf(mono.ignoreElement().thenReturn(object), mono.then(Mono.just(object)));
    }

    @AfterTemplate
    Mono<S> after(Mono<T> mono, S object) {
      return mono.thenReturn(object);
    }
  }

  /** Don't unnecessarily pass an empty publisher to {@link Mono#switchIfEmpty(Mono)}. */
  static final class MonoSwitchIfEmptyOfEmptyPublisher<T> {
    @BeforeTemplate
    Mono<T> before(Mono<T> mono) {
      return mono.switchIfEmpty(Mono.empty());
    }

    @AfterTemplate
    Mono<T> after(Mono<T> mono) {
      return mono;
    }
  }

  /** Don't unnecessarily pass an empty publisher to {@link Flux#switchIfEmpty(Publisher)}. */
  static final class FluxSwitchIfEmptyOfEmptyPublisher<T> {
    @BeforeTemplate
    Flux<T> before(Flux<T> flux) {
      return flux.switchIfEmpty(Refaster.anyOf(Mono.empty(), Flux.empty()));
    }

    @AfterTemplate
    Flux<T> after(Flux<T> flux) {
      return flux;
    }
  }

  /** Avoid vacuous invocations of {@link Mono#ignoreElement()} and {@link Mono#then()}. */
  static final class MonoVoid {
    @BeforeTemplate
    Mono<Void> before(Mono<Void> mono) {
      return Refaster.anyOf(mono.ignoreElement(), mono.then());
    }

    @AfterTemplate
    Mono<Void> after(Mono<Void> mono) {
      return mono;
    }
  }

  /** Prefer {@link Flux#concatMap(Function)} over more contrived alternatives. */
  static final class FluxConcatMap<T, S> {
    @BeforeTemplate
    Flux<S> before(Flux<T> flux, Function<? super T, ? extends Publisher<? extends S>> function) {
      return Refaster.anyOf(flux.flatMap(function, 1), flux.flatMapSequential(function, 1));
    }

    @AfterTemplate
    Flux<S> after(Flux<T> flux, Function<? super T, ? extends Publisher<? extends S>> function) {
      return flux.concatMap(function);
    }
  }

  /** Avoid contrived alternatives to {@link Mono#flatMapIterable(Function)}. */
  static final class MonoFlatMapIterable<T, S> {
    @BeforeTemplate
    Flux<S> before(Mono<T> mono, Function<? super T, ? extends Iterable<? extends S>> function) {
      return Refaster.anyOf(
          mono.map(function).flatMapIterable(identity()), mono.flux().concatMapIterable(function));
    }

    @AfterTemplate
    Flux<S> after(Mono<T> mono, Function<? super T, ? extends Iterable<? extends S>> function) {
      return mono.flatMapIterable(function);
    }
  }

  /**
   * Prefer {@link Mono#flatMapIterable(Function)} to flatten a {@link Mono} of some {@link
   * Iterable} over less efficient alternatives.
   */
  static final class MonoFlatMapIterableIdentity<T, S extends Iterable<T>> {
    @BeforeTemplate
    Flux<T> before(Mono<S> mono) {
      return mono.flatMapMany(Flux::fromIterable);
    }

    @AfterTemplate
    @UseImportPolicy(STATIC_IMPORT_ALWAYS)
    Flux<T> after(Mono<S> mono) {
      return mono.flatMapIterable(identity());
    }
  }

  /** Prefer {@link Mono#flatMapMany(Function)} over more contrived alternatives. */
  static final class MonoFlatMapMany<T, S> {
    @BeforeTemplate
    Flux<S> before(
        Mono<T> mono,
        Function<? super T, ? extends Publisher<? extends S>> function,
        boolean delayUntilEnd,
        int maxConcurrency,
        int prefetch) {
      return Refaster.anyOf(
          mono.flux().concatMap(function),
          mono.flux().concatMap(function, prefetch),
          mono.flux().concatMapDelayError(function),
          mono.flux().concatMapDelayError(function, prefetch),
          mono.flux().concatMapDelayError(function, delayUntilEnd, prefetch),
          mono.flux().flatMap(function, maxConcurrency),
          mono.flux().flatMap(function, maxConcurrency, prefetch),
          mono.flux().flatMapDelayError(function, maxConcurrency, prefetch),
          mono.flux().flatMapSequential(function, maxConcurrency),
          mono.flux().flatMapSequential(function, maxConcurrency, prefetch),
          mono.flux().flatMapSequentialDelayError(function, maxConcurrency, prefetch));
    }

    @BeforeTemplate
    Flux<S> before(Mono<T> mono, Function<? super T, Publisher<? extends S>> function) {
      return Refaster.anyOf(mono.flux().switchMap(function), mono.flux().switchMap(function));
    }

    @AfterTemplate
    Flux<S> after(Mono<T> mono, Function<? super T, ? extends Publisher<? extends S>> function) {
      return mono.flatMapMany(function);
    }
  }

  /**
   * Prefer {@link Flux#concatMapIterable(Function)} over {@link Flux#flatMapIterable(Function)}, as
   * the former has equivalent semantics but a clearer name.
   */
  static final class FluxConcatMapIterable<T, S> {
    @BeforeTemplate
    Flux<S> before(Flux<T> flux, Function<? super T, ? extends Iterable<? extends S>> function) {
      return Refaster.anyOf(
          flux.flatMapIterable(function), flux.map(function).concatMapIterable(identity()));
    }

    @AfterTemplate
    Flux<S> after(Flux<T> flux, Function<? super T, ? extends Iterable<? extends S>> function) {
      return flux.concatMapIterable(function);
    }
  }

  /** Prefer {@link Flux#zipWithIterable(Iterable)} over more contrived alternatives. */
  static final class FluxZipWithIterable<T, S> {
    @BeforeTemplate
    Flux<Tuple2<T, S>> before(Flux<T> flux, Iterable<S> iterable) {
      return flux.zipWith(Flux.fromIterable(iterable));
    }

    @AfterTemplate
    Flux<Tuple2<T, S>> after(Flux<T> flux, Iterable<S> iterable) {
      return flux.zipWithIterable(iterable);
    }
  }

  /** Prefer {@link Flux#zipWithIterable(Iterable, BiFunction)} over more contrived alternatives. */
  static final class FluxZipWithIterableBiFunction<T, S, R> {
    @BeforeTemplate
    Flux<R> before(
        Flux<T> flux,
        Iterable<S> iterable,
        BiFunction<? super T, ? super S, ? extends R> function) {
      return flux.zipWith(Flux.fromIterable(iterable), function);
    }

    @AfterTemplate
    Flux<R> after(
        Flux<T> flux,
        Iterable<S> iterable,
        BiFunction<? super T, ? super S, ? extends R> function) {
      return flux.zipWithIterable(iterable, function);
    }
  }

  /**
   * Don't use {@link Mono#flatMapMany(Function)} to implicitly convert a {@link Mono} to a {@link
   * Flux}.
   */
  abstract static class MonoFlatMapToFlux<T, S> {
    @Placeholder(allowsIdentity = true)
    abstract Mono<S> valueTransformation(@MayOptionallyUse T value);

    @BeforeTemplate
    Flux<S> before(Mono<T> mono) {
      return mono.flatMapMany(v -> valueTransformation(v));
    }

    @AfterTemplate
    Flux<S> after(Mono<T> mono) {
      return mono.flatMap(v -> valueTransformation(v)).flux();
    }
  }

  /** Prefer {@link Mono#flux()}} over more contrived alternatives. */
  static final class MonoFlux<T> {
    @BeforeTemplate
    Flux<T> before(Mono<T> mono) {
      return Flux.concat(mono);
    }

    @AfterTemplate
    Flux<T> after(Mono<T> mono) {
      return mono.flux();
    }
  }

  /**
   * Prefer a collection using {@link MoreCollectors#toOptional()} over more contrived alternatives.
   */
  // XXX: Consider creating a plugin which flags/discourages `Mono<Optional<T>>` method return
  // types, just as we discourage nullable `Boolean`s and `Optional`s.
  static final class MonoCollectToOptional<T> {
    @BeforeTemplate
    Mono<Optional<T>> before(Mono<T> mono) {
      return mono.map(Optional::of).defaultIfEmpty(Optional.empty());
    }

    @AfterTemplate
    @UseImportPolicy(STATIC_IMPORT_ALWAYS)
    Mono<Optional<T>> after(Mono<T> mono) {
      return mono.flux().collect(toOptional());
    }
  }

  /** Prefer {@link Mono#cast(Class)} over {@link Mono#map(Function)} with a cast. */
  static final class MonoCast<T, S> {
    @BeforeTemplate
    Mono<S> before(Mono<T> mono) {
      return mono.map(Refaster.<S>clazz()::cast);
    }

    @AfterTemplate
    Mono<S> after(Mono<T> mono) {
      return mono.cast(Refaster.<S>clazz());
    }
  }

  /** Prefer {@link Flux#cast(Class)} over {@link Flux#map(Function)} with a cast. */
  static final class FluxCast<T, S> {
    @BeforeTemplate
    Flux<S> before(Flux<T> flux) {
      return flux.map(Refaster.<S>clazz()::cast);
    }

    @AfterTemplate
    Flux<S> after(Flux<T> flux) {
      return flux.cast(Refaster.<S>clazz());
    }
  }

  /**
   * Prefer {@link Mono#defaultIfEmpty(Object)} over {@link Mono#switchIfEmpty(Mono)} where possible
   * .
   */
  static final class MonoDefaultIfEmpty<T, S> {
    @BeforeTemplate
    Mono<T> before(Mono<T> mono, T fallback) {
      return mono.switchIfEmpty(Mono.just(fallback));
    }

    @AfterTemplate
    Mono<T> after(Mono<T> mono, T fallback) {
      return mono.defaultIfEmpty(fallback);
    }
  }

  /**
   * Prefer {@link Flux#defaultIfEmpty(Object)} over {@link Flux#switchIfEmpty(Publisher)} where
   * possible .
   */
  static final class FluxDefaultIfEmpty<T, S> {
    @BeforeTemplate
    Flux<T> before(Flux<T> flux, T fallback) {
      return flux.switchIfEmpty(Refaster.anyOf(Mono.just(fallback), Flux.just(fallback)));
    }

    @AfterTemplate
    Flux<T> after(Flux<T> flux, T fallback) {
      return flux.defaultIfEmpty(fallback);
    }
  }

  /** Avoid vacuous invocations of {@link Mono#ignoreElement()}. */
  static final class MonoThen<T, S> {
    @BeforeTemplate
    Mono<Void> before(Mono<T> mono) {
      return mono.ignoreElement().then();
    }

    @AfterTemplate
    Mono<Void> after(Mono<T> mono) {
      return mono.then();
    }
  }

  /** Avoid vacuous invocations of {@link Flux#ignoreElements()}. */
  static final class FluxThen<T> {
    @BeforeTemplate
    Mono<Void> before(Flux<T> flux) {
      return flux.ignoreElements().then();
    }

    @BeforeTemplate
    Mono<Void> before2(Flux<Void> flux) {
      return flux.ignoreElements();
    }

    @AfterTemplate
    Mono<Void> after(Flux<T> flux) {
      return flux.then();
    }
  }

  /** Avoid vacuous invocations of {@link Mono#ignoreElement()}. */
  static final class MonoThenEmpty<T> {
    @BeforeTemplate
    Mono<Void> before(Mono<T> mono, Publisher<Void> publisher) {
      return mono.ignoreElement().thenEmpty(publisher);
    }

    @AfterTemplate
    Mono<Void> after(Mono<T> mono, Publisher<Void> publisher) {
      return mono.thenEmpty(publisher);
    }
  }

  /** Avoid vacuous invocations of {@link Flux#ignoreElements()}. */
  static final class FluxThenEmpty<T> {
    @BeforeTemplate
    Mono<Void> before(Flux<T> flux, Publisher<Void> publisher) {
      return flux.ignoreElements().thenEmpty(publisher);
    }

    @AfterTemplate
    Mono<Void> after(Flux<T> flux, Publisher<Void> publisher) {
      return flux.thenEmpty(publisher);
    }
  }

  /** Avoid vacuous invocations of {@link Mono#ignoreElement()}. */
  static final class MonoThenMany<T, S> {
    @BeforeTemplate
    Flux<S> before(Mono<T> mono, Publisher<S> publisher) {
      return mono.ignoreElement().thenMany(publisher);
    }

    @AfterTemplate
    Flux<S> after(Mono<T> mono, Publisher<S> publisher) {
      return mono.thenMany(publisher);
    }
  }

  /** Avoid vacuous invocations of {@link Flux#ignoreElements()}. */
  static final class FluxThenMany<T, S> {
    @BeforeTemplate
    Flux<S> before(Flux<T> flux, Publisher<S> publisher) {
      return flux.ignoreElements().thenMany(publisher);
    }

    @AfterTemplate
    Flux<S> after(Flux<T> flux, Publisher<S> publisher) {
      return flux.thenMany(publisher);
    }
  }

  /** Avoid vacuous invocations of {@link Mono#ignoreElement()}. */
  static final class MonoThenMono<T, S> {
    @BeforeTemplate
    Mono<S> before(Mono<T> mono1, Mono<S> mono2) {
      return mono1.ignoreElement().then(mono2);
    }

    @BeforeTemplate
    Mono<Void> before2(Mono<T> mono1, Mono<Void> mono2) {
      return mono1.thenEmpty(mono2);
    }

    @AfterTemplate
    Mono<S> after(Mono<T> mono1, Mono<S> mono2) {
      return mono1.then(mono2);
    }
  }

  /** Avoid vacuous invocations of {@link Flux#ignoreElements()}. */
  static final class FluxThenMono<T, S> {
    @BeforeTemplate
    Mono<S> before(Flux<T> flux, Mono<S> mono) {
      return flux.ignoreElements().then(mono);
    }

    @BeforeTemplate
    Mono<Void> before2(Flux<T> flux, Mono<Void> mono) {
      return flux.thenEmpty(mono);
    }

    @AfterTemplate
    Mono<S> after(Flux<T> flux, Mono<S> mono) {
      return flux.then(mono);
    }
  }

  /** Prefer {@link PublisherProbe#empty()}} over more verbose alternatives. */
  static final class PublisherProbeEmpty<T> {
    @BeforeTemplate
    PublisherProbe<T> before() {
      return Refaster.anyOf(PublisherProbe.of(Mono.empty()), PublisherProbe.of(Flux.empty()));
    }

    @AfterTemplate
    PublisherProbe<T> after() {
      return PublisherProbe.empty();
    }
  }

  /** Prefer {@link Mono#as(Function)} when creating a {@link StepVerifier}. */
  static final class StepVerifierFromMono<T> {
    @BeforeTemplate
    StepVerifier.FirstStep<? extends T> before(Mono<T> mono) {
      return StepVerifier.create(mono);
    }

    @AfterTemplate
    StepVerifier.FirstStep<? extends T> after(Mono<T> mono) {
      return mono.as(StepVerifier::create);
    }
  }

  /** Prefer {@link Flux#as(Function)} when creating a {@link StepVerifier}. */
  static final class StepVerifierFromFlux<T> {
    @BeforeTemplate
    StepVerifier.FirstStep<? extends T> before(Flux<T> flux) {
      return StepVerifier.create(flux);
    }

    @AfterTemplate
    StepVerifier.FirstStep<? extends T> after(Flux<T> flux) {
      return flux.as(StepVerifier::create);
    }
  }

  /** Don't unnecessarily call {@link StepVerifier.Step#expectNext(Object[])}. */
  static final class StepVerifierStepExpectNextEmpty<T> {
    @BeforeTemplate
    @SuppressWarnings("unchecked")
    StepVerifier.Step<T> before(StepVerifier.Step<T> step) {
      return step.expectNext();
    }

    @AfterTemplate
    StepVerifier.Step<T> after(StepVerifier.Step<T> step) {
      return step;
    }
  }

  /** Prefer {@link StepVerifier.Step#expectNext(Object)} over more verbose alternatives. */
  static final class StepVerifierStepExpectNext<T> {
    @BeforeTemplate
    StepVerifier.Step<T> before(StepVerifier.Step<T> step, T object) {
      return Refaster.anyOf(
          step.expectNextMatches(e -> e.equals(object)), step.expectNextMatches(object::equals));
    }

    @AfterTemplate
    StepVerifier.Step<T> after(StepVerifier.Step<T> step, T object) {
      return step.expectNext(object);
    }
  }

  /** Prefer {@link StepVerifier.LastStep#verifyComplete()} over more verbose alternatives. */
  static final class StepVerifierLastStepVerifyComplete {
    @BeforeTemplate
    Duration before(StepVerifier.LastStep step) {
      return step.expectComplete().verify();
    }

    @AfterTemplate
    Duration after(StepVerifier.LastStep step) {
      return step.verifyComplete();
    }
  }

  /** Prefer {@link StepVerifier.LastStep#verifyError()} over more verbose alternatives. */
  static final class StepVerifierLastStepVerifyError {
    @BeforeTemplate
    Duration before(StepVerifier.LastStep step) {
      return step.expectError().verify();
    }

    @AfterTemplate
    Duration after(StepVerifier.LastStep step) {
      return step.verifyError();
    }
  }

  /** Prefer {@link StepVerifier.LastStep#verifyError(Class)} over more verbose alternatives. */
  static final class StepVerifierLastStepVerifyErrorClass<T extends Throwable> {
    @BeforeTemplate
    Duration before(StepVerifier.LastStep step, Class<T> clazz) {
      return Refaster.anyOf(
          step.expectError(clazz).verify(),
          step.verifyErrorSatisfies(t -> assertThat(t).isInstanceOf(clazz)));
    }

    @AfterTemplate
    Duration after(StepVerifier.LastStep step, Class<T> clazz) {
      return step.verifyError(clazz);
    }
  }

  /**
   * Prefer {@link StepVerifier.LastStep#verifyErrorMatches(Predicate)} over more verbose
   * alternatives.
   */
  static final class StepVerifierLastStepVerifyErrorMatches {
    @BeforeTemplate
    Duration before(StepVerifier.LastStep step, Predicate<Throwable> predicate) {
      return step.expectErrorMatches(predicate).verify();
    }

    @AfterTemplate
    Duration after(StepVerifier.LastStep step, Predicate<Throwable> predicate) {
      return step.verifyErrorMatches(predicate);
    }
  }

  /**
   * Prefer {@link StepVerifier.LastStep#verifyErrorSatisfies(Consumer)} over more verbose
   * alternatives.
   */
  static final class StepVerifierLastStepVerifyErrorSatisfies {
    @BeforeTemplate
    Duration before(StepVerifier.LastStep step, Consumer<Throwable> consumer) {
      return step.expectErrorSatisfies(consumer).verify();
    }

    @AfterTemplate
    Duration after(StepVerifier.LastStep step, Consumer<Throwable> consumer) {
      return step.verifyErrorSatisfies(consumer);
    }
  }

  /**
   * Prefer {@link StepVerifier.LastStep#verifyErrorMessage(String)} over more verbose alternatives.
   */
  static final class StepVerifierLastStepVerifyErrorMessage {
    @BeforeTemplate
    Duration before(StepVerifier.LastStep step, String message) {
      return step.expectErrorMessage(message).verify();
    }

    @AfterTemplate
    Duration after(StepVerifier.LastStep step, String message) {
      return step.verifyErrorMessage(message);
    }
  }

  /**
   * Prefer {@link StepVerifier.LastStep#verifyTimeout(Duration)} over more verbose alternatives.
   */
  static final class StepVerifierLastStepVerifyTimeout {
    @BeforeTemplate
    Duration before(StepVerifier.LastStep step, Duration duration) {
      return step.expectTimeout(duration).verify();
    }

    @AfterTemplate
    Duration after(StepVerifier.LastStep step, Duration duration) {
      return step.verifyTimeout(duration);
    }
  }
}
