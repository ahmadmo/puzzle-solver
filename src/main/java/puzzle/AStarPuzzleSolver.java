package puzzle;

import java.util.*;
import java.util.concurrent.*;

/**
 * @author ahmad
 */
final class AStarPuzzleSolver extends AbstractHeuristicPuzzleSolver {

    AStarPuzzleSolver() {
    }

    @Override
    List<QueueCommand> processNeighbour(Node current, Node neighbour,
                                        HeuristicAlgorithm algorithm, double epoch) {
        double cost = current.g + algorithm.epsilon;
        boolean notOpen = false;
        List<QueueCommand> commands = new ArrayList<>(2);
        if (cost < neighbour.g) {
            neighbour.closed = false;
            notOpen = true;
            commands.add(new RemoveNode(neighbour));
        }
        if (notOpen && !neighbour.closed) {
            neighbour.g = cost;
            neighbour.epoch = epoch;
            neighbour.parent = current;
            commands.add(new AddNode(neighbour));
        }
        return commands;
    }

    @Override
    public Deque<PuzzleMap> solve(PuzzleMap start, HeuristicAlgorithm algorithm) {
        Map<PuzzleMap, Node> cache = new HashMap<>();
        PriorityQueue<Node> open = new PriorityQueue<>();
        double epoch = 1;

        Node startNode = new Node(start, algorithm);
        startNode.g = 0;
        startNode.epoch = epoch;

        cache.put(start, startNode);
        open.add(startNode);

        while (open.size() > 0) {
            Node node = open.poll();
            if (node.puzzle.isSolved) {
                return node.buildPath();
            }
            node.closed = true;
            List<QueueCommand> commands = processNeighbours(cache, node, algorithm, epoch);
            executeQueueCommands(open, commands);
            epoch += algorithm.epsilon;
        }

        return null;
    }

    @Override
    public Deque<PuzzleMap> solveParallel(PuzzleMap start, HeuristicAlgorithm algorithm, int parallelism)
            throws InterruptedException, ExecutionException {

        ConcurrentMap<PuzzleMap, Node> cache = new ConcurrentHashMap<>();
        PriorityQueue<Node> open = new PriorityQueue<>();
        double epoch = 1;

        Node startNode = new Node(start, algorithm);
        startNode.g = 0;
        startNode.epoch = epoch;

        cache.put(start, startNode);
        open.add(startNode);

        ExecutorService executorService = Executors.newFixedThreadPool(parallelism);

        try {
            while (open.size() > 0) {
                List<Node> selection = new ArrayList<>(parallelism);
                for (int i = 0; i < parallelism && open.size() > 0; i++) {
                    Node node = open.poll();
                    if (node.puzzle.isSolved) {
                        return node.buildPath();
                    }
                    selection.add(node);
                }

                List<Callable<List<QueueCommand>>> tasks = new ArrayList<>();
                for (int i = 0; i < selection.size(); i++) {
                    Node node = selection.get(i);
                    double t = epoch + (i * algorithm.epsilon);
                    tasks.add(() -> processNeighbours(cache, node, algorithm, t));
                }

                List<QueueCommand> commands = new ArrayList<>();
                List<Future<List<QueueCommand>>> futures = executorService.invokeAll(tasks);
                for (Future<List<QueueCommand>> future : futures) {
                    commands.addAll(future.get());
                }
                executeQueueCommands(open, commands);

                epoch += (selection.size() * algorithm.epsilon);
            }

            return null;
        } finally {
            executorService.shutdown();
        }
    }

}
