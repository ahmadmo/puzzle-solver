package puzzle;

import java.util.Deque;

/**
 * @author ahmad
 */
public interface HeuristicPuzzleSolver {

    Deque<PuzzleMap> solve(PuzzleMap start, HeuristicAlgorithm algorithm) throws Exception;

    Deque<PuzzleMap> solveParallel(PuzzleMap start, HeuristicAlgorithm algorithm, int parallelism) throws Exception;
}
