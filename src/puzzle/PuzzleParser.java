package puzzle;

import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author ahmad
 */
public class PuzzleParser {

    private static final Map<Character, Piece.Type> pieceTypes = new HashMap<>();

    static {
        Piece.Type[] types = {
                new Piece.Type(false, 'A', 1, 2),
                new Piece.Type(false, 'B', 1, 2),
                new Piece.Type(false, 'C', 1, 1),
                new Piece.Type(true, 'D', 2, 2),
                new Piece.Type(false, 'E', 2, 1),
                new Piece.Type(false, 'F', 1, 1),
                new Piece.Type(false, 'G', 1, 1),
                new Piece.Type(false, 'H', 1, 2),
                new Piece.Type(false, 'I', 1, 2),
                new Piece.Type(false, 'J', 1, 1)};
        for (Piece.Type type : types) {
            pieceTypes.put(type.label, type);
        }
    }

    public static PuzzleMap parse(char[][] map) throws ParseException {
        PuzzleMap.Border border = parseBorder(map);
        Set<String> seen = new HashSet<>();
        Set<Piece> pieces = new HashSet<>();
        for (int y = 1; y < map.length - 1; y++) {
            char[] row = map[y];
            for (int x = 1; x < row.length - 1; x++) {
                if (seen.add(String.valueOf(y) + x)) {
                    char cell = row[x];
                    if (cell != ' ') {
                        Piece.Type type = pieceTypes.get(cell);
                        if (type == null) {
                            throw new ParseException("unknown label: " + cell, y + x);
                        } else {
                            Piece piece = new Piece(type, x - 1, y - 1);
                            for (int h = 0; h < type.height; h++) {
                                for (int w = 0; w < type.width; w++) {
                                    seen.add(String.valueOf(y + h) + (x + w));
                                }
                            }
                            pieces.add(piece);
                        }
                    }
                }
            }
        }
        try {
            return new PuzzleMap("Initial Puzzle", border, pieces);
        } catch (BadMoveException e) {
            throw new ParseException(e.getMessage(), 0);
        }
    }

    private static PuzzleMap.Border parseBorder(char[][] map) {
        char[] top = map[0];
        char[] bottom = map[map.length - 1];
        char[] left = new char[map.length - 2];
        char[] right = new char[map.length - 2];
        int x = 0;
        for (int y = 1; y < map.length - 1; y++) {
            char[] row = map[y];
            left[x] = row[0];
            right[x] = row[row.length - 1];
            ++x;
        }
        return new PuzzleMap.Border(top, left, right, bottom);
    }
}
