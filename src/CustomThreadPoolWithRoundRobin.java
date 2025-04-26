import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomThreadPoolWithRoundRobin extends CustomThreadPool {
    private final BlockingQueue<Runnable>[] taskQueues;
    private final AtomicInteger roundRobinIndex = new AtomicInteger(0);
    private CustomRejectedExecutionHandler rejectedHandler;

    @SuppressWarnings("unchecked")
    public CustomThreadPoolWithRoundRobin(int corePoolSize, int maxPoolSize, long keepAliveTimeSeconds, int queueSize, int minSpareThreads) {
        super(corePoolSize, maxPoolSize, keepAliveTimeSeconds, queueSize, minSpareThreads);
        this.taskQueues = new ArrayBlockingQueue[corePoolSize];

        for (int i = 0; i < corePoolSize; i++) {
            taskQueues[i] = new ArrayBlockingQueue<>(queueSize);
        }
    }

    @Override
    public void execute(Runnable command) {
        int index = roundRobinIndex.getAndIncrement() % taskQueues.length;
        BlockingQueue<Runnable> targetQueue = taskQueues[index];

        if (!targetQueue.offer(command)) {
            getRejectedHandler().rejectedExecution(command, this);
        } else {
            ensureWorkerThreads();
        }
    }

    public CustomRejectedExecutionHandler getRejectedHandler() {
        return rejectedHandler;
    }
}