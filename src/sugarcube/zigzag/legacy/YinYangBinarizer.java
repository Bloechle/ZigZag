package sugarcube.zigzag.legacy;


import sugarcube.zigzag.util.Box2D;
import sugarcube.zigzag.util.ImageUtil;
import sugarcube.zigzag.ImageBinarizer;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.Arrays;

public class YinYangBinarizer extends ImageBinarizer
{
    private static final int[][] GAUSSIAN_MASK = new int[][]
            {
                    {1, 4, 6, 4, 1},
                    {4, 16, 24, 16, 4},
                    {6, 24, 36, 24, 6},
                    {4, 16, 24, 16, 4},
                    {1, 4, 6, 4, 1}};
    private final int pixelSkip = 24;
    private BufferedImage rgbImage, grayImage;
    private WritableRaster rgbRaster, grayRaster;
    private int width, height, gridSize, nbOfRows, nbOfCols;
    private int[][][] grid;

    public YinYangBinarizer()
    {
        this(MODE_BINARY);
    }

    public YinYangBinarizer(int mode)
    {
        super(64, 100, mode);
    }

    public BufferedImage filterImplementation(BufferedImage image)
    {
        width = image.getWidth();
        height = image.getHeight();

        rgbImage = image;
        rgbRaster = image.getRaster();

        grayImage = ImageUtil.convertToGrayLevel(image, null);
        grayRaster = grayImage.getRaster();

        addChronoTime("Gray level conversion");

        gridSize = size / 8; // 64/8 -> 8
        int halfSize = size / 2; // 64/2 -> 32

        nbOfRows = (int) Math.ceil(height / (double) gridSize);
        nbOfCols = (int) Math.ceil(width / (double) gridSize);

        this.grid = new int[nbOfRows][nbOfCols][3];

        int by5 = 5;
        int area = by5 * by5;
        int srcLumMin = 255 * area;

        //computes luminosity min max over small masks to mitigate salt & pepper
        for (int yBox = 0; yBox < height - by5; yBox += by5)
            for (int xBox = 0; xBox < width - by5; xBox += by5)
            {
                int lum = 0;
                for (int y = yBox; y < yBox + by5; y++)
                    for (int x = xBox; x < xBox + by5; x++)
                        lum += grayRaster.getSample(x, y, 0);

                if (lum < srcLumMin)
                    srcLumMin = lum;
            }
        srcLumMin /= area;

        int srcLumMin_ = srcLumMin;

        executeInParallel(nbOfRows, (threadIndex, startY, endY) ->
        {
            for (int y = startY; y < endY; y++)
                for (int x = 0; x < nbOfCols; x++)
                    grid[y][x] = computeRGBGridDot(x * gridSize, y * gridSize, halfSize, srcLumMin_);
        });

        if (doDebug)
        {
            int shift = 0;
            BufferedImage gridDebugImage = new BufferedImage(size + 1, size + 1, BufferedImage.TYPE_BYTE_GRAY);
            for (int y = 0; y < gridDebugImage.getHeight(); y++)
                for (int x = 0; x < gridDebugImage.getWidth(); x++)
                    gridDebugImage.getRaster().setSample(x, y, 0, shift++ % pixelSkip == 0 ? 0 : 255);
//            writeDebugImage(gridDebugImage, "Grid");
        }

        int marginal = 8;
        enhanceWhiteGrid(marginal);
        applyGaussianFilterToGrid(0, 1, 2);
        int bgStdDev = computeGridStandardDeviation();

        if (doDebug)
            writeDebugImage(debugImage(false), "BG");

        subractBackground();

        addChronoTime("Compute background " + size);

        writeDebugImage(rgbImage, "FG");

        normalizeSubtractedGrayImage();

        addChronoTime("Normalize result");

        writeDebugImage(grayImage, "Norm");

        gridSize = size; //64
        int halfSize4 = size * 4 / 2; //64 * 4 => 256

        nbOfRows = (int) Math.ceil(height / (double) gridSize);
        nbOfCols = (int) Math.ceil(width / (double) gridSize);

        grid = new int[nbOfRows][nbOfCols][2];

        int imageOtsu = ImageUtil.computeOtsuThreshold(ImageUtil.histogram(grayRaster, 0, new Box2D(0, width, 0, height), pixelSkip));

        if (bgStdDev <= 2 * marginal)
        {
            for (int ty = 0; ty < nbOfRows; ty++)
                for (int tx = 0; tx < nbOfCols; tx++)
                    grid[ty][tx][0] = imageOtsu;
        } else
        {
            executeInParallel(nbOfRows, (threadIndex, startY, endY) ->
            {
                for (int y = startY; y < endY; y++)
                    for (int x = 0; x < nbOfCols; x++)
                    {
                        Box2D box = new Box2D(x * gridSize, y * gridSize, halfSize4).constrain(width, height);

                        int otsu = ImageUtil.computeOtsuThreshold(ImageUtil.histogram(grayRaster, 0, box, pixelSkip));

                        otsu = otsu - (otsu - 128) / 16;

                        grid[y][x][0] = Math.min(otsu, 250);
                        grid[y][x][1] = ImageUtil.computeStandardDeviation(box, grayRaster, pixelSkip);
                    }
            });

            boolean doLoop = true;
            while (doLoop)
            {
                doLoop = false;
                for (int ty = 0; ty < nbOfRows; ty++)
                    for (int tx = 0; tx < nbOfCols; tx++)
                    {
                        if (grid[ty][tx][1] <= marginal)
                        {
                            doLoop = true;
                            int otsu = 0;
                            int counter = 0;

                            if (tx > 0 && grid[ty][tx - 1][1] > marginal)
                            {
                                counter++;
                                otsu += grid[ty][tx - 1][0];
                            }
                            if (ty > 0 && grid[ty - 1][tx][1] > marginal)
                            {
                                counter++;
                                otsu += grid[ty - 1][tx][0];
                            }
                            if (tx < nbOfCols - 1 && grid[ty][tx + 1][1] > marginal)
                            {
                                counter++;
                                otsu += grid[ty][tx + 1][0];
                            }
                            if (ty < nbOfRows - 1 && grid[ty + 1][tx][1] > marginal)
                            {
                                counter++;
                                otsu += grid[ty + 1][tx][0];
                            }

                            if (counter > 0)
                            {
                                otsu /= counter;
                                grid[ty][tx][0] = otsu;
                                grid[ty][tx][1] = marginal + 1;
                            }
                        }
                    }
            }
        }

        if (doDebug)
            writeDebugImage(debugImage(), "Otsu");

        addChronoTime("Adaptive Otsu");

        return  binarizeImage(grayImage, false,  (x, y, value) -> mode== MODE_BINARY ?  value < thresholdAt(x, y) ? 0 : 255 : value < thresholdAt(x / 2, y / 2) ? 0 : 255);
    }

    private int[] computeRGBGridDot(int cx, int cy, int halfSize, int srcLumMin)
    {
        Box2D box = new Box2D(cx, cy, halfSize).constrain(width, height);
        int maxHistIndex = maxHistogramValueIndex(ImageUtil.histogram(grayRaster, 0, box, pixelSkip), srcLumMin + (250 - srcLumMin) / 16, 255);

        int[] rgb = new int[3];
        int[] mean = new int[3];
        int counter = 0;
        int shift = 0;

        for (int y = box.y0; y < box.y1; y++)
            for (int x = box.x0; x < box.x1; x++)
                if (shift++ % pixelSkip == 0)
                {
                    int lum = grayRaster.getSample(x, y, 0);
                    if (lum >= maxHistIndex - 1 && lum <= maxHistIndex + 1)
                    {
                        counter++;
                        rgbRaster.getPixel(x, y, rgb);
                        for (int i = 0; i < 3; i++)
                            mean[i] += rgb[i];
                    }
                }

        if (counter == 0)
            Arrays.fill(mean, 255);
        else
            divideBy(mean, counter);

        return mean;
    }

    private void enhanceWhiteGrid(int marginal)
    {
        int tooWhite = 255-marginal;
        boolean doLoop = true;

        while (doLoop)
        {
            doLoop = false;
            for (int ty = 0; ty < nbOfRows; ty++)
                for (int tx = 0; tx < nbOfCols; tx++)
                {
                    int lum = mean(grid[ty][tx]);
                    if (lum >= tooWhite)
                    {
                        doLoop = true;
                        int[] rgbMean = new int[3];

                        int counter = 0;

                        if (tx > 0 && mean(grid[ty][tx - 1]) < tooWhite)
                        {
                            counter++;
                            addTo(rgbMean, grid[ty][tx - 1]);
                        }
                        if (ty > 0 && mean(grid[ty - 1][tx]) < tooWhite)
                        {
                            counter++;
                            addTo(rgbMean, grid[ty - 1][tx]);
                        }
                        if (tx < nbOfCols - 1 && mean(grid[ty][tx + 1]) < tooWhite)
                        {
                            counter++;
                            addTo(rgbMean, grid[ty][tx + 1]);
                        }
                        if (ty < nbOfRows - 1 && mean(grid[ty + 1][tx]) < tooWhite)
                        {
                            counter++;
                            addTo(rgbMean, grid[ty + 1][tx]);
                        }

                        if (counter > 0)
                        {
                            divideBy(rgbMean, counter);
                            setTo(grid[ty][tx], mean(rgbMean) >= tooWhite ? new int[]{tooWhite - 1, tooWhite - 1, tooWhite - 1} : rgbMean);
                        }
                    }
                }
        }
    }

    private int computeGridStandardDeviation()
    {
        int bgMean = 0;
        for (int ty = 0; ty < nbOfRows; ty++)
            for (int tx = 0; tx < nbOfCols; tx++)
                bgMean += mean(grid[ty][tx]);

        bgMean /= (nbOfRows * nbOfCols);


        double bgStdDev = 0;
        for (int ty = 0; ty < nbOfRows; ty++)
            for (int tx = 0; tx < nbOfCols; tx++)
            {
                int diff = mean(grid[ty][tx]) - bgMean;
                bgStdDev += diff * diff;
            }

        bgStdDev /= (nbOfRows * nbOfCols);

        return (int) Math.sqrt(bgStdDev);
    }

    private void subractBackground()
    {
        WritableRaster rgbRaster = rgbImage.getRaster();
        WritableRaster grayRaster = grayImage.getRaster();

        int x, y;
        int[] rgb = new int[3];
        int[] rgbBg = new int[3];

        int maxY = nbOfRows - 1;
        int maxX = nbOfCols - 1;

        for (y = 0; y < height; y++)
        {
            for (x = 0; x < width; x++)
            {
                //subtracts background from image
                rgbRaster.getPixel(x, y, rgb);

                //bilinear color interpolation
                float rx = x / (float) gridSize;
                float ry = y / (float) gridSize;

                if (rx > maxX)
                    rx = maxX;
                if (ry > maxY)
                    ry = maxY;

                int tx = (int) rx;
                int ty = (int) ry;

                float dx = rx - tx;
                float dy = ry - ty;

                int[] p0 = grid[ty][tx];
                int[] p1 = grid[ty][tx < maxX ? tx + 1 : maxX];
                int[] p2 = grid[ty < maxY ? ty + 1 : maxY][tx];
                int[] p3 = grid[ty < maxY ? ty + 1 : maxY][tx < maxX ? tx + 1 : maxX];

                for (int i = 0; i < 3; i++)
                    rgbBg[i] = (int) ((p0[i] * (1f - dx) + p1[i] * dx) * (1f - dy) + (p2[i] * (1f - dx) + p3[i] * dx) * dy);

                //we subtract background color
                for (int i = 0; i < 3; i++)
                    if ((rgb[i] = rgb[i] + (255 - rgbBg[i])) > 255)
                        rgb[i] = 255;

                if (doDebug)
                    rgbRaster.setPixel(x, y, rgb);

                int min = 255;
                for (int i = 0; i < 3; i++)
                    if (rgb[i] < min)
                        min = rgb[i];

                //we use the minimum and maximum color components to improve the detection (induced by colored background subtraction)
                grayRaster.setSample(x, y, 0, min);
            }
        }
    }

    private BufferedImage debugImage(boolean rawGridImage)
    {
        int nbOfRows = grid.length;
        int nbOfCols = grid[0].length;
        int maxY = nbOfRows - 1;
        int maxX = nbOfCols - 1;

        int[] rgb = new int[3];

        BufferedImage img;
        WritableRaster raster;

        if (rawGridImage)
        {
            img = new BufferedImage(grid[0].length, grid.length, BufferedImage.TYPE_INT_RGB);
            raster = img.getRaster();
            for (int y = 0; y < img.getHeight(); y++)
                for (int x = 0; x < img.getWidth(); x++)
                {
                    raster.setPixel(x, y, grid[y][x]);
                }
        } else
        {
            img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            raster = img.getRaster();
            for (int y = 0; y < img.getHeight(); y++)
                for (int x = 0; x < img.getWidth(); x++)
                {
                    // bilinear interpolation
                    float rx = x / (float) gridSize;
                    float ry = y / (float) gridSize;

                    if (rx > maxX)
                        rx = maxX;
                    if (ry > maxY)
                        ry = maxY;

                    int tx = (int) rx;
                    int ty = (int) ry;

                    float dx = rx - tx;
                    float dy = ry - ty;

                    int[] p0 = grid[ty][tx];
                    int[] p1 = grid[ty][tx < maxX ? tx + 1 : maxX];
                    int[] p2 = grid[ty < maxY ? ty + 1 : maxY][tx];
                    int[] p3 = grid[ty < maxY ? ty + 1 : maxY][tx < maxX ? tx + 1 : maxX];

                    for (int i = 0; i < 3; i++)
                        rgb[i] = ((int) ((p0[i] * (1f - dx) + p1[i] * dx) * (1f - dy) + (p2[i] * (1f - dx) + p3[i] * dx) * dy));

                    raster.setPixel(x, y, rgb);
                }
        }

        return img;
    }

    private static void addTo(int[] p, int[] q)
    {
        for (int i = 0; i < p.length; i++)
            p[i] += q[i];
    }

    private static void divideBy(int[] p, int value)
    {
        if (value != 0)
            for (int i = 0; i < p.length; i++)
                p[i] /= value;
    }

    private static void setTo(int[] p, int[] q)
    {
        System.arraycopy(q, 0, p, 0, p.length);
    }

    private static int mean(int[] p)
    {
        int mean = 0;
        for (int i = 0; i < p.length; i++)
            mean += p[i];
        return p.length > 0 ? mean / p.length : mean;
    }

    private static int maxHistogramValueIndex(int[] histogram, int fromLumIndex, int toLumIndex)
    {
        //get index value with max occurrence
        int maxHistValue = 0;
        int maxHistIndex = fromLumIndex;

        for (int i = fromLumIndex; i <= toLumIndex; i++)
            if (histogram[i] > maxHistValue)
                maxHistValue = histogram[maxHistIndex = i];
        return maxHistIndex;
    }

    private void applyGaussianFilterToGrid(int... components)
    {
        int nbOfRows = grid.length;
        int nbOfCols = grid[0].length;
        int[][] gaussianGrid = new int[nbOfRows][nbOfCols];

        int[][] mask = GAUSSIAN_MASK;
        int radius = mask.length / 2;

        for (int c : components)
        {
            for (int ty = 0; ty < nbOfRows; ty++)
                for (int tx = 0; tx < nbOfCols; tx++)
                {
                    int counter = 0;
                    int gaussian = 0;
                    for (int y = ty - radius; y <= ty + radius; y++)
                        for (int x = tx - radius; x <= tx + radius; x++)
                            if (y >= 0 && x >= 0 && y < nbOfRows && x < nbOfCols)
                            {
                                int m = mask[y - ty + radius][x - tx + radius];
                                counter += m;
                                gaussian += grid[y][x][c] * m;
                            }

                    gaussian /= counter;
                    gaussianGrid[ty][tx] = gaussian;
                }

            for (int ty = 0; ty < nbOfRows; ty++)
                for (int tx = 0; tx < nbOfCols; tx++)
                    grid[ty][tx][c] = gaussianGrid[ty][tx];
        }
    }


    private void normalizeSubtractedGrayImage()
    {
        int by5 = 5;
        int area = by5 * by5;
        int lumMin = 255 * area;
        int lumMax = 0;
        //computes luminosity min max over small masks to mitigate salt & pepper
        for (int yBox = 0; yBox < height - by5; yBox += by5)
            for (int xBox = 0; xBox < width - by5; xBox += by5)
            {
                int lum = 0;
                for (int y = yBox; y < yBox + by5; y++)
                    for (int x = xBox; x < xBox + by5; x++)
                        lum += grayRaster.getSample(x, y, 0);

                if (lum < lumMin)
                    lumMin = lum;
                if (lum > lumMax)
                    lumMax = lum;
            }

        lumMin /= area;
        lumMax /= area;

        int delta = lumMax - lumMin;
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
            {
                int lum = (grayRaster.getSample(x, y, 0) - lumMin) * 255 / delta;
                grayRaster.setSample(x, y, 0, lum < 0 ? 0 : lum > 255 ? 255 : lum);
            }

    }

    private int thresholdAt(int x, int y)
    {
        //subtracts background from image
        int maxY = grid.length - 1;
        int maxX = grid[0].length - 1;

        //bilinear interpolation
        float rx = x / (float) gridSize;
        float ry = y / (float) gridSize;

        if (rx > maxX)
            rx = maxX;
        if (ry > maxY)
            ry = maxY;

        int tx = (int) rx;
        int ty = (int) ry;

        float dx = rx - tx;
        float dy = ry - ty;

        int p0 = grid[ty][tx][0];
        int p1 = grid[ty][tx < maxX ? tx + 1 : maxX][0];
        int p2 = grid[ty < maxY ? ty + 1 : maxY][tx][0];
        int p3 = grid[ty < maxY ? ty + 1 : maxY][tx < maxX ? tx + 1 : maxX][0];

        return ((int) ((p0 * (1f - dx) + p1 * dx) * (1f - dy) + (p2 * (1f - dx) + p3 * dx) * dy));
    }

    private BufferedImage debugImage()
    {
        WritableRaster raster;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        raster = img.getRaster();
        for (int y = 0; y < img.getHeight(); y++)
            for (int x = 0; x < img.getWidth(); x++)
                raster.setSample(x, y, 0, thresholdAt(x, y));

//        BufferedImage tileImg = new BufferedImage(tiles[0].length, tiles.length, BufferedImage.TYPE_INT_RGB);
//        raster = tileImg.getRaster();
//        for (int y = 0; y < tileImg.getHeight(); y++)
//            for (int x = 0; x < tileImg.getWidth(); x++)
//            {
//                raster.setPixel(x, y, tiles[y][x]);
//            }

        return img;
    }


}