import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class Worker extends Thread {

    private final Scheduler scheduler;
    private final ResourceManager resourceManager;
    private final Statistics statistics;
    private final AtomicInteger runningJobs;

    public Worker(
            String name,
            Scheduler scheduler,
            ResourceManager resourceManager,
            Statistics statistics,
            AtomicInteger runningJobs) {

        super(name);

        this.scheduler = scheduler;
        this.resourceManager = resourceManager;
        this.statistics = statistics;
        this.runningJobs = runningJobs;
    }

    @Override
    public void run() {

        try {

            while (true) {

                // ถ้าไม่มีงาน จะ BLOCK ตรงนี้
                Job job = scheduler.takeNextJob();

                // ได้สัญญาณปิด
                if (job == Job.POISON_PILL) {
                    break;
                }

                processJob(job);
            }

        
            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                throw new IllegalStateException(
                    getName() + " interrupted before normal shutdown",
                    e
                );
            }
    }

    private void processJob(Job job)
            throws InterruptedException {

        runningJobs.incrementAndGet();

        try {

            job.setState(Job.State.RUNNING);

            job.setStartTime(
                    System.currentTimeMillis()
            );

            System.out.printf(
                "[%s] %s START%n",
                getName(),
                job.getId()
            );

            // -----------------
            // Main work
            // -----------------

            Thread.sleep(job.getWorkMs());

            // -----------------
            // Shared Resource
            // -----------------

            if (job.getResource()
                    != Job.ResourceType.NONE) {

                useResource(job);
            }

            // -----------------
            // Complete
            // -----------------

            job.setCompletionTime(
                    System.currentTimeMillis()
            );

            job.setState(Job.State.COMPLETED);

            statistics.addCompletedJob(job);

            System.out.printf(
                "[%s] %s COMPLETE%n",
                getName(),
                job.getId()
            );

        } finally {

            runningJobs.decrementAndGet();
        }
    }

    private void useResource(Job job)
            throws InterruptedException {

        Semaphore semaphore =
            resourceManager.getSemaphore(
                job.getResource()
            );

        job.setState(
                Job.State.WAITING_RESOURCE
        );

        long waitStart =
                System.currentTimeMillis();

        System.out.printf(
            "[%s] %s WAIT %s%n",
            getName(),
            job.getId(),
            job.getResource()
        );

        /*
         * สำคัญ:
         * acquired จะเป็น true
         * เฉพาะ acquire สำเร็จ
         */
        boolean acquired = false;

        try {

            semaphore.acquire();

            acquired = true;

            long waitEnd =
                    System.currentTimeMillis();

            job.setResourceWaitTime(
                    waitEnd - waitStart
            );

            System.out.printf(
                "[%s] %s ACQUIRE %s%n",
                getName(),
                job.getId(),
                job.getResource()
            );

            Thread.sleep(
                    job.getResourceMs()
            );

        } finally {

            /*
             * release เฉพาะกรณี
             * acquire สำเร็จแล้ว
             */
            if (acquired) {

                semaphore.release();

                System.out.printf(
                    "[%s] %s RELEASE %s%n",
                    getName(),
                    job.getId(),
                    job.getResource()
                );
            }
        }
    }
}