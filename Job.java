//เป็น object ที่เก็บข้อมูลและสถานะของงานแต่ละงาน ตามข้อมูล Job ที่โจทย์กำหนด 
// เช่น id, arrival, priority, work time และ resource
public class Job {

    public enum ResourceType { //enum ช่วยลดความผิดพลาดได้มากกว่า String และทำให้ Worker ตรวจสอบได้ง่าย
        NONE, PRINTER, DATABASE 
        //์None คือ ทำงานปกติไม่ใช้ resource
        //Printer คือ ใช้ resource เป็น printer
        //Database คือ ใช้ resource เป็น database
    }

    public enum State { //เอาไว้บอกว่า Job อยู่ในสถานะอะไร
        ARRIVED, // Job เข้ามาในระบบ
        READY, //๋๋Job เข้า Ready Queue แล้วกำลังรอ Worker
        RUNNING, // Worker รับ Job ไปทำแล้ว
        WAITING_RESOURCE, // Job ทำงานมาถึงจุดที่ต้องใช้ printer/database แต่ Semaphore ยังไม่ว่างจึงกำลังรอ
        COMPLETED // Job ทำงานทั้งหมดเสร็จแล้ว
    }
    
    public static final Job POISON_PILL = new Job("__POISON__", 0, 0, 0, ResourceType.NONE, 0);
    
    // ข้อมูลหลักที่มาจาก CSV
    private final String id;
    private final long arrivalMs; //Job นี้ ถูกกำหนดให้เข้ามาหลังจาก Simulation เริ่มไปแล้วกี่ milliseconds
    private final int priority; //เก็บระดับ Priority ของ Job โดย 1 สูงที่สุด
    private final long workMs; //เวลาที่ใ้ช้ทำงานหลัก Job
    private final ResourceType resource; //๋Job แชร์ resource อะไร
    private final long resourceMs; //ระยะเวลาที่ใช้ resource
    //ใช้ final เนื่องจากไม่ควรเปลี่ยนระหว่าง simulation

    //ข้อมูลเวลาที่เกิดขึ้นตอน Runtime ค่าพวกนี้จะเกิดขึ้นจริงระหว่าง simulation
    private long actualArrivalTime; //เวลาจริงที่ Job Arrived
    private long startTime; //เวลาที่เริ่มทำ Job จริง
    private long completionTime; // เวลาที่ Job เสร็จทั้งหมด
    private long resourceWaitTime; //ระยะเวลาที่รอ Semaphore ก่อนจะได้ Resource

    private volatile State state; //เกี่ยวกับ multithreading โดยตรง คำถามจารย์ได้
    // state สามารถถูกเปลี่ยนหรืออ่านจากหลาย Thread
    // ex. JobGenerator - Arrived
    // ex. Scheduler - ready
    // ex. Worker - running
    // ex. Worker - waiting_resource
    // ex. Worker - completed
    // ex. Monitor - อ่านสถานะ 

    //Constructor
    public Job(String id,
               long arrivalMs,
               int priority,
               long workMs,
               ResourceType resource,
               long resourceMs) {

        this.id = id;
        this.arrivalMs = arrivalMs;
        this.priority = priority;
        this.workMs = workMs;
        this.resource = resource;
        this.resourceMs = resourceMs;
        this.state = State.ARRIVED;
    }

    public String getId() {
        return id;
    }

    public long getArrivalMs() {
        return arrivalMs;
    }

    public int getPriority() {
        return priority;
    }

    public long getWorkMs() {
        return workMs;
    }

    public ResourceType getResource() {
        return resource;
    }

    public long getResourceMs() {
        return resourceMs;
    }

    public long getActualArrivalTime() {
        return actualArrivalTime;
    }

    public void setActualArrivalTime(long time) {
        this.actualArrivalTime = time;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long time) {
        this.startTime = time;
    }

    public long getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(long time) {
        this.completionTime = time;
    }

    public long getResourceWaitTime() {
        return resourceWaitTime;
    }

    public void setResourceWaitTime(long time) {
        this.resourceWaitTime = time;
    }

    public State getState() {
        return state;
    }

    // ไว้เปลี่ยนสถานะ Job ตอน simulation
    public void setState(State state) {
        this.state = state;
    }

    public long getWaitingTime() {
        return startTime - actualArrivalTime;
    }

    public long getTurnaroundTime() {
        return completionTime - actualArrivalTime;
    }
}