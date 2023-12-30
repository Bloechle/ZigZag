package sugarcube.zigzag.evaluation;


import sugarcube.zigzag.util.Box2D;
import sugarcube.zigzag.util.ImageUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

public class OCRPage
{

    public OCRSymbol[] symbols = new OCRSymbol[0];

    public OCRPage()
    {

    }

    public OCRPage(OCRSymbol[] symbols)
    {
        this.symbols = symbols;
    }

    public String text()
    {
        StringBuilder text = new StringBuilder(symbols.length);
        for (OCRSymbol symbol : symbols)
            text.append(symbol.symbol);
        return text.toString();
    }

    public String writeOcrFile()
    {
        StringBuilder text = new StringBuilder();

        for (OCRSymbol symbol : symbols)
        {
            text.append(symbol.symbol).append(" ");
            text.append(String.format("%.3f", symbol.confidence)).append(" ");
            Point[] corners = symbol.box.cornerPoints();
            for (int i = 0; i < corners.length; i++)
            {
                Point p = corners[i];
                if (i > 0)
                    text.append(" ");
                text.append(p.x).append(" ").append(p.y);
            }
            text.append("\n");
        }
        return text.toString();
    }

    public void writeOcrFile(String path)
    {
        ImageUtil.writeText(new File(path), writeOcrFile());
    }

    public void readOcrFile(String path)
    {
        try
        {
            LinkedList<OCRSymbol> symbols = new LinkedList<>();
            BufferedReader input = new BufferedReader(path.contains("/") || path.contains("\\") ? new FileReader(path) : new BufferedReader(new InputStreamReader(EvaluationOCR.class.getResourceAsStream(path), StandardCharsets.UTF_8)));
            String line;
            while ((line = input.readLine()) != null)
            {
                if (!(line = line.trim()).isEmpty())
                {
                    //System.out.println(line);
                    String[] tokens = line.split(" ");
                    if (tokens.length < 10)
                        System.out.println("Strange CSV line: " + line);

                    int minX = Integer.parseInt(tokens[2]);
                    int maxX = minX;
                    int minY = Integer.parseInt(tokens[3]);
                    int maxY = minY;

                    for (int i = 4; i < tokens.length - 1; i += 2)
                    {
                        int x = Integer.parseInt(tokens[i]);
                        int y = Integer.parseInt(tokens[i + 1]);

                        if (x < minX)
                            minX = x;
                        else if (x > maxX)
                            maxX = x;

                        if (y < minY)
                            minY = y;
                        else if (y > maxY)
                            maxY = y;
                    }
                    symbols.add(new OCRSymbol(tokens[0].trim(), Float.parseFloat(tokens[1].trim()), new Box2D(minX, maxX + 1, minY, maxY + 1)));
                }
            }

            this.symbols = symbols.toArray(new OCRSymbol[0]);
            input.close();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void drawOCRBoxes(File imgPath, File resPath)
    {
        try
        {
            BufferedImage binImage = ImageUtil.readImage(imgPath);
            BufferedImage ocrImage = new BufferedImage(binImage.getWidth(), binImage.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = ocrImage.createGraphics();
            g.drawImage(binImage, 0, 0, null);
            for (int i = 0; i < symbols.length; i++)
            {
                OCRSymbol symbol = symbols[i];
                Box2D box = symbol.box;
                float a = Math.max(symbol.confidence - 0.4f, 0f) * 10f / 6f;

                g.setColor(new Color(1f - a, a, 0f, 0.2f));
                g.fillRect(box.x0, box.y0, box.width(), box.height());
                g.setColor(new Color(1f - a, a, 0f, 0.8f));
                g.drawRect(box.x0, box.y0, box.width(), box.height());
//                System.out.println(symbol.symbol + " " + box.x0 + " " + box.y0 + " " + box.width() + " " + box.height());
            }
            ImageUtil.writeImage(ocrImage, resPath);

        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void writeOcrDebugImage(File inputImageFile, String suffix)
    {
        BufferedImage resultImage = ImageUtil.readImage(inputImageFile);
        BufferedImage debugImage = new BufferedImage(resultImage.getWidth(), resultImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = debugImage.createGraphics();
        g.drawImage(resultImage, 0, 0, null);

        Font font = new Font("Arial", Font.BOLD, 16);
        Font fontSmall = new Font("Arial", Font.BOLD, 8);

        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        for (int i = 0; i < symbols.length; i++)
        {
            OCRSymbol ocr = symbols[i];
            Box2D box = ocr.box;
            g.setColor(new Color(1 - ocr.confidence, ocr.confidence, 0, 0.5f));
            g.fillRect(box.x0, box.y0, box.width(), box.height());
            g.setColor(new Color(0, 0, 0, 0.5f));
            g.drawLine(box.x0, box.y0, box.x0, box.y1);
        }

        g.setColor(Color.BLACK);
        for (int i = 0; i < symbols.length; i++)
        {
            OCRSymbol ocr = symbols[i];
            Box2D box = ocr.box;
            g.setFont(font);
            g.drawString(ocr.symbol, box.x0, box.y0);
            g.setFont(fontSmall);
            g.drawString("" + (i + 1), box.x0, box.y0 + 10);
        }

        ImageUtil.writeImage(debugImage, inputImageFile, suffix);
    }

    public void writeJpgDebugImage(OCRPage gtPage, File folder, String pngFilename)
    {
        BufferedImage resultImage = ImageUtil.readImage(new File(folder, pngFilename));
        BufferedImage debugImage = new BufferedImage(resultImage.getWidth(), resultImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = debugImage.createGraphics();
        g.drawImage(resultImage, 0, 0, null);
        g.setFont(new Font("Arial", Font.BOLD, 18));

//        for (OCRSymbol gt : gtPage.symbols)
//            if (gt.isMatchCorrect())
//                truePositive++;
//            else
//                falseNegative++;
//
//        for (OCRSymbol ocr : symbols)
//            if (!ocr.isMatchCorrect())
//                falsePositive++;

        for (OCRSymbol gt : gtPage.symbols)
        {
            Box2D box = gt.box;

            g.setColor(new Color(0, 0, 0, 0.5f));
            g.drawRect(box.x0, box.y0, box.width(), box.height());

            if (!gt.isMatchCorrect())
            {
                g.setColor(new Color(0, 0, 0, 0.5f));
                g.drawString(gt.symbol, box.x0, box.y0);
            }

            if (gt.match == null)
            {
                g.setColor(new Color(1, 1, 0, 0.5f));
                g.fillRect(box.x0, box.y0, box.width(), box.height());
            }
        }

        for (OCRSymbol ocr : symbols)
        {
            Box2D box = ocr.box;
            OCRSymbol gt = ocr.match;


            if (gt == null)
            {
                g.setColor(new Color(1, 0.5f, 0, 0.25f));
                g.fillRect(box.x0, box.y0, box.width(), box.height());
                g.setColor(new Color(1, 0, 0, 0.5f));
                g.drawString(ocr.symbol, box.centerX(), box.y0);
            } else
            {
                if (!ocr.isMatchCorrect())
                {
                    g.setColor(new Color(1, 0, 0, 0.5f));
                    g.drawString(ocr.symbol, box.centerX(), box.y0);
                }

                g.setColor(ocr.symbol.equals(gt.symbol) ? new Color(0, 1, 0, 0.5f) : new Color(1, 0, 0, 0.5f));
                g.fillRect(box.x0, box.y0, box.width(), box.height());
            }
        }
        ImageUtil.writeImage(debugImage, new File(folder, pngFilename.replace(".png", ".jpg")));
    }

    public void upsample()
    {
        for (OCRSymbol symbol : symbols)
            symbol.upsample();
    }

    public void downsample()
    {
        for (OCRSymbol symbol : symbols)
            symbol.downsample();
    }

    public void normalizeSymbols()
    {
        for(OCRSymbol symbol : symbols)
            symbol.normalize();
    }


    public void clearMatches()
    {
        for (OCRSymbol symbol : symbols)
            symbol.match = null;
    }

    public void detectAndRemoveOverlappingSymbols()
    {
        for (int i = 0; i < symbols.length; i++)
        {
            OCRSymbol si = symbols[i];
            for (int j = i + 1; j < symbols.length; j++)
            {
                OCRSymbol sj = symbols[j];

                if (sj.confidence >= 0 && si.symbol.equals(sj.symbol) && si.box.contains(sj.box.centerX(), sj.box.centerY()) && sj.box.contains(si.box.centerX(), si.box.centerY()))
                {
                    System.out.println("OCRPage - Overlapping symbols: " + si.symbol + " vs " + sj.symbol);
                    sj.confidence = -1;
                }
            }
        }

        LinkedList<OCRSymbol> cleanedList = new LinkedList<>();
        for (OCRSymbol symbol : symbols)
            if (symbol.confidence >= 0)
                cleanedList.add(symbol);

        this.symbols = cleanedList.toArray(new OCRSymbol[0]);
    }

    public void matchWithGroundTruth(OCRPage gtFile)
    {
        clearMatches();
        gtFile.clearMatches();

        detectAndRemoveOverlappingSymbols();

        for (OCRSymbol ocr : symbols)
        {
            int cx = ocr.box.centerX();
            int cy = ocr.box.centerY();
            int distance;
            int bestDistance = Integer.MAX_VALUE;
            OCRSymbol bestGtSymbol = null;

            for (OCRSymbol gt : gtFile.symbols)
            {
                if (gt.box.contains(cx, cy) && (distance = gt.box.sqrDistToCenter(cx, cy)) < bestDistance)
                {
                    if (gt.isMatchCorrect())
                    {
                        //already found
                    } else
                    {
                        bestDistance = distance;
                        bestGtSymbol = gt;
                    }

                }
            }

            if (bestGtSymbol != null)
            {
                ocr.match = bestGtSymbol;
                bestGtSymbol.match = ocr;
            }
        }
    }

    public int nbOfOrphans()
    {
        int orphans = 0;
        for (OCRSymbol symbol : symbols)
            if (symbol.match == null)
                orphans++;
        return orphans;
    }

    public RecognitionErrors calculateErrors(OCRPage gtPage)
    {
        matchWithGroundTruth(gtPage);

        RecognitionErrors errors = new RecognitionErrors();

        for (OCRSymbol symbol : symbols)
            if (symbol.match != null)
                errors.error += symbol.symbol.equals(symbol.match.symbol) ? 0 : 1;
            else
                errors.ghost++;

        errors.orphan = gtPage.nbOfOrphans();

        int truePositive = 0;
        int falsePositive = 0;
        int falseNegative = 0;

        for (OCRSymbol gt : gtPage.symbols)
            if (gt.isMatchCorrect())
                truePositive++;
            else
                falseNegative++;

        for (OCRSymbol ocr : symbols)
            if (!ocr.isMatchCorrect())
                falsePositive++;

        double precision = truePositive / (double) (truePositive + falsePositive);
        double recall = truePositive / (double) (truePositive + falseNegative);

        errors.accuracy = truePositive / (double) (truePositive + falseNegative + falsePositive) * 100;
        errors.precision = precision * 100;
        errors.recall = recall * 100;
        errors.fscore = (2 * (precision * recall) / (precision + recall)) * 100;
        errors.levenshtein = ImageUtil.computeNormalizedLevenshteinDistance(gtPage.text(), text()) * 100;

        return errors;
    }


    public static OCRPage read(String path)
    {
        OCRPage page = new OCRPage();
        page.readOcrFile(path);
        return page;
    }

}
