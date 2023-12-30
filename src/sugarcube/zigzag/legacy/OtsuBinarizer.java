package sugarcube.zigzag.legacy;

import sugarcube.zigzag.util.ImageUtil;
import sugarcube.zigzag.ImageBinarizer;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

public class OtsuBinarizer extends ImageBinarizer
{

    public OtsuBinarizer()
    {
        super(0,100);
    }

    public BufferedImage filterImplementation(BufferedImage rgbImage)
    {
        int width = rgbImage.getWidth();
        int height = rgbImage.getHeight();

        BufferedImage grayImage = ImageUtil.convertToGrayLevel(rgbImage, null);
        WritableRaster grayRaster = grayImage.getRaster();

        int otsuThreshold = ImageUtil.computeOtsuThreshold(ImageUtil.histogram(grayRaster, 0));

        executeInParallel(height, (threadIndex, startY, endY) ->
        {
            for (int y = startY; y < endY; y++)
                for (int x = 0; x < width; x++)
                {
                    grayRaster.setSample(x, y, 0, grayRaster.getSample(x, y, 0) < otsuThreshold ? 0 : 255);
                }
        });

        return grayImage;
    }
}