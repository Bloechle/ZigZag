package sugarcube.zigzag;

import sugarcube.zigzag.util.ArgsParser;
import sugarcube.zigzag.util.ImageFilter;
import sugarcube.zigzag.util.ImageUtil;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;

public class ZigZag extends ImageFilter {
    private int histWThresh = 256;

    public ZigZag() {
        this(DEFAULT_SIZE);
    }

    public ZigZag(int size) {
        this(size, MODE_GRAY_LEVEL);
    }

    public ZigZag(int size, int mode) {
        this(size, 100, mode);
    }

    public ZigZag(int size, int percent, int mode) {
        this(size, percent, mode, DEFAULT_THREADS);
    }

    public ZigZag(int size, int percent, int mode, int numThreads) {
        super(size, percent, mode, numThreads);
    }

    public ZigZag setHistWThresh(int threshold) {
        this.histWThresh = threshold;
        return this;
    }

    @Override
    public BufferedImage filterImplementation(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        BufferedImage gImg = ImageUtil.convertToGray(img, null);
        WritableRaster gRast = gImg.getRaster();
        WritableRaster rgbRast = img.getRaster();

        boolean isColorMode = mode == MODE_COLOR;

        int[][][] rgbInt = new int[isColorMode ? 3 : 1][height][width];
        int[][] gInt = rgbInt[0];


        computeIntegralImages(gRast, gInt);
        computeMaskImage(gRast, rgbRast, gInt, isColorMode, height, width);
        writeDebugImage(img, "Mask");

        int[][] cInt = new int[height][width];
        if (isColorMode) {
            computeColorIntegralImages(gRast, rgbRast, cInt, rgbInt, height, width);
        } else {
            computeGrayIntegralImages(rgbRast, gRast, cInt, gInt, height, width);
        }

        generateForeground(gRast, rgbRast, cInt, gInt, rgbInt, isColorMode, height, width);

        writeDebugImage(gImg, "FG");
        writeDebugImage(img, "BG");

        if (mode == MODE_COLOR) {
            return img;
        }

        int otsuThreshold = isBinaryMode() ? Math.min(250, ImageUtil.computeOtsuThreshold(ImageUtil.getHistogram(gRast, 0, 10))) : 250;

        return binarizeImage(gImg, false, (x, y, value) -> value < otsuThreshold ? (mode == MODE_GRAY_LEVEL ? value : 0) : 255);
    }

    private void computeIntegralImages(WritableRaster gRast, int[][] gInt) {
        for (int y = 0; y < gInt.length; y++) {
            for (int sum = 0, x = 0; x < gInt[0].length; x++) {
                sum += gRast.getSample(x, y, 0);
                gInt[y][x] = y == 0 ? sum : gInt[y - 1][x] + sum;
            }
        }
    }

    private void computeMaskImage(WritableRaster gRast, WritableRaster rgbRast, int[][] gInt, boolean isColorMode, int height, int width) {
        executeInParallel(height, (threadIndex, startY, endY) -> {
            int halfSize = size / 2;
            int side = 2 * halfSize + 1;
            int numPixels = side * side;
            int x1, x2, y1, y2, mean, value;

            for (int y = startY; y < endY; y++) {
                y1 = y - halfSize - 1;
                y2 = y + halfSize;
                if (y1 < 0) y2 = (y1 = 0) + side;
                else if (y2 >= height) y1 = (y2 = height - 1) - side;

                for (int x = 0; x < width; x++) {
                    x1 = x - halfSize - 1;
                    x2 = x + halfSize;
                    if (x1 < 0) x2 = (x1 = 0) + side;
                    else if (x2 >= width) x1 = (x2 = width - 1) - side;

                    mean = percent * (gInt[y2][x2] - gInt[y1][x2] - gInt[y2][x1] + gInt[y1][x1]) / (100 * numPixels);
                    value = gRast.getSample(x, y, 0);
                    if (value > histWThresh) value = 0;
                    if (isColorMode) {
                        gRast.setSample(x, y, 0, value < mean ? 0 : value);
                    } else {
                        rgbRast.setSample(x, y, 0, value < mean ? 0 : value);
                        if (debugEnabled) {
                            for (int band = 1; band < 3; band++) {
                                rgbRast.setSample(x, y, band, value < mean ? 0 : value);
                            }
                        }
                    }
                }
            }
        });
    }

    private void computeColorIntegralImages(WritableRaster gRast, WritableRaster rgbRast, int[][] cInt, int[][][] rgbInt, int height, int width) {
        int[] rgbPixel = new int[3];
        int[] rgbSum = new int[3];
        for (int y = 0; y < height; y++) {
            for (int counter = 0, x = 0; x < width; x++) {
                if (gRast.getSample(x, y, 0) > 0) {
                    counter++;
                    rgbRast.getPixel(x, y, rgbPixel);
                    for (int band = 0; band < 3; band++) rgbSum[band] += rgbPixel[band];
                }
                cInt[y][x] = y == 0 ? counter : cInt[y - 1][x] + counter;
                for (int band = 0; band < 3; band++)
                    rgbInt[band][y][x] = y == 0 ? rgbSum[band] : rgbInt[band][y - 1][x] + rgbSum[band];
            }
        }
    }

    private void computeGrayIntegralImages(WritableRaster rgbRast, WritableRaster gRast, int[][] cInt, int[][] gInt, int height, int width) {
        for (int y = 0; y < height; y++) {
            for (int gSum = 0, counter = 0, x = 0; x < width; x++) {
                if (rgbRast.getSample(x, y, 0) > 0) {
                    counter++;
                    gSum += gRast.getSample(x, y, 0);
                }
                cInt[y][x] = y == 0 ? counter : cInt[y - 1][x] + counter;
                gInt[y][x] = y == 0 ? gSum : gInt[y - 1][x] + gSum;
            }
        }
    }

    private void generateForeground(WritableRaster gRast, WritableRaster rgbRast, int[][] cInt, int[][] gInt, int[][][] rgbInt, boolean isColorMode, int height, int width) {
        executeInParallel(height, (threadIndex, startY, endY) -> {
            int halfSize = size / 2;
            int side = 2 * halfSize + 1;
            int[] rgbPixel = isColorMode ? new int[3] : null;
            int x1, x2, y1, y2, numPixels, mean, value;

            for (int y = startY; y < endY; y++) {
                y1 = y - halfSize - 1;
                y2 = y + halfSize;
                if (y1 < 0) y2 = (y1 = 0) + side;
                else if (y2 >= height) y1 = (y2 = height - 1) - side;

                for (int x = 0; x < width; x++) {
                    x1 = x - halfSize - 1;
                    x2 = x + halfSize;
                    if (x1 < 0) x2 = (x1 = 0) + side;
                    else if (x2 >= width) x1 = (x2 = width - 1) - side;

                    numPixels = (cInt[y2][x2] - cInt[y1][x2] - cInt[y2][x1] + cInt[y1][x1]);
                    if (isColorMode) {
                        rgbRast.getPixel(x, y, rgbPixel);
                        for (int band = 0; band < 3; band++) {
                            mean = numPixels == 0 ? 0 : (rgbInt[band][y2][x2] - rgbInt[band][y1][x2] - rgbInt[band][y2][x1] + rgbInt[band][y1][x1]) / numPixels;
                            rgbPixel[band] = rgbPixel[band] >= mean ? 255 : rgbPixel[band] * 256 / mean;
                            if (debugEnabled) rgbPixel[band] = mean;
                        }
                        rgbRast.setPixel(x, y, rgbPixel);
                        gRast.setSample(x, y, 0, Math.min(Math.min(rgbPixel[0], rgbPixel[1]), rgbPixel[2]));
                    } else {
                        mean = numPixels == 0 ? 0 : (gInt[y2][x2] - gInt[y1][x2] - gInt[y2][x1] + gInt[y1][x1]) / numPixels;
                        value = gRast.getSample(x, y, 0);
                        gRast.setSample(x, y, 0, value >= mean ? 255 : value * 256 / mean);
                        if (debugEnabled) {
                            for (int band = 0; band < 3; band++) {
                                rgbRast.setSample(x, y, band, mean);
                            }
                        }
                    }
                }
            }
        });
    }


    public static void main(String... args) {
        ArgsParser argsParser = new ArgsParser(
                args.length == 0 ? new String[]{"-input", "C:/Projects/ZigZag/eval/Test/input/", "-output", "C:/Projects/ZigZag/eval/Test/output"} : args
        );

        try {
            if (argsParser.has("-input")) {
                processImage(argsParser);
            } else {
                argsParser.printUsage();
            }
        } catch (Exception e) {
            e.printStackTrace();
            argsParser.printUsage();
        }
    }

    private static void processImage(ArgsParser argsParser) {
        int size = argsParser.getInt("-size", 30);
        int percent = argsParser.getInt("-percent", 100);
        int mode = argsParser.getInt("-mode", ImageFilter.MODE_BINARY_UPSAMPLED);
        int threads = argsParser.getInt("-threads", Math.max(1, Runtime.getRuntime().availableProcessors() / 2));
        boolean debug = argsParser.getBoolean("-debug", false);
        String inputFilePath = argsParser.get("-input");
        String outputFilePath = argsParser.get("-output", inputFilePath.replaceFirst("\\.(?=[^.]+$)", "_ZZ."));

        File inputFile = new File(inputFilePath);
        File outputFile = new File(outputFilePath);

        if (inputFile.isDirectory()) {
            processDirectory(inputFile, outputFile, size, percent, mode, threads, debug);
        } else {
            processSingleImage(size, percent, mode, threads, debug, inputFilePath, outputFilePath);
        }

        if (argsParser.getBoolean("-exit", false)) {
            System.exit(0);
        }
    }

    private static void processDirectory(File inputFile, File outputFile, int size, int percent, int mode, int threads, boolean debug) {
        if (!outputFile.exists()) {
            outputFile.mkdirs();
        }

        File[] imageFiles = inputFile.listFiles((dir, name) -> {
            String lowerName = name.toLowerCase();
            return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png") || lowerName.endsWith(".bmp");
        });

        if (imageFiles != null) {
            for (File imageFile : imageFiles) {
                String outputFileName = new File(outputFile, imageFile.getName().replaceFirst("\\.[^.]+$", ".png")).getPath();
                processSingleImage(size, percent, mode, threads, debug, imageFile.getPath(), outputFileName);
            }
        }
    }

    private static void processSingleImage(int size, int percent, int mode, int threads, boolean debug, String inputFilePath, String outputFilePath) {
        ZigZag zigzag = new ZigZag(size, percent, mode, threads);
        zigzag.debugEnabled = debug;

        zigzag.applyFilter(inputFilePath, outputFilePath);
        zigzag.dispose();
    }


}
