package sugarcube.zigzag.util;


public class Box2D {
    public final int x0, y0, x1, y1;

    public Box2D(int x0, int x1, int y0, int y1) {
        this.x0 = x0;
        this.x1 = x1;
        this.y0 = y0;
        this.y1 = y1;
    }

    public Box2D(int cx, int cy, int radius) {
        this(cx - radius, cx + radius + 1, cy - radius, cy + radius + 1);
    }

    @Override
    public String toString() {
        return String.format("(%d,%d,%d,%d)", x0, y0, x1 - x0, y1 - y0);
    }
}