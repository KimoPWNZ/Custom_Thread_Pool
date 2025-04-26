import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomThreadPool implements Executor {
    private final int corePoolSize;
    private final int maxPoolSize;
    private final long keepAliveTimeMillis;
    private final int queueSize;
    private final int minSpareThreads;
    private final BlockingQueue<Runnable> taskQueue;
    private final ThreadFactory threadFactory;
    private final CustomRejectedExecutionHandler rejectedHandler;
    private final ConcurrentLinkedQueue<Worker> workers = new ConcurrentLinkedQueue<>();
    private final AtomicInteger activeThreads = new AtomicInteger();
    private volatile boolean isShutdown = false;

    public CustomThreadPool(int corePoolSize, int maxPoolSize, long keepAliveTimeSeconds, int queueSize, int minSpareThreads) {
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.keepAliveTimeMillis = TimeUnit.SECONDS.toMillis(keepAliveTimeSeconds);
        this.queueSize = queueSize;
        this.minSpareThreads = minSpareThreads;
        this.taskQueue = new ArrayBlockingQueue<>(queueSize);
        this.threadFactory = new CustomThreadFactory();
        this.rejectedHandler = (runnable, executor) -> System.out.println("[Rejected] Task was rejected due to overload!");
    }

    @Override
    public void execute(Runnable command) {
        if (isShutdown) throw new RejectedExecutionException("Executor is shutting down");
        if (!taskQueue.offer(command)) {
            rejectedHandler.rejectedExecution(command, this);
        } else {
            ensureWorkerThreads();
        }
    }

    protected BlockingQueue<Runnable> getTaskQueue() {
        return taskQueue;
    }

    protected void ensureWorkerThreads() {
        if (activeThreads.get() < Math.max(corePoolSize, minSpareThreads)) {
            createWorker();
        }
    }

    private void createWorker() {
        Worker worker = new Worker();
        Thread thread = threadFactory.newThread(worker);
        workers.add(worker);
        thread.start();
        activeThreads.incrementAndGet();
    }

    public void shutdown() {
        isShutdown = true;
        workers.forEach(Worker::shutdown);
    }

    public void shutdownNow() {
        isShutdown = true;
        workers.forEach(Worker::shutdownNow);
    }

    public boolean isShutdown() {
        return isShutdown;
    }

    private class Worker implements Runnable {
        private volatile boolean running = true;

        @Override
        public void run() {
            while (running) {
                try {
                    Runnable task = taskQueue.poll(keepAliveTimeMillis, TimeUnit.MILLISECONDS);
                    if (task != null) {
                        task.run();
                    } else if (activeThreads.get() > corePoolSize) {
                        running = false;
                        break;
                    }
                } catch (InterruptedException ignored) {
                }
            }
            activeThreads.decrementAndGet();
        }

        public void shutdown() {
            running = false;
        }

        public void shutdownNow() {
            running = false;
            Thread.currentThread().interrupt();
        }
    }

    private class CustomThreadFactory implements ThreadFactory {
        private final AtomicInteger threadCount = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            String threadName = "MyPool-worker-" + threadCount.incrementAndGet();
            System.out.println("[ThreadFactory] Creating new thread: " + threadName);
            return new Thread(r, threadName);
        }
    }
}