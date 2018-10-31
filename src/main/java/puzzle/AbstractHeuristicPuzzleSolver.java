package puzzle;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author ahmad
 */
abstract class AbstractHeuristicPuzzleSolver implements HeuristicPuzzleSolver {

    static final class Node implements Comparable<Node> {

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

    static abstract class QueueCommand implements Comparable<QueueCommand> {

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

    static final class AddNode extends QueueCommand {

        AddNode(Node node) {
            super(node, true);
        }

        @Override
        public int compareTo(QueueCommand o) {
            return o instanceof AddNode ? 0 : 1;
        }
    }

    static final class RemoveNode extends QueueCommand {

        RemoveNode(Node node) {
            super(node, false);
        }

        @Override
        public int compareTo(QueueCommand o) {
            return o instanceof RemoveNode ? 0 : -1;
        }
    }

    abstract List<QueueCommand> processNeighbour(Node current, Node neighbour,
                                                 HeuristicAlgorithm algorithm, double epoch);

    List<QueueCommand> processNeighbours(Map<PuzzleMap, Node> cache, Node node,
                                         HeuristicAlgorithm algorithm, double epoch) {
        return node.neighbours()
                .flatMap(puzzle -> {
                    Node neighbour = cache.computeIfAbsent(puzzle, k -> new Node(puzzle, algorithm));
                    return processNeighbour(node, neighbour, algorithm, epoch).stream();
                })
                .collect(Collectors.toList());
    }

    void executeQueueCommands(Queue<Node> queue, List<QueueCommand> commands) {
        commands.stream().sorted().distinct().forEach(command -> command.execute(queue));
    }
}
