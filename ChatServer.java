import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;




public class ChatServer {

    private static final Set<String> names = new HashSet<>();
    private static final Set<PrintWriter> writers = new HashSet<>();
    private static final AtomicInteger clientCount = new AtomicInteger(0);
    private static volatile String coordinator = null;

    public static void main(String[] args) throws Exception {
        System.out.println("The chat server is running...");
        ExecutorService pool = Executors.newFixedThreadPool(500);
        try (ServerSocket listener = new ServerSocket(59001)) {
            while (true) {
                pool.execute(new Handler(listener.accept()));
            }
        }
    }

    private static class Handler implements Runnable {
        private String name;
        private final Socket socket;
        private Scanner in;
        private PrintWriter out;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new Scanner(socket.getInputStream());
                out = new PrintWriter(socket.getOutputStream(), true);

                while (true) {
                    out.println("SUBMITNAME");
                    name = in.nextLine();
                    if (name == null || name.isEmpty()) {
                        return;
                    }
                    synchronized (names) {
                        if (!names.contains(name)) {
                            names.add(name);
                            break;
                        }
                    }
                }

                out.println("NAMEACCEPTED " + name);
                if (clientCount.incrementAndGet() == 1) {
                    coordinator = name;
                    out.println("COORDINATOR " + name);
                    broadcastMessage("MESSAGE " + name + " is the coordinator.");
                } else {
                    out.println("NOTCOORDINATOR");
                }

                writers.add(out);
                while (true) {
                    String input = in.nextLine();
                    if (input == null) {
                        return;
                    }
                    broadcastMessage("MESSAGE " + name + ": " + input);
                }
            } catch (Exception e) {
                System.out.println(e);
            } finally {
                if (name != null && name.equals(coordinator) && clientCount.decrementAndGet() > 0) {
                    coordinator = null; // Reset or reassign coordinator
                    broadcastMessage("MESSAGE The coordinator has left.");
                } else {
                    clientCount.decrementAndGet();
                }
                if (name != null) {
                    names.remove(name);
                    if (out != null) {
                        writers.remove(out);
                    }
                    broadcastMessage("MESSAGE " + name + " has left");
                }
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }

        private void broadcastMessage(String message) {
            for (PrintWriter writer : writers) {
                writer.println(message);
            }
        }
    }
}
