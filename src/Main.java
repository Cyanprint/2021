public class Main {
    public static void main(String[] args) {
        Network.solvePosition("input.txt").forEach(System.out::println);
        Network.solvePosition("part2.txt").forEach(System.out::println);
    }
}