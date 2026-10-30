
import java.util.concurrent.BlockingQueue;

public class Scheduler extends Thread {

    private final BlockingQueue<Job> arrivalQueue;
    private final ReadyQueue readyQueue;
    private final int workerCount;

    public Scheduler(
            BlockingQueue<Job> arrivalQueue,
            ReadyQueue readyQueue,
            int workerCount) {

        this.arrivalQueue = arrivalQueue;
        this.readyQueue = readyQueue;
        this.workerCount = workerCount;

        setName("Scheduler");
    }

    @Override
    public void run() {

        try {

            while (true) {

                // รับ Job จาก Arrival Queue
                Job job = arrivalQueue.take();

                // JobGenerator ส่งครบแล้ว
                if (job == Job.POISON_PILL) {
                    break;
                }

                // เปลี่ยนสถานะเป็น READY
                job.setState(Job.State.READY);

                // ส่ง Job เข้า ReadyQueue
                readyQueue.addJob(job);

                System.out.println(
                    "[Scheduler] "
                    + job.getId()
                    + " READY"
                );
            }

            // ส่ง Poison Pill ให้ Worker ทุกตัว
            for (int i = 0; i < workerCount; i++) {
                readyQueue.addJob(Job.POISON_PILL);
            }

        
            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                throw new IllegalStateException(
                    "Scheduler interrupted before dispatch completed",
                    e
                );
            }
    }

    // Worker เรียกเพื่อรับ Job
    public Job takeNextJob()
            throws InterruptedException {

        return readyQueue.takeJob();
    }

    // Monitor เรียกดูจำนวน Job ที่รอ
    public int readySize() {
        return readyQueue.size();
    }
}
