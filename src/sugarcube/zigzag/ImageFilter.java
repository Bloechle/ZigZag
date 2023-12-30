package sugarcube.zigzag;

import sugarcube.zigzag.util.Chronometer;
import sugarcube.zigzag.util.ImageUtil;
import sugarcube.zigzag.util.SliceProcessThreadPool;
import sugarcube.zigzag.util.SliceProcessable;
import sugarcube.zigzag.legacy.BradleyBinarizer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;

public abstract class ImageFilter
{

    public static int DEFAULT_NB_OF_THREADS = 10;

    public interface BatchCallback
    {
        void imageProcessed(File file);
    }

    public interface ImageFunction
    {
        int computeValue(int x, int y, int value);
    }

    protected final int nbOfThreads;
    protected boolean doDebug, doPrintTimes;
    private final String debugSuffix;
    private File inputFile;
    private final Chronometer chronometer;
    private SliceProcessThreadPool threadPool;


    public ImageFilter()
    {
        this(DEFAULT_NB_OF_THREADS);
    }

    public ImageFilter(int nbOfThreads)
    {
        this.nbOfThreads = nbOfThreads;
        chronometer = new Chronometer(getName());
        debugSuffix = "_" + getName().substring(0, 4);
    }

    public ImageFilter doDebug(boolean doDebug)
    {
        this.doDebug = doDebug;
        this.doPrintTimes = true;
        return this;
    }

    public ImageFilter doPrintTimes(boolean doPrintTimes)
    {
        this.doPrintTimes = doPrintTimes;
        return this;
    }

    public SliceProcessThreadPool threadPool()
    {
        return threadPool;
    }

    public Chronometer chronometer()
    {
        return chronometer;
    }

    public void addChronoTime(String text)
    {
        chronometer.addTime(text);
    }

    public String getName()
    {
        return getClass().getSimpleName();
    }

    public String customName()
    {
        return getName();
    }

    public File debugFile()
    {
        return doDebug ? inputFile : null;
    }

    public BufferedImage filter(BufferedImage image)
    {
        threadPool = new SliceProcessThreadPool(nbOfThreads);
        chronometer.reset(inputFile.getName() + " (" + nbOfThreads + " threads)");
        try
        {
            image = filterImplementation(ImageUtil.convertToRGBIfNeeded(image));
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        chronometer.stop(inputFile);
        if (doPrintTimes)
            chronometer.printTimes();
        threadPool.kill();
        return image;
    }

    public abstract BufferedImage filterImplementation(BufferedImage image);

    public void writeDebugImage(BufferedImage image, String suffix)
    {
        if (doDebug)
            ImageUtil.writeImage(image, debugFile(), debugSuffix + "_" + suffix + ".png");
    }

    public BufferedImage filter(File inputFile)
    {
        try
        {
            if (!inputFile.exists())
                System.out.println("file not found: " + inputFile.getPath());
            else
                return filter(ImageIO.read(this.inputFile = inputFile));

        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public void filter(String inputFilePath, String outputFilePath)
    {
        filter(new File(inputFilePath), new File(outputFilePath));
    }

    public void filter(File inputFile, File outputFile)
    {
        try
        {
            if (!inputFile.exists())
                System.out.println("file not found: " + inputFile.getPath());
            else
            {
                if (outputFile == null)
                {
                    String suffix = debugSuffix + ".png";
                    outputFile = new File(inputFile.getPath().replace(".png", suffix).replace(".jpg", suffix).replace(".bmp", suffix));
                }
                outputFile.getParentFile().mkdirs();
                ImageUtil.writeImage(filter(inputFile), outputFile);
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void executeInParallel(int size, SliceProcessable runnable)
    {
        (threadPool == null ? SliceProcessThreadPool.EMPTY_THREAD_POOL : threadPool).execute(size, runnable);
    }

    public void executeInParallel(int size, boolean doSlice, SliceProcessable runnable)
    {
        (threadPool == null ? SliceProcessThreadPool.EMPTY_THREAD_POOL : threadPool).execute(size, doSlice, runnable);
    }

    public void dispose()
    {

    }

    public void batchProcess(File inFolder, File outFolder, BatchCallback callback, boolean doSkipGroundtruth, String... imageExtensions)
    {
        outFolder.mkdirs();
        System.out.println(getName() + " batch processing " + inFolder.getPath() + " -> " + outFolder.getPath());
        File[] files = imageExtensions.length == 0 ? ImageUtil.listImageFiles(inFolder) : ImageUtil.listFiles(inFolder, imageExtensions);
        for (int i = 0; i < files.length; i++)
        {
            File inFile = files[i];
            if (doSkipGroundtruth && (inFile.getName().contains("_.")))
                continue;
            File outFile = new File(outFolder, ImageUtil.removeExtension(inFile.getName()) + ".png");
            filter(inFile, outFile);
            System.out.println((i + 1) + "/" + files.length + " " + inFile.getName() + " (" + chronometer.elapsedTime() + " ms)");
            if (callback != null)
                callback.imageProcessed(outFile);
        }
        chronometer.writeProcessingTimes(new File(outFolder, customName() + ".csv"));
        System.out.println(customName() + " batch processing done");
    }

    public void applyFunction(WritableRaster grayRaster, ImageFunction function)
    {
        executeInParallel(grayRaster.getHeight(), (threadIndex, startY, endY) ->
        {
            for (int y = startY; y < endY; y++)
                for (int x = 0; x < grayRaster.getWidth(); x++)
                    grayRaster.setSample(x, y, 0, function.computeValue(x, y, grayRaster.getSample(x, y, 0)));
        });
    }

    public static void testFilter(ImageFilter imageFilter, String path)
    {
        ImageUtil.deleteFiles(new File(path), "YinY", "ZigZ", "Bern", "Brad", "Mich", "BigB");
        for (File file : ImageUtil.listImageFiles(new File(path)))
            imageFilter.filter(file, null);
    }

    public static void main(String... args)
    {
        testFilter(new BradleyBinarizer(20).doDebug(false).doPrintTimes(true), ImageUtil.getDesktopPath() + "/ZigZag/Test/Hard/");
    }
}
