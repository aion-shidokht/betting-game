package internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.function.Predicate;

public final class RetryExecutor<R> {
    private final int maxAttempt;
    private final Predicate<R> failedPredicate;
    private final long sleepTimeMillis;
    private final Logger logger = LoggerFactory.getLogger(RetryExecutor.class);


    public RetryExecutor(Predicate<R> failedPredicate,
                         int maxAttempt,
                         long sleepTimeMillis) {

        this.failedPredicate = failedPredicate;
        this.maxAttempt = maxAttempt;
        this.sleepTimeMillis = sleepTimeMillis;
    }


    public R execute(Callable<R> callable) {
        R result = null;
        for (int attemptNumber = 1; attemptNumber <= maxAttempt; attemptNumber++) {
            try {
                result = callable.call();
                if (!failedPredicate.test(result)) {
                    return result;
                }
                //will retry if exception is thrown
            } catch (Throwable t) {
                logger.error("Error occurred during retry." + callable.getClass().getName());
            }

            try {
                Thread.sleep(sleepTimeMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return result;
    }
}