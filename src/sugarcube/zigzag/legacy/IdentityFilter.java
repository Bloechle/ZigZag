package sugarcube.zigzag.legacy;

import sugarcube.zigzag.ImageFilter;

import java.awt.image.BufferedImage;

public class IdentityFilter extends ImageFilter
{

    public IdentityFilter()
    {
        super();
    }

    public BufferedImage filterImplementation(BufferedImage rgbImage)
    {
        return rgbImage;
    }
}