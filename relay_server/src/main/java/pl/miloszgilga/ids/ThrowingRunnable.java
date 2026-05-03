package pl.miloszgilga.ids;

@FunctionalInterface
public interface ThrowingRunnable {
    void run() throws Exception;
}
