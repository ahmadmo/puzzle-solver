import puzzle.*;

import java.util.Deque;
import java.util.concurrent.TimeUnit;

/**
 * @author ahmad
 */
public class PuzzleTester {

    public static void main(String[] args) throws Exception {

        char[][] map = {
                {'X', 'X', 'X', 'X', 'X', 'X'},
                {'X', 'A', 'D', 'D', 'H', 'X'},
                {'X', 'A', 'D', 'D', 'H', 'X'},
                {'X', 'B', 'E', 'E', 'I', 'X'},
                {'X', 'B', 'F', 'G', 'I', 'X'},
                {'X', 'C', ' ', ' ', ' ', 'X'},
//                {'X', ' ', ' ', ' ', ' ', 'X'},
//                {'X', 'C', ' ', ' ', 'J', 'X'},
                {'X', 'X', 'Z', 'Z', 'X', 'X'}
        };

        PuzzleMap puzzle = PuzzleParser.parse(map);

        HeuristicAlgorithm distanceAlgorithm = HeuristicAlgorithm.MANHATTAN;
        int parallelism = Runtime.getRuntime().availableProcessors() * 2;
        HeuristicPuzzleSolver solver = HeuristicPuzzleSolverFactory.getAStarPuzzleSolver();

        long start = System.nanoTime();

//        Deque<PuzzleMap> solution = solver.solve(puzzle, distanceAlgorithm);
        Deque<PuzzleMap> solution = solver.solveParallel(puzzle, distanceAlgorithm, parallelism);

        long elapsed = System.nanoTime();

        for (PuzzleMap step : solution) {
            System.out.println(step);
            System.out.println("------------------------------");
        }

        System.out.println("total moves = " + (solution.size() - 1));
        System.out.println("time = " + (TimeUnit.NANOSECONDS.toMillis(elapsed - start)) + " ms");
    }
}
