package puzzle;

/**
 * @author ahmad
 */
enum MoveStrategy {

    UP {
        @Override
        protected Piece move(Piece piece) {
            return piece.moveTo(piece.x, piece.y - 1);
        }
    },
    DOWN {
        @Override
        protected Piece move(Piece piece) {
            return piece.moveTo(piece.x, piece.y + 1);
        }
    },
    LEFT {
        @Override
        protected Piece move(Piece piece) {
            return piece.moveTo(piece.x - 1, piece.y);
        }
    },
    RIGHT {
        @Override
        protected Piece move(Piece piece) {
            return piece.moveTo(piece.x + 1, piece.y);
        }
    };

    protected abstract Piece move(Piece piece);
}
