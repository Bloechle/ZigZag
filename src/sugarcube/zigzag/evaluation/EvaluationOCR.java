package sugarcube.zigzag.evaluation;

import sugarcube.zigzag.util.ImageUtil;
import sugarcube.zigzag.ImageFilter;

import java.io.File;

public class EvaluationOCR
{
    public interface OCR
    {
        OCRPage generateOCRPage(File file);
    }

    public static void evaluateOCR(OCR ocr, String datasetPath, boolean writeDebugJpg, ImageFilter... imageFilters)
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
                    OCRPage gtPage = OCRPage.read(new File(inFolder, ImageUtil.removeExtension(outFile.getName())+".ocr").getPath());
                    OCRPage ocrPage = ocr.generateOCRPage(outFile);
                    gtPage.normalizeSymbols();
                    ocrPage.normalizeSymbols();
                    if (algoName.contains("UP"))
                        ocrPage.downsample();

//            binOcr.writeOcrFile(file.getPath().replace(".png", ".ocr"));

                    RecognitionErrors errors = ocrPage.calculateErrors(gtPage);
                    errors.name = outFile.getName();
                    errors.counter = eval.rows.size() + 1;
                    errors.millis = (int) imageFilter.chronometer().elapsedTime();
                    eval.addErrorsRow(errors);
                    System.out.println(eval.rows.getLast().toCsvString(true));

                    if (writeDebugJpg)
                        ocrPage.writeJpgDebugImage(gtPage, outFolder, outFile.getName());
                }, true);
            }

            if (!eval.rows.isEmpty())
            {
                eval.computeMeanAndSDev(algoName);
                System.out.println(eval.mean.toCsvString(true) + "\n\n" + algoName + " evaluation done");
                ImageUtil.writeText(new File(outFolder, algoName + ".csv"), eval.toCsvString(true));

                File evaluationFile = new File(inFolder.getParentFile(), "Evaluations-" + inFolder.getName() + ".csv");
                String[] evaluations = evaluationFile.exists() ? ImageUtil.readTextLines(evaluationFile, true) : new String[]{RecognitionEvaluation.csvHeader(true)};
                ImageUtil.writeText(evaluationFile, String.join("\n", evaluations) + "\n" + eval.toCsvMeanString(true, true));
            }

            System.out.println("OCR Evaluation Done");
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void generateOcrFiles(OCR ocr, String path)
    {
        try
        {
            ImageUtil.deleteFiles(new File(path), ".ocr");
            File[] files = ImageUtil.listImageFiles(new File(path));
            for (int i = 0; i < files.length; i++)
            {
                File file = files[i];
                String name = file.getName();
                if (name.contains("_."))
                {
                    ocr.generateOCRPage(file).writeOcrFile(ImageUtil.removeExtension(file.getPath())+"_.ocr");
                    System.out.println((i + 1) + "/" + files.length + " " + file.getName());
                }
            }

        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

}
