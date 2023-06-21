public record Move(int amphipodID, Coordinate from, Coordinate to) {
    @Override
    public String toString() {
        return String.format("Amphipod %d: [%s - %s]", amphipodID, from, to);
    }
}