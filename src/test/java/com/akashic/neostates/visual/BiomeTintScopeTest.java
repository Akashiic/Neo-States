package com.akashic.neostates.visual;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

class BiomeTintScopeTest {

    @Test
    void closedByDefault() {
        assertFalse(BiomeTintScope.active());
    }

    @Test
    void openBetweenBeginAndEnd() {
        BiomeTintScope.begin();
        try {
            assertTrue(BiomeTintScope.active());
        } finally {
            BiomeTintScope.end();
        }
        assertFalse(BiomeTintScope.active());
    }

    @Test
    void nestingKeepsItOpenUntilTheOutermostEnd() {
        BiomeTintScope.begin();
        BiomeTintScope.begin();
        BiomeTintScope.end();
        assertTrue(BiomeTintScope.active());
        BiomeTintScope.end();
        assertFalse(BiomeTintScope.active());
    }

    @Test
    void aThrowInsideTheWindowStillClosesIt() {
        try {
            BiomeTintScope.begin();
            throw new IllegalStateException("the renderer threw");
        } catch (IllegalStateException expected) {
            // the renderer failing is the point
        } finally {
            BiomeTintScope.end();
        }
        assertFalse(BiomeTintScope.active());
    }

    @Test
    void anEndWithoutABeginIsIgnored() {
        BiomeTintScope.end();
        assertFalse(BiomeTintScope.active());
    }

    @Test
    void anotherThreadHasItsOwnWindow() throws InterruptedException {
        BiomeTintScope.begin();
        try {
            AtomicBoolean seen = new AtomicBoolean(true);
            Thread worker = new Thread(() -> seen.set(BiomeTintScope.active()), "tint-scope-test");
            worker.start();
            worker.join();
            assertFalse(seen.get());
            assertTrue(BiomeTintScope.active());
        } finally {
            BiomeTintScope.end();
        }
    }
}
