package puzzle;

/**
 * @author ahmad
 */
public enum HeuristicAlgorithm {

    MANHATTAN(0.001) {
        @Override
        double heuristic(int x1, int y1, int x2, int y2) {
            double dx = Math.abs(x1 - x2);
            double dy = Math.abs(y1 - y2);
            return D * (dx + dy);
        }
    },
    EUCLIDEAN(0.00001) {
        @Override
        double heuristic(int x1, int y1, int x2, int y2) {
            return Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2);
        }
    },
    DIAGONAL(0.000001) {
        @Override
        double heuristic(int x1, int y1, int x2, int y2) {
            double dx = Math.abs(x1 - x2);
            double dy = Math.abs(y1 - y2);
            return D * (dx + dy) + (D2 - 2 * D) * Math.min(dx, dy);
        }
    };

    private static final double D = 1;
    private static final double D2 = Math.sqrt(2);

    public final double epsilon;

    HeuristicAlgorithm(double epsilon) {
        this.epsilon = epsilon;
    }

    abstract double heuristic(int x1, int y1, int x2, int y2);
}
