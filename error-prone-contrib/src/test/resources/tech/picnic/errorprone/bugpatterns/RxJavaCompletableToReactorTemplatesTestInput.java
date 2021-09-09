package tech.picnic.errorprone.bugpatterns;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import java.util.Arrays;

final class RxJavaCompletableReactorTemplatesTest implements RefasterTemplateTestCase {

  Completable testCompletableAmb() {
    return Completable.amb(Arrays.asList(Completable.complete(), Completable.complete()));
  }

  Completable testCompletableDefer() {
    return Completable.defer(() -> Completable.complete());
  }

  Completable testCompletableErrorThrowable() {
    return Completable.error(new IllegalStateException());
  }

  Completable testCompletableErrorCallable() {
    return Completable.error(
        () -> {
          throw new IllegalStateException();
        });
  }

  Completable testCompletableFromAction() {
    return Completable.fromAction(() -> { });
  }
}
