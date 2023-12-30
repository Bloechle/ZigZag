package sugarcube.zigzag.legacy;

import sugarcube.zigzag.util.ImageUtil;
import sugarcube.zigzag.ImageBinarizer;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

public class MichalakBinarizer extends ImageBinarizer
{

    public final int contrast;

    public MichalakBinarizer()
    {
        this(32);
    }

    public MichalakBinarizer(int size)
    {
        this(size, 16);
    }

    public MichalakBinarizer(int size, int contrast)
    {
        super(size, contrast);
        this.contrast = contrast;
    }

    public BufferedImage filterImplementation(BufferedImage rgbImage)
    {
        int width = rgbImage.getWidth();
        int height = rgbImage.getHeight();

        BufferedImage grayImage = ImageUtil.convertToGrayLevel(rgbImage, null);
        BufferedImage bgImage = ImageUtil.scaleImage(ImageUtil.decimageImage(grayImage, 1.0 / size), size, false, ImageUtil.createGrayLevelImage(width, height));

        writeDebugImage(bgImage, "BG");

        WritableRaster grayRaster = grayImage.getRaster();
        WritableRaster bgRaster = bgImage.getRaster();


        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
            {
                int value = 255 - contrast * (bgRaster.getSample(x, y, 0) - grayRaster.getSample(x, y, 0));
                grayRaster.setSample(x, y, 0, value < 0 ? 0 : value > 255 ? 255 : value);
            }

        int otsuThreshold = ImageUtil.computeOtsuThreshold(ImageUtil.histogram(grayRaster, 0));

        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                grayRaster.setSample(x, y, 0, grayRaster.getSample(x, y, 0) < otsuThreshold ? 0 : 255);

        return grayImage;
    }

}
