import java.util.concurrent.Semaphore;
public class ResourceManager {

    private final Semaphore printer;
    private final Semaphore database;

    public ResourceManager(int printerPermits,
                           int databasePermits) {

        printer = new Semaphore(printerPermits, true);
        database = new Semaphore(databasePermits, true);
    }

    public Semaphore getSemaphore(Job.ResourceType resource) {

        switch (resource) {

            case PRINTER:
                return printer;

            case DATABASE:
                return database;

            default:
                return null;
        }
    }

    public int getPrinterAvailable() {
        return printer.availablePermits();
    }

    public int getDatabaseAvailable() {
        return database.availablePermits();
    }
}