@FunctionalInterface
interface CustomRejectedExecutionHandler {
    void rejectedExecution(Runnable r, CustomThreadPool executor);
}