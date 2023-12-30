package sugarcube.zigzag.util;

import javax.imageio.ImageIO;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedList;

public abstract class ImageUtil
{
    public static int getValueAt(BufferedImage grayImage, int x, int y)
    {
        return grayImage.getRaster().getSample(x < 0 ? 0 : x >= grayImage.getWidth() ? grayImage.getWidth() - 1 : x, y < 0 ? 0 : y >= grayImage.getHeight() ? grayImage.getHeight() - 1 : y, 0);
    }

    public static BufferedImage createGrayLevelImage(int width, int height)
    {
        return new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
    }

    public static BufferedImage convertToGrayLevel(BufferedImage rgbImage, BufferedImage grayImage)
    {
        if (grayImage == null)
            grayImage = createGrayLevelImage(rgbImage.getWidth(), rgbImage.getHeight());
        Graphics g = grayImage.getGraphics();
        g.drawImage(rgbImage, 0, 0, null);
        g.dispose();
        return grayImage;
    }

    public static BufferedImage convertToRGBIfNeeded(BufferedImage image)
    {
        int width = image.getWidth();
        int height = image.getHeight();

        //normalizes image to standard RGB
        switch (image.getType())
        {
            case BufferedImage.TYPE_INT_RGB:
            case BufferedImage.TYPE_INT_BGR:
            case BufferedImage.TYPE_3BYTE_BGR:
                break;
            default:
                System.out.println("ImageUtil - conversion to RGB buffered image from type=" + image.getType());
                BufferedImage rgbImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
                rgbImage.createGraphics().drawImage(image, 0, 0, width, height, null);
                image = rgbImage;
                break;
        }
        return image;
    }


    public static int[] histogram(WritableRaster raster, int band)
    {
        int[] histogram = new int[256];
        for (int y = 0; y < raster.getHeight(); y++)
            for (int x = 0; x < raster.getWidth(); x++)
                histogram[raster.getSample(x, y, band)]++;
        return histogram;
    }

    public static int[] histogram(WritableRaster raster, int band, int marginInPercent)
    {
        int h = raster.getHeight();
        int w = raster.getWidth();
        int dh = h * marginInPercent / 100;
        int dw = w * marginInPercent / 100;
        h -= dh;
        w -= dw;
        int[] histogram = new int[256];
        for (int y = dh; y < h; y++)
            for (int x = dw; x < w; x++)
                histogram[raster.getSample(x, y, band)]++;
        return histogram;
    }

    public static int[] histogram(WritableRaster raster, int band, Box2D box, int pixelSkip)
    {
        int[] histogram = new int[256];
        int index = 0;
        for (int y = box.y0; y < box.y1; y++)
            for (int x = box.x0; x < box.x1; x++)
                if (index++ % pixelSkip == 0)
                    histogram[raster.getSample(x, y, band)]++;
        return histogram;
    }

    public static int computeOtsuThreshold(int[] histogram)
    {
        int s = histogram.length;

        float maxBetween = 0;
        int maxIndex = 0;

        for (int th = 0; th < s; th++)
        {
            float lSum = 0;
            float lMean = 0;
            for (int i = 0; i < th; i++)
            {
                lSum += histogram[i];
                lMean += histogram[i] * i;
            }
            float rSum = 0;
            float rMean = 0;
            for (int i = th; i < s; i++)
            {
                rSum += histogram[i];
                rMean += histogram[i] * i;
            }

            lMean /= lSum;
            rMean /= rSum;

            if (lSum > 0.0 && rSum > 0.0)
            {
                float between = lSum * rSum * (rMean - lMean) * (rMean - lMean);
                if (between > maxBetween)
                {
                    maxBetween = between;
                    maxIndex = th;
                }
            }
        }

        return maxIndex;
    }

    public static int computeStandardDeviation(Box2D box, WritableRaster grayRaster, int pixelSkip)
    {
        int mean = 0;
        int index = 0;
        int counter = 0;

        for (int y = box.y0; y < box.y1; y++)
            for (int x = box.x0; x < box.x1; x++)
                if (index++ % pixelSkip == 0)
                {
                    counter++;
                    mean += grayRaster.getSample(x, y, 0);
                }

        if (counter > 0)
            mean /= counter;

        double sdev = 0;
        counter = 0;
        index = 0;

        for (int y = box.y0; y < box.y1; y++)
            for (int x = box.x0; x < box.x1; x++)
                if (index++ % pixelSkip == 0)
                {
                    counter++;
                    int diff = grayRaster.getSample(x, y, 0) - mean;
                    sdev += diff * diff;
                }

        sdev /= counter;
        return (int) Math.sqrt(sdev);
    }

    public static BufferedImage scaleImage(BufferedImage srcImage, double scale, boolean bicubic, BufferedImage scaledImage)
    {
        if (scaledImage == null)
            scaledImage = new BufferedImage((int) Math.round(srcImage.getWidth() * scale), (int) Math.round(srcImage.getHeight() * scale), srcImage.getType());
        AffineTransform tm = new AffineTransform();
        tm.scale(scale, scale);
        new AffineTransformOp(tm, bicubic ? AffineTransformOp.TYPE_BICUBIC : AffineTransformOp.TYPE_BILINEAR).filter(srcImage, scaledImage);
        return scaledImage;
    }

    public static void decimateImage(BufferedImage upImage, BufferedImage grayImage, boolean gammaCorrection, SliceProcessThreadPool threadPool)
    {
        if (threadPool == null)
            threadPool = SliceProcessThreadPool.EMPTY_THREAD_POOL;

        WritableRaster upRaster = upImage.getRaster();
        WritableRaster grayRaster = grayImage.getRaster();
        int width = grayImage.getWidth();
        int height = grayImage.getHeight();

        int maxX = width - 1;
        int maxY = height - 1;

        threadPool.execute(height, (threadIndex, startY, endY) ->
        {
            int[] grayValues = new int[9];
            int xUp, yUp, sum, mean;
            for (int y = startY; y < endY; y++)
                for (int x = 0; x < width; x++)
                {
                    xUp = 2 * x - 1;
                    yUp = 2 * y - 1;

                    if (x == 0)
                        xUp++;
                    else if (x == maxX)
                        xUp--;

                    if (y == 0)
                        yUp++;
                    else if (y == maxY)
                        yUp--;

                    sum = 0;
                    for (int value : upRaster.getSamples(xUp, yUp, 3, 3, 0, grayValues))
                        sum += value;

                    mean = sum / 9;

                    if (gammaCorrection)
                        mean = (int) (255 * Math.pow(mean / 255.0, 1.5));

                    grayRaster.setSample(x, y, 0, mean);
                }
        });
    }

    public static BufferedImage decimageImage(BufferedImage img, double scale)
    {
        WritableRaster raster = img.getRaster();

        int imgWidth = img.getWidth();
        int imgHeight = img.getHeight();

        int height = (int) Math.round(imgHeight * scale);
        int width = (int) Math.round(imgWidth * scale);
        int bands = raster.getNumBands();

        int[][][] result = new int[height][width][bands];

        double txA, tyA, txB, tyB, xsize, ysize;
        int ix, iy;

        scale = 1.0 / scale;

        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                for (int c = 0; c < bands; c++)
                {
                    txA = x * scale;
                    tyA = y * scale;
                    txB = (x + 1) * scale;
                    tyB = (y + 1) * scale;

                    double area = 0;
                    double sum = 0;

                    for (double ty = Math.floor(tyA); ty <= Math.ceil(tyB); ty++)
                    {
                        iy = (int) ty;
                        ysize = 255.0;
                        if (ty < tyA)
                            ysize *= 1.0 - (tyA - ty);
                        if (ty > tyB)
                            ysize *= 1.0 - (ty - tyB);

                        for (double tx = Math.floor(txA); tx <= Math.ceil(txB); tx++)
                        {
                            ix = (int) tx;
                            xsize = ysize;
                            if (tx < txA)
                                xsize *= 1.0 - (txA - tx);
                            if (tx > txB)
                                xsize *= 1.0 - (tx - txB);

                            sum += raster.getSample(ix < imgWidth ? ix : imgWidth - 1, iy < imgHeight ? iy : imgHeight - 1, c) * xsize;
                            area += xsize;
                        }
                    }
                    result[y][x][c] = (int) Math.round(sum / area);
                }

        BufferedImage res = new BufferedImage(width, height, img.getType());
        raster = res.getRaster();
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                for (int c = 0; c < bands; c++)
                    raster.setSample(x, y, c, result[y][x][c]);

        return res;
    }

    public static String[] readTextLines(File file, boolean removeEmptyLines)
    {
        LinkedList<String> lines = new LinkedList<>();
        try
        {
            BufferedReader input = new BufferedReader(new FileReader(file));
            String line;
            while ((line = input.readLine()) != null)
            {
                if (removeEmptyLines && line.trim().isEmpty())
                    continue;
                lines.add(line);
            }
            input.close();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return lines.toArray(new String[0]);
    }

    public static String readText(File file)
    {
        StringBuilder contents = new StringBuilder();
        BufferedReader input = null;
        try
        {
            input = new BufferedReader(new FileReader(file));
            String line;
            while ((line = input.readLine()) != null)
            {
                contents.append(line).append("\n");
            }
            input.close();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return contents.toString();
    }

    public static void writeText(File file, String text)
    {
        try
        {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8));
            writer.write(text);
            writer.close();
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static BufferedImage readImage(File file)
    {
        try
        {
            return ImageIO.read(file);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public static void writeImage(BufferedImage bi, File file)
    {
        writeImage(bi, file, null);
    }

    public static void writeImage(BufferedImage bi, File file, String suffix)
    {
        if (bi != null && file != null)
        {
            if (suffix != null && !suffix.isEmpty())
                file = new File(ImageUtil.removeExtension(file.getPath()) + (suffix.contains(".") ? suffix : suffix + ".png"));

            try
            {
                ImageIO.write(bi, file.getPath().contains(".png") ? "png" : "jpg", file);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public static File[] listImageFiles(File folder)
    {
        return listFiles(folder, ".jpg", ".jpeg", ".png", ".bmp");
    }

    public static File[] listFiles(File folder, String... extensions)
    {
        LinkedList<File> files = new LinkedList<>();
        for (File child : folder.listFiles())
        {
            String name = child.getName().toLowerCase();
            for (String ext : extensions)
                if (name.endsWith(ext))
                    files.add(child);
        }
        return files.toArray(new File[0]);
    }

    public static String removeExtension(String path)
    {
        int i = path.lastIndexOf(".");
        return i > 0 ? path.substring(0, i) : path;
    }

    public static double computeNormalizedLevenshteinDistance(String gtText, String text)
    {
        return (gtText.length() - computeLevenshteinDistance(gtText, text)) / (double) gtText.length();
    }

    public static int computeLevenshteinDistance(String gtText, String text)
    {
        // degenerate cases
        if (gtText.equals(text))
            return 0;
        if (gtText.isEmpty())
            return text.length();
        if (text.isEmpty())
            return gtText.length();

        // i == 0
        int[] costs = new int[text.length() + 1];
        for (int j = 0; j < costs.length; j++)
            costs[j] = j;
        for (int i = 1; i <= gtText.length(); i++)
        {
            // j == 0; nw = lev(i - 1, j)
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= text.length(); j++)
            {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]), gtText.charAt(i - 1) == text.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[text.length()];
    }


    public static void deleteFiles(File folder, String... patterns)
    {
        try
        {
            if (folder.exists() && folder.isDirectory())
            {
                for (File file : folder.listFiles())
                {
                    boolean doDelete = patterns.length == 0;
                    if (!doDelete)
                        for (String pattern : patterns)
                            if (file.getName().contains(pattern))
                            {
                                doDelete = true;
                                break;
                            }
                    if (doDelete)
                        file.delete();
                }
                Thread.sleep(100);
            } else
                System.out.println("Folder not found: " + folder.getPath());

        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void renameFilesWithName(File folder, String filenamePattern, String newFilenamePattern)
    {
        try
        {
            for (File file : folder.listFiles())
            {
                if (file.getName().contains(filenamePattern))
                    file.renameTo(new File(file.getPath().replace(filenamePattern, newFilenamePattern)));
            }
            Thread.sleep(200);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static double[] calculateAccuracyPrecisionRecallF1ScoreMseAndPsnr(BufferedImage gtImage, BufferedImage binImage)
    {
        int width = gtImage.getWidth();
        int height = gtImage.getHeight();

        WritableRaster gtRaster = gtImage.getRaster();
        WritableRaster binRaster = binImage.getRaster();

        int correctPixels = 0;
        int truePositive = 0;
        int trueNegative = 0;
        int falsePositive = 0;
        int falseNegative = 0;
        int nbOfPixels = width * height;
        long squaredErrorSum = 0;
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
            {
                boolean gtIsBlack = gtRaster.getSample(x, y, 0) < 128;
                boolean binIsBlack = binRaster.getSample(x, y, 0) < 128;

                if (binIsBlack == gtIsBlack)
                    correctPixels++;

                if (binIsBlack && gtIsBlack)
                    truePositive++;

                if (!binIsBlack && !gtIsBlack)
                    trueNegative++;

                if (binIsBlack && !gtIsBlack)
                    falsePositive++;

                if (!binIsBlack && gtIsBlack)
                    falseNegative++;

                squaredErrorSum += Math.abs((gtRaster.getSample(x, y, 0) < 128 ? 0 : 1) - (binRaster.getSample(x, y, 0) < 128 ? 0 : 1));
            }

        double accuracy = correctPixels / (double) nbOfPixels;
        double precision = truePositive / (double) (truePositive + falsePositive);
        double recall = truePositive / (double) (truePositive + falseNegative);
        double f1score = 2 * (precision * recall) / (precision + recall);
        double mse = (double) squaredErrorSum / (width * height);
        double psnr = 10 * Math.log10(1 / mse);

        return new double[]{accuracy, precision, recall, f1score, mse, psnr};
    }

    public static String getDesktopPath()
    {
        return FileSystemView.getFileSystemView().getHomeDirectory().getPath().replace('\\', '/') + "/";
    }

    public static void converFolderToPng(String folderPath, String imageExt, boolean doRemoveOriginals)
    {
        for (File file : ImageUtil.listFiles(new File(folderPath), imageExt))
        {
            try
            {

                BufferedImage image = ImageIO.read(file);
                if (image != null)
                {
                    System.out.println("Converting image: " + file.getPath());
                    ImageUtil.writeImage(image, new File(file.getPath().replace(imageExt, ".png")));
                    if (doRemoveOriginals)
                        file.delete();
                } else
                {
                    System.out.println("Image not found: " + file.getPath());
                }
            } catch (Exception e)
            {
                e.printStackTrace();
                System.out.println("Exception with image: " + file.getPath());
            }
        }
    }

}
