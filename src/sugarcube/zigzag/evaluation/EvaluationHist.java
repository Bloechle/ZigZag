package sugarcube.zigzag.evaluation;

import sugarcube.zigzag.util.ImageUtil;
import sugarcube.zigzag.ImageBinarizer;
import sugarcube.zigzag.ImageFilter;
import sugarcube.zigzag.ZigZagFilter;
import sugarcube.zigzag.legacy.*;

import java.awt.image.BufferedImage;
import java.io.File;

public class EvaluationHist
{


    public static void main(String... args)
    {
        String zigzagPath = "C:/Projects/ZigZag/eval/";

        ImageFilter[] nick = filters(new NickBinarizer(20), new NickBinarizer(30), new NickBinarizer(40));
        ImageFilter[] sauvola = filters(new SauvolaBinarizer(20), new SauvolaBinarizer(30), new SauvolaBinarizer(40));
        ImageFilter[] bradley = filters(new BradleyBinarizer(20), new BradleyBinarizer(30), new BradleyBinarizer(40));
        ImageFilter[] niblack = filters(new NiblackBinarizer(20), new NiblackBinarizer(30), new NiblackBinarizer(40));
        ImageFilter[] bernsen = filters(new BernsenBinarizer(20), new BernsenBinarizer(30), new BernsenBinarizer(40));
        ImageFilter[] otsu = filters(new OtsuBinarizer());
        ImageFilter[] michalak = filters(new MichalakBinarizer());
        ImageFilter[] yinyang64 = filters(new YinYangBinarizer(ImageBinarizer.MODE_BINARY));

        ImageFilter[] zigzag = filters(
                new ZigZagFilter(20, 100, ImageBinarizer.MODE_BINARY),
                new ZigZagFilter(30, 100, ImageBinarizer.MODE_BINARY),
                new ZigZagFilter(40, 100, ImageBinarizer.MODE_BINARY)
        );


//        for (ImageFilter[] filter : new ImageFilter[][]{nick, yinyang32, yinyang64, sauvola, otsu, bradley, michalak, bernsen, niblack})
//            evaluate(zigzagPath + "Nabuco/dataset/", filter);
//
//        for (int percent=50; percent<=100; percent+=10)
//            evaluate(zigzagPath + "Nabuco/dataset/",
//                    new ZigZagFilter(20, percent, ImageBinarizer.MODE_BINARY),
//                    new ZigZagFilter(30, percent, ImageBinarizer.MODE_BINARY),
//                    new ZigZagFilter(40, percent, ImageBinarizer.MODE_BINARY));


        evaluate(zigzagPath + "Nabuco/dataset/", new ZigZagFilter(30, 60, ImageBinarizer.MODE_BINARY).setHistoricalWhiteThreshold(245));
    }

    private static ImageFilter[] filters(ImageFilter... filters)
    {
        return filters;
    }


    public static void evaluate(String datasetPath, ImageFilter... imageFilters)
    {
        try
        {
            File inFolder = new File(datasetPath);
            if (!inFolder.exists())
            {
                System.out.println("Folder does not exists: " + inFolder.getPath());
                return;
            }

            String algoName = imageFilters[0].customName() + "[" + imageFilters.length + "]";
            File outFolder = new File(inFolder.getParentFile(), "results/" + algoName + "/");
            if (!outFolder.mkdirs())
                ImageUtil.deleteFiles(outFolder);

            RecognitionEvaluation eval = new RecognitionEvaluation();

            for (ImageFilter imageFilter : imageFilters)
            {
                imageFilter.doPrintTimes(true);
                imageFilter.batchProcess(inFolder, outFolder, outFile ->
                {
                    RecognitionErrors errors = new RecognitionErrors();
                    errors.name = outFile.getName();
                    errors.counter = eval.rows.size() + 1;
                    errors.millis = (int) imageFilter.chronometer().elapsedTime();

                    File gtFile = new File(inFolder, outFile.getName().replace(".png", "_.png"));
                    if (gtFile.exists())
                    {
                        BufferedImage gtImage = gtFile.exists() ? ImageUtil.readImage(gtFile) : null;
                        double[] stats = ImageUtil.calculateAccuracyPrecisionRecallF1ScoreMseAndPsnr(gtImage, ImageUtil.readImage(outFile));
                        errors.accuracy = stats[0] * 100;
                        errors.precision = stats[1] * 100;
                        errors.recall = stats[2] * 100;
                        errors.fscore = stats[3] * 100;
                        errors.mse = stats[4] * 100;
                        errors.psnr = stats[5];
                    }
                    eval.addErrorsRow(errors);
                }, true);
            }
            if (!eval.rows.isEmpty())
            {
                eval.computeMeanAndSDev(algoName);
                System.out.println(eval.mean.toCsvString(false) + "\n\n" + algoName + " evaluation done");
                ImageUtil.writeText(new File(outFolder, algoName + ".csv"), eval.toCsvString(false));

                File evaluationFile = new File(inFolder.getParentFile(), "Evaluations-" + inFolder.getName() + ".csv");
                String[] evaluations = evaluationFile.exists() ? ImageUtil.readTextLines(evaluationFile, true) : new String[]{RecognitionEvaluation.csvHeader(false)};
                ImageUtil.writeText(evaluationFile, String.join("\n", evaluations) + "\n" + eval.mean.toCsvString(false));
            }

            System.out.println("Image Evaluation Done");
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }


}
