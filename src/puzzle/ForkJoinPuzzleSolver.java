package puzzle;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

/**
 * @author ahmad
 */
final class ForkJoinPuzzleSolver implements HeuristicPuzzleSolver {

    private final double neighbourExploreFactor;

    ForkJoinPuzzleSolver(double neighbourExploreFactor) {
        if (neighbourExploreFactor > 1) {
            throw new IllegalArgumentException();
        }
        this.neighbourExploreFactor = neighbourExploreFactor;
    }

    private static final class Solution {

        private Deque<PuzzleMap> path;

        int size() {
            return path == null ? Integer.MAX_VALUE : path.size();
        }

        Solution copy(Solution other) {
            path = new ArrayDeque<>(other.path);
            return this;
        }

        Solution addFirst(PuzzleMap puzzle) {
            if (path == null) {
                path = new ArrayDeque<>();
            }
            path.addFirst(puzzle);
            return this;
        }
    }

    private static final class Context {
        private final ConcurrentMap<PuzzleMap, Solution> history = new ConcurrentHashMap<>();
    }

    private static final class PuzzleComparator implements Comparator<PuzzleMap> {

        private final HeuristicAlgorithm algorithm;

        private PuzzleComparator(HeuristicAlgorithm algorithm) {
            this.algorithm = algorithm;
        }

        @Override
        public int compare(PuzzleMap lhs, PuzzleMap rhs) {
            double l = lhs.estimatedDistanceToGoal(algorithm);
            double r = rhs.estimatedDistanceToGoal(algorithm);
            return Double.compare(l, r);
        }
    }

    private final class SearchTask extends RecursiveTask<Solution> {

        private final PuzzleMap puzzle;
        private final HeuristicAlgorithm algorithm;
        private final Context context;

        private SearchTask(PuzzleMap puzzle, HeuristicAlgorithm algorithm, Context context) {
            this.puzzle = puzzle;
            this.algorithm = algorithm;
            this.context = context;
        }

        @Override
        protected Solution compute() {

            boolean[] seen = {true};
            Solution solution = context.history.computeIfAbsent(puzzle, k -> {
                seen[0] = false;
                return new Solution();
            });

            if (seen[0]) {
                return solution;
            }
            if (puzzle.isSolved) {
                return solution.addFirst(puzzle);
            }

            List<PuzzleMap> neighbours = new ArrayList<>();

            Set<Piece> pieces = puzzle.getPieces();
            for (Piece piece : pieces) {
                for (MoveStrategy strategy : MoveStrategy.values()) {
                    try {
                        neighbours.add(puzzle.move(piece, strategy));
                    } catch (BadMoveException ignored) {
                        // simply ignore this exception
                    }
                }
            }

            neighbours.sort(new PuzzleComparator(algorithm));

            int threshold = (int) Math.ceil(neighbours.size() * neighbourExploreFactor);
            List<SearchTask> tasks = new ArrayList<>(threshold);

            for (Iterator<PuzzleMap> it = neighbours.iterator(); threshold > 0 && it.hasNext(); threshold--) {
                SearchTask task = new SearchTask(it.next(), algorithm, context);
                tasks.add(task);
                task.fork();
            }

            Solution best = new Solution();
            for (SearchTask task : tasks) {
                Solution subSolution = task.join();
                if (subSolution.size() < best.size()) {
                    best = subSolution;
                }
            }

            if (best.size() != Integer.MAX_VALUE) {
                solution.copy(best).addFirst(puzzle);
            }

            return solution;
        }
    }

    @Override
    public Deque<PuzzleMap> solve(PuzzleMap start, HeuristicAlgorithm algorithm) {
        return solveParallel(start, algorithm, Runtime.getRuntime().availableProcessors());
    }

    @Override
    public Deque<PuzzleMap> solveParallel(PuzzleMap start, HeuristicAlgorithm algorithm, int parallelism) {
        ForkJoinPool fjp = new ForkJoinPool(parallelism);
        SearchTask search = new SearchTask(start, algorithm, new Context());
        Solution solution = fjp.invoke(search);
        fjp.shutdown();
        return solution.path;
    }
}
