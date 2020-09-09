package com.atex.plugins.mailimporter;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;

/**
 * AbstractProcessorTest
 *
 * @author mnova
 */
public abstract class AbstractProcessorTest extends CamelTestSupport {

    protected <T> MockEndpoint setupMockEndpoint(final int count,
                                                 final Class<T> bodyClass) {
        final MockEndpoint mock = getMockEndpoint("mock:result");
        mock.setExpectedMessageCount(count);
        for (int idx = 0; idx < count; idx++) {
            mock.message(idx)
                .predicate()
                .body(bodyClass)
                .expression(new Expression() {
                    @Override
                    public <T> T evaluate(final Exchange e, final Class<T> type) {
                        return e.getOut().getBody(type);
                    }
                }).isNotNull();
        }
        return mock;
    }

}