import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class WorkloadLoader {

    public static List<Job> load(String filename)
            throws Exception {

        List<Job> jobs = new ArrayList<>();

        BufferedReader br =
                new BufferedReader(
                        new FileReader(filename)
                );

        String line;

        // ข้าม header
        br.readLine();

        while ((line = br.readLine()) != null) {

            line = line.trim();

            if (line.isEmpty()) {
                continue;
            }

            String[] data = line.split(",");

            String id = data[0].trim();

            long arrivalMs =
                    Long.parseLong(data[1].trim());

            int priority =
                    Integer.parseInt(data[2].trim());

            long workMs =
                    Long.parseLong(data[3].trim());

            Job.ResourceType resource =
                    Job.ResourceType.valueOf(
                            data[4].trim()
                    );

            long resourceMs =
                    Long.parseLong(data[5].trim());

            jobs.add(
                new Job(
                    id,
                    arrivalMs,
                    priority,
                    workMs,
                    resource,
                    resourceMs
                )
            );
        }

        br.close();

        return jobs;
    }
}