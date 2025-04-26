public class Main {
    public static void main(String[] args) throws InterruptedException {
        // Передаём 5-й аргумент minSpareThreads
        CustomThreadPool threadPool = new CustomThreadPool(2, 4, 5, 5, 1);

        for (int i = 1; i <= 10; i++) {
            int taskId = i;
            threadPool.execute(() -> {
                System.out.println("[Task-" + taskId + "] Начало выполнения");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("[Task-" + taskId + "] Завершение выполнения");
            });
        }

        Thread.sleep(10000);

        System.out.println("[Main] Завершаем работу пула");
        threadPool.shutdown();

        Thread.sleep(3000);
        System.out.println("[Main] Все задачи завершены, программа завершена");
    }
}