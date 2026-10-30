//Thread ที่จำลองการเกิด/เข้ามาของ Job ตามเวลาที่กำหนดใน CSV แล้วส่ง Job 
// เหล่านั้นเข้า arrivalQueue เพื่อให้ Scheduler รับไปจัดคิวต่อ
// ใน class มีโครงสร้างแบบ PRoducer-Consumer

import java.util.List;
import java.util.concurrent.BlockingQueue; //Queue ที่ JobGenerator ใช้ส่ง Job ไปหา Scheduler
import java.util.Comparator;

public class JobGenerator extends Thread { // JobGenerator เป็น Thread ตัวนึง
    //ซึ่งสมารถทำงานพร้อม Worker

    private final List<Job> jobs; //Job ทั้งหมด
    private final BlockingQueue<Job> arrivalQueue; // Job เข้ามาในระบบแล้ว แต่ Scheduler ยังต้องรับไปจัดการ
    private final long simulationStart; //เก็บเวลที่ simulation เริ่ม

    //Constructor
    public JobGenerator(List<Job> jobs, BlockingQueue<Job> arrivalQueue, long simulationStart) {

        super("JobGenerator");

        this.jobs = new java.util.ArrayList<>(jobs);
        this.jobs.sort(Comparator.comparingLong(Job::getArrivalMs));
        this.arrivalQueue = arrivalQueue;
        this.simulationStart = simulationStart;
    }

    @Override
    public void run() {

        try {

            for (Job job : jobs) {
                //ด้านในจะมีการคำนวณ arrivalMs ก่อน
                //target = เวลาจริงเป้าหมายที่ Job ตัวนี้ควร Arrive
                long target = simulationStart + job.getArrivalMs();

                //wait - ตอนนี้เหลืออีกกี่ ms ถึงเวลาที่ Job นี้ควรเข้าระบบ
                long wait = target - System.currentTimeMillis();

                //ถ้ายังไม่ถึงเวลาก็ให้ sleep
                if (wait > 0) {
                    Thread.sleep(wait);
                }

                //๋Job เข้ามาในระบบจริงตอนเวลาเท่าไหร่
                job.setActualArrivalTime(System.currentTimeMillis());

                job.setState(Job.State.ARRIVED);

                arrivalQueue.put(job); //ใช้เพิ่ม Job เข้า BlockingQueue เพื่อให้ Scheduler รับไปจัดการต่อ

                System.out.printf("[%s] %s ARRIVED priority=%d%n",getName(),job.getId(),job.getPriority());
                //เพราะเป็น BlockingQueue การส่งและรับ Job ระหว่าง Thread 
                //จึงทำได้อย่างเหมาะสมโดยไม่ต้องเขียน synchronization ของ queue เอง
            }

            // บอก Scheduler ว่าส่งหมดแล้ว
            arrivalQueue.put(Job.POISON_PILL);
            //InterruptedException ถูก throw สถานะ interrupt จะถูก clear ไป 
            // การเรียก interrupt() อีกครั้งทำให้ข้อมูลว่า Thread เคยถูกขอให้หยุดยังคงอยู่
        
            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                throw new IllegalStateException("JobGenerator interrupted before completing all jobs",e);
            }
    }
}