package tech.picnic.errorprone.refasterrules;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.time.Duration;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.publisher.PublisherProbe;
import reactor.util.context.Context;
import reactor.util.function.Tuple2;
import tech.picnic.errorprone.refaster.test.RefasterRuleCollectionTestCase;

final class ReactorRulesTest implements RefasterRuleCollectionTestCase {
  @Override
  public ImmutableSet<?> elidedTypesAndStaticImports() {
    return ImmutableSet.of(assertThat(0), HashMap.class, ImmutableMap.class);
  }

  ImmutableSet<Mono<?>> testMonoFromSupplier() {
    return ImmutableSet.of(
        Mono.fromCallable((Callable<?>) null),
        Mono.fromCallable(() -> getClass().getDeclaredConstructor()),
        Mono.fromCallable(() -> toString()),
        Mono.fromCallable(getClass()::getDeclaredConstructor),
        Mono.fromCallable(this::toString));
  }

  ImmutableSet<Mono<Integer>> testMonoFromOptional() {
    return ImmutableSet.of(
        Mono.fromCallable(() -> Optional.of(1).orElse(null)),
        Mono.fromSupplier(() -> Optional.of(2).orElse(null)));
  }

  Mono<Tuple2<String, Integer>> testMonoZip() {
    return Mono.just("foo").zipWith(Mono.just(1));
  }

  Mono<String> testMonoZipWithCombinator() {
    return Mono.just("foo").zipWith(Mono.just(1), String::repeat);
  }

  Flux<Tuple2<String, Integer>> testFluxZip() {
    return Flux.just("foo", "bar").zipWith(Flux.just(1, 2));
  }

  Flux<String> testFluxZipWithCombinator() {
    return Flux.just("foo", "bar").zipWith(Flux.just(1, 2), String::repeat);
  }

  Flux<String> testFluxZipWithIterable() {
    return Flux.just("foo", "bar").zipWithIterable(ImmutableSet.of(1, 2), String::repeat);
  }

  Mono<Void> testMonoDeferredError() {
    return Mono.defer(() -> Mono.error(new IllegalStateException()));
  }

  Flux<Void> testFluxDeferredError() {
    return Flux.defer(() -> Flux.error(new IllegalStateException()));
  }

  Mono<Void> testMonoErrorSupplier() {
    return Mono.error(() -> ((Supplier<RuntimeException>) null).get());
  }

  Flux<Void> testFluxErrorSupplier() {
    return Flux.error(() -> ((Supplier<RuntimeException>) null).get());
  }

  Mono<String> testMonoThenReturn() {
    return Mono.empty().then(Mono.just("foo"));
  }

  Flux<Integer> testFluxTake() {
    return Flux.just(1, 2, 3).take(1);
  }

  Mono<Integer> testMonoSwitchIfEmptyOfEmptyPublisher() {
    return Mono.just(1).switchIfEmpty(Mono.empty());
  }

  ImmutableSet<Flux<Integer>> testFluxSwitchIfEmptyOfEmptyPublisher() {
    return ImmutableSet.of(
        Flux.just(1).switchIfEmpty(Mono.empty()), Flux.just(2).switchIfEmpty(Flux.empty()));
  }

  ImmutableSet<Flux<Integer>> testFluxConcatMap() {
    return ImmutableSet.of(
        Flux.just(1).flatMap(Mono::just, 1), Flux.just(2).flatMapSequential(Mono::just, 1));
  }

  ImmutableSet<Flux<Integer>> testFluxConcatMapWithPrefetch() {
    return ImmutableSet.of(
        Flux.just(1).flatMap(Mono::just, 1, 3), Flux.just(2).flatMapSequential(Mono::just, 1, 4));
  }

  Flux<Integer> testFluxConcatMapIterable() {
    return Flux.just(1, 2).flatMapIterable(ImmutableList::of);
  }

  Flux<Integer> testFluxConcatMapIterableWithPrefetch() {
    return Flux.just(1, 2).flatMapIterable(ImmutableList::of, 3);
  }

  Flux<String> testMonoFlatMapToFlux() {
    return Mono.just("foo").flatMapMany(s -> Mono.fromSupplier(() -> s + s));
  }

  ImmutableSet<Mono<String>> testMonoMap() {
    return ImmutableSet.of(
        Mono.just("foo").flatMap(s -> Mono.just(s)),
        Mono.just("bar").flatMap(s -> Mono.just(s.substring(1))));
  }

  ImmutableSet<Flux<Integer>> testFluxMap() {
    return ImmutableSet.of(
        Flux.just(1).concatMap(n -> Mono.just(n)),
        Flux.just(1).concatMap(n -> Flux.just(n * 2)),
        Flux.just(1).concatMap(n -> Mono.just(n), 3),
        Flux.just(1).concatMap(n -> Flux.just(n * 2), 3),
        Flux.just(1).concatMapDelayError(n -> Mono.just(n)),
        Flux.just(1).concatMapDelayError(n -> Flux.just(n * 2)),
        Flux.just(1).concatMapDelayError(n -> Mono.just(n), 3),
        Flux.just(1).concatMapDelayError(n -> Flux.just(n * 2), 3),
        Flux.just(1).flatMap(n -> Mono.just(n), 3),
        Flux.just(1).flatMap(n -> Flux.just(n * 2), 3),
        Flux.just(1).flatMap(n -> Mono.just(n), 3, 4),
        Flux.just(1).flatMap(n -> Flux.just(n * 2), 3, 4),
        Flux.just(1).flatMapDelayError(n -> Mono.just(n), 3, 4),
        Flux.just(1).flatMapDelayError(n -> Flux.just(n * 2), 3, 4),
        Flux.just(1).flatMapSequential(n -> Mono.just(n), 3),
        Flux.just(1).flatMapSequential(n -> Flux.just(n * 2), 3),
        Flux.just(1).flatMapSequential(n -> Mono.just(n), 3, 4),
        Flux.just(1).flatMapSequential(n -> Flux.just(n * 2), 3, 4),
        Flux.just(1).flatMapSequentialDelayError(n -> Mono.just(n), 3, 4),
        Flux.just(1).flatMapSequentialDelayError(n -> Flux.just(n * 2), 3, 4),
        Flux.just(1).switchMap(n -> Mono.just(n)),
        Flux.just(1).switchMap(n -> Flux.just(n * 2)));
  }

  ImmutableSet<Mono<String>> testMonoMapNotNull() {
    return ImmutableSet.of(
        Mono.just("foo").flatMap(s -> Mono.justOrEmpty(s)),
        Mono.just("bar").flatMap(s -> Mono.fromSupplier(() -> s.substring(1))));
  }

  ImmutableSet<Flux<Integer>> testFluxMapNotNull() {
    return ImmutableSet.of(
        Flux.just(1).concatMap(n -> Mono.justOrEmpty(n)),
        Flux.just(1).concatMap(n -> Mono.fromSupplier(() -> n * 2)),
        Flux.just(1).concatMap(n -> Mono.justOrEmpty(n), 3),
        Flux.just(1).concatMap(n -> Mono.fromSupplier(() -> n * 2), 3),
        Flux.just(1).concatMapDelayError(n -> Mono.justOrEmpty(n)),
        Flux.just(1).concatMapDelayError(n -> Mono.fromSupplier(() -> n * 2)),
        Flux.just(1).concatMapDelayError(n -> Mono.justOrEmpty(n), 3),
        Flux.just(1).concatMapDelayError(n -> Mono.fromSupplier(() -> n * 2), 3),
        Flux.just(1).flatMap(n -> Mono.justOrEmpty(n), 3),
        Flux.just(1).flatMap(n -> Mono.fromSupplier(() -> n * 2), 3),
        Flux.just(1).flatMap(n -> Mono.justOrEmpty(n), 3, 4),
        Flux.just(1).flatMap(n -> Mono.fromSupplier(() -> n * 2), 3, 4),
        Flux.just(1).flatMapDelayError(n -> Mono.justOrEmpty(n), 3, 4),
        Flux.just(1).flatMapDelayError(n -> Mono.fromSupplier(() -> n * 2), 3, 4),
        Flux.just(1).flatMapSequential(n -> Mono.justOrEmpty(n), 3),
        Flux.just(1).flatMapSequential(n -> Mono.fromSupplier(() -> n * 2), 3),
        Flux.just(1).flatMapSequential(n -> Mono.justOrEmpty(n), 3, 4),
        Flux.just(1).flatMapSequential(n -> Mono.fromSupplier(() -> n * 2), 3, 4),
        Flux.just(1).flatMapSequentialDelayError(n -> Mono.justOrEmpty(n), 3, 4),
        Flux.just(1).flatMapSequentialDelayError(n -> Mono.fromSupplier(() -> n * 2), 3, 4),
        Flux.just(1).switchMap(n -> Mono.justOrEmpty(n)),
        Flux.just(1).switchMap(n -> Mono.fromSupplier(() -> n * 2)));
  }

  Flux<String> testMonoFlux() {
    return Flux.concat(Mono.just("foo"));
  }

  ImmutableSet<Mono<Optional<String>>> testMonoCollectToOptional() {
    return ImmutableSet.of(
        Mono.just("foo").map(Optional::of).defaultIfEmpty(Optional.empty()),
        Mono.just("bar").map(Optional::of).switchIfEmpty(Mono.just(Optional.empty())));
  }

  Mono<Number> testMonoCast() {
    return Mono.just(1).map(Number.class::cast);
  }

  Flux<Number> testFluxCast() {
    return Flux.just(1).map(Number.class::cast);
  }

  ImmutableSet<Flux<String>> testConcatMapIterableIdentity() {
    return ImmutableSet.of(
        Flux.just(ImmutableList.of("foo")).concatMap(list -> Flux.fromIterable(list)),
        Flux.just(ImmutableList.of("bar")).concatMap(Flux::fromIterable));
  }

  ImmutableSet<Flux<String>> testConcatMapIterableIdentityWithPrefetch() {
    return ImmutableSet.of(
        Flux.just(ImmutableList.of("foo")).concatMap(list -> Flux.fromIterable(list), 1),
        Flux.just(ImmutableList.of("bar")).concatMap(Flux::fromIterable, 2));
  }

  Mono<Integer> testMonoDoOnError() {
    return Mono.just(1).doOnError(IllegalArgumentException.class::isInstance, e -> {});
  }

  Flux<Integer> testFluxDoOnError() {
    return Flux.just(1).doOnError(IllegalArgumentException.class::isInstance, e -> {});
  }

  Mono<Integer> testMonoOnErrorComplete() {
    return Mono.just(1).onErrorResume(e -> Mono.empty());
  }

  ImmutableSet<Flux<Integer>> testFluxOnErrorComplete() {
    return ImmutableSet.of(
        Flux.just(1).onErrorResume(e -> Mono.empty()),
        Flux.just(2).onErrorResume(e -> Flux.empty()));
  }

  ImmutableSet<Mono<Integer>> testMonoOnErrorCompleteClass() {
    return ImmutableSet.of(
        Mono.just(1).onErrorComplete(IllegalArgumentException.class::isInstance),
        Mono.just(2).onErrorResume(IllegalStateException.class, e -> Mono.empty()));
  }

  ImmutableSet<Flux<Integer>> testFluxOnErrorCompleteClass() {
    return ImmutableSet.of(
        Flux.just(1).onErrorComplete(IllegalArgumentException.class::isInstance),
        Flux.just(2).onErrorResume(IllegalStateException.class, e -> Mono.empty()),
        Flux.just(3).onErrorResume(AssertionError.class, e -> Flux.empty()));
  }

  Mono<Integer> testMonoOnErrorCompletePredicate() {
    return Mono.just(1).onErrorResume(e -> e.getCause() == null, e -> Mono.empty());
  }

  ImmutableSet<Flux<Integer>> testFluxOnErrorCompletePredicate() {
    return ImmutableSet.of(
        Flux.just(1).onErrorResume(e -> e.getCause() == null, e -> Mono.empty()),
        Flux.just(2).onErrorResume(e -> e.getCause() != null, e -> Flux.empty()));
  }

  Mono<Integer> testMonoOnErrorContinue() {
    return Mono.just(1).onErrorContinue(IllegalArgumentException.class::isInstance, (e, v) -> {});
  }

  Flux<Integer> testFluxOnErrorContinue() {
    return Flux.just(1).onErrorContinue(IllegalArgumentException.class::isInstance, (e, v) -> {});
  }

  Mono<Integer> testMonoOnErrorMap() {
    return Mono.just(1).onErrorMap(IllegalArgumentException.class::isInstance, e -> e);
  }

  Flux<Integer> testFluxOnErrorMap() {
    return Flux.just(1).onErrorMap(IllegalArgumentException.class::isInstance, e -> e);
  }

  Mono<Integer> testMonoOnErrorResume() {
    return Mono.just(1)
        .onErrorResume(IllegalArgumentException.class::isInstance, e -> Mono.just(2));
  }

  Flux<Integer> testFluxOnErrorResume() {
    return Flux.just(1)
        .onErrorResume(IllegalArgumentException.class::isInstance, e -> Flux.just(2));
  }

  Mono<Integer> testMonoOnErrorReturn() {
    return Mono.just(1).onErrorReturn(IllegalArgumentException.class::isInstance, 2);
  }

  Flux<Integer> testFluxOnErrorReturn() {
    return Flux.just(1).onErrorReturn(IllegalArgumentException.class::isInstance, 2);
  }

  ImmutableSet<Context> testContextEmpty() {
    return ImmutableSet.of(Context.of(new HashMap<>()), Context.of(ImmutableMap.of()));
  }

  ImmutableSet<PublisherProbe<Void>> testPublisherProbeEmpty() {
    return ImmutableSet.of(PublisherProbe.of(Mono.empty()), PublisherProbe.of(Flux.empty()));
  }

  StepVerifier.FirstStep<Integer> testStepVerifierFromMono() {
    return StepVerifier.create(Mono.just(1));
  }

  StepVerifier.FirstStep<Integer> testStepVerifierFromFlux() {
    return StepVerifier.create(Flux.just(1));
  }

  StepVerifier.Step<Integer> testStepVerifierStepExpectNextEmpty() {
    return StepVerifier.create(Mono.just(0)).expectNext();
  }

  ImmutableSet<StepVerifier.Step<String>> testStepVerifierStepExpectNext() {
    return ImmutableSet.of(
        StepVerifier.create(Mono.just("foo")).expectNextMatches(s -> s.equals("bar")),
        StepVerifier.create(Mono.just("baz")).expectNextMatches("qux"::equals));
  }

  Duration testStepVerifierLastStepVerifyComplete() {
    return StepVerifier.create(Mono.empty()).expectComplete().verify();
  }

  Duration testStepVerifierLastStepVerifyError() {
    return StepVerifier.create(Mono.empty()).expectError().verify();
  }

  ImmutableSet<Duration> testStepVerifierLastStepVerifyErrorClass() {
    return ImmutableSet.of(
        StepVerifier.create(Mono.empty()).expectError(IllegalArgumentException.class).verify(),
        StepVerifier.create(Mono.empty())
            .verifyErrorSatisfies(t -> assertThat(t).isInstanceOf(IllegalStateException.class)));
  }

  Duration testStepVerifierLastStepVerifyErrorMatches() {
    return StepVerifier.create(Mono.empty())
        .expectErrorMatches(IllegalArgumentException.class::equals)
        .verify();
  }

  Duration testStepVerifierLastStepVerifyErrorSatisfies() {
    return StepVerifier.create(Mono.empty()).expectErrorSatisfies(t -> {}).verify();
  }

  Duration testStepVerifierLastStepVerifyErrorMessage() {
    return StepVerifier.create(Mono.empty()).expectErrorMessage("foo").verify();
  }

  Duration testStepVerifierLastStepVerifyTimeout() {
    return StepVerifier.create(Mono.empty()).expectTimeout(Duration.ZERO).verify();
  }
}
