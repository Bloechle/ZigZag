package sugarcube.zigzag.evaluation;

import sugarcube.zigzag.ImageBinarizer;
import sugarcube.zigzag.ImageFilter;
import sugarcube.zigzag.ZigZagFilter;

public class LetTryMyFilter
{
    public static void main(String... args)
    {

        ImageFilter filter = new ZigZagFilter(30, 100, ImageBinarizer.MODE_BINARY_UPSAMPLED);

        filter.doDebug(true).doPrintTimes(true);

        ImageFilter.testFilter(filter,   "C:/Projects/ZigZag/eval/Ask/");
    }

}
