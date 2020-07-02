package in.projecteka.gateway;

import lombok.AllArgsConstructor;
import org.reactivestreams.Subscription;
import org.slf4j.MDC;
import reactor.core.CoreSubscriber;
import reactor.util.context.Context;

import java.util.stream.Collectors;

@AllArgsConstructor
public class MdcContextLifter implements CoreSubscriber {
    private final CoreSubscriber coreSubscriber;


    @Override
    public Context currentContext() {
        return this.coreSubscriber.currentContext();
    }

    @Override
    public void onSubscribe(Subscription s) {
        this.coreSubscriber.onSubscribe(s);
    }

    @Override
    public void onNext(Object o) {
        this.copyToMdc();
        this.coreSubscriber.onNext(o);
    }

    @Override
    public void onError(Throwable t) {
        this.coreSubscriber.onError(t);
    }

    @Override
    public void onComplete() {
        this.coreSubscriber.onComplete();
    }

    private void copyToMdc() {
        Context currentContext = this.coreSubscriber.currentContext();
        if (!currentContext.isEmpty()) {
            var map = currentContext.stream()
                    .collect(Collectors.toMap(
                            context -> context.getKey().toString()
                            , context -> context.getValue().toString()
            ));
            MDC.setContextMap(map);
        }
        else MDC.clear();
    }
}
