package sugarcube.zigzag.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedList;

public abstract class ImageUtil {
    public static BufferedImage createGrayImage(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
    }

    public static BufferedImage convertToGray(BufferedImage rgbImg, BufferedImage grayImg) {
        if (grayImg == null)
            grayImg = createGrayImage(rgbImg.getWidth(), rgbImg.getHeight());
        Graphics g = grayImg.getGraphics();
        g.drawImage(rgbImg, 0, 0, null);
        g.dispose();
        return grayImg;
    }

    public static BufferedImage convertToRGBIfNeeded(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();

        switch (img.getType()) {
            case BufferedImage.TYPE_INT_RGB:
            case BufferedImage.TYPE_INT_BGR:
            case BufferedImage.TYPE_3BYTE_BGR:
                break;
            default:
                System.out.println("ImageUtil - converting to RGB from type=" + img.getType());
                BufferedImage rgbImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
                rgbImage.createGraphics().drawImage(img, 0, 0, width, height, null);
                img = rgbImage;
                break;
        }
        return img;
    }

    public static int[] getHistogram(WritableRaster raster, int band, int marginPercent) {
        int h = raster.getHeight();
        int w = raster.getWidth();
        int marginH = h * marginPercent / 100;
        int marginW = w * marginPercent / 100;
        h -= marginH;
        w -= marginW;
        int[] histogram = new int[256];
        for (int y = marginH; y < h; y++)
            for (int x = marginW; x < w; x++)
                histogram[raster.getSample(x, y, band)]++;
        return histogram;
    }

    public static int computeOtsuThreshold(int[] histogram) {
        int total = histogram.length;
        float maxBetween = 0;
        int threshold = 0;

        for (int i = 0; i < total; i++) {
            float sumB = 0, meanB = 0;
            for (int j = 0; j < i; j++) {
                sumB += histogram[j];
                meanB += histogram[j] * j;
            }
            float sumF = 0, meanF = 0;
            for (int j = i; j < total; j++) {
                sumF += histogram[j];
                meanF += histogram[j] * j;
            }

            meanB /= sumB;
            meanF /= sumF;

            if (sumB > 0 && sumF > 0) {
                float between = sumB * sumF * (meanF - meanB) * (meanF - meanB);
                if (between > maxBetween) {
                    maxBetween = between;
                    threshold = i;
                }
            }
        }
        return threshold;
    }

    public static BufferedImage scaleImage(BufferedImage srcImg, double scale, boolean bicubic, BufferedImage scaleImg) {
        if (scaleImg == null)
            scaleImg = new BufferedImage((int) Math.round(srcImg.getWidth() * scale), (int) Math.round(srcImg.getHeight() * scale), srcImg.getType());
        AffineTransform transform = new AffineTransform();
        transform.scale(scale, scale);
        new AffineTransformOp(transform, bicubic ? AffineTransformOp.TYPE_BICUBIC : AffineTransformOp.TYPE_BILINEAR).filter(srcImg, scaleImg);
        return scaleImg;
    }

    public static void decimateImage(BufferedImage upImg, BufferedImage gImg, boolean gammaCorrection, SliceProcessThreadPool threadPool) {
        if (threadPool == null)
            threadPool = SliceProcessThreadPool.EMPTY_THREAD_POOL;

        WritableRaster upRaster = upImg.getRaster();
        WritableRaster grayRaster = gImg.getRaster();
        int width = gImg.getWidth();
        int height = gImg.getHeight();

        int maxX = width - 1;
        int maxY = height - 1;

        threadPool.execute(height, (threadIndex, startY, endY) -> {
            int[] grayValues = new int[9];
            for (int y = startY; y < endY; y++) {
                for (int x = 0; x < width; x++) {
                    int xUp = 2 * x - 1;
                    int yUp = 2 * y - 1;

                    if (x == 0) xUp++;
                    else if (x == maxX) xUp--;

                    if (y == 0) yUp++;
                    else if (y == maxY) yUp--;

                    int sum = 0;
                    for (int value : upRaster.getSamples(xUp, yUp, 3, 3, 0, grayValues))
                        sum += value;

                    int mean = sum / 9;

                    if (gammaCorrection)
                        mean = (int) (255 * Math.pow(mean / 255.0, 1.5));

                    grayRaster.setSample(x, y, 0, mean);
                }
            }
        });
    }

    public static void writeText(File file, String text) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8))) {
            writer.write(text);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeImage(BufferedImage img, File file) {
        writeImage(img, file, null);
    }

    public static void writeImage(BufferedImage img, File file, String suffix) {
        if (img != null && file != null) {
            if (suffix != null && !suffix.isEmpty())
                file = new File(removeExtension(file.getPath()) + (suffix.contains(".") ? suffix : suffix + ".png"));

            try {
                ImageIO.write(img, file.getPath().contains(".png") ? "png" : "jpg", file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static File[] listImageFiles(File folder) {
        return listFiles(folder, ".jpg", ".jpeg", ".png", ".bmp");
    }

    public static File[] listFiles(File folder, String... extensions) {
        LinkedList<File> files = new LinkedList<>();
        for (File child : folder.listFiles()) {
            String name = child.getName().toLowerCase();
            for (String ext : extensions)
                if (name.endsWith(ext))
                    files.add(child);
        }
        return files.toArray(new File[0]);
    }

    public static String removeExtension(String path) {
        int i = path.lastIndexOf(".");
        return i > 0 ? path.substring(0, i) : path;
    }
}