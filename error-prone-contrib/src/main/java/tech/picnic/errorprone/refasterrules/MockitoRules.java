package tech.picnic.errorprone.refasterrules;

import static com.google.errorprone.refaster.ImportPolicy.STATIC_IMPORT_ALWAYS;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import com.google.errorprone.refaster.annotation.UseImportPolicy;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;
import tech.picnic.errorprone.refaster.annotation.OnlineDocumentation;

/** Refaster rules related to Mockito expressions and statements. */
@OnlineDocumentation
final class MockitoRules {
  private MockitoRules() {}

  /**
   * Prefer {@link Mockito#never()}} over explicitly specifying that the associated invocation must
   * happen precisely zero times.
   */
  static final class Never {
    @BeforeTemplate
    VerificationMode before() {
      return times(0);
    }

    @AfterTemplate
    @UseImportPolicy(STATIC_IMPORT_ALWAYS)
    VerificationMode after() {
      return never();
    }
  }

  /**
   * Prefer {@link Mockito#verify(Object)} over explicitly specifying that the associated invocation
   * must happen precisely once; this is the default behavior.
   */
  static final class VerifyOnce<T> {
    @BeforeTemplate
    T before(T mock) {
      return verify(mock, times(1));
    }

    @AfterTemplate
    @UseImportPolicy(STATIC_IMPORT_ALWAYS)
    T after(T mock) {
      return verify(mock);
    }
  }
}
