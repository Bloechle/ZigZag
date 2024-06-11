package sugarcube.zigzag.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;

public abstract class ImageFilter {
    public static final int MODE_BINARY = 0;
    public static final int MODE_BINARY_UPSAMPLED = 1;
    public static final int MODE_BINARY_ANTIALIASED = 2;
    public static final int MODE_GRAY_LEVEL = 3;
    public static final int MODE_COLOR = 4;
    public static final int DEFAULT_SIZE = 30;
    public static final int DEFAULT_THREADS = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);

    protected final int size;
    protected final int percent;
    protected final int mode;
    protected boolean losslessEnabled = true;
    protected boolean debugEnabled;
    private final int numThreads;
    private final String debugSuffix = "_ZZ";
    private SliceProcessThreadPool threadPool;
    private File inputFile;
    private long processingTime;


    public interface ImageFunction {
        int computeValue(int x, int y, int value);
    }

    public ImageFilter(int size, int percent, int mode, int numThreads) {
        this.size = size;
        this.percent = percent;
        this.mode = mode;
        this.numThreads = numThreads;
    }

    public ImageFilter(int size, int percent) {
        this(size, percent, MODE_BINARY, DEFAULT_THREADS);
    }

    public ImageFilter(int size, int percent, int mode) {
        this(size, percent, mode, DEFAULT_THREADS);
    }

    public SliceProcessThreadPool getThreadPool() {
        return threadPool;
    }

    public String getName() {
        return getClass().getSimpleName();
    }

    public File getDebugFile() {
        return debugEnabled ? inputFile : null;
    }

    public long getProcessingTime() {
        return processingTime;
    }

    public BufferedImage applyFilter(BufferedImage image) {
        threadPool = new SliceProcessThreadPool(numThreads);
        try {
            long startTime = System.currentTimeMillis();
            image = filterImplementation(ImageUtil.convertToRGBIfNeeded(image));
            processingTime = System.currentTimeMillis() - startTime;
        } catch (Exception e) {
            ImageUtil.printStackTrace(e);
        }
        threadPool.kill();
        return image;
    }

    public abstract BufferedImage filterImplementation(BufferedImage img);

    public void writeDebugImage(BufferedImage img, String suffix) {
        if (debugEnabled) {
            ImageUtil.writeImage(img, getDebugFile(), debugSuffix + "_" + suffix + ".png");
        }
    }

    public BufferedImage applyFilter(File inFile) {
        try {
            if (!inFile.exists()) {
                System.out.println("File not found: " + inFile.getPath());
            } else {
                return applyFilter(ImageIO.read(this.inputFile = inFile));
            }
        } catch (Exception e) {
            ImageUtil.printStackTrace(e);
        }
        return null;
    }

    public void applyFilter(String inPath, String outPath) {
        applyFilter(new File(inPath), new File(outPath));
    }

    public void applyFilter(File inFile, File outFile) {
        try {
            if (!inFile.exists()) {
                System.out.println("File not found: " + inFile.getPath());
            } else {
                if (outFile == null) {
                    String suffix = debugSuffix + (losslessEnabled ? ".png" : ".jpg");
                    outFile = new File(inFile.getPath().replace(".png", suffix).replace(".jpg", suffix).replace(".bmp", suffix));
                }

                if (outFile.getParentFile().mkdirs())
                    System.out.println("Directory created " + outFile.getParentFile());

                ImageUtil.writeImage(applyFilter(inFile), outFile);

                System.out.println("Processed file: " + inFile.getPath() + " in " + processingTime + " ms");
            }
        } catch (Exception e) {
            ImageUtil.printStackTrace(e);
        }
    }

    public void executeInParallel(int size, SliceProcessable runnable) {
        (threadPool == null ? SliceProcessThreadPool.EMPTY_THREAD_POOL : threadPool).execute(size, runnable);
    }

    public void dispose() {
        if (threadPool != null) {
            threadPool.kill();
        }
    }

    public void applyFunction(WritableRaster gRast, ImageFunction fct) {
        executeInParallel(gRast.getHeight(), (threadIndex, startY, endY) -> {
            for (int y = startY; y < endY; y++) {
                for (int x = 0; x < gRast.getWidth(); x++) {
                    gRast.setSample(x, y, 0, fct.computeValue(x, y, gRast.getSample(x, y, 0)));
                }
            }
        });
    }

    public BufferedImage binarizeImage(BufferedImage gImg, boolean gammaCorrection, ImageFunction threshFct) {
        if (mode == MODE_BINARY) {
            applyFunction(gImg.getRaster(), threshFct);
        } else {
            BufferedImage upImage = ImageUtil.scaleImage(gImg, 2, false, null);
            WritableRaster upRaster = upImage.getRaster();
            applyFunction(upRaster, threshFct);
            if (mode == MODE_BINARY_UPSAMPLED) {
                return upImage;
            }
            ImageUtil.decimateImage(upImage, gImg, gammaCorrection, getThreadPool());
        }
        return gImg;
    }

    public boolean isBinaryMode() {
        return mode == MODE_BINARY || mode == MODE_BINARY_UPSAMPLED || mode == MODE_BINARY_ANTIALIASED;
    }
}