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

        volatile Node parent;
        volatile double g = Integer.MAX_VALUE;
        volatile double epoch = 1;
        volatile boolean closed;

        Node(PuzzleMap puzzle, HeuristicAlgorithm algorithm) {
            this.puzzle = puzzle;
            h = puzzle.estimatedDistanceToGoal(algorithm);
        }

        double f() {
            return h * epoch + g;
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

    private List<QueueCommand> processNeighbour(Node current, Node neighbour,
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

    private Stream<QueueCommand> processNeighbours(Map<PuzzleMap, Node> cache, Node node,
                                                   HeuristicAlgorithm algorithm, double epoch) {
        return node.neighbours()
                .flatMap(puzzle -> {
                    Node neighbour = cache.computeIfAbsent(puzzle, k -> new Node(puzzle, algorithm));
                    return processNeighbour(node, neighbour, algorithm, epoch).stream();
                });
    }

    private void executeQueueCommands(Queue<Node> queue, Stream<QueueCommand> commands) {
        commands.sorted().distinct().forEach(command -> command.execute(queue));
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
            Stream<QueueCommand> commands = processNeighbours(cache, node, algorithm, epoch);
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

                final double e = epoch;

                List<QueueCommand> commands = fjp.submit(() ->
                        IntStream.range(0, selection.size())
                                .parallel()
                                .mapToObj(i -> {
                                    Node node = selection.get(i);
                                    double t = e + (i * algorithm.epsilon);
                                    return processNeighbours(cache, node, algorithm, t);
                                })
                                .flatMap(Function.identity())
                                .collect(Collectors.toList())).get();

                executeQueueCommands(open, commands.stream());

                epoch += (selection.size() * algorithm.epsilon);
            }

            return null;
        } finally {
            fjp.shutdown();
        }
    }

}
