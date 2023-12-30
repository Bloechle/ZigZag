package sugarcube.zigzag.legacy;

import sugarcube.zigzag.util.ImageUtil;
import sugarcube.zigzag.ImageBinarizer;

import java.awt.image.BufferedImage;

public class BernsenBinarizer extends ImageBinarizer
{
    private final int k;

    public BernsenBinarizer()
    {
        this(DEFAULT_SIZE);
    }

    public BernsenBinarizer(int size)
    {
        this(size, 15);
    }

    public BernsenBinarizer(int size, int k)
    {
        super(size, k);
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
                    int min = 255;
                    int max = 0;

                    for (int v = 0; v < size; v++)
                        for (int u = 0; u < size; u++)
                        {
                            int value = ImageUtil.getValueAt(srcImage, x - u + du, y - v + dv);

                            if (value < min)
                                min = value;
                            if (value > max)
                                max = value;
                        }

                    if (max - min <= k)
                        resImage.getRaster().setSample(x, y, 0, 255);
                    else
                        resImage.getRaster().setSample(x, y, 0, srcImage.getRaster().getSample(x, y, 0) < (max + min) / 2 ? 0 : 255);
                }
        });
        return resImage;
    }
}
