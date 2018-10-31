package puzzle;

import java.util.*;

/**
 * @author ahmad
 */
public final class PuzzleMap {

    static final class Border {

        private enum Position {
            TOP, BOTTOM, LEFT, RIGHT
        }

        static final char EXIT_LABEL = 'Z';

        private final char[][] chars = new char[Position.values().length][];
        private final List<int[]> exitPositions = new ArrayList<>();

        private final int width;
        private final int height;

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

            chars[Position.TOP.ordinal()] = top;
            chars[Position.BOTTOM.ordinal()] = bottom;
            chars[Position.LEFT.ordinal()] = left;
            chars[Position.RIGHT.ordinal()] = right;

            width = top.length;
            height = left.length + 2;

            for (int x = 0; x < width; x++) {
                if (isExit(x, Position.TOP)) {
                    exitPositions.add(new int[]{x - 1, -1});
                }
                if (isExit(x, Position.BOTTOM)) {
                    exitPositions.add(new int[]{x - 1, height - 2});
                }
            }
            for (int y = 0; y < height - 2; y++) {
                if (isExit(y, Position.LEFT)) {
                    exitPositions.add(new int[]{-1, y});
                }
                if (isExit(y, Position.RIGHT)) {
                    exitPositions.add(new int[]{width - 2, y});
                }
            }
        }

        private char[] at(Position position) {
            return chars[position.ordinal()];
        }

        private boolean isExit(int i, Position position) {
            char[] border = chars[position.ordinal()];
            return i > 0 && i < border.length && border[i] == EXIT_LABEL;
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
        if (y >= 0 && y < len) {
            return y;
        }
        Border.Position position = y < 0 ? Border.Position.TOP : Border.Position.BOTTOM;
        if (piece.type.isMain) {
            for (int x = 0; x < piece.type.width; x++) {
                if (!border.isExit(piece.x + x + 1, position)) {
                    throw BadMoveException.hitBorder();
                }
            }
            return -1;
        }
        throw BadMoveException.hitBorder();
    }

    private int columnIndex(Piece piece, int deltaX, int len) throws BadMoveException {
        int x = piece.x + deltaX;
        if (x >= 0 && x < len) {
            return x;
        }
        Border.Position position = x < 0 ? Border.Position.LEFT : Border.Position.RIGHT;
        if (piece.type.isMain) {
            for (int y = 0; y < piece.type.height; y++) {
                if (!border.isExit(piece.y + y, position)) {
                    throw BadMoveException.hitBorder();
                }
            }
            return -1;
        }
        throw BadMoveException.hitBorder();
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
        sb.append(border.at(Border.Position.TOP)).append('\n');
        for (int i = 0; i < puzzle.length; i++) {
            sb.append(border.at(Border.Position.LEFT)[i]);
            Character[] row = puzzle[i];
            for (Character cell : row) {
                sb.append(cell == null ? ' ' : cell);
            }
            sb.append(border.at(Border.Position.RIGHT)[i]);
            sb.append('\n');
        }
        sb.append(border.at(Border.Position.BOTTOM));
        return sb.toString();
    }

    Set<Piece> movablePieces() {
        Set<Piece> result = new HashSet<>();
        for (int y = 0; y < puzzle.length; y++) {
            Character[] row = puzzle[y];
            for (int x = 0; x < row.length; x++) {
                Character cell = row[x];
                if (cell == null) {
                    findPieceAt(x - 1, y, result);
                    findPieceAt(x + 1, y, result);
                    findPieceAt(x, y - 1, result);
                    findPieceAt(x, y + 1, result);
                }
            }
        }
        for (int[] position : border.exitPositions) {
            int x = position[0];
            int y = position[1];
            findPieceAt(x - 1, y, result);
            findPieceAt(x + 1, y, result);
            findPieceAt(x, y - 1, result);
            findPieceAt(x, y + 1, result);
        }
        return result;
    }

    private void findPieceAt(int x, int y, Set<Piece> result) {
        if (y < 0 || y >= puzzle.length) return;
        if (x < 0 || x >= puzzle[0].length) return;

        Character label = puzzle[y][x];
        if (label == null) return;

        for (Piece piece : pieces) {
            if (piece.type.label == label) {
                for (int w = 0; w < piece.type.width; w++) {
                    if (piece.x + w == x) {
                        for (int h = 0; h < piece.type.height; h++) {
                            if (piece.y + h == y) {
                                result.add(piece);
                                return;
                            }
                        }
                    }
                }
            }
        }

        throw new IllegalStateException();
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
