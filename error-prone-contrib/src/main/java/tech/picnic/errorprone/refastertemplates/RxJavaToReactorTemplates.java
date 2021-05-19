package tech.picnic.errorprone.refastertemplates;

import com.google.errorprone.refaster.Refaster;
import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import java.util.Map;
import org.reactivestreams.Publisher;
import reactor.adapter.rxjava.RxJava2Adapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

final class RxJavaToReactorTemplates {
  private RxJavaToReactorTemplates() {}

  // XXX: `function` type change; look into `Refaster.canBeCoercedTo(...)`.
  static final class FlowableFilter<S, T extends S> {
    @BeforeTemplate
    Flowable<T> before(Flowable<T> flowable, Predicate<S> predicate) {
      return flowable.filter(predicate);
    }

    @AfterTemplate
    Flowable<T> after(Flowable<T> flowable, java.util.function.Predicate<S> predicate) {
      return flowable
          .as(RxJava2Adapter::flowableToFlux)
          .filter(predicate)
          .as(RxJava2Adapter::fluxToFlowable);
    }
  }

  static final class FlowableFirstElement<T> {
    @BeforeTemplate
    Maybe<T> before(Flowable<T> flowable) {
      return flowable.firstElement();
    }

    @AfterTemplate
    Maybe<T> after(Flowable<T> flowable) {
      return flowable.as(RxJava2Adapter::flowableToFlux).next().as(RxJava2Adapter::monoToMaybe);
    }
  }

  // XXX: `function` type change; look into `Refaster.canBeCoercedTo(...)`.
  static final class FlowableFlatMap<I, T extends I, O, P extends Publisher<? extends O>> {
    @BeforeTemplate
    Flowable<O> before(Flowable<T> flowable, Function<I, P> function) {
      return flowable.flatMap(function);
    }

    @AfterTemplate
    Flowable<O> after(Flowable<I> flowable, java.util.function.Function<I, P> function) {
      return flowable
          .as(RxJava2Adapter::flowableToFlux)
          .flatMap(function)
          .as(RxJava2Adapter::fluxToFlowable);
    }
  }

  // XXX: `function` type change; look into `Refaster.canBeCoercedTo(...)`.
  static final class FlowableMap<I, T extends I, O> {
    @BeforeTemplate
    Flowable<O> before(Flowable<T> flowable, Function<I, O> function) {
      return flowable.map(function);
    }

    @AfterTemplate
    Flowable<O> after(Flowable<T> flowable, java.util.function.Function<I, O> function) {
      return flowable
          .as(RxJava2Adapter::flowableToFlux)
          .map(function)
          .as(RxJava2Adapter::fluxToFlowable);
    }
  }

  static final class FlowableToMap<I, T extends I, O> {
    @BeforeTemplate
    Single<Map<O, T>> before(Flowable<T> flowable, Function<I, O> function) {
      return flowable.toMap(function);
    }

    @AfterTemplate
    Single<Map<O, T>> after(Flowable<T> flowable, java.util.function.Function<I, O> function) {
      return flowable
          .as(RxJava2Adapter::flowableToFlux)
          .collectMap(function)
          .as(RxJava2Adapter::monoToSingle);
    }
  }

  static final class FlowableSwitchIfEmptyPublisher<S, T extends S> {
    @BeforeTemplate
    Flowable<S> before(Flowable<S> flowable, Publisher<S> publisher) {
      return flowable.switchIfEmpty(publisher);
    }

    @AfterTemplate
    Flowable<S> after(Flowable<S> flowable, Publisher<S> publisher) {
      return flowable
          .as(RxJava2Adapter::flowableToFlux)
          .switchIfEmpty(publisher)
          .as(RxJava2Adapter::fluxToFlowable);
    }
  }

  // XXX: Stephan, this is actually the static method, therefore I don't think it is correct.
  //  static final class MaybeDefer<S, T extends S> {
  //    @BeforeTemplate
  //    Maybe<S> before(Maybe<S> maybe, Callable<? extends Maybe<T>> source) {
  //      return maybe.defer(source);
  //    }
  //
  //    @AfterTemplate
  //    Maybe<S> after(Maybe<S> maybe, Callable<Maybe<T>> source) {
  //      return maybe.defer()
  //    }
  //  }

  //  static final class MaybeFlatMapSingleElement<
  //      I, T extends I, O, P extends SingleSource<? extends O>> { // <S, T extends S, O> {
  //    @BeforeTemplate
  //    Maybe<O> before(Maybe<T> maybe, Function<I, P> function) {
  //      return maybe.flatMapSingleElement(function);
  //    }
  //
  //    @AfterTemplate
  //    Maybe<O> after(Maybe<T> maybe, java.util.function.Function<I, P> function) {
  //      return maybe
  //          .as(RxJava2Adapter::maybeToMono)
  //          .flatMap(function)
  //          .as(RxJava2Adapter::monoToMaybe);
  //    }
  //  }

  static final class MaybeIgnoreElement<T> {
    @BeforeTemplate
    Completable before(Maybe<T> maybe) {
      return maybe.ignoreElement();
    }

    @AfterTemplate
    Completable after(Maybe<T> maybe) {
      return maybe
          .as(RxJava2Adapter::maybeToMono)
          .ignoreElement()
          .as(RxJava2Adapter::monoToCompletable);
    }
  }

  // ignoreelement.

  static final class MaybeSwitchIfEmpty<S, T extends S> {
    @BeforeTemplate
    Single<S> before(Maybe<S> maybe, Single<T> single) {
      return maybe.switchIfEmpty(single);
    }

    @AfterTemplate
    Single<S> after(Maybe<S> maybe, Single<T> single) {
      return maybe
          .as(RxJava2Adapter::maybeToMono)
          .switchIfEmpty(single.as(RxJava2Adapter::singleToMono))
          .as(RxJava2Adapter::monoToSingle);
    }
  }

  // XXX: `function` type change; look into `Refaster.canBeCoercedTo(...)`.
  static final class SingleFilter<S, T extends S> {
    @BeforeTemplate
    Maybe<T> before(Single<T> single, Predicate<S> predicate) {
      return single.filter(predicate);
    }

    @AfterTemplate
    Maybe<T> after(Single<T> single, java.util.function.Predicate<S> predicate) {
      return single
          .as(RxJava2Adapter::singleToMono)
          .filter(predicate)
          .as(RxJava2Adapter::monoToMaybe);
    }
  }

  // XXX: Check this one with Stephan:
  // XXX: `function` type change; look into `Refaster.canBeCoercedTo(...)`.
  //  static final class SingleFlatMap<I, T extends I, O> {  // P extends Single<? extends O>
  //    @BeforeTemplate
  //    Single<O> before(Single<T> single, Function<I, Single<? extends O>> function) {
  //      return single.flatMap(function);
  //    }
  //
  //    @AfterTemplate
  //    Single<O> after(Single<T> single, java.util.function.Function<I, Mono<? extends O>>
  // function) {
  //      return single
  //          .as(RxJava2Adapter::singleToMono)
  //          .flatMap(function)
  //          .as(RxJava2Adapter::monoToSingle);
  //    }
  //  }

  // XXX: `function` type change; look into `Refaster.canBeCoercedTo(...)`.
  static final class SingleMap<I, T extends I, O> {
    @BeforeTemplate
    Single<O> before(Single<T> single, Function<I, O> function) {
      return single.map(function);
    }

    @AfterTemplate
    Single<O> after(Single<T> single, java.util.function.Function<I, O> function) {
      return single.as(RxJava2Adapter::singleToMono).map(function).as(RxJava2Adapter::monoToSingle);
    }
  }

  static final class FluxToFlowableToFlux<T> {
    @BeforeTemplate
    Flux<T> before(Flux<T> flux, BackpressureStrategy strategy) {
      return Refaster.anyOf(
          flux.as(RxJava2Adapter::fluxToObservable)
              .toFlowable(strategy)
              .as(RxJava2Adapter::flowableToFlux),
          flux.as(RxJava2Adapter::fluxToFlowable).as(RxJava2Adapter::flowableToFlux));
    }

    @AfterTemplate
    Flux<T> after(Flux<T> flux) {
      return flux;
    }
  }

  // XXX: Stephan, what should we do with the naming here? Since it is not entirely correct now.
  static final class MonoToFlowableToMono<T> {
    @BeforeTemplate
    Mono<Void> before(Mono<Void> mono) {
      return mono.as(RxJava2Adapter::monoToCompletable).as(RxJava2Adapter::completableToMono);
    }

    @BeforeTemplate
    Mono<T> before2(Mono<T> mono) {
      return Refaster.anyOf(
          mono.as(RxJava2Adapter::monoToMaybe).as(RxJava2Adapter::maybeToMono),
          mono.as(RxJava2Adapter::monoToSingle).as(RxJava2Adapter::singleToMono));
    }

    @AfterTemplate
    Mono<T> after(Mono<T> mono) {
      return mono;
    }
  }
}
