import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.stream.Stream;


public class Network {
    private static final int[] HALLWAY = {1, 2, 4, 6, 8, 10, 11};
    private static final int[] MOVE_COSTS = {1, 10, 100, 1000};
    private static int BURROW_DEPTH = 0;
    private static int NUM_AMPHIPODS = 0;
    private static Frame frame;

    public static ArrayList<Move> solvePosition(String filename) {
        List<String> input = new ArrayList<String>();
        try (Stream<String> stream = Files.lines(Paths.get("src", filename))) {
            stream.forEach(input::add);
            stream.close();
        } catch (IOException exception) {
            System.err.printf("Caught IOException: %s", exception);
        } 

        BURROW_DEPTH = input.size() - 3;
        NUM_AMPHIPODS = BURROW_DEPTH * 4;

        /* Assigning each amphipod to its location ID (each hallway/burrow slot has its own ID) */
        int[] startingPositions = new int[NUM_AMPHIPODS];
        for (int i = 0; i < BURROW_DEPTH; i++) {
            String line = input.get(i + 2);
            for (int j = 0; j < 4; j++) {
                char c = line.charAt(2 * j + 3);
                int unit = (c - 'A') * BURROW_DEPTH;
                while (startingPositions[unit] != 0) {
                    unit++;
                }
                /* +7 corresponds to the seven slots that are available in the hallway */
                startingPositions[unit] = 4 * i + j + 7;
            }
        }

        frame = new Frame(startingPositions);
        Queue<State> queue = new PriorityQueue<>(Comparator.comparingInt(State::totalCost));
        queue.add(new State(startingPositions, 0, new ArrayList<Move>()));

        // Should not approach anywhere near 2^31 - 1, so no need for longs
        State bestState = new State(startingPositions, Integer.MAX_VALUE, new ArrayList<Move>());
        Map<String, Integer> alreadyProcessed = new HashMap<>();
        while (!queue.isEmpty()) {
            State toProcess = queue.poll();
            if (toProcess.totalCost >= bestState.totalCost) {
                break;
            }

            for (int unit = 0; unit < NUM_AMPHIPODS; unit++) {
                boolean[] validPos = findValidPositions(toProcess.positions, unit);
                for (int i = 0; i < validPos.length; i++) {
                    // Concept of inversion in play here
                    if (!validPos[i]) {
                        continue;
                    }

                    int cost = calcCost(unit, toProcess.positions[unit], i);
                    State next = toProcess.moveUnit(unit, i, cost);
                    if (next.isFinishedState()) {
                        bestState = (bestState.totalCost < next.totalCost) ? bestState : next;
                    } else {
                        // Checks if this is the lowest cost of this state
                        String state = next.getState();
                        if (next.totalCost < alreadyProcessed.getOrDefault(state, Integer.MAX_VALUE)) {
                            alreadyProcessed.put(state, next.totalCost);
                            queue.add(next);
                        }
                    }
                }
            }
        }
        System.out.printf("Best cost: %d%n", bestState.totalCost);
        return bestState.moves;
    }

    /* This architecture represents A, B, C, and D as 0, 1, 2, and 3 respectively */
    private static int getType(int unit) {
        if (unit == -1) {
            return -1;
        }
        return unit / BURROW_DEPTH;
    }

    private static boolean[] findValidPositions(int[] positions, int unit) {
        // Is it in a hallway? If so, check rooms. If not, check hallways.
        if (positions[unit] < 7) { 
            return findValidRoomPositions(positions, unit);
        } else {
            return findValidHallPositions(positions, unit);
        }
    }

    private static boolean[] findValidHallPositions(int[] positions, int unit) {
        // Creates the 1d Array corresponding to possible positions
        int[] occupied = new int[NUM_AMPHIPODS + 7];
        // Set all of them to -1
        for (int i = 0; i < NUM_AMPHIPODS + 7; i++) {
            occupied[i] = -1;
        }
        // Set the ones that are occupied to each amphipod's corresponding "id"
        for (int i = 0; i < NUM_AMPHIPODS; i++) {
            occupied[positions[i]] = i;
        }

        // boolean array of hallway tiles 
        boolean[] hallway = new boolean[7];

        // Get position of the target amphipod, and get its type.
        int position = positions[unit];
        int type = getType(unit);
        // If any positions above it are blocked, then return the array, which should be default false.
        for (int i = position - 4; i > 6; i -= 4) {
            if (occupied[i] > -1) {
                return hallway;
            }
        }

        // If it's in its burrow, check if all below are of its type. If so, the hallway (all false) is returned.
        if ((position + 1) % 4 == type) {
            boolean gottaMove = false;
            for (int i = position + 4; i < NUM_AMPHIPODS + 7; i += 4) {
                if (getType(occupied[i]) != type) {
                    gottaMove = true;
                    break;
                }
            }
            if (!gottaMove) {
                return hallway;
            }
        }

        // Get the burrow tile the amphipod will leave from
        int burrowOpening = position;
        while (burrowOpening > 10) {
            burrowOpening -= 4;
        }

        // Check for each of the hallway tiles, which one the amphipod can move to.
        for (int i = 0; i < 7; i++) {
            if (occupied[i] == -1 && checkHallwayClear(i, burrowOpening, occupied)) {
                hallway[i] = true;
            }
        }
        return hallway;
    }

    private static boolean[] findValidRoomPositions(int[] positions, int unit) {
        // Creates the 1d Array corresponding to possible positions
        int[] occupied = new int[NUM_AMPHIPODS + 7];
        // Set all of them to -1
        for (int i = 0; i < NUM_AMPHIPODS + 7; i++) {
            occupied[i] = -1;
        }
        // Set the ones that are occupied to each amphipod's corresponding "id"
        for (int i = 0; i < NUM_AMPHIPODS; i++) {
            occupied[positions[i]] = i;
        }

        // boolean array of all tiles 
        boolean[] network = new boolean[NUM_AMPHIPODS + 7];
        // Get position and type of target amphipod
        int position = positions[unit];
        int type = getType(unit);
        // Get the opening id of the burrow of which it should go into.
        int opening = type + 7;

        // If it cannot reach its burrow, return the network (false).
        if (!checkHallwayClear(position, opening, occupied)) {
            return network;
        }

        // If it can, check the burrow.
        int target = opening;
        for (int i = 0; i < BURROW_DEPTH; i++) {
            /* Update the target tile to be the last one that isn't empty.
             * If a tile's not empty, and there's an amphipod that isn't home here, return the network (false). */
            if (occupied[opening + 4 * i] == -1) {
                target = opening + 4 * i;
            } else if (getType(occupied[opening + 4 * i]) != type) {
                return network;
            }
        }

        // Set the home burrow tile to true, and return the network!
        network[target] = true;
        return network;
    }

    // Checks if the path from the hallway to the room is clear
    private static boolean checkHallwayClear(int hallPos, int roomPos, int[] occupied) {
        /* Takes the hallway and the room positions, and pulls out all hallway points between the two.
         * Additionally, by taking the min and a max, it only needs to check from left to right. */
        int min = Math.min(hallPos + 1, roomPos - 5);
        int max = Math.max(hallPos - 1, roomPos - 6);

        /* If there's anything in the way, return false. */
        for (int i = min; i <= max; i++) {
            if (occupied[i] != -1) {
                return false;
            }
        }
        return true;
    }

    // Calculate the cost of a move
    private static int calcCost(int unit, int from, int to) {
        // Checks for which one is greater, and makes from < to.
        if (from > to) {
            int temp = from;
            from = to;
            to = temp;
        }

        // Finds the depth of the burrow, as well as its column.
        int depth = (to - 3) / 4;
        int burrowCol = ((to + 1) % 4) * 2 + 3;
        // Finds the total cost of a move
        int dist = Math.abs(HALLWAY[from] - burrowCol) + depth;
        int type = getType(unit);
        return MOVE_COSTS[type] * dist;
    }

    // Creates a record of the current network state. 
    private record State(int[] positions, int totalCost, ArrayList<Move> moves) {
        public State moveUnit(int unit, int position, int moveCost) {
            Coordinate from, to;
            int fromIndex = positions[unit];
            if (fromIndex < 7) {
                from = new Coordinate(HALLWAY[fromIndex], 1);
                to = new Coordinate(((position + 1) % 4) * 2 + 3, (position - 3) / 4 + 1);
            } else {
                from = new Coordinate(((fromIndex + 1) % 4) * 2 + 3, (fromIndex - 3) / 4 + 1);
                to = new Coordinate(HALLWAY[position], 1);
            }
            int[] newPositions = Arrays.copyOf(positions, positions.length);
            ArrayList<Move> newMoves = new ArrayList<>(moves);
            newMoves.add(new Move(unit, from, to));
            newPositions[unit] = position;
            State network = new State(newPositions, totalCost + moveCost, newMoves);
            return network;
        }

        // Checks if it's in the end state.
        public boolean isFinishedState() {
            for (int i = 0; i < positions.length; i++) {
                int type = getType(i);
                if (positions[i] < 7 || (positions[i] + 1) % 4 != type) {
                    return false;
                }
            }
            return true;
        }

        public String getState() {
            int[] occupied = new int[NUM_AMPHIPODS + 7];
            for (int i = 0; i < NUM_AMPHIPODS + 7; i++) {
                occupied[i] = -1;
            }
            for (int i = 0; i < NUM_AMPHIPODS; i++) {
                occupied[positions[i]] = i;
            }

            String state = "";
            for (int i = 0; i < NUM_AMPHIPODS + 7; i++) {
                int type = getType(occupied[i]);
                if (type == -1) {
                    state += ".";
                } else {
                    state += type;
                }
            }
            return state;
        }
    }

    public static Frame getFrame() {
        return frame;
    } 
}