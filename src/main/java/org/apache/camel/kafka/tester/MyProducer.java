package org.apache.camel.kafka.tester;

import java.util.concurrent.atomic.LongAdder;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.aggregate.GroupedExchangeAggregationStrategy;


/**
 * A Camel Java DSL Router
 */
public class MyProducer extends RouteBuilder {
    private final LongAdder longAdder;
    private final boolean aggregate;
    private final int batchSize;

    public MyProducer(LongAdder longAdder, boolean aggregate, int batchSize) {
        this.longAdder = longAdder;
        this.aggregate = aggregate;
        this.batchSize = batchSize;
    }

    /**
     * Let's configure the Camel routing rules using Java code...
     */
    public void configure() {

        if (!aggregate) {
            from("dataset:testSet?produceDelay=0&minRate={{?min.rate}}&initialDelay={{initial.delay:2000}}")
                    .to("kafka:test")
                    .process(exchange -> longAdder.increment());
        } else {
            from("dataset:testSet?produceDelay=0&initialDelay={{initial.delay:2000}}&minRate={{?min.rate}}&preloadSize={{?preload.size}}")
                    .aggregate(constant(true), new GroupedExchangeAggregationStrategy())
                    .completionSize(batchSize)
                    .to("kafka:test")
                    .process(exchange -> longAdder.add(batchSize));
        }
    }

}
