package com.legara.bookbridge.app;

public class ConcurrencyDemo {
    private static final int THREADS = 4;
    private static final int LOANS_PER_THREAD = 250_000;

    static class UnsafeLoanCounter {
        private int processed;

        void recordLoan() {
            processed++;
        }

        int total() {
            return processed;
        }
    }

    static class SafeLoanCounter {
        private int processed;

        synchronized void recordLoan() {
            processed++;
        }

        synchronized int total() {
            return processed;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int expected = THREADS * LOANS_PER_THREAD;
        System.out.println("Summary count of processed loans, " + THREADS + " threads x " + LOANS_PER_THREAD
                + " = " + expected + " expected");
        for (int run = 1; run <= 5; run++) {
            System.out.println("Run " + run + ": unsafe = " + countUnsafe() + ", synchronized = " + countSafe());
        }
    }

    public static int countUnsafe() throws InterruptedException {
        UnsafeLoanCounter counter = new UnsafeLoanCounter();
        runThreads(counter::recordLoan);
        return counter.total();
    }

    public static int countSafe() throws InterruptedException {
        SafeLoanCounter counter = new SafeLoanCounter();
        runThreads(counter::recordLoan);
        return counter.total();
    }

    private static void runThreads(Runnable recordOneLoan) throws InterruptedException {
        Thread[] workers = new Thread[THREADS];
        for (int i = 0; i < THREADS; i++) {
            workers[i] = new Thread(() -> {
                for (int n = 0; n < LOANS_PER_THREAD; n++) {
                    recordOneLoan.run();
                }
            });
            workers[i].start();
        }
        for (Thread worker : workers) {
            worker.join();
        }
    }

    public static int expectedTotal() {
        return THREADS * LOANS_PER_THREAD;
    }
}
