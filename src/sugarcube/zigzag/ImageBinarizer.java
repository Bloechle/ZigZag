package sugarcube.zigzag;

import sugarcube.zigzag.util.ImageUtil;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

public abstract class ImageBinarizer extends ImageFilter
{
    public static final int MODE_BINARY = 0;
    public static final int MODE_BINARY_UPSAMPLED = 1;
    public static final int MODE_BINARY_ANTIALIASED = 2;
    public static final int MODE_GRAY_LEVEL = 3;
    public static final int MODE_COLOR = 4;

    public static int DEFAULT_SIZE = 20;

    protected final int size, percent, mode;

    public ImageBinarizer(int size, int percent)
    {
        this(size, percent, MODE_BINARY, DEFAULT_NB_OF_THREADS);
    }

    public ImageBinarizer(int size, int percent, int mode)
    {
        this(size, percent, mode, DEFAULT_NB_OF_THREADS);
    }

    public ImageBinarizer(int size, int percent, int mode, int nbOfThreads)
    {
        super(nbOfThreads);
        this.size = size;
        this.percent = percent;
        this.mode = mode;
    }

    public BufferedImage binarizeImage(BufferedImage grayImage, boolean gammaCorrection, ImageFunction thresholdFunction)
    {
        if (mode == MODE_BINARY)
        {
            applyFunction(grayImage.getRaster(), thresholdFunction);
            addChronoTime("Image Thresholding");
        } else
        {
            BufferedImage upImage = ImageUtil.scaleImage(grayImage, 2, false, null);
            WritableRaster upRaster = upImage.getRaster();
            addChronoTime("Image Upsampling");


            applyFunction(upRaster, thresholdFunction);
            addChronoTime("Image Thresholding");

            if (mode == MODE_BINARY_UPSAMPLED)
                return upImage;

            ImageUtil.decimateImage(upImage, grayImage, gammaCorrection, threadPool());
            addChronoTime("Image Decimation");
        }
        return grayImage;
    }

    public boolean isBinaryMode()
    {
        switch (mode)
        {
            case MODE_BINARY:
            case MODE_BINARY_UPSAMPLED:
            case MODE_BINARY_ANTIALIASED:
                return true;
        }
        return false;
    }

    public String customName()
    {
        String postfix = "";
        switch (mode)
        {
            case MODE_BINARY:
                postfix = "BW";
                break;
            case MODE_BINARY_UPSAMPLED:
                postfix = "BW-UP";
                break;
            case MODE_BINARY_ANTIALIASED:
                postfix = "BW-AA";
                break;
            case MODE_GRAY_LEVEL:
                postfix = "GL";
                break;
        }
        return getName().replace("Filter", "").replace("Binarizer", "") + size + "_" + percent + postfix;
    }
}
