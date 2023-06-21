import java.time.Instant;
import java.time.Duration;
public class Main {
    public static void main(String[] args) {
        Instant start = Instant.now();
        Network.solvePosition("day23a.txt").forEach(Network.getFrame().panel()::makeMove);
        Network.solvePosition("day23b.txt").forEach(Network.getFrame().panel()::makeMove);
        Instant end = Instant.now();
        System.out.printf("Runtime: %f ms%n", Duration.between(start, end).toNanos() / 1000000.0);
    }
}