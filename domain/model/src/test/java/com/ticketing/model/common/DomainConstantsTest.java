package com.ticketing.model.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class DomainConstantsTest {

    @Test
    void constantsAreDefined() {
        assertThat(DomainConstants.MAX_TICKETS_PER_PURCHASE).isEqualTo(10);
        assertThat(DomainConstants.RESERVATION_TIMEOUT_MINUTES).isEqualTo(10L);
    }

    @Test
    void constructorIsPrivate() throws Exception {
        var constructor = DomainConstants.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
        constructor.setAccessible(true);
        constructor.newInstance();
    }
}
