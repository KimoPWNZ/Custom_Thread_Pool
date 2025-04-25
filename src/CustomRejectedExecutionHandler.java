@FunctionalInterface
public interface CustomRejectedExecutionHandler {
    void rejectedExecution(Runnable r, CustomThreadPool executor);
}