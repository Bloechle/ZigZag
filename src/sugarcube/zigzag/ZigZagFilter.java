package sugarcube.zigzag;

import sugarcube.zigzag.util.ImageUtil;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.Arrays;

public class ZigZagFilter extends ImageBinarizer
{
    private int historicalWhiteThreshold = 256;

    public ZigZagFilter()
    {
        this(DEFAULT_SIZE);
    }

    public ZigZagFilter(int size)
    {
        this(size, MODE_GRAY_LEVEL);
    }

    public ZigZagFilter(int size, int mode)
    {
        this(size, 100, mode);
    }

    public ZigZagFilter(int size, int percent, int mode)
    {
        super(size, percent, mode);
    }

    public ZigZagFilter setHistoricalWhiteThreshold(int threshold)
    {
        this.historicalWhiteThreshold = threshold;
        return this;
    }

    public BufferedImage filterImplementation(BufferedImage rgbImage)
    {
        int width = rgbImage.getWidth();
        int height = rgbImage.getHeight();

        BufferedImage grayImage = ImageUtil.convertToGrayLevel(rgbImage, null);
        WritableRaster grayRaster = grayImage.getRaster();
        WritableRaster rgbRaster = rgbImage.getRaster();

        boolean isColorMode = mode == MODE_COLOR;

        int[][][] rgbIntegral = new int[isColorMode ? 3 : 1][height][width];
        int[][] grayIntegral = rgbIntegral[0];
        int[][] countIntegral = new int[height][width];

        for (int y = 0; y < height; y++)
            for (int sum = 0, x = 0; x < width; x++)
            {
                sum += grayRaster.getSample(x, y, 0);
                grayIntegral[y][x] = y == 0 ? sum : grayIntegral[y - 1][x] + sum;
            }
        addChronoTime("Integral Gray Image");

        executeInParallel(height, (threadIndex, startY, endY) ->
        {
            int halfSize = size / 2;
            int side = 2 * halfSize + 1;
            int nbOfPixels = side * side;
            int x1, x2, y1, y2, mean, value;

            for (int y = startY; y < endY; y++)
            {
                y1 = y - halfSize - 1;
                y2 = y + halfSize;
                if (y1 < 0)
                    y2 = (y1 = 0) + side;
                else if (y2 >= height)
                    y1 = (y2 = height - 1) - side;

                for (int x = 0; x < width; x++)
                {
                    x1 = x - halfSize - 1;
                    x2 = x + halfSize;
                    if (x1 < 0)
                        x2 = (x1 = 0) + side;
                    else if (x2 >= width)
                        x1 = (x2 = width - 1) - side;

                    //70% for historical document (dibco, nabuco)
                    //100% for wezut OCR

                    mean = percent * (grayIntegral[y2][x2] - grayIntegral[y1][x2] - grayIntegral[y2][x1] + grayIntegral[y1][x1]) / (100 * nbOfPixels);

                    value = grayRaster.getSample(x, y, 0);

                    if (value > historicalWhiteThreshold)
                        value = 0;

                    if (isColorMode)
                        grayRaster.setSample(x, y, 0, value < mean ? 0 : value);
                    else
                    {
                        rgbRaster.setSample(x, y, 0, value < mean ? 0 : value);
                        if (doDebug)
                            for (int band = 1; band < 3; band++)
                                rgbRaster.setSample(x, y, band, value < mean ? 0 : value);
                    }
                }
            }
        });
        writeDebugImage(rgbImage, "Mean");

        addChronoTime("Mean Image");

        if (isColorMode)
        {
            int[] rgb = new int[3];
            int[] rgbSum = new int[3];
            for (int y = 0; y < height; y++)
                for (int count = 0, x = 0; x < width; x++)
                {
                    if (grayRaster.getSample(x, y, 0) > 0)
                    {
                        count++;
                        rgbRaster.getPixel(x, y, rgb);
                        for (int band = 0; band < 3; band++)
                            rgbSum[band] += rgb[band];
                    }
                    countIntegral[y][x] = y == 0 ? count : countIntegral[y - 1][x] + count;
                    for (int band = 0; band < 3; band++)
                        rgbIntegral[band][y][x] = y == 0 ? rgbSum[band] : rgbIntegral[band][y - 1][x] + rgbSum[band];
                }
        } else
            for (int y = 0; y < height; y++)
                for (int graySum = 0, count = 0, x = 0; x < width; x++)
                {
                    if (rgbRaster.getSample(x, y, 0) > 0)
                    {
                        count++;
                        graySum += grayRaster.getSample(x, y, 0);
                    }
                    countIntegral[y][x] = y == 0 ? count : countIntegral[y - 1][x] + count;
                    grayIntegral[y][x] = y == 0 ? graySum : grayIntegral[y - 1][x] + graySum;
                }


        addChronoTime("Integral Image");

        executeInParallel(height, (threadIndex, startY, endY) ->
        {
            int halfSize = size / 2;
            int side = 2 * halfSize + 1;
            int[] rgb = isColorMode ? new int[3] : null;
            int x1, x2, y1, y2, nbOfPixels, mean, value;
            for (int y = startY; y < endY; y++)
            {
                y1 = y - halfSize - 1;
                y2 = y + halfSize;
                if (y1 < 0)
                    y2 = (y1 = 0) + side;
                else if (y2 >= height)
                    y1 = (y2 = height - 1) - side;

                for (int x = 0; x < width; x++)
                {
                    x1 = x - halfSize - 1;
                    x2 = x + halfSize;
                    if (x1 < 0)
                        x2 = (x1 = 0) + side;
                    else if (x2 >= width)
                        x1 = (x2 = width - 1) - side;

                    nbOfPixels = (countIntegral[y2][x2] - countIntegral[y1][x2] - countIntegral[y2][x1] + countIntegral[y1][x1]);

                    if (isColorMode)
                    {
                        rgbRaster.getPixel(x, y, rgb);
                        for (int band = 0; band < 3; band++)
                        {
                            mean = nbOfPixels == 0 ? 0 : (rgbIntegral[band][y2][x2] - rgbIntegral[band][y1][x2] - rgbIntegral[band][y2][x1] + rgbIntegral[band][y1][x1]) / nbOfPixels;
                            rgb[band] = rgb[band] >= mean ? 255 : rgb[band] * 256 / mean;
                            if (doDebug)
                                rgb[band] = mean;
                        }
                        rgbRaster.setPixel(x, y, rgb);
                        grayRaster.setSample(x, y, 0, Math.min(Math.min(rgb[0], rgb[1]), rgb[2]));
//                        grayRaster.setSample(x, y, 0, (rgb[0]+rgb[1]+rgb[2])/3);
                    } else
                    {
                        mean = nbOfPixels == 0 ? 0 : (grayIntegral[y2][x2] - grayIntegral[y1][x2] - grayIntegral[y2][x1] + grayIntegral[y1][x1]) / nbOfPixels;
                        value = grayRaster.getSample(x, y, 0);

                        grayRaster.setSample(x, y, 0, value >= mean ? 255 : value * 256 / mean);

                        if (doDebug)
                            for (int band = 0; band < 3; band++)
                                rgbRaster.setSample(x, y, band, mean);
                    }

                }
            }
        });

        addChronoTime("Foreground Generation (" + size + "px)");

        writeDebugImage(grayImage, "FG");
        writeDebugImage(rgbImage, "BG");

        if (mode == MODE_COLOR)
            return rgbImage;

        int otsuThreshold = isBinaryMode() ? Math.min(250, ImageUtil.computeOtsuThreshold(ImageUtil.histogram(grayRaster, 0, 10))) : 250;

        addChronoTime("Otsu Computation (" + otsuThreshold + ")");

        return binarizeImage(grayImage, false, (x, y, value) -> value < otsuThreshold ? (mode == MODE_GRAY_LEVEL ? value : 0) : 255);
    }

    public static void main(String... args)
    {
        System.out.println(Arrays.toString(args));
        if (args.length == 5)
        {
            int size = Integer.parseInt(args[0]);
            int percent = Integer.parseInt(args[1]);
            int mode = Integer.parseInt(args[2]);
            new ZigZagFilter(size, percent, mode).filter(args[3], args[4]);
        }
    }

}