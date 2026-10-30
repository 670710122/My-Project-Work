
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Statistics {

    // เก็บ Job ที่ทำงานเสร็จแล้ว
    private final List<Job> completedJobs =
            Collections.synchronizedList(new ArrayList<>());

    // เก็บเหตุการณ์ของระบบ
    private final List<String> eventLogs =
            Collections.synchronizedList(new ArrayList<>());

    // =============================
    // Completed Jobs
    // =============================

    public void addCompletedJob(Job job) {
        completedJobs.add(job);
    }

    public int getCompletedCount() {
        return completedJobs.size();
    }

    // =============================
    // Average Waiting Time
    // =============================

    public double getAverageWaitingTime() {

        synchronized (completedJobs) {

            if (completedJobs.isEmpty()) {
                return 0;
            }

            long total = 0;

            for (Job job : completedJobs) {
                total += job.getWaitingTime();
            }

            return (double) total / completedJobs.size();
        }
    }

    // =============================
    // Average Turnaround Time
    // =============================

    public double getAverageTurnaroundTime() {

        synchronized (completedJobs) {

            if (completedJobs.isEmpty()) {
                return 0;
            }

            long total = 0;

            for (Job job : completedJobs) {
                total += job.getTurnaroundTime();
            }

            return (double) total / completedJobs.size();
        }
    }

    // =============================
    // Average Resource Wait Time
    // =============================

    public double getAverageResourceWaitTime() {

        synchronized (completedJobs) {

            long total = 0;
            int count = 0;

            for (Job job : completedJobs) {

                if (job.getResource() != Job.ResourceType.NONE) {
                    total += job.getResourceWaitTime();
                    count++;
                }
            }

            if (count == 0) {
                return 0;
            }

            return (double) total / count;
        }
    }

    // =============================
    // Completed Jobs List
    // =============================

    public List<Job> getCompletedJobs() {

        synchronized (completedJobs) {
            return new ArrayList<>(completedJobs);
        }
    }

    // =============================
    // Event Logging
    // =============================

    public void addLog(String message) {
        eventLogs.add(message);
    }

    public List<String> getEventLogs() {

        synchronized (eventLogs) {
            return new ArrayList<>(eventLogs);
        }
    }
}
