import java.io.*;
import java.net.*;

public class t_Server {

    static final int PORT = 5000;

    // โฟลเดอร์ที่ใช้รัน Server
    static final String SERVER_FOLDER = ".";

    // Accumulated server-side processing time; idle time waiting for commands is excluded.
    static final java.util.concurrent.atomic.AtomicLong PROCESSING_NANOS =
            new java.util.concurrent.atomic.AtomicLong();
    static final java.util.concurrent.atomic.AtomicLong REQUEST_COUNT =
            new java.util.concurrent.atomic.AtomicLong();
    static volatile ServerSocket listeningServer;

    static String summary() {
        return String.format(java.util.Locale.US,
                "Total active server processing time: %.6f seconds | Requests processed: %d",
                PROCESSING_NANOS.get() / 1_000_000_000.0,
                REQUEST_COUNT.get());
    }

    public static void main(String[] args) throws IOException {

        try (ServerSocket server = new ServerSocket(PORT)) {
            listeningServer = server;

            System.out.println("Traditional Server started...");
            System.out.println("Port: " + PORT);
            System.out.println(
                    "Server folder: "
                            + new File(SERVER_FOLDER).getAbsolutePath()
            );
            System.out.println("Waiting for client...");

            while (true) {

                Socket socket;
                try {
                    socket = server.accept();
                } catch (SocketException e) {
                    if (server.isClosed()) break;
                    throw e;
                }

                // แต่ละ Worker เชื่อมต่อด้วย Socket ของตัวเอง
                new Thread(() -> handleClient(socket)).start();
            }
        }
        System.out.println("Server stopped. " + summary());
    }

    static void handleClient(Socket socket) {

        try (
                Socket connection = socket;
                DataInputStream in = new DataInputStream(
                        connection.getInputStream()
                );
                DataOutputStream out = new DataOutputStream(
                        connection.getOutputStream()
                )
        ) {

            String command = in.readUTF();

            if ("EXIT".equals(command)) {
                String stats = summary();
                out.writeUTF(stats);
                out.flush();
                System.out.println("EXIT requested. " + stats);
                if (listeningServer != null) listeningServer.close();
                return;
            }

            long processingStart = System.nanoTime();
            try {
            // DOWNLOAD เป็นคำสั่งฝั่ง Client
            // Client แบ่งไฟล์ แล้วส่ง GET แต่ละช่วงมาที่ Server
            switch (command) {

                case "LIST":
                    handleList(out);
                    break;

                case "INFO":
                    handleInfo(in, out);
                    break;

                case "GET":
                    handleGet(in, out);
                    break;

                default:
                    out.writeUTF("ERROR 400 Unknown command");
                    out.flush();
                    break;
            }
            } finally {
                PROCESSING_NANOS.addAndGet(System.nanoTime() - processingStart);
                REQUEST_COUNT.incrementAndGet();
            }

        } catch (IOException e) {

            System.out.println(
                    "Server error: " + e.getMessage()
            );
        }
    }

    // =========================================
    // LIST: แสดงชื่อและขนาดไฟล์
    // =========================================
    static void handleList(DataOutputStream out)
            throws IOException {

        File folder = new File(SERVER_FOLDER);
        File[] files = folder.listFiles();

        if (files == null) {

            out.writeUTF("ERROR 404 Folder not found");
            out.flush();
            return;
        }

        StringBuilder response = new StringBuilder();

        for (File file : files) {

            if (!file.isFile()) {
                continue;
            }

            // ไม่แสดงไฟล์โค้ดและไฟล์ที่ compile แล้ว
            if (file.getName().endsWith(".java")
                    || file.getName().endsWith(".class")) {
                continue;
            }

            if (response.length() > 0) {
                response.append(" | ");
            }

            response.append("FILE ")
                    .append(file.getName())
                    .append(" ")
                    .append(file.length());
        }

        if (response.length() == 0) {
            out.writeUTF("ERROR 404 No files found");
        } else {
            out.writeUTF(response.toString());
        }

        out.flush();
    }

    // =========================================
    // INFO: รับชื่อไฟล์ แล้วตอบขนาด
    // Client จะจำชื่อไฟล์ไว้ใช้กับ GET / DOWNLOAD
    // =========================================
    static void handleInfo(
            DataInputStream in,
            DataOutputStream out
    ) throws IOException {

        String filename = in.readUTF();
        File file = new File(SERVER_FOLDER, filename);

        if (file.isFile()) {

            out.writeUTF("SIZE " + file.length());

        } else {

            out.writeUTF("ERROR 404 File not found");
        }

        out.flush();
    }

    // =========================================
    // GET: ส่งข้อมูลตาม offset และ length
    // =========================================
    static void handleGet(
            DataInputStream in,
            DataOutputStream out
    ) throws IOException {

        String filename = in.readUTF();
        long offset = in.readLong();
        long length = in.readLong();

        File file = new File(SERVER_FOLDER, filename);

        if (!file.isFile()) {

            out.writeUTF("ERROR 404 File not found");
            out.flush();
            return;
        }

        // เปิดไฟล์ก่อนตอบ OK
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {

            long fileSize = raf.length();

            // ตรวจช่วงข้อมูล โดยหลีกเลี่ยง offset + length ล้นค่า
            if (offset < 0
                    || length < 0
                    || offset > fileSize
                    || length > fileSize - offset) {

                out.writeUTF("ERROR 416 Invalid range");
                out.flush();
                return;
            }

            raf.seek(offset);

            out.writeUTF("OK");
            out.flush();

            // Traditional I/O: อ่านเข้า buffer แล้วส่งผ่าน network
            byte[] buffer = new byte[8192];
            long remaining = length;

            while (remaining > 0) {

                int size = (int) Math.min(
                        buffer.length,
                        remaining
                );

                int read = raf.read(buffer, 0, size);

                if (read == -1) {
                    throw new EOFException(
                            "File ended before the requested range completed"
                    );
                }

                out.write(buffer, 0, read);
                remaining -= read;
            }

            out.flush();
        }
    }
}