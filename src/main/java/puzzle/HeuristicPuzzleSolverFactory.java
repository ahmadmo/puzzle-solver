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

    public static HeuristicPuzzleSolver getForkJoinPuzzleSolver(double neighbourExploreFactor) {
        return new ForkJoinPuzzleSolver(neighbourExploreFactor);
    }
}
