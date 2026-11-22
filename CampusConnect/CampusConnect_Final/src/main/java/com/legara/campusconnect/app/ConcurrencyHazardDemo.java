package com.legara.campusconnect.app;

public class ConcurrencyHazardDemo {

    static class UnsafeCounter {
        private int processed;

        void increment() {
            processed++;
        }

        int get() {
            return processed;
        }
    }

    static class SafeCounter {
        private int processed;

        synchronized void increment() {
            processed++;
        }

        synchronized int get() {
            return processed;
        }
    }

    public static int countUnsafe(int threads, int incrementsPerThread) throws InterruptedException {
        UnsafeCounter counter = new UnsafeCounter();
        runConcurrently(threads, incrementsPerThread, counter::increment);
        return counter.get();
    }

    public static int countSynchronized(int threads, int incrementsPerThread) throws InterruptedException {
        SafeCounter counter = new SafeCounter();
        runConcurrently(threads, incrementsPerThread, counter::increment);
        return counter.get();
    }

    private static void runConcurrently(int threads, int incrementsPerThread, Runnable increment)
            throws InterruptedException {
        Thread[] workers = new Thread[threads];
        for (int t = 0; t < threads; t++) {
            workers[t] = new Thread(() -> {
                for (int i = 0; i < incrementsPerThread; i++) {
                    increment.run();
                }
            });
            workers[t].start();
        }
        for (Thread worker : workers) {
            worker.join();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int threads = 4;
        int increments = 250_000;
        System.out.println("Shared 'requests processed' counter, " + threads + " threads x " + increments + " increments");
        System.out.println("Expected total        : " + threads * increments);
        for (int run = 1; run <= 3; run++) {
            System.out.println("Unsafe int (run " + run + ")  : " + countUnsafe(threads, increments));
        }
        System.out.println("synchronized methods  : " + countSynchronized(threads, increments));
    }
}
