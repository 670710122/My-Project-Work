
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Main {

    public static void main(String[] args) throws Exception {

        // =====================================================
        // 1. ตรวจ Command Line Arguments
        // =====================================================

        if (args.length != 5) {
            printUsage();
            return;
        }

        String workloadFile = args[0];
        String policyName = args[1].toLowerCase();

        int workerCount;
        int printerPermits;
        int databasePermits;

        try {
            workerCount = Integer.parseInt(args[2]);
            printerPermits = Integer.parseInt(args[3]);
            databasePermits = Integer.parseInt(args[4]);

        } catch (NumberFormatException e) {
            printUsage();
            return;
        }

        if (workerCount <= 0
                || printerPermits <= 0
                || databasePermits <= 0) {

            printUsage();
            return;
        }

        // =====================================================
        // 2. Load Workload
        // =====================================================

        List<Job> jobs =
                WorkloadLoader.load(workloadFile);

        // =====================================================
        // 3. Shared Objects
        // =====================================================

        BlockingQueue<Job> arrivalQueue =
                new LinkedBlockingQueue<>();

        if (!policyName.equals("fcfs")
                && !policyName.equals("priority")) {

            printUsage();
            return;
        }

        ReadyQueue readyQueue =
        new ReadyQueue(policyName);

        // =====================================================
        // 4. สร้าง Resource / Statistics
        // =====================================================

        ResourceManager resourceManager =
                new ResourceManager(
                        printerPermits,
                        databasePermits
                );

        Statistics statistics =
                new Statistics();

        AtomicInteger runningJobs =
                new AtomicInteger(0);

        // =====================================================
        // 5. เวลาเริ่ม Simulation
        // =====================================================

        long simulationStart =
                System.currentTimeMillis();

        // =====================================================
        // 6. สร้าง Scheduler
        // =====================================================

        Scheduler scheduler =
                new Scheduler(
                        arrivalQueue,
                        readyQueue,
                        workerCount
                );

        // =====================================================
        // 7. สร้าง JobGenerator
        // =====================================================

        JobGenerator generator =
                new JobGenerator(
                        jobs,
                        arrivalQueue,
                        simulationStart
                );

        // =====================================================
        // 8. สร้าง Monitor
        // =====================================================

        Monitor monitor =
        new Monitor(
                scheduler,
                statistics,
                resourceManager,
                runningJobs
        );

        // =====================================================
        // 9. สร้าง Worker Threads
        // =====================================================

        Worker[] workers =
                new Worker[workerCount];

        for (int i = 0; i < workerCount; i++) {

            workers[i] = new Worker(
                "Worker-" + (i + 1),
                scheduler,
                resourceManager,
                statistics,
                runningJobs
        );
                
        }

        // =====================================================
        // 10. Thread Error Handler
        // =====================================================

        // เก็บข้อผิดพลาดตัวแรกที่เกิดขึ้น
        AtomicReference<Throwable> failure =
                new AtomicReference<>();

        // Main Thread ที่ควบคุม Simulation
        Thread mainThread =
                Thread.currentThread();

        // เมื่อ Thread ใดเกิด Uncaught Exception
        Thread.UncaughtExceptionHandler handler =
                (thread, error) -> {

                    // บันทึก Error
                    failure.compareAndSet(null, error);

                    System.err.println(
                            "[ERROR] "
                            + thread.getName()
                            + ": "
                            + error.getMessage()
                    );

                    // แจ้ง Main ให้เข้าสู่ Emergency Shutdown
                    mainThread.interrupt();
                };

        // กำหนด Error Handler ให้ Thread ทุกตัว
        generator.setUncaughtExceptionHandler(handler);
        scheduler.setUncaughtExceptionHandler(handler);
        monitor.setUncaughtExceptionHandler(handler);

        for (Worker worker : workers) {
            worker.setUncaughtExceptionHandler(handler);
        }

        // =====================================================
        // 11. Start Simulation / Graceful Shutdown
        // =====================================================

        boolean normalShutdown = false;

        boolean interrupted = false;

        try {

            System.out.println(
                    "===== SIMULATION START ====="
            );

            // เริ่ม Thread ทุกตัว
            scheduler.start();
            generator.start();
            monitor.start();

            for (Worker worker : workers) {
                worker.start();
            }

            // ---------------------------------------------
            // Graceful Shutdown
            // ---------------------------------------------

            // รอ JobGenerator ส่ง Job ครบ
            generator.join();

            // รอ Scheduler ส่ง Poison Pill ให้ Worker
            scheduler.join();

            // รอ Worker ทุกตัวทำงานจนเสร็จ
            for (Worker worker : workers) {
                worker.join();
            }

            // ตรวจว่ามี Thread ใดเกิดข้อผิดพลาดหรือไม่
            if (failure.get() != null) {

                throw new IllegalStateException(
                        "A simulation thread failed",
                        failure.get()
                );
            }

            // ตรวจว่าทุก Job ทำงานเสร็จจริง
            if (statistics.getCompletedCount()
                    != jobs.size()) {

                throw new IllegalStateException(
                        "Completed jobs do not match workload"
                );
            }

            normalShutdown = true;

        } catch (InterruptedException e) {

            // Main ถูก Interrupt
            interrupted = true;

            failure.compareAndSet(null, e);

        } catch (RuntimeException e) {

            failure.compareAndSet(null, e);

        } finally {

            // ---------------------------------------------
            // Emergency Shutdown
            // ---------------------------------------------

            if (!normalShutdown) {

                System.err.println(
                        "[SYSTEM] Emergency Shutdown"
                );

                // หยุด JobGenerator
                generator.interrupt();

                // หยุด Scheduler
                scheduler.interrupt();

                // หยุด Worker ทุกตัว
                for (Worker worker : workers) {
                    worker.interrupt();
                }
            }

            // ปิด Monitor ในทุกกรณี
            monitor.shutdown();

            // รอให้ทุก Thread สิ้นสุดจริง
            interrupted |= awaitTermination(generator);
            interrupted |= awaitTermination(scheduler);

            for (Worker worker : workers) {
                interrupted |= awaitTermination(worker);
            }

            interrupted |= awaitTermination(monitor);
        }

        // คืนสถานะ Interrupt ให้ Main
        if (interrupted) {
            Thread.currentThread().interrupt();
        }

        // หาก Shutdown ผิดปกติ ไม่แสดง Summary สำเร็จ
        if (!normalShutdown) {

            throw new IllegalStateException(
                    "Simulation terminated before completion",
                    failure.get()
            );
        }

        // =====================================================
        // 12. End Simulation
        // =====================================================

        long simulationEnd =
                System.currentTimeMillis();

        long totalTime =
                simulationEnd - simulationStart;

        // =====================================================
        // 13. Throughput
        // =====================================================

        double throughput =
                totalTime == 0 ? 0 : (statistics.getCompletedCount() * 1000.0) / totalTime;

        // =====================================================
        // 14. Summary
        // =====================================================

        System.out.println();

        System.out.println(
                "========== SUMMARY =========="
        );

        System.out.printf(
                "Completed Jobs       : %d%n",
                statistics.getCompletedCount()
        );

        System.out.printf(
                "Average Waiting Time : %.0f ms%n",
                statistics.getAverageWaitingTime()
        );

        System.out.printf(
                "Average TAT          : %.0f ms%n",
                statistics.getAverageTurnaroundTime()
        );

        System.out.printf(
                "Average Resource Wait: %.0f ms%n",
                statistics.getAverageResourceWaitTime()
        );

        System.out.printf(
                "Throughput           : %.2f jobs/sec%n",
                throughput
        );

        System.out.printf(
                "Simulation Time      : %d ms%n",
                totalTime
        );

        System.out.println(
                "============================="
        );
    }

    // =====================================================
    // 15. รอให้ Thread หยุดจริง
    // =====================================================

    private static boolean awaitTermination(Thread thread) {

        boolean interrupted = false;

        while (thread.isAlive()) {

            try {
                thread.join();

            } catch (InterruptedException e) {
                interrupted = true;
            }
        }

        return interrupted;
    }

    // =====================================================
    // 16. Usage
    // =====================================================

    private static void printUsage() {

        System.out.println(
                "Usage: java Main "
                + "<workload.csv> "
                + "<fcfs|priority> "
                + "<workers> "
                + "<printerPermits> "
                + "<databasePermits>"
        );
    }
}
