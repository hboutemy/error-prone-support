package tech.picnic.errorprone.refastertemplates;

import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import com.google.errorprone.refaster.annotation.Repeated;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import org.reactivestreams.Publisher;
import reactor.adapter.rxjava.RxJava2Adapter;
import reactor.core.publisher.Flux;

/** The Refaster templates for the migration of the RxJava Flowable type to Reactor */
public final class RxJavaFlowableToReactorTemplates {

  private RxJavaFlowableToReactorTemplates() {}

  // XXX: public static Flowable amb(Iterable)

  // XXX: Write test
  static final class FlowableAmbArray<T> {
    @BeforeTemplate
    Flowable<T> before(Publisher<? extends T>... sources) {
      return Flowable.ambArray(sources);
    }

    @AfterTemplate
    Flowable<T> after(Publisher<? extends T>... sources) {
      return RxJava2Adapter.fluxToFlowable(Flux.firstWithSignal(sources));
    }
  }

  // XXX: public static int bufferSize()
  // XXX: public static Flowable combineLatest(Function,Publisher[])
  // XXX: public static Flowable combineLatest(Iterable,Function)
  // XXX: public static Flowable combineLatest(Iterable,Function,int)
  // XXX: public static Flowable combineLatest(Publisher[],Function)
  // XXX: public static Flowable combineLatest(Publisher[],Function,int)

  // XXX: This wouldn't work for this case right?
  //     return Flowable.combineLatest(
  //  getEnabledConsentRequests(requiredConsentTopics, locale), // returns Flowable
  //            Flowable.fromIterable(requiredConsentTopics), // returns Flowable
  //          this::filterByTopic)
  //  XXX: Add test
  static final class FlowableCombineLatest<T1, T2, R> {
    @BeforeTemplate
    Flowable<R> before(
        Publisher<? extends T1> p1,
        Publisher<? extends T2> p2,
        BiFunction<? super T1, ? super T2, ? extends R> combiner) {
      return Flowable.combineLatest(p1, p2, combiner);
    }

    @AfterTemplate
    Flowable<R> after(
        Publisher<? extends T1> p1,
        Publisher<? extends T2> p2,
        BiFunction<? super T1, ? super T2, ? extends R> combiner) {
      return RxJava2Adapter.fluxToFlowable(
          Flux.<T1, T2, R>combineLatest(
              p1,
              p2,
              RxJavaToReactorTemplates.RxJava2ReactorMigrationUtil.toJdkBiFunction(combiner)));
    }
  }

  // XXX: public static Flowable combineLatest(Publisher,Publisher,Publisher,Function3)
  // XXX: public static Flowable combineLatest(Publisher,Publisher,Publisher,Publisher,Function4)
  // XXX: public static Flowable
  // combineLatest(Publisher,Publisher,Publisher,Publisher,Publisher,Function5)
  // XXX: public static Flowable
  // combineLatest(Publisher,Publisher,Publisher,Publisher,Publisher,Publisher,Function6)
  // XXX: public static Flowable
  // combineLatest(Publisher,Publisher,Publisher,Publisher,Publisher,Publisher,Publisher,Function7)
  // XXX: public static Flowable
  // combineLatest(Publisher,Publisher,Publisher,Publisher,Publisher,Publisher,Publisher,Publisher,Function8)
  // XXX: public static Flowable
  // combineLatest(Publisher,Publisher,Publisher,Publisher,Publisher,Publisher,Publisher,Publisher,Publisher,Function9)
  // XXX: public static Flowable combineLatestDelayError(Function,int,Publisher[])
  // XXX: public static Flowable combineLatestDelayError(Function,Publisher[])
  // XXX: public static Flowable combineLatestDelayError(Iterable,Function)
  // XXX: public static Flowable combineLatestDelayError(Iterable,Function,int)
  // XXX: public static Flowable combineLatestDelayError(Publisher[],Function)
  // XXX: public static Flowable combineLatestDelayError(Publisher[],Function,int)
  // XXX: public static Flowable concat(Iterable)
  // XXX: public static Flowable concat(Publisher)
  // XXX: public static Flowable concat(Publisher,int)
  // XXX: public static Flowable concat(Publisher,Publisher)
  // XXX: public static Flowable concat(Publisher,Publisher,Publisher)
  // XXX: public static Flowable concat(Publisher,Publisher,Publisher,Publisher)
  // XXX: public static Flowable concatArray(Publisher[])
  // XXX: public static Flowable concatArrayDelayError(Publisher[])
  // XXX: public static Flowable concatArrayEager(int,int,Publisher[])
  // XXX: public static Flowable concatArrayEager(Publisher[])
  // XXX: public static Flowable concatArrayEagerDelayError(int,int,Publisher[])
  // XXX: public static Flowable concatArrayEagerDelayError(Publisher[])
  // XXX: public static Flowable concatDelayError(Iterable)
  // XXX: public static Flowable concatDelayError(Publisher)
  // XXX: public static Flowable concatDelayError(Publisher,int,boolean)
  // XXX: public static Flowable concatEager(Iterable)
  // XXX: public static Flowable concatEager(Iterable,int,int)
  // XXX: public static Flowable concatEager(Publisher)
  // XXX: public static Flowable concatEager(Publisher,int,int)
  // XXX: public static Flowable create(FlowableOnSubscribe,BackpressureStrategy)

  static final class FlowableDefer<T> {
    @BeforeTemplate
    Flowable<T> before(Callable<? extends Publisher<? extends T>> supplier) {
      return Flowable.defer(supplier);
    }

    @AfterTemplate
    Flowable<T> after(Callable<? extends Publisher<T>> supplier) {
      return RxJava2Adapter.fluxToFlowable(
          Flux.defer(
              RxJavaToReactorTemplates.RxJava2ReactorMigrationUtil.callableAsSupplier(supplier)));
    }
  }

  static final class FlowableEmpty<T> {
    @BeforeTemplate
    Flowable<T> before() {
      return Flowable.empty();
    }

    @AfterTemplate
    Flowable<T> after() {
      return RxJava2Adapter.fluxToFlowable(Flux.empty());
    }
  }

  // XXX: Use `CanBeCoercedTo`.
  static final class FlowableErrorCallable<T> {
    @BeforeTemplate
    Flowable<T> before(Callable<? extends Throwable> throwable) {
      return Flowable.error(throwable);
    }

    @AfterTemplate
    Flowable<T> after(Supplier<? extends Throwable> throwable) {
      return RxJava2Adapter.fluxToFlowable(Flux.error(throwable));
    }
  }

  static final class FlowableErrorThrowable<T> {
    @BeforeTemplate
    Flowable<T> before(Throwable throwable) {
      return Flowable.error(throwable);
    }

    @AfterTemplate
    Flowable<T> after(Throwable throwable) {
      return RxJava2Adapter.fluxToFlowable(Flux.error(throwable));
    }
  }
  // XXX: public static Flowable fromArray(Object[])
  // XXX: public static Flowable fromCallable(Callable)
  // XXX: public static Flowable fromFuture(Future)
  // XXX: public static Flowable fromFuture(Future,long,TimeUnit)
  // XXX: public static Flowable fromFuture(Future,long,TimeUnit,Scheduler)
  // XXX: public static Flowable fromFuture(Future,Scheduler)
  // XXX: public static Flowable fromIterable(Iterable)
  // XXX: public static Flowable fromPublisher(Publisher)
  // XXX: public static Flowable generate(Callable,BiConsumer)
  // XXX: public static Flowable generate(Callable,BiConsumer,Consumer)
  // XXX: public static Flowable generate(Callable,BiFunction)
  // XXX: public static Flowable generate(Callable,BiFunction,Consumer)
  // XXX: public static Flowable generate(Consumer)
  // XXX: public static Flowable interval(long,long,TimeUnit)
  // XXX: public static Flowable interval(long,long,TimeUnit,Scheduler)
  // XXX: public static Flowable interval(long,TimeUnit)
  // XXX: public static Flowable interval(long,TimeUnit,Scheduler)
  // XXX: public static Flowable intervalRange(long,long,long,long,TimeUnit)
  // XXX: public static Flowable intervalRange(long,long,long,long,TimeUnit,Scheduler)

  static final class FlowableJust<T> {
    @BeforeTemplate
    Flowable<T> before(T t) {
      return Flowable.just(t);
    }

    @AfterTemplate
    Flowable<T> after(T t) {
      return RxJava2Adapter.fluxToFlowable(Flux.just(t));
    }
  }

  static final class FlowableJustTwo<T> {
    @BeforeTemplate
    Flowable<T> before(T t, @Repeated T arguments) {
      return Flowable.just(t, arguments);
    }

    @AfterTemplate
    Flowable<T> after(T t, @Repeated T arguments) {
      return RxJava2Adapter.fluxToFlowable(Flux.just(t, arguments));
    }
  }

  // XXX: public static Flowable just(Object,Object,Object)
  // XXX: public static Flowable just(Object,Object,Object,Object)
  // XXX: public static Flowable just(Object,Object,Object,Object,Object)
  // XXX: public static Flowable just(Object,Object,Object,Object,Object,Object)
  // XXX: public static Flowable just(Object,Object,Object,Object,Object,Object,Object)
  // XXX: public static Flowable just(Object,Object,Object,Object,Object,Object,Object,Object)
  // XXX: public static Flowable
  // just(Object,Object,Object,Object,Object,Object,Object,Object,Object)
  // XXX: public static Flowable
  // just(Object,Object,Object,Object,Object,Object,Object,Object,Object,Object)
  // XXX: public static Flowable merge(Iterable)
  // XXX: public static Flowable merge(Iterable,int)
  // XXX: public static Flowable merge(Iterable,int,int)
  // XXX: public static Flowable merge(Publisher)
  // XXX: public static Flowable merge(Publisher,int)
  // XXX: public static Flowable merge(Publisher,Publisher)
  // XXX: public static Flowable merge(Publisher,Publisher,Publisher)
  // XXX: public static Flowable merge(Publisher,Publisher,Publisher,Publisher)
  // XXX: public static Flowable mergeArray(int,int,Publisher[])
  // XXX: public static Flowable mergeArray(Publisher[])
  // XXX: public static Flowable mergeArrayDelayError(int,int,Publisher[])
  // XXX: public static Flowable mergeArrayDelayError(Publisher[])
  // XXX: public static Flowable mergeDelayError(Iterable)
  // XXX: public static Flowable mergeDelayError(Iterable,int)
  // XXX: public static Flowable mergeDelayError(Iterable,int,int)
  // XXX: public static Flowable mergeDelayError(Publisher)
  // XXX: public static Flowable mergeDelayError(Publisher,int)
  // XXX: public static Flowable mergeDelayError(Publisher,Publisher)
  // XXX: public static Flowable mergeDelayError(Publisher,Publisher,Publisher)
  // XXX: public static Flowable mergeDelayError(Publisher,Publisher,Publisher,Publisher)
  // XXX: public static Flowable never()
  // XXX: public static Flowable range(int,int)
  // XXX: public static Flowable rangeLong(long,long)
  // XXX: public static Single sequenceEqual(Publisher,Publisher)
  // XXX: public static Single sequenceEqual(Publisher,Publisher,BiPredicate)
  // XXX: public static Single sequenceEqual(Publisher,Publisher,BiPredicate,int)
  // XXX: public static Single sequenceEqual(Publisher,Publisher,int)
  // XXX: public static Flowable switchOnNext(Publisher)
  // XXX: public static Flowable switchOnNext(Publisher,int)
  // XXX: public static Flowable switchOnNextDelayError(Publisher)
  // XXX: public static Flowable switchOnNextDelayError(Publisher,int)
  // XXX: public static Flowable timer(long,TimeUnit)
  // XXX: public static Flowable timer(long,TimeUnit,Scheduler)
  // XXX: public static Flowable unsafeCreate(Publisher)
  // XXX: public static Flowable using(Callable,Function,Consumer)
  // XXX: public static Flowable using(Callable,Function,Consumer,boolean)
  // XXX: public static Flowable zip(Iterable,Function)
  // XXX: public static Flowable zip(Publisher,Function)
  // XXX: public static Flowable zip(Publisher,Publisher,BiFunction)
  // XXX: public static Flowable zip(Publisher,Publisher,BiFunction,boolean)
  // XXX: public static Flowable zip(Publisher,Publisher,BiFunction,boolean,int)
  // XXX: public static Flowable zip(Publisher,Publisher,Publisher,Function3)
  // XXX: public static Flowable zip(Publisher,Publisher,Publisher,Publisher,Function4)
  // XXX: public static Flowable zip(Publisher,Publisher,Publisher,Publisher,Publisher,Function5)
  // XXX: public static Flowable
  // zip(Publisher,Publisher,Publisher,Publisher,Publisher,Publisher,Function6)
  // XXX: public static Flowable
  // zip(Publisher,Publisher,Publisher,Publisher,Publisher,Publisher,Publisher,Function7)
  // XXX: public static Flowable
  // zip(Publisher,Publisher,Publisher,Publisher,Publisher,Publisher,Publisher,Publisher,Function8)
  // XXX: public static Flowable
  // zip(Publisher,Publisher,Publisher,Publisher,Publisher,Publisher,Publisher,Publisher,Publisher,Function9)
  // XXX: public static Flowable zipArray(Function,boolean,int,Publisher[])
  // XXX: public static Flowable zipIterable(Iterable,Function,boolean,int)

  // XXX: public final Single all(Predicate)
  // XXX: public final Flowable ambWith(Publisher)
  // XXX: public final Single any(Predicate)
  // XXX: public final Object as(FlowableConverter)
  // XXX: public final Object blockingFirst()
  // XXX: public final Object blockingFirst(Object)
  // XXX: public final void blockingForEach(Consumer)
  // XXX: public final Iterable blockingIterable()
  // XXX: public final Iterable blockingIterable(int)
  // XXX: public final Object blockingLast()
  // XXX: public final Object blockingLast(Object)
  // XXX: public final Iterable blockingLatest()
  // XXX: public final Iterable blockingMostRecent(Object)
  // XXX: public final Iterable blockingNext()

  // XXX: public final Object blockingSingle()
  // XXX: public final Object blockingSingle(Object)
  // XXX: public final void blockingSubscribe()
  // XXX: public final void blockingSubscribe(Consumer)
  // XXX: public final void blockingSubscribe(Consumer,Consumer)
  // XXX: public final void blockingSubscribe(Consumer,Consumer,Action)
  // XXX: public final void blockingSubscribe(Consumer,Consumer,Action,int)
  // XXX: public final void blockingSubscribe(Consumer,Consumer,int)
  // XXX: public final void blockingSubscribe(Consumer,int)
  // XXX: public final void blockingSubscribe(Subscriber)
  // XXX: public final Flowable buffer(Callable)
  // XXX: public final Flowable buffer(Callable,Callable)
  // XXX: public final Flowable buffer(Flowable,Function)
  // XXX: public final Flowable buffer(Flowable,Function,Callable)
  // XXX: public final Flowable buffer(int)
  // XXX: public final Flowable buffer(int,Callable)
  // XXX: public final Flowable buffer(int,int)
  // XXX: public final Flowable buffer(int,int,Callable)
  // XXX: public final Flowable buffer(long,long,TimeUnit)
  // XXX: public final Flowable buffer(long,long,TimeUnit,Scheduler)
  // XXX: public final Flowable buffer(long,long,TimeUnit,Scheduler,Callable)
  // XXX: public final Flowable buffer(long,TimeUnit)
  // XXX: public final Flowable buffer(long,TimeUnit,int)
  // XXX: public final Flowable buffer(long,TimeUnit,Scheduler)
  // XXX: public final Flowable buffer(long,TimeUnit,Scheduler,int)
  // XXX: public final Flowable buffer(long,TimeUnit,Scheduler,int,Callable,boolean)
  // XXX: public final Flowable buffer(Publisher)
  // XXX: public final Flowable buffer(Publisher,Callable)
  // XXX: public final Flowable buffer(Publisher,int)
  // XXX: public final Flowable cache()
  // XXX: public final Flowable cacheWithInitialCapacity(int)
  // XXX: public final Flowable cast(Class)
  // XXX: public final Single collect(Callable,BiConsumer)
  // XXX: public final Single collectInto(Object,BiConsumer)
  // XXX: public final Flowable compose(FlowableTransformer)
  // XXX: public final Flowable concatMap(Function)
  // XXX: public final Flowable concatMap(Function,int)
  // XXX: public final Completable concatMapCompletable(Function)
  // XXX: public final Completable concatMapCompletable(Function,int)
  // XXX: public final Completable concatMapCompletableDelayError(Function)
  // XXX: public final Completable concatMapCompletableDelayError(Function,boolean)
  // XXX: public final Completable concatMapCompletableDelayError(Function,boolean,int)
  // XXX: public final Flowable concatMapDelayError(Function)
  // XXX: public final Flowable concatMapDelayError(Function,int,boolean)
  // XXX: public final Flowable concatMapEager(Function)
  // XXX: public final Flowable concatMapEager(Function,int,int)
  // XXX: public final Flowable concatMapEagerDelayError(Function,boolean)
  // XXX: public final Flowable concatMapEagerDelayError(Function,int,int,boolean)
  // XXX: public final Flowable concatMapIterable(Function)
  // XXX: public final Flowable concatMapIterable(Function,int)
  // XXX: public final Flowable concatMapMaybe(Function)
  // XXX: public final Flowable concatMapMaybe(Function,int)
  // XXX: public final Flowable concatMapMaybeDelayError(Function)
  // XXX: public final Flowable concatMapMaybeDelayError(Function,boolean)
  // XXX: public final Flowable concatMapMaybeDelayError(Function,boolean,int)
  // XXX: public final Flowable concatMapSingle(Function)
  // XXX: public final Flowable concatMapSingle(Function,int)
  // XXX: public final Flowable concatMapSingleDelayError(Function)
  // XXX: public final Flowable concatMapSingleDelayError(Function,boolean)
  // XXX: public final Flowable concatMapSingleDelayError(Function,boolean,int)
  // XXX: public final Flowable concatWith(CompletableSource)
  // XXX: public final Flowable concatWith(MaybeSource)

  static final class FlowableConcatWithPublisher<T> {
    @BeforeTemplate
    Flowable<T> before(Flowable<T> flowable, Publisher<T> source) {
      return flowable.concatWith(source);
    }

    @AfterTemplate
    Flowable<T> after(Flowable<T> flowable, Publisher<T> source) {
      return flowable
          .as(RxJava2Adapter::flowableToFlux)
          .concatWith(source)
          .as(RxJava2Adapter::fluxToFlowable);
    }
  }

  // XXX: public final Flowable concatWith(SingleSource)
  // XXX: public final Single contains(Object)
  // XXX: public final Single count()
  // XXX: public final Flowable debounce(Function)
  // XXX: public final Flowable debounce(long,TimeUnit)
  // XXX: public final Flowable debounce(long,TimeUnit,Scheduler)
  // XXX: public final Flowable defaultIfEmpty(Object)
  // XXX: public final Flowable delay(Function)
  // XXX: public final Flowable delay(long,TimeUnit)
  // XXX: public final Flowable delay(long,TimeUnit,boolean)
  // XXX: public final Flowable delay(long,TimeUnit,Scheduler)
  // XXX: public final Flowable delay(long,TimeUnit,Scheduler,boolean)
  // XXX: public final Flowable delay(Publisher,Function)
  // XXX: public final Flowable delaySubscription(long,TimeUnit)
  // XXX: public final Flowable delaySubscription(long,TimeUnit,Scheduler)
  // XXX: public final Flowable delaySubscription(Publisher)
  // XXX: public final Flowable dematerialize()
  // XXX: public final Flowable dematerialize(Function)
  // XXX: public final Flowable distinct()
  // XXX: public final Flowable distinct(Function)
  // XXX: public final Flowable distinct(Function,Callable)
  // XXX: public final Flowable distinctUntilChanged()
  // XXX: public final Flowable distinctUntilChanged(BiPredicate)
  // XXX: public final Flowable distinctUntilChanged(Function)
  // XXX: public final Flowable doAfterNext(Consumer)
  // XXX: public final Flowable doAfterTerminate(Action)
  // XXX: public final Flowable doFinally(Action)
  // XXX: public final Flowable doOnCancel(Action)
  // XXX: public final Flowable doOnComplete(Action)
  // XXX: public final Flowable doOnEach(Consumer)
  // XXX: public final Flowable doOnEach(Subscriber)
  // XXX: public final Flowable doOnError(Consumer)
  // XXX: public final Flowable doOnLifecycle(Consumer,LongConsumer,Action)
  // XXX: public final Flowable doOnNext(Consumer)
  // XXX: public final Flowable doOnRequest(LongConsumer)
  // XXX: public final Flowable doOnSubscribe(Consumer)
  // XXX: public final Flowable doOnTerminate(Action)
  // XXX: public final Maybe elementAt(long)
  // XXX: public final Single elementAt(long,Object)
  // XXX: public final Single elementAtOrError(long)

  // XXX: `Refaster.canBeCoercedTo(...)`.
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
  // XXX: public final Single first(Object)

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

  // XXX: public final Single firstOrError()

  // XXX: `Refaster.canBeCoercedTo(...)`.
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

  // XXX: public final Flowable flatMap(Function,BiFunction)
  // XXX: public final Flowable flatMap(Function,BiFunction,boolean)
  // XXX: public final Flowable flatMap(Function,BiFunction,boolean,int)
  // XXX: public final Flowable flatMap(Function,BiFunction,boolean,int,int)
  // XXX: public final Flowable flatMap(Function,BiFunction,int)
  // XXX: public final Flowable flatMap(Function,boolean)
  // XXX: public final Flowable flatMap(Function,boolean,int)
  // XXX: public final Flowable flatMap(Function,boolean,int,int)
  // XXX: public final Flowable flatMap(Function,Function,Callable)
  // XXX: public final Flowable flatMap(Function,Function,Callable,int)
  // XXX: public final Flowable flatMap(Function,int)
  // XXX: public final Completable flatMapCompletable(Function)
  // XXX: public final Completable flatMapCompletable(Function,boolean,int)
  // XXX: public final Flowable flatMapIterable(Function)
  // XXX: public final Flowable flatMapIterable(Function,BiFunction)
  // XXX: public final Flowable flatMapIterable(Function,BiFunction,int)
  // XXX: public final Flowable flatMapIterable(Function,int)
  // XXX: public final Flowable flatMapMaybe(Function)
  // XXX: public final Flowable flatMapMaybe(Function,boolean,int)
  // XXX: public final Flowable flatMapSingle(Function)
  // XXX: public final Flowable flatMapSingle(Function,boolean,int)
  // XXX: public final Disposable forEach(Consumer)
  // XXX: public final Disposable forEachWhile(Predicate)
  // XXX: public final Disposable forEachWhile(Predicate,Consumer)
  // XXX: public final Disposable forEachWhile(Predicate,Consumer,Action)
  // XXX: public final Flowable groupBy(Function)
  // XXX: public final Flowable groupBy(Function,boolean)
  // XXX: public final Flowable groupBy(Function,Function)
  // XXX: public final Flowable groupBy(Function,Function,boolean)
  // XXX: public final Flowable groupBy(Function,Function,boolean,int)
  // XXX: public final Flowable groupBy(Function,Function,boolean,int,Function)
  // XXX: public final Flowable groupJoin(Publisher,Function,Function,BiFunction)
  // XXX: public final Flowable hide()
  // XXX: public final Completable ignoreElements()
  // XXX: public final Single isEmpty()
  // XXX: public final Flowable join(Publisher,Function,Function,BiFunction)
  // XXX: public final Single last(Object)
  // XXX: public final Maybe lastElement()
  // XXX: public final Single lastOrError()
  // XXX: public final Flowable lift(FlowableOperator)
  // XXX: public final Flowable limit(long)

  // XXX: `Refaster.canBeCoercedTo(...)`.
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

  // XXX: public final Flowable materialize()
  // XXX: public final Flowable mergeWith(CompletableSource)
  // XXX: public final Flowable mergeWith(MaybeSource)
  // XXX: public final Flowable mergeWith(Publisher)
  // XXX: public final Flowable mergeWith(SingleSource)
  // XXX: public final Flowable observeOn(Scheduler)
  // XXX: public final Flowable observeOn(Scheduler,boolean)
  // XXX: public final Flowable observeOn(Scheduler,boolean,int)
  // XXX: public final Flowable ofType(Class)
  // XXX: public final Flowable onBackpressureBuffer()
  // XXX: public final Flowable onBackpressureBuffer(boolean)
  // XXX: public final Flowable onBackpressureBuffer(int)
  // XXX: public final Flowable onBackpressureBuffer(int,Action)
  // XXX: public final Flowable onBackpressureBuffer(int,boolean)
  // XXX: public final Flowable onBackpressureBuffer(int,boolean,boolean)
  // XXX: public final Flowable onBackpressureBuffer(int,boolean,boolean,Action)
  // XXX: public final Flowable onBackpressureBuffer(long,Action,BackpressureOverflowStrategy)
  // XXX: public final Flowable onBackpressureDrop()
  // XXX: public final Flowable onBackpressureDrop(Consumer)
  // XXX: public final Flowable onBackpressureLatest()
  // XXX: public final Flowable onErrorResumeNext(Function)
  // XXX: public final Flowable onErrorResumeNext(Publisher)
  // XXX: public final Flowable onErrorReturn(Function)
  // XXX: public final Flowable onErrorReturnItem(Object)
  // XXX: public final Flowable onExceptionResumeNext(Publisher)
  // XXX: public final Flowable onTerminateDetach()
  // XXX: public final ParallelFlowable parallel()
  // XXX: public final ParallelFlowable parallel(int)
  // XXX: public final ParallelFlowable parallel(int,int)
  // XXX: public final ConnectableFlowable publish()
  // XXX: public final Flowable publish(Function)
  // XXX: public final Flowable publish(Function,int)
  // XXX: public final ConnectableFlowable publish(int)
  // XXX: public final Flowable rebatchRequests(int)
  // XXX: public final Maybe reduce(BiFunction)
  // XXX: public final Single reduce(Object,BiFunction)
  // XXX: public final Single reduceWith(Callable,BiFunction)
  // XXX: public final Flowable repeat()
  // XXX: public final Flowable repeat(long)
  // XXX: public final Flowable repeatUntil(BooleanSupplier)
  // XXX: public final Flowable repeatWhen(Function)
  // XXX: public final ConnectableFlowable replay()
  // XXX: public final Flowable replay(Function)
  // XXX: public final Flowable replay(Function,int)
  // XXX: public final Flowable replay(Function,int,long,TimeUnit)
  // XXX: public final Flowable replay(Function,int,long,TimeUnit,Scheduler)
  // XXX: public final Flowable replay(Function,int,Scheduler)
  // XXX: public final Flowable replay(Function,long,TimeUnit)
  // XXX: public final Flowable replay(Function,long,TimeUnit,Scheduler)
  // XXX: public final Flowable replay(Function,Scheduler)
  // XXX: public final ConnectableFlowable replay(int)
  // XXX: public final ConnectableFlowable replay(int,long,TimeUnit)
  // XXX: public final ConnectableFlowable replay(int,long,TimeUnit,Scheduler)
  // XXX: public final ConnectableFlowable replay(int,Scheduler)
  // XXX: public final ConnectableFlowable replay(long,TimeUnit)
  // XXX: public final ConnectableFlowable replay(long,TimeUnit,Scheduler)
  // XXX: public final ConnectableFlowable replay(Scheduler)
  // XXX: public final Flowable retry()
  // XXX: public final Flowable retry(BiPredicate)
  // XXX: public final Flowable retry(long)
  // XXX: public final Flowable retry(long,Predicate)
  // XXX: public final Flowable retry(Predicate)
  // XXX: public final Flowable retryUntil(BooleanSupplier)
  // XXX: public final Flowable retryWhen(Function)
  // XXX: public final void safeSubscribe(Subscriber)
  // XXX: public final Flowable sample(long,TimeUnit)
  // XXX: public final Flowable sample(long,TimeUnit,boolean)
  // XXX: public final Flowable sample(long,TimeUnit,Scheduler)
  // XXX: public final Flowable sample(long,TimeUnit,Scheduler,boolean)
  // XXX: public final Flowable sample(Publisher)
  // XXX: public final Flowable sample(Publisher,boolean)
  // XXX: public final Flowable scan(BiFunction)
  // XXX: public final Flowable scan(Object,BiFunction)
  // XXX: public final Flowable scanWith(Callable,BiFunction)
  // XXX: public final Flowable serialize()
  // XXX: public final Flowable share()
  // XXX: public final Single single(Object)
  // XXX: public final Maybe singleElement()
  // XXX: public final Single singleOrError()
  // XXX: public final Flowable skip(long)
  // XXX: public final Flowable skip(long,TimeUnit)
  // XXX: public final Flowable skip(long,TimeUnit,Scheduler)
  // XXX: public final Flowable skipLast(int)
  // XXX: public final Flowable skipLast(long,TimeUnit)
  // XXX: public final Flowable skipLast(long,TimeUnit,boolean)
  // XXX: public final Flowable skipLast(long,TimeUnit,Scheduler)
  // XXX: public final Flowable skipLast(long,TimeUnit,Scheduler,boolean)
  // XXX: public final Flowable skipLast(long,TimeUnit,Scheduler,boolean,int)
  // XXX: public final Flowable skipUntil(Publisher)
  // XXX: public final Flowable skipWhile(Predicate)
  // XXX: public final Flowable sorted()
  // XXX: public final Flowable sorted(java.util.Comparator)
  // XXX: public final Flowable startWith(Iterable)
  // XXX: public final Flowable startWith(Object)
  // XXX: public final Flowable startWith(Publisher)
  // XXX: public final Flowable startWithArray(Object[])
  // XXX: public final Disposable subscribe()
  // XXX: public final Disposable subscribe(Consumer)
  // XXX: public final Disposable subscribe(Consumer,Consumer)
  // XXX: public final Disposable subscribe(Consumer,Consumer,Action)
  // XXX: public final Disposable subscribe(Consumer,Consumer,Action,Consumer)
  // XXX: public final void subscribe(FlowableSubscriber)
  // XXX: public final void subscribe(Subscriber)
  // XXX: public final Flowable subscribeOn(Scheduler)
  // XXX: public final Flowable subscribeOn(Scheduler,boolean)
  // XXX: public final Subscriber subscribeWith(Subscriber)

  static final class FlowableSwitchIfEmpty<T> {
    @BeforeTemplate
    Flowable<T> before(Flowable<T> flowable, Publisher<T> publisher) {
      return flowable.switchIfEmpty(publisher);
    }

    @AfterTemplate
    Flowable<T> after(Flowable<T> flowable, Publisher<T> publisher) {
      return flowable
          .as(RxJava2Adapter::flowableToFlux)
          .switchIfEmpty(publisher)
          .as(RxJava2Adapter::fluxToFlowable);
    }
  }

  // XXX: public final Flowable switchMap(Function)
  // XXX: public final Flowable switchMap(Function,int)
  // XXX: public final Completable switchMapCompletable(Function)
  // XXX: public final Completable switchMapCompletableDelayError(Function)
  // XXX: public final Flowable switchMapDelayError(Function)
  // XXX: public final Flowable switchMapDelayError(Function,int)
  // XXX: public final Flowable switchMapMaybe(Function)
  // XXX: public final Flowable switchMapMaybeDelayError(Function)
  // XXX: public final Flowable switchMapSingle(Function)
  // XXX: public final Flowable switchMapSingleDelayError(Function)
  // XXX: public final Flowable take(long)
  // XXX: public final Flowable take(long,TimeUnit)
  // XXX: public final Flowable take(long,TimeUnit,Scheduler)
  // XXX: public final Flowable takeLast(int)
  // XXX: public final Flowable takeLast(long,long,TimeUnit)
  // XXX: public final Flowable takeLast(long,long,TimeUnit,Scheduler)
  // XXX: public final Flowable takeLast(long,long,TimeUnit,Scheduler,boolean,int)
  // XXX: public final Flowable takeLast(long,TimeUnit)
  // XXX: public final Flowable takeLast(long,TimeUnit,boolean)
  // XXX: public final Flowable takeLast(long,TimeUnit,Scheduler)
  // XXX: public final Flowable takeLast(long,TimeUnit,Scheduler,boolean)
  // XXX: public final Flowable takeLast(long,TimeUnit,Scheduler,boolean,int)
  // XXX: public final Flowable takeUntil(Predicate)
  // XXX: public final Flowable takeUntil(Publisher)
  // XXX: public final Flowable takeWhile(Predicate)
  // XXX: public final Flowable throttleFirst(long,TimeUnit)
  // XXX: public final Flowable throttleFirst(long,TimeUnit,Scheduler)
  // XXX: public final Flowable throttleLast(long,TimeUnit)
  // XXX: public final Flowable throttleLast(long,TimeUnit,Scheduler)
  // XXX: public final Flowable throttleLatest(long,TimeUnit)
  // XXX: public final Flowable throttleLatest(long,TimeUnit,boolean)
  // XXX: public final Flowable throttleLatest(long,TimeUnit,Scheduler)
  // XXX: public final Flowable throttleLatest(long,TimeUnit,Scheduler,boolean)
  // XXX: public final Flowable throttleWithTimeout(long,TimeUnit)
  // XXX: public final Flowable throttleWithTimeout(long,TimeUnit,Scheduler)
  // XXX: public final Flowable timeInterval()
  // XXX: public final Flowable timeInterval(Scheduler)
  // XXX: public final Flowable timeInterval(TimeUnit)
  // XXX: public final Flowable timeInterval(TimeUnit,Scheduler)
  // XXX: public final Flowable timeout(Function)
  // XXX: public final Flowable timeout(Function,Flowable)
  // XXX: public final Flowable timeout(long,TimeUnit)
  // XXX: public final Flowable timeout(long,TimeUnit,Publisher)
  // XXX: public final Flowable timeout(long,TimeUnit,Scheduler)
  // XXX: public final Flowable timeout(long,TimeUnit,Scheduler,Publisher)
  // XXX: public final Flowable timeout(Publisher,Function)
  // XXX: public final Flowable timeout(Publisher,Function,Publisher)
  // XXX: public final Flowable timestamp()
  // XXX: public final Flowable timestamp(Scheduler)
  // XXX: public final Flowable timestamp(TimeUnit)
  // XXX: public final Flowable timestamp(TimeUnit,Scheduler)
  // XXX: public final Object to(Function)
  // XXX: public final Future toFuture()
  // XXX: public final Single toList()
  // XXX: public final Single toList(Callable)
  // XXX: public final Single toList(int)

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

  // XXX: public final Single toMap(Function,Function)
  // XXX: public final Single toMap(Function,Function,Callable)
  // XXX: public final Single toMultimap(Function)
  // XXX: public final Single toMultimap(Function,Function)
  // XXX: public final Single toMultimap(Function,Function,Callable)
  // XXX: public final Single toMultimap(Function,Function,Callable,Function)
  // XXX: public final Observable toObservable()
  // XXX: public final Single toSortedList()
  // XXX: public final Single toSortedList(int)
  // XXX: public final Single toSortedList(java.util.Comparator)
  // XXX: public final Single toSortedList(java.util.Comparator,int)
  // XXX: public final Flowable unsubscribeOn(Scheduler)
  // XXX: public final Flowable window(Callable)
  // XXX: public final Flowable window(Callable,int)
  // XXX: public final Flowable window(long)
  // XXX: public final Flowable window(long,long)
  // XXX: public final Flowable window(long,long,int)
  // XXX: public final Flowable window(long,long,TimeUnit)
  // XXX: public final Flowable window(long,long,TimeUnit,Scheduler)
  // XXX: public final Flowable window(long,long,TimeUnit,Scheduler,int)
  // XXX: public final Flowable window(long,TimeUnit)
  // XXX: public final Flowable window(long,TimeUnit,long)
  // XXX: public final Flowable window(long,TimeUnit,long,boolean)
  // XXX: public final Flowable window(long,TimeUnit,Scheduler)
  // XXX: public final Flowable window(long,TimeUnit,Scheduler,long)
  // XXX: public final Flowable window(long,TimeUnit,Scheduler,long,boolean)
  // XXX: public final Flowable window(long,TimeUnit,Scheduler,long,boolean,int)
  // XXX: public final Flowable window(Publisher)
  // XXX: public final Flowable window(Publisher,Function)
  // XXX: public final Flowable window(Publisher,Function,int)
  // XXX: public final Flowable window(Publisher,int)
  // XXX: public final Flowable withLatestFrom(Iterable,Function)
  // XXX: public final Flowable withLatestFrom(Publisher,BiFunction)
  // XXX: public final Flowable withLatestFrom(Publisher[],Function)
  // XXX: public final Flowable withLatestFrom(Publisher,Publisher,Function3)
  // XXX: public final Flowable withLatestFrom(Publisher,Publisher,Publisher,Function4)
  // XXX: public final Flowable withLatestFrom(Publisher,Publisher,Publisher,Publisher,Function5)
  // XXX: public final Flowable zipWith(Iterable,BiFunction)
  // XXX: public final Flowable zipWith(Publisher,BiFunction)
  // XXX: public final Flowable zipWith(Publisher,BiFunction,boolean)
  // XXX: public final Flowable zipWith(Publisher,BiFunction,boolean,int)
  // XXX: public final TestSubscriber test()
  // XXX: public final TestSubscriber test(long)
  // XXX: public final TestSubscriber test(long,boolean)

}
