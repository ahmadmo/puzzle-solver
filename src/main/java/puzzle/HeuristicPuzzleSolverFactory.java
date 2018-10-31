package puzzle;

/**
 * @author ahmad
 */
public final class HeuristicPuzzleSolverFactory {

    private HeuristicPuzzleSolverFactory() {
    }

    public static HeuristicPuzzleSolver getAStarPuzzleSolver() {
        return new AStarPuzzleSolver();
    }

    public static HeuristicPuzzleSolver getForkJoinPuzzleSolver() {
        return new ForkJoinPuzzleSolver();
    }
}
