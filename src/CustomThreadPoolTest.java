import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class CustomThreadPoolTest {

    @Test
    void testTaskExecution() throws InterruptedException {
        CustomThreadPool threadPool = new CustomThreadPool(3, 6, 10, 10, 1);

        final int[] taskCounter = {0};
        CountDownLatch latch = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            int taskId = i + 1;
            threadPool.execute(() -> {
                System.out.println("[Task-" + taskId + "] Начало выполнения");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                synchronized (taskCounter) {
                    taskCounter[0]++;
                }
                System.out.println("[Task-" + taskId + "] Завершено");
                latch.countDown();
            });
        }

        boolean allTasksCompleted = latch.await(15, TimeUnit.SECONDS);

        assertTrue(allTasksCompleted, "Not all tasks completed within the timeout");
        assertEquals(10, taskCounter[0], "All tasks should be executed");
        threadPool.shutdown();
    }

    @Test
    void testRejectionPolicy() throws InterruptedException {
        CustomThreadPool threadPool = new CustomThreadPool(1, 1, 1, 1, 1);

        final int[] rejectedCounter = {0};

        CustomRejectedExecutionHandler rejectedHandler = (r, executor) -> {
            synchronized (rejectedCounter) {
                rejectedCounter[0]++;
            }
            System.out.println("[Rejected] Task was rejected due to overload!");
        };

        threadPool = new CustomThreadPool(1, 1, 1, 1, 1) {
            @Override
            public void execute(Runnable command) {
                if (isShutdown()) throw new RejectedExecutionException("Executor is shutting down");
                if (!getTaskQueue().offer(command)) {
                    rejectedHandler.rejectedExecution(command, this);
                } else {
                    ensureWorkerThreads();
                }
            }
        };

        for (int i = 0; i < 10; i++) {
            threadPool.execute(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
            });
        }

        Thread.sleep(5000);

        assertTrue(rejectedCounter[0] > 0, "Some tasks should be rejected");
        threadPool.shutdown();
    }
}