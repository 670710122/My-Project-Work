import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Locale;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class t_Client {

    static final String HOST = "localhost";
    static final int PORT = 5000;

    // จำไฟล์ที่เลือกจาก INFO สำเร็จ
    static String selectedFilename;
    static long selectedSize = -1;

    public static void main(String[] args) {

        try (Scanner sc = new Scanner(System.in)) {

            while (true) {

                System.out.println();
                System.out.print(
                        "Enter command (LIST / INFO / GET / DOWNLOAD / EXIT): ");

                if (!sc.hasNextLine()) {
                    break;
                }

                String command = sc.nextLine()
                        .trim()
                        .toUpperCase(Locale.ROOT);

                try {

                    switch (command) {

                        case "LIST":
                            System.out.println(measuredRequest("LIST", null));
                            break;

                        case "INFO":
                            selectFile(sc);
                            break;

                        case "GET":
                            if (hasSelectedFile()) {
                                int workers = readWorkers(sc);
                                int workerId = readWorkerId(sc, workers);
                                getSingleWorker(workers, workerId);
                            }
                            break;

                        case "DOWNLOAD":
                            if (hasSelectedFile()) {

                                int workers = readWorkers(sc);
                                Path output = readOutput(sc);

                                // แบ่งงานแล้วเรียก GET แต่ละช่วง
                                downloadFile(workers, output);
                            }
                            break;

                        case "EXIT":
                            // Request the matching server's accumulated active processing time.
                            // Also ask this server to shut down; does not affect the other server.
                            System.out.println(request("EXIT", null));
                            System.out.println("Client closed; matching server stopped.");
                            return;

                        default:
                            System.out.println("Invalid command.");
                    }

                } catch (NumberFormatException e) {

                    System.out.println("Invalid numeric value.");

                } catch (InterruptedException e) {

                    Thread.currentThread().interrupt();
                    System.out.println("Download interrupted.");
                    return;

                } catch (Exception e) {

                    System.out.println("Error: " + e.getMessage());
                }
            }
        }
    }

    // =========================================
    // ส่ง LIST หรือ INFO แล้วรับคำตอบ
    // =========================================
    static String request(String command, String filename)
            throws IOException {

        try (
                Socket socket = new Socket(HOST, PORT);
                DataOutputStream out = new DataOutputStream(
                        socket.getOutputStream());
                DataInputStream in = new DataInputStream(
                        socket.getInputStream())) {

            out.writeUTF(command);

            if (filename != null) {
                out.writeUTF(filename);
            }

            out.flush();

            return in.readUTF();
        }
    }

    // =========================================
    // INFO: เลือกไฟล์และจำชื่อไว้
    // =========================================
    // Measures the application response payload (not full TCP wire throughput).
    static String measuredRequest(String command, String filename) throws IOException {
        long start = System.nanoTime();
        String response = request(command, filename);
        long elapsed = System.nanoTime() - start;
        long bytes = response.getBytes(StandardCharsets.UTF_8).length;
        printTransferRate(command + " response", bytes, elapsed);
        return response;
    }

    static void printTransferRate(String operation, long bytes, long nanos) {
        double seconds = Math.max(nanos / 1_000_000_000.0, 1e-9);
        double mibPerSecond = (bytes / (1024.0 * 1024.0)) / seconds;
        System.out.println("Operation   : " + operation);
        System.out.println("Data        : " + bytes + " bytes");
        System.out.printf(Locale.ROOT, "Time        : %.6f seconds%n", seconds);
        System.out.printf(Locale.ROOT, "Throughput  : %.6f MiB/s%n", mibPerSecond);
    }

    static void selectFile(Scanner sc) throws IOException {

        // ล้างไฟล์เดิม ป้องกัน INFO ล้มเหลวแล้วโหลดไฟล์เก่า
        selectedFilename = null;
        selectedSize = -1;

        System.out.print("Enter file name: ");
        String filename = sc.nextLine().trim();

        if (filename.isEmpty()) {
            System.out.println("File name cannot be empty.");
            return;
        }

        String response = measuredRequest("INFO", filename);

        System.out.println(response);

        if (!response.startsWith("SIZE ")) {
            return;
        }

        long size = parseSize(response);

        selectedFilename = filename;
        selectedSize = size;

        System.out.println("Selected file: " + selectedFilename);
        System.out.println("Use GET or DOWNLOAD to download this file.");
    }

    static boolean hasSelectedFile() {

        if (selectedFilename == null || selectedSize < 0) {

            System.out.println(
                    "Please use INFO to select a file first.");

            return false;
        }

        return true;
    }

    static long parseSize(String response) throws IOException {

        if (!response.startsWith("SIZE ")) {
            throw new IOException(response);
        }

        try {

            long size = Long.parseLong(
                    response.substring(5).trim());

            if (size < 0) {
                throw new IOException("Invalid file size.");
            }

            return size;

        } catch (NumberFormatException e) {

            throw new IOException("Invalid SIZE response.", e);
        }
    }

    // =========================================
    // DOWNLOAD: รับจำนวน Worker
    // =========================================
    static int readWorkers(Scanner sc) {

        while (true) {

            System.out.print("Enter workers (1 or 10): ");

            try {

                int workers = Integer.parseInt(
                        sc.nextLine().trim());

                if (workers == 1 || workers == 10) {
                    return workers;
                }

            } catch (NumberFormatException e) {
                // ให้กรอกใหม่
            }

            System.out.println("Please enter 1 or 10.");
        }
    }

    // =========================================
    // DOWNLOAD: รับ path พร้อมชื่อไฟล์ปลายทาง
    // =========================================
    static Path readOutput(Scanner sc) {

        while (true) {

            System.out.print(
                    "Enter downloaded file path (including file name): ");

            String input = sc.nextLine().trim();

            if (input.isEmpty()) {
                System.out.println("Output path cannot be empty.");
                continue;
            }

            try {

                Path output = Paths.get(input)
                        .toAbsolutePath()
                        .normalize();

                if (Files.exists(output)) {

                    System.out.println(
                            "Output already exists. Please use another name.");

                    continue;
                }

                return output;

            } catch (InvalidPathException e) {

                System.out.println("Invalid output path.");
            }
        }
    }

    // =========================================
    // GET: ตั้งชื่อปลายทางอัตโนมัติ ไม่ถามเพิ่ม
    // =========================================
    static Path automaticOutput() {

        String filename = new File(selectedFilename).getName();
        String base = "downloaded_traditional_" + filename;

        Path output = Paths.get(base).toAbsolutePath();

        int number = 1;

        while (Files.exists(output)) {

            output = Paths.get(
                    number++ + "_" + base).toAbsolutePath();
        }

        return output;
    }

    // =========================================
    // ดาวน์โหลดด้วย Worker ตามจำนวนที่กำหนด
    // =========================================
    // GET: download only the selected worker's byte range.
    static int readWorkerId(Scanner sc, int workers) {
        while (true) {
            System.out.print("Choose worker (1-" + workers + "): ");
            int id;
            try {
                id = Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid worker number.");
                continue;
            }
            if (id >= 1 && id <= workers)
                return id;
            System.out.println("Worker must be between 1 and " + workers);
        }
    }

    static void getSingleWorker(int workers, int workerId) throws Exception {
        final String filename = selectedFilename;
        long fileSize = parseSize(request("INFO", filename));
        selectedSize = fileSize;
        long partSize = fileSize / workers;
        long offset = (workerId - 1L) * partSize;
        long length = workerId == workers ? fileSize - offset : partSize;

        String prefix = "traditional";
        String base = prefix + "_worker" + workerId + "_" + new File(filename).getName();
        Path output = Paths.get(base).toAbsolutePath();
        for (int i = 1; Files.exists(output); i++) {
            output = Paths.get(i + "_" + base).toAbsolutePath();
        }
        Files.createFile(output);
        System.out.println("GET only Worker " + workerId + "/" + workers);
        System.out.println("Requested offset: " + offset + " | length: " + length);
        // Write the returned range starting at byte 0 of the partial output.
        long startTime = System.nanoTime();
        boolean success = getRange(workerId, filename, output, offset, length, true);
        long elapsed = System.nanoTime() - startTime;
        if (success) {
            printTransferRate("GET Worker " + workerId + "/" + workers, length, elapsed);
            System.out.println("Partial range saved as: " + output);
            System.out.println("This is NOT the complete file; full-file hash check is not applicable.");
        } else {
            System.out.println("GET failed. Partial output: " + output);
        }
    }

    static void downloadFile(int workers, Path output)
            throws Exception {

        final String filename = selectedFilename;

        // ตรวจขนาดอีกครั้งโดยไม่ถามผู้ใช้
        long fileSize = parseSize(request("INFO", filename));

        selectedSize = fileSize;

        Files.createDirectories(output.getParent());

        // สร้างแบบห้ามทับไฟล์เดิม แม้มีไฟล์ถูกสร้างระหว่างรอ
        Files.createFile(output);

        System.out.println("File: " + filename);
        System.out.println("File size: " + fileSize + " bytes");
        System.out.println("Workers: " + workers);
        System.out.println("Output: " + output);

        long startTime = System.nanoTime();

        try (
                RandomAccessFile result = new RandomAccessFile(
                        output.toFile(),
                        "rw")) {
            result.setLength(fileSize);
        }

        long partSize = fileSize / workers;

        Thread[] threads = new Thread[workers];
        boolean[] workerSuccess = new boolean[workers];

        for (int i = 0; i < workers; i++) {

            final int index = i;
            final int workerID = i + 1;

            final long offset = i * partSize;

            // Worker สุดท้ายรับเศษที่เหลือ
            final long length = i == workers - 1
                    ? fileSize - offset
                    : partSize;

            threads[i] = new Thread(() -> {

                workerSuccess[index] = getRange(
                        workerID,
                        filename,
                        output,
                        offset,
                        length);
            });

            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        long endTime = System.nanoTime();

        for (boolean success : workerSuccess) {

            if (!success) {

                System.out.println(
                        "Download failed: some workers did not finish.");

                System.out.println("Partial file: " + output);
                return;
            }
        }

        double timeSeconds = (endTime - startTime) / 1_000_000_000.0;

        double sizeMiB = fileSize / (1024.0 * 1024.0);
        double throughput = sizeMiB / timeSeconds;

        System.out.println();
        System.out.println("Download complete!");
        System.out.println("Saved as: " + output);
        System.out.println("Method      : Traditional I/O");
        System.out.println("Workers     : " + workers);

        System.out.printf(
                "Time        : %.6f seconds%n",
                timeSeconds);

        System.out.printf(
                "Throughput  : %.3f MiB/s%n",
                throughput);

        verifyFile(filename, output, fileSize);
    }

    // =========================================
    // Worker ส่ง GET filename offset length
    // =========================================
    static boolean getRange(int id, String filename, Path output, long offset, long length) {
        return getRange(id, filename, output, offset, length, false);
    }

    static boolean getRange(
            int id,
            String filename,
            Path output,
            long offset,
            long length,
            boolean partialOutput) {

        try (
                Socket socket = new Socket(HOST, PORT);
                DataOutputStream out = new DataOutputStream(
                        socket.getOutputStream());
                DataInputStream in = new DataInputStream(
                        socket.getInputStream())) {

            out.writeUTF("GET");
            out.writeUTF(filename);
            out.writeLong(offset);
            out.writeLong(length);

            out.flush();

            String response = in.readUTF();

            if (!response.equals("OK")) {

                System.out.println(
                        "Worker " + id + ": " + response);

                return false;
            }

            try (
                    RandomAccessFile file = new RandomAccessFile(
                            output.toFile(),
                            "rw")) {

                // แต่ละ Worker เขียนคนละช่วงของไฟล์เดียวกัน
                file.seek(partialOutput ? 0 : offset);

                byte[] buffer = new byte[8192];
                long remaining = length;

                while (remaining > 0) {

                    int size = (int) Math.min(
                            buffer.length,
                            remaining);

                    int read = in.read(buffer, 0, size);

                    if (read == -1) {

                        throw new EOFException(
                                "Incomplete data.");
                    }

                    file.write(buffer, 0, read);

                    remaining -= read;
                }
            }

            System.out.println(
                    "Worker " + id
                            + " finished"
                            + " | offset = " + offset
                            + " | length = " + length);

            return true;

        } catch (IOException e) {

            System.out.println(
                    "Worker " + id + " error: " + e.getMessage());

            return false;
        }
    }

    // =========================================
    // ตรวจขนาดและ SHA-256
    // =========================================
    static void verifyFile(
            String filename,
            Path output,
            long expectedSize) throws Exception {

        long downloadedSize = Files.size(output);

        System.out.println();
        System.out.println(
                "Expected size   : " + expectedSize + " bytes");
        System.out.println(
                "Downloaded size : " + downloadedSize + " bytes");
        System.out.println(
                "Size check      : "
                        + (expectedSize == downloadedSize ? "PASS" : "FAIL"));

        String downloadedHash = getSHA256(output);

        System.out.println(
                "Downloaded SHA-256 : " + downloadedHash);

        // ไม่ถาม original path เพิ่ม
        // เปรียบเทียบเมื่อมีต้นฉบับตามชื่อนี้อยู่ในเครื่อง Client
        Path original = Paths.get(filename);

        if (Files.isRegularFile(original)) {

            String originalHash = getSHA256(original);

            System.out.println(
                    "Original SHA-256   : " + originalHash);

            System.out.println(
                    "Hash check         : "
                            + (originalHash.equals(downloadedHash)
                                    ? "PASS"
                                    : "FAIL"));

        } else {

            System.out.println(
                    "Hash comparison skipped: "
                            + "original file is not available locally.");
        }
    }

    static String getSHA256(Path path) throws Exception {

        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (InputStream file = Files.newInputStream(path)) {

            byte[] buffer = new byte[8192];
            int read;

            while ((read = file.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }

        StringBuilder hash = new StringBuilder();

        for (byte b : digest.digest()) {
            hash.append(String.format("%02x", b & 0xff));
        }

        return hash.toString();
    }
}