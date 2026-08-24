package com.example.springproxy;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/** Tracks how many times the legacy REST petstore was actually called. */
@Component
public class LegacyCallCounter {

    private final AtomicInteger count = new AtomicInteger();

    public int increment() {
        return count.incrementAndGet();
    }

    public int getCount() {
        return count.get();
    }
}
