
import java.util.concurrent.atomic.AtomicInteger;

public class Monitor extends Thread {

    private final Scheduler scheduler;
    private final Statistics statistics;
    private final ResourceManager resourceManager;
    private final AtomicInteger runningJobs;

    private volatile boolean running = true;

    public Monitor(
            Scheduler scheduler,
            Statistics statistics,
            ResourceManager resourceManager,
            AtomicInteger runningJobs) {

        super("Monitor");

        this.scheduler = scheduler;
        this.statistics = statistics;
        this.resourceManager = resourceManager;
        this.runningJobs = runningJobs;
    }

    @Override
    public void run() {

        try {

            while (running) {

                // อ่านข้อมูลสถานะระบบ
                int ready = scheduler.readySize();

                int runningCount = runningJobs.get();

                int completed =
                        statistics.getCompletedCount();

                // อ่านจำนวน Semaphore Permits
                int printerAvailable =
                        resourceManager.getPrinterAvailable();

                int databaseAvailable =
                        resourceManager.getDatabaseAvailable();

                // รวมข้อความ Monitor เป็น String เดียว
                String report = String.format(
                        "%n========== MONITOR ==========%n"
                        + "READY     : %d%n"
                        + "RUNNING   : %d%n"
                        + "COMPLETED : %d%n"
                        + "PRINTER AVAILABLE : %d%n"
                        + "DATABASE AVAILABLE : %d%n"
                        + "=============================%n",
                        ready,
                        runningCount,
                        completed,
                        printerAvailable,
                        databaseAvailable
                );

                // พิมพ์ข้อความครั้งเดียว
                System.out.print(report);

                // แสดงสถานะทุกประมาณ 1 วินาที
                Thread.sleep(1000);
            }

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
        }
    }

    public void shutdown() {

        running = false;
        interrupt();
    }
}
