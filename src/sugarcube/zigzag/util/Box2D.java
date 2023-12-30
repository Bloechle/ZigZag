package sugarcube.zigzag.util;

import java.awt.*;

public class Box2D
{
    //from x0/y0 included to x1/y1 excluded
    public final int x0, y0, x1, y1;

    public Box2D(int x0, int x1, int y0, int y1)
    {
        this.x0 = x0;
        this.x1 = x1;
        this.y0 = y0;
        this.y1 = y1;
    }

    public Box2D(int cx, int cy, int radius)
    {
        x0 = cx - radius;
        x1 = cx + radius + 1;
        y0 = cy - radius;
        y1 = cy + radius + 1;
    }

    public Point[] cornerPoints()
    {
        return new Point[]
                {p0(), p1(), p2(), p3()};
    }

    public Point p0()
    {
        return new Point(x0, y0);
    }

    public Point p1()
    {
        return new Point(x1, y0);
    }

    public Point p2()
    {
        return new Point(x1, y1);
    }

    public Point p3()
    {
        return new Point(x0, y1);
    }

    public int width()
    {
        return x1 - x0;
    }

    public int height()
    {
        return y1 - y0;
    }

    public Box2D constrain(int width, int height)
    {
        return constrainWidth(width).constrainHeight(height);
    }

    public Box2D constrainWidth(int width)
    {
        return x0 < 0 ? new Box2D(0, x1 - x0, y0, y1) : (x1 > width ? new Box2D(width - (x1 - x0), width, y0, y1) : this);
    }

    public Box2D constrainHeight(int height)
    {
        return y0 < 0 ? new Box2D(x0, x1, 0, y1 - y0) : (y1 > height ? new Box2D(x0, x1, height - (y1 - y0), height) : this);
    }

    public boolean contains(int x, int y)
    {
        return x >= x0 && x < x1 && y >= y0 && y < y1;
    }

    public int centerX()
    {
        return (x0 + x1) / 2;
    }

    public int centerY()
    {
        return (y0 + y1) / 2;
    }

    public int sqrDistToCenter(int cx, int cy)
    {
        cx = centerX() - cx;
        cy = centerY() - cy;
        return cx * cx + cy * cy;
    }

    public String toString()
    {
        return "(" + x0 + "," + y0 + "," + width() + "," + height() + ")";
    }

}
