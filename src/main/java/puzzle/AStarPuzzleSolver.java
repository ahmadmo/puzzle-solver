package puzzle;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author ahmad
 */
final class AStarPuzzleSolver implements HeuristicPuzzleSolver {

    private static final class Node implements Comparable<Node> {

        final PuzzleMap puzzle;
        final double h;

        Node parent;
        double g = Integer.MAX_VALUE;

        Node(PuzzleMap puzzle, HeuristicAlgorithm algorithm) {
            this.puzzle = puzzle;
            h = puzzle.estimatedDistanceToGoal(algorithm);
        }

        double f() {
            return h + g;
        }

        Stream<PuzzleMap> neighbours() {
            return puzzle.movablePieces().stream()
                    .flatMap(piece -> Arrays.stream(MoveStrategy.values())
                            .map(strategy -> move(piece, strategy))
                            .filter(Objects::nonNull));
        }

        private PuzzleMap move(Piece piece, MoveStrategy strategy) {
            try {
                return puzzle.move(piece, strategy);
            } catch (BadMoveException ignored) {
                return null;
            }
        }

        Deque<PuzzleMap> buildPath() {
            Node node = this;
            Deque<PuzzleMap> path = new ArrayDeque<>();
            while (node != null) {
                path.addFirst(node.puzzle);
                node = node.parent;
            }
            return path;
        }

        @Override
        public int hashCode() {
            return puzzle.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof Node)) return false;
            Node that = (Node) obj;
            return puzzle.equals(that.puzzle);
        }

        @Override
        public int compareTo(Node o) {
            return Double.compare(f(), o.f());
        }
    }

    private static abstract class QueueCommand implements Comparable<QueueCommand> {

        private final Node node;
        private final boolean add;

        QueueCommand(Node node, boolean add) {
            this.node = node;
            this.add = add;
        }

        void execute(Queue<Node> queue) {
            if (add) queue.add(node);
            else queue.remove(node);
        }

        @Override
        public int hashCode() {
            return node.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            QueueCommand that = (QueueCommand) obj;
            return node.equals(that.node);
        }
    }

    private static final class AddNode extends QueueCommand {

        AddNode(Node node) {
            super(node, true);
        }

        @Override
        public int compareTo(QueueCommand o) {
            return o instanceof AddNode ? 0 : 1;
        }
    }

    private static final class RemoveNode extends QueueCommand {

        RemoveNode(Node node) {
            super(node, false);
        }

        @Override
        public int compareTo(QueueCommand o) {
            return o instanceof RemoveNode ? 0 : -1;
        }
    }

    private Stream<QueueCommand> processNeighbour(Node current, Node neighbour, HeuristicAlgorithm algorithm) {
        double cost = current.g + algorithm.epsilon;
        if (cost >= neighbour.g) {
            return Stream.empty();
        }
        neighbour.g = cost;
        neighbour.parent = current;
        return Stream.of(new RemoveNode(neighbour), new AddNode(neighbour));
    }

    private Stream<QueueCommand> processNeighbours(Map<PuzzleMap, Node> cache, Node node,
                                                   HeuristicAlgorithm algorithm) {
        return node.neighbours()
                .flatMap(puzzle -> {
                    Node neighbour = cache.computeIfAbsent(puzzle, k -> new Node(puzzle, algorithm));
                    return processNeighbour(node, neighbour, algorithm);
                });
    }

    private void executeQueueCommands(Queue<Node> queue, Stream<QueueCommand> commands) {
        commands.sorted().distinct().forEach(command -> command.execute(queue));
    }

    @Override
    public Deque<PuzzleMap> solve(PuzzleMap start, HeuristicAlgorithm algorithm) {
        Map<PuzzleMap, Node> cache = new HashMap<>();
        PriorityQueue<Node> open = new PriorityQueue<>();

        Node startNode = new Node(start, algorithm);
        startNode.g = 0;

        cache.put(start, startNode);
        open.add(startNode);

        while (open.size() > 0) {
            Node node = open.poll();
            if (node.puzzle.isSolved) {
                return node.buildPath();
            }
            Stream<QueueCommand> commands = processNeighbours(cache, node, algorithm);
            executeQueueCommands(open, commands);
        }

        return null;
    }

    @Override
    public Deque<PuzzleMap> solveParallel(PuzzleMap start, HeuristicAlgorithm algorithm, int parallelism)
            throws InterruptedException, ExecutionException {

        ConcurrentMap<PuzzleMap, Node> cache = new ConcurrentHashMap<>();
        PriorityQueue<Node> open = new PriorityQueue<>();

        Node startNode = new Node(start, algorithm);
        startNode.g = 0;

        cache.put(start, startNode);
        open.add(startNode);

        ForkJoinPool fjp = new ForkJoinPool(parallelism);

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

                List<QueueCommand> commands = fjp.submit(() ->
                        IntStream.range(0, selection.size())
                                .parallel()
                                .mapToObj(i -> processNeighbours(cache, selection.get(i), algorithm))
                                .flatMap(Function.identity())
                                .collect(Collectors.toList())).get();

                executeQueueCommands(open, commands.stream());
            }

            return null;
        } finally {
            fjp.shutdown();
        }
    }

}
