package io.muoncore.internal;

import io.muoncore.MuonFuture;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ImmediateReturnFuture<X> implements MuonFuture<X> {

    private X theReturn;

    public ImmediateReturnFuture(X val) {
        this.theReturn = val;
    }

    @Override
    public Publisher<X> toPublisher() {
        return new ImmediatePublisher();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public X get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return get();
    }

    @Override
    public X get() {
        return theReturn;
    }

    class ImmediatePublisher implements Publisher<X> {
        @Override
        public void subscribe(Subscriber<? super X> s) {
            s.onSubscribe(new DefaultPromiseSubscription(s));
        }

        class DefaultPromiseSubscription implements Subscription {

            private Subscriber<? super X> sub;

            public DefaultPromiseSubscription(Subscriber<? super X> sub) {
                this.sub = sub;
            }

            @Override
            public void request(long n) {
                sub.onNext(theReturn);
                sub.onComplete();
            }

            @Override
            public void cancel() {
                //noop
            }
        }
    }
}