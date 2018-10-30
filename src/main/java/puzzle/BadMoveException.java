package puzzle;

/**
 * @author ahmad
 */
final class BadMoveException extends Exception {

    private BadMoveException(String message) {
        super(message);
    }

    static BadMoveException overlap() {
        return new BadMoveException("piece overlap occurred.");
    }

    static BadMoveException hitBorder() {
        return new BadMoveException("piece hit the border.");
    }
}
