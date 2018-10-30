package puzzle;

import java.util.*;

/**
 * @author ahmad
 */
public final class PuzzleMap {

    static final class Border {

        static final char EXIT_LABEL = 'Z';

        private final char[] top;
        private final char[] left;
        private final char[] right;
        private final char[] bottom;

        private final int width;
        private final int height;

        private final List<int[]> exitPositions;

        Border(char[] top, char[] left, char[] right, char[] bottom) {
            if (top.length <= 2) {
                throw new IllegalArgumentException("border width must be >= 2");
            }
            if (left.length < 1) {
                throw new IllegalArgumentException("border height must be >= 2");
            }

            if (top.length != bottom.length) {
                throw new IllegalArgumentException("top.length != bottom.length");
            }
            if (left.length != right.length) {
                throw new IllegalArgumentException("left.length != right.length");
            }

            this.top = top;
            this.left = left;
            this.right = right;
            this.bottom = bottom;

            width = top.length;
            height = left.length + 2;

            exitPositions = new ArrayList<>();

            for (int x = 0; x < width; x++) {
                if (isExitTop(x)) {
                    exitPositions.add(new int[]{x - 1, -1});
                }
                if (isExitBottom(x)) {
                    exitPositions.add(new int[]{x - 1, height - 2});
                }
            }
            for (int y = 0; y < height - 2; y++) {
                if (isExitLeft(y)) {
                    exitPositions.add(new int[]{-1, y});
                }
                if (isExitRight(y)) {
                    exitPositions.add(new int[]{width - 2, y});
                }
            }
        }

        private boolean isExitTop(int x) {
            return x > 0 && x < top.length && top[x] == EXIT_LABEL;
        }

        private boolean isExitBottom(int x) {
            return x > 0 && x < bottom.length && bottom[x] == EXIT_LABEL;
        }

        private boolean isExitLeft(int y) {
            return y > 0 && y < left.length && left[y] == EXIT_LABEL;
        }

        private boolean isExitRight(int y) {
            return y > 0 && y < right.length && right[y] == EXIT_LABEL;
        }
    }

    final boolean isSolved;

    private final String title;
    private final Border border;
    private final Set<Piece> pieces;
    private final Character[][] puzzle;
    private final Piece main;

    private String renderedMap;

    PuzzleMap(String title, Border border, Set<Piece> pieces) throws BadMoveException {
        this.border = border;
        this.pieces = pieces;
        validatePieces();
        main = findMain();
        puzzle = buildPuzzle();
        isSolved = checkPuzzleState();
        this.title = isSolved ? title + " ** SOLVED **" : title;
    }

    private void validatePieces() {
        Map<Character, Piece.Type> map = new HashMap<>();
        for (Piece piece : pieces) {
            Piece.Type type = map.get(piece.type.label);
            if (type == null) {
                map.put(piece.type.label, piece.type);
            } else if (!type.equals(piece.type)) {
                throw new IllegalStateException("different pieces with same labels were found.");
            }
        }
    }

    private Piece findMain() {
        Piece p = null;
        for (Piece piece : pieces) {
            if (piece.type.isMain) {
                if (p == null) {
                    p = piece;
                } else {
                    throw new IllegalStateException("more than one main piece found in the puzzle.");
                }
            }
        }
        if (p == null) {
            throw new IllegalStateException("main piece is missing from the puzzle.");
        }
        return p;
    }

    private Character[][] buildPuzzle() throws BadMoveException {
        Character[][] puzzle = new Character[border.height - 2][];
        Arrays.setAll(puzzle, i -> new Character[border.width - 2]);
        for (Piece piece : pieces) {
            for (int h = 0; h < piece.type.height; h++) {
                int rowIndex = rowIndex(piece, h, puzzle.length);
                if (rowIndex >= 0) {
                    Character[] row = puzzle[rowIndex];
                    for (int w = 0; w < piece.type.width; w++) {
                        int columnIndex = columnIndex(piece, w, puzzle[0].length);
                        if (columnIndex >= 0) {
                            if (row[columnIndex] == null) {
                                row[columnIndex] = piece.type.label;
                            } else {
                                throw BadMoveException.overlap();
                            }
                        }
                    }
                }
            }
        }
        return puzzle;
    }

    private int rowIndex(Piece piece, int deltaY, int len) throws BadMoveException {
        int y = piece.y + deltaY;
        if (y < 0) {
            if (piece.type.isMain) {
                for (int x = 0; x < piece.type.width; x++) {
                    if (!border.isExitTop(piece.x + x + 1)) {
                        throw BadMoveException.hitBorder();
                    }
                }
                return -1;
            }
            throw BadMoveException.hitBorder();
        } else if (y >= len) {
            if (piece.type.isMain) {
                for (int x = 0; x < piece.type.width; x++) {
                    if (!border.isExitBottom(piece.x + x + 1)) {
                        throw BadMoveException.hitBorder();
                    }
                }
                return -1;
            }
            throw BadMoveException.hitBorder();
        }
        return y;
    }

    private int columnIndex(Piece piece, int deltaX, int len) throws BadMoveException {
        int x = piece.x + deltaX;
        if (x < 0) {
            if (piece.type.isMain) {
                for (int y = 0; y < piece.type.height; y++) {
                    if (!border.isExitLeft(piece.y + y)) {
                        throw BadMoveException.hitBorder();
                    }
                }
                return -1;
            }
            throw BadMoveException.hitBorder();
        } else if (x >= len) {
            if (piece.type.isMain) {
                for (int y = 0; y < piece.type.height; y++) {
                    if (!border.isExitRight(piece.y + y)) {
                        throw BadMoveException.hitBorder();
                    }
                }
                return -1;
            }
            throw BadMoveException.hitBorder();
        }
        return x;
    }

    private boolean checkPuzzleState() {
        for (Character[] row : puzzle) {
            for (Character cell : row) {
                if (cell != null && cell == main.type.label) {
                    return false;
                }
            }
        }
        pieces.remove(main);
        return true;
    }

    private String renderMap() {
        StringBuilder sb = new StringBuilder();
        sb.append(title).append("\n\n");
        sb.append(border.top).append('\n');
        for (int i = 0; i < puzzle.length; i++) {
            sb.append(border.left[i]);
            Character[] row = puzzle[i];
            for (Character cell : row) {
                sb.append(cell == null ? ' ' : cell);
            }
            sb.append(border.right[i]);
            sb.append('\n');
        }
        sb.append(border.bottom);
        return sb.toString();
    }

    Set<Piece> getPieces() {
        return Collections.unmodifiableSet(pieces);
    }

    double estimatedDistanceToGoal(HeuristicAlgorithm algorithm) {
        double d = 0;
        for (int[] position : border.exitPositions) {
            d += algorithm.heuristic(main.x, main.y, position[0], position[1]);
        }
        return d / border.exitPositions.size();
    }

    PuzzleMap move(Piece piece, MoveStrategy strategy) throws BadMoveException {
        Piece next = strategy.move(piece);
        if (!pieces.contains(piece) || !piece.type.equals(next.type)) {
            throw new IllegalStateException("unexpected piece move.");
        }
        if (pieces.contains(next)) {
            throw BadMoveException.overlap();
        }
        String fromPosition = "[" + piece.x + ", " + piece.y + "]";
        String toPosition = "[" + next.x + ", " + next.y + "]";
        String title = piece.type + " moved from " + fromPosition + " to " + toPosition;
        Set<Piece> newSet = new HashSet<>(pieces);
        newSet.remove(piece);
        newSet.add(next);
        return new PuzzleMap(title, border, newSet);
    }

    @Override
    public int hashCode() {
        return pieces.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof PuzzleMap)) return false;
        PuzzleMap that = (PuzzleMap) obj;
        return pieces.equals(that.pieces);
    }

    @Override
    public String toString() {
        if (renderedMap == null) {
            renderedMap = renderMap();
        }
        return renderedMap;
    }
}
