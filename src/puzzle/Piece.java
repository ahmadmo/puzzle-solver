package puzzle;

import java.util.Objects;

/**
 * @author ahmad
 */
final class Piece {

    static final class Type {

        final boolean isMain;
        final char label;
        final int width;
        final int height;

        Type(boolean isMain, char label, int width, int height) {
            this.isMain = isMain;
            this.label = label;
            this.width = width;
            this.height = height;
        }

        @Override
        public int hashCode() {
            return Objects.hash(isMain, label, width, height);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof Type)) return false;
            Type that = (Type) obj;
            return isMain == that.isMain &&
                    label == that.label &&
                    width == that.width &&
                    height == that.height;
        }

        @Override
        public String toString() {
            return label + "(" + width + "x" + height + ")" + (isMain ? "*" : "");
        }
    }

    final Type type;
    final int x;
    final int y;

    Piece(Type type, int x, int y) {
        this.type = type;
        this.x = x;
        this.y = y;
    }

    Piece moveTo(int x, int y) {
        return new Piece(type, x, y);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, x, y);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof Piece)) return false;
        Piece that = (Piece) obj;
        return type.equals(that.type) &&
                x == that.x && y == that.y;
    }

    @Override
    public String toString() {
        return type.toString() + "@[" + x + ", " + y + "]";
    }
}
