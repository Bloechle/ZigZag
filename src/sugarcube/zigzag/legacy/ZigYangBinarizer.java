package sugarcube.zigzag.legacy;

import sugarcube.zigzag.util.ImageUtil;
import sugarcube.zigzag.ImageBinarizer;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

public class ZigYangBinarizer extends ImageBinarizer
{

    public ZigYangBinarizer()
    {
        this(DEFAULT_SIZE);
    }

    public ZigYangBinarizer(int size)
    {
        this(size, MODE_BINARY);
    }

    public ZigYangBinarizer(int size, int mode)
    {
        super(size <= 0 ? 24 : size, 100, mode);
    }

    public BufferedImage filterImplementation(BufferedImage rgbImage)
    {
        int width = rgbImage.getWidth();
        int height = rgbImage.getHeight();

        BufferedImage grayImage = ImageUtil.createGrayLevelImage(width, height);
        WritableRaster grayRaster = grayImage.getRaster();
        WritableRaster rgbRaster = rgbImage.getRaster();

        int[][][] rgbIntegral = new int[6][height][width];

        executeInParallel(3, false, (threadBand, min, max) ->
        {
            int[][] integral = rgbIntegral[threadBand];
            int[][] sqrIntegral = rgbIntegral[threadBand + 3];
            for (int y = 0; y < height; y++)
            {
                int value, sum = 0, sqrSum = 0;
                for (int x = 0; x < width; x++)
                {
                    value = rgbRaster.getSample(x, y, threadBand);
                    sum += value;
                    sqrSum += value * value;
                    integral[y][x] = y == 0 ? sum : integral[y - 1][x] + sum;
                    sqrIntegral[y][x] = y == 0 ? sqrSum : sqrIntegral[y - 1][x] + sqrSum;
                }
            }
        });

        addChronoTime("Integral Image");

        executeInParallel(height, (threadIndex, startY, endY) ->
        {
            int halfSize = size / 2;
            int nbOfPixels = 2 * halfSize + 1;
            nbOfPixels *= nbOfPixels;

            int x1, x2, y1, y2;
            for (int y = startY; y < endY; y++)
            {
                for (int x = 0; x < width; x++)
                {
                    x1 = x - halfSize - 1;
                    x2 = x + halfSize;
                    y1 = y - halfSize - 1;
                    y2 = y + halfSize;

                    if (x1 < 0)
                        x1 = -x1;
                    else if (x2 >= width)
                        x2 = width - (x2 - width) - 2;

                    if (y1 < 0)
                        y1 = -y1;
                    else if (y2 >= height)
                        y2 = height - (y2 - height) - 2;

                    int maxSum = 0;
                    int maxBand = 0;
                    int sum;

                    for (int band = 0; band < 3; band++)
                        if ((sum = (rgbIntegral[band][y2][x2] - rgbIntegral[band][y1][x2] - rgbIntegral[band][y2][x1] + rgbIntegral[band][y1][x1])) > maxSum)
                        {
                            maxSum = sum;
                            maxBand = band;
                        }

                    int value = rgbRaster.getSample(x, y, maxBand);
                    int sqrBand = maxBand + 3;

                    // Bradley integral image mean computation
                    double mean = maxSum / (double) nbOfPixels;
                    double sdev = rgbIntegral[sqrBand][y2][x2] - rgbIntegral[sqrBand][y1][x2] - rgbIntegral[sqrBand][y2][x1] + rgbIntegral[sqrBand][y1][x1];

                    sdev = Math.sqrt(sdev / nbOfPixels - mean * mean);

                    // Niblack
                    mean = (mean + 0.5 * sdev);
                    grayRaster.setSample(x, y, 0, value >= mean ? 255 : (mean <= 0 ? 0 : value * 256 / mean));
                }
            }
        });

        addChronoTime("FG Image Generation (" + size + "px)");

        writeDebugImage(grayImage, "FG");

        int otsuThreshold = Math.min((isBinaryMode() ? percent : 120) * ImageUtil.computeOtsuThreshold(ImageUtil.histogram(grayRaster, 0)) / 100, 250);
        addChronoTime("Otsu Computation (" + otsuThreshold + ")");

        return binarizeImage(grayImage, false, (x, y, value) -> value < otsuThreshold ? (mode == MODE_GRAY_LEVEL ? value : 0) : 255);
    }

}
