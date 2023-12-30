package sugarcube.zigzag.legacy;

import sugarcube.zigzag.util.ImageUtil;
import sugarcube.zigzag.ImageFilter;

import java.awt.image.BufferedImage;

public class GrayLevelFilter extends ImageFilter
{

    public GrayLevelFilter()
    {
        super();
    }

    public BufferedImage filterImplementation(BufferedImage rgbImage)
    {
        return ImageUtil.convertToGrayLevel(rgbImage, null);
    }
}