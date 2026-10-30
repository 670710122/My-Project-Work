
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ReadyQueue {

    private final BlockingQueue<Job> queue;

    // นับเฉพาะ Job จริง ไม่รวม POISON_PILL
    private final AtomicInteger readyJobs =
            new AtomicInteger(0);

    public ReadyQueue(String policy) {

        if (policy.equalsIgnoreCase("fcfs")) {

            // FCFS: Job ที่เข้าคิวก่อนถูกเลือกก่อน
            queue = new LinkedBlockingQueue<>();

        } else if (policy.equalsIgnoreCase("priority")) {

            queue = new PriorityBlockingQueue<>(
                11,
                (a, b) -> {

                    // POISON_PILL ต้องอยู่หลัง Job จริง
                    if (a == Job.POISON_PILL) {
                        return b == Job.POISON_PILL ? 0 : 1;
                    }

                    if (b == Job.POISON_PILL) {
                        return -1;
                    }

                    // Priority เลขน้อยสำคัญกว่า
                    int result = Integer.compare(
                        a.getPriority(),
                        b.getPriority()
                    );

                    if (result != 0) {
                        return result;
                    }

                    // Tie-break ด้วย Arrival Time
                    result = Long.compare(
                        a.getArrivalMs(),
                        b.getArrivalMs()
                    );

                    if (result != 0) {
                        return result;
                    }

                    // ถ้า Arrival Time เท่ากัน ใช้ ID
                    return a.getId().compareTo(b.getId());
                }
            );

        } else {
            throw new IllegalArgumentException(
                "Invalid Scheduling Policy: " + policy
            );
        }
    }

    // เพิ่ม Job เข้า Ready Queue
    public void addJob(Job job) {

        if (job != Job.POISON_PILL) {
            readyJobs.incrementAndGet();
        }

        queue.add(job);
    }

    // Worker ดึง Job
    public Job takeJob() throws InterruptedException {

        Job job = queue.take();

        if (job != Job.POISON_PILL) {
            readyJobs.decrementAndGet();
        }

        return job;
    }

    // จำนวน Job จริงที่ยังรอใน Queue
    public int size() {
        return readyJobs.get();
    }
}
