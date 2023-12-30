package sugarcube.zigzag.legacy;

import sugarcube.zigzag.util.ImageUtil;
import sugarcube.zigzag.ImageBinarizer;

import java.awt.image.BufferedImage;

public class NickBinarizer extends ImageBinarizer
{
    private final double k;

    public NickBinarizer()
    {
        this(DEFAULT_SIZE);
    }

    public NickBinarizer(int size)
    {
        this(size, -0.2f);
    }

    public NickBinarizer(int size, double k)
    {
        super(size, (int)Math.round(k*100));
        this.k = k;
    }

    public BufferedImage filterImplementation(BufferedImage rgbImage)
    {
        int width = rgbImage.getWidth();
        int height = rgbImage.getHeight();

        BufferedImage srcImage = ImageUtil.convertToGrayLevel(rgbImage, null);
        BufferedImage resImage = ImageUtil.createGrayLevelImage(width, height);

        int dv = size / 2;
        int du = size / 2;


        executeInParallel(height, (threadIndex, startY, endY) ->
        {
            for (int y = startY; y < endY; y++)
                for (int x = 0; x < width; x++)
                {
                    double mean = 0;
                    double sqrSum = 0;

                    int value, nbOfPixels = 0;

                    for (int v = 0; v < size; v++)
                        for (int u = 0; u < size; u++)
                        {
                            nbOfPixels++;
                            value = ImageUtil.getValueAt(srcImage, x - u + du, y - v + dv);
                            mean += value;
                        }

                    mean /= nbOfPixels;

                    for (int v = 0; v < size; v++)
                        for (int u = 0; u < size; u++)
                        {
                            value = ImageUtil.getValueAt(srcImage, x - u + du, y - v + dv);
                            sqrSum += value * value;
                        }

                    resImage.getRaster().setSample(x, y, 0, srcImage.getRaster().getSample(x, y, 0) < mean + k * Math.sqrt((sqrSum - mean * mean) / nbOfPixels) ? 0 : 255);
                }
        });

        return resImage;
    }
}
