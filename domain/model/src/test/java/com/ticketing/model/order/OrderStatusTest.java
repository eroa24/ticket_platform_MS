package com.ticketing.model.order;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class OrderStatusTest {

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"CONFIRMED", "REJECTED", "EXPIRED", "COMPLIMENTARY"})
    void isTerminal_returnsTrueForTerminalStates(OrderStatus status) {
        assertThat(status.isTerminal()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"PENDING", "PROCESSING"})
    void isTerminal_returnsFalseForNonTerminalStates(OrderStatus status) {
        assertThat(status.isTerminal()).isFalse();
    }
}
