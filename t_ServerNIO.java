import java.io.*;
import java.net.*;
import java.nio.channels.*;

public class t_ServerNIO {

    static final int PORT = 5001;
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

            System.out.println("NIO Server started...");
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
            // DOWNLOAD ทำงานที่ Client
            // Client แบ่งไฟล์แล้วส่ง GET แต่ละช่วงมาที่ Server
            switch (command) {

                case "LIST":
                    handleList(out);
                    break;

                case "INFO":
                    handleInfo(in, out);
                    break;

                case "GET":
                    handleGet(connection, in, out);
                    break;

                default:
                    out.writeUTF("ERROR 400 Unknown command");
                    out.flush();
            }
            } finally {
                PROCESSING_NANOS.addAndGet(System.nanoTime() - processingStart);
                REQUEST_COUNT.incrementAndGet();
            }

        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    // แสดงรายชื่อและขนาดไฟล์
    static void handleList(DataOutputStream out)
            throws IOException {

        File[] files = new File(SERVER_FOLDER).listFiles();

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

        out.writeUTF(
                response.length() == 0
                        ? "ERROR 404 No files found"
                        : response.toString()
        );

        out.flush();
    }

    // ตอบขนาดไฟล์ที่ Client เลือก
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

    // ส่งข้อมูลเฉพาะช่วงด้วย NIO
    static void handleGet(
            Socket socket,
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

        try (
                RandomAccessFile raf = new RandomAccessFile(file, "r");
                FileChannel fileChannel = raf.getChannel()
        ) {

            long fileSize = fileChannel.size();

            if (offset < 0
                    || length < 0
                    || offset > fileSize
                    || length > fileSize - offset) {

                out.writeUTF("ERROR 416 Invalid range");
                out.flush();
                return;
            }

            out.writeUTF("OK");
            out.flush();

            WritableByteChannel socketChannel =
                    Channels.newChannel(socket.getOutputStream());

            long position = offset;
            long remaining = length;

            while (remaining > 0) {

                long transferred = fileChannel.transferTo(
                        position,
                        remaining,
                        socketChannel
                );

                if (transferred <= 0) {
                    throw new IOException(
                            "Incomplete transfer"
                                    + " | remaining = " + remaining
                    );
                }

                position += transferred;
                remaining -= transferred;
            }

            out.flush();

            System.out.println(
                    "GET completed"
                            + " | file = " + filename
                            + " | offset = " + offset
                            + " | length = " + length
            );
        }
    }
}