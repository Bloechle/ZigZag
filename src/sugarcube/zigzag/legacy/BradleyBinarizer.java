package sugarcube.zigzag.legacy;

import sugarcube.zigzag.util.ImageUtil;
import sugarcube.zigzag.ImageBinarizer;

import java.awt.image.BufferedImage;

public class BradleyBinarizer extends ImageBinarizer
{
    public BradleyBinarizer()
    {
        this(DEFAULT_SIZE);
    }
    public BradleyBinarizer(int size)
    {
        this(size, 20);
    }

    public BradleyBinarizer(int size, int percent)
    {
        super(size, percent);
    }

    public BufferedImage filterImplementation(BufferedImage rgbImage)
    {
        int width = rgbImage.getWidth();
        int height = rgbImage.getHeight();

        BufferedImage srcImage = ImageUtil.convertToGrayLevel(rgbImage, null);
        BufferedImage resImage = ImageUtil.createGrayLevelImage(width, height);

        int[][] integralImg = new int[height][width];

        int halfSize = size / 2;
        int nbOfPixels = size * size;

        for (int x = 0; x < width; x++)
            for (int sum = 0, y = 0; y < height; y++)
            {
                sum += srcImage.getRaster().getSample(x, y, 0);
                integralImg[y][x] = x == 0 ? sum : integralImg[y][x - 1] + sum;
            }

        executeInParallel(height, (threadIndex, startY, endY) ->
        {
            for (int y = startY; y < endY; y++)
            {
                for (int x = 0; x < width; x++)
                {

                    int x1 = x - halfSize - 1;
                    int x2 = x + halfSize;
                    int y1 = y - halfSize - 1;
                    int y2 = y + halfSize;

                    int count = nbOfPixels;

                    if (x1 < 0 || y1 < 0 || x2 >= width || y2 >= height)
                    {
                        if (x1 < 0)
                            x1 = 0;
                        if (y1 < 0)
                            y1 = 0;
                        if (x2 >= width)
                            x2 = width - 1;
                        if (y2 >= height)
                            y2 = height - 1;
                        count = (x2 - x1) * (y2 - y1);
                    }

                    int mean = (integralImg[y2][x2] - integralImg[y1][x2] - integralImg[y2][x1] + integralImg[y1][x1]) / count;
                    resImage.getRaster().setSample(x, y, 0, srcImage.getRaster().getSample(x, y, 0) <= (mean * (100 - percent) / 100) ? 0 : 255);
                }
            }
        });

        return resImage;
    }
}
