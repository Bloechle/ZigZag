package sugarcube.zigzag.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.BitSet;
import java.util.LinkedList;

public class ConnectedComponents extends LinkedList<ConnectedComponents.CCPixel>
{
    static class CCPixel
    {
        int x, y;
    }

    static class CCBox
    {
        int minX, minY, maxX, maxY, size = 0;

        public CCBox(int x, int y)
        {
            minX = maxX = x;
            minY = maxY = y;
        }

        public int maxSide()
        {
            return maxX - minX > maxY - minY ? maxX - minX + 1 : maxY - minY + 1;
        }

        public int minSide()
        {
            return maxX - minX < maxY - minY ? maxX - minX + 1 : maxY - minY + 1;
        }

        public int area()
        {
            return (maxX - minX + 1) * (maxY - minY + 1);
        }

        public CCBox include(int x, int y)
        {
            size++;
            if (x < minX)
                minX = x;
            else if (x > maxX)
                maxX = x;

            if (y < minY)
                minY = y;
            else if (y > maxY)
                maxY = y;

            return this;
        }

    }

    private static final int[] DX8 =
            {0, 1, 0, -1, 1, 1, -1, -1};
    private static final int[] DY8 =
            {1, 0, -1, 0, 1, -1, 1, -1};

    private LinkedList<CCPixel> cache = new LinkedList<>();

    public ConnectedComponents()
    {
    }

    public void add(int x, int y)
    {
        CCPixel p = cache.isEmpty() ? new CCPixel() : cache.remove();
        p.x = x;
        p.y = y;
        this.add(p);
    }

    public CCPixel remove()
    {
        CCPixel p = super.remove();
        cache.add(p);
        return p;
    }

    public static BufferedImage cleanCC(BufferedImage img, File debugFile)
    {
        int minDelta = 10;
        int maxDelta = 100;

        WritableRaster raster = img.getRaster();
        int width = img.getWidth();
        int height = img.getHeight();

        int x, y, px, py, i, rgb;

        BitSet cc = new BitSet(width * height);
        BitSet binary = new BitSet(width * height);

        ConnectedComponents stack = new ConnectedComponents();

        BufferedImage debugImage = null;
        if (debugFile != null)
        {
            debugImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = (Graphics2D) debugImage.getGraphics();
            g.setPaint(Color.WHITE);
            g.fillRect(0, 0, width, height);
        }

        for (y = 0; y < height; y++)
            for (x = 0; x < width; x++)
            {
                if (!cc.get(x + y * width))
                {
                    if (raster.getSample(x, y, 0) <= 128)
                    {
                        stack.add(x, y);
                        cc.set(x + y * width);
                        rgb = (int) (Math.random() * 0xffffff) & 0xff00ff;

                        CCBox box = new CCBox(x, y);
                        while (!stack.isEmpty())
                        {
                            CCPixel p = stack.remove();
                            box.include(p.x, p.y);

                            if (debugImage != null)
                                debugImage.setRGB(p.x, p.y, rgb);

                            for (i = 0; i < DX8.length; i++)
                                if ((px = p.x + DX8[i]) >= 0 && (py = p.y + DY8[i]) >= 0 && px < width && py < height && !cc.get(px + py * width))
                                    if (raster.getSample(px, py, 0) <= 128)
                                    {
                                        stack.add(px, py);
                                        cc.set(px + py * width);
                                    }
                        }

                        if (box.size <= 2)
                        {
                            for (py = box.minY; py <= box.maxY; py++)
                                for (px = box.minX; px <= box.maxX; px++)
                                {
                                    raster.setSample(px, py, 0, 255);
                                    if (debugImage != null)
                                        debugImage.setRGB(px, py, 0x0000ff);
                                }
                        } else
                        {

//                        for (ty = box.minY; ty <= box.maxY; ty++)
//                            for (tx = box.minX; tx <= box.maxX; tx++)
//                                if (tx == box.minX || tx == box.maxX || ty == box.minY || tx == box.maxX)
//                                    if (debug != null)
//                                        debug.setRGB(tx, ty, 0x00bb00);

                            int side = box.minSide();

                            boolean isSmallCC = side < minDelta;

                            if (isSmallCC)
                                side = box.maxSide();
                            else if (side > maxDelta)
                                side = maxDelta;

                            int dx = isSmallCC ? minDelta : side * 3;
                            int dy = isSmallCC ? minDelta : side / 3;

                            if (dx > maxDelta)
                                dx = maxDelta;
                            if (dy > maxDelta)
                                dy = maxDelta;

                            if (box.area() < width * height / 3)
                                for (py = box.minY - dy; py <= box.maxY + dy; py++)
                                    for (px = box.minX - dx; px <= box.maxX + dx; px++)
                                        if (px >= 0 && py >= 0 && px < width && py < height)
                                        {
                                            binary.set(px + py * width);
                                            if (debugImage != null)
                                                debugImage.setRGB(px, py, 0x00bb00);
                                        }
                        }
                    }
                }
            }

        cc.clear();
        int th = (minDelta + 1) * 2 * (minDelta + 1) * 2 * 2;
        for (y = 0; y < height; y++)
            for (x = 0; x < width; x++)
            {
                if (!cc.get(x + y * width))
                {

                    if (binary.get(x + y * width))
                    {
                        stack.add(x, y);
                        cc.set(x + y * width);

                        CCBox box = new CCBox(x, y);
                        while (!stack.isEmpty())
                        {
                            CCPixel p = stack.remove();
                            box.include(p.x, p.y);

                            for (i = 0; i < DX8.length; i++)
                                if ((px = p.x + DX8[i]) >= 0 && (py = p.y + DY8[i]) >= 0 && px < width && py < height && !cc.get(px + py * width))
                                {
                                    if (binary.get(px + py * width))
                                    {
                                        stack.add(px, py);
                                        cc.set(px + py * width);
                                    }
                                }
                        }

                        if (box.size <= th)
                        {
                            for (py = box.minY; py <= box.maxY; py++)
                                for (px = box.minX; px <= box.maxX; px++)
                                {
                                    raster.setSample(px, py, 0, 255);
                                    if (debugImage != null)
                                        debugImage.setRGB(px, py, 0xff0000);
                                }
                        }
                    }
                }
            }

        if (debugFile != null)
            ImageUtil.writeImage(debugImage, debugFile, "_3_YY.png");
        return debugImage;
    }

}
