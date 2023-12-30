package sugarcube.zigzag.evaluation;

import java.util.LinkedList;

public class RecognitionEvaluation
{

    public final LinkedList<RecognitionErrors> rows = new LinkedList<>();
    public final RecognitionErrors mean = new RecognitionErrors();
    public final RecognitionErrors sdev = new RecognitionErrors();


    public RecognitionEvaluation()
    {
    }

    public static String csvHeader(boolean isOcrMode)
    {
        if (isOcrMode)
            return "name;counter;millis;error;ghost;orphan;accuracy;precision;recall;fscore;levenshtein";
        else
            return "name;counter;millis;accuracy;precision;recall;fscore;mse;psnr";
    }

    public void addErrorsRow(RecognitionErrors row)
    {
        rows.add(row);
    }

    public void computeMeanAndSDev(String algoName)
    {
        mean.name = algoName;
        sdev.name = algoName;
        for (RecognitionErrors row : rows)
        {
            mean.millis += row.millis;
            mean.error += row.error;
            mean.ghost += row.ghost;
            mean.orphan += row.orphan;
            mean.accuracy += row.accuracy;
            mean.precision += row.precision;
            mean.recall += row.recall;
            mean.fscore += row.fscore;
            mean.levenshtein += row.levenshtein;
            mean.mse += row.mse;
            mean.psnr += row.psnr;
        }

        int size = rows.size();
        mean.counter = rows.size();
        mean.millis /= size;
        mean.accuracy /= size;
        mean.precision /= size;
        mean.recall /= size;
        mean.fscore /= size;
        mean.levenshtein /= size;
        mean.mse /= size;
        mean.psnr /= size;

        for (RecognitionErrors row : rows)
        {
            sdev.millis += sqr(mean.millis - row.millis);
            sdev.accuracy += sqr(mean.accuracy - row.accuracy);
            sdev.precision += sqr(mean.precision - row.precision);
            sdev.recall += sqr(mean.recall - row.recall);
            sdev.fscore += sqr(mean.fscore - row.fscore);
            sdev.levenshtein += sqr(mean.levenshtein - row.levenshtein);
            sdev.mse += sqr(mean.mse - row.mse);
            sdev.psnr += sqr(mean.psnr - row.psnr);
        }

        sdev.millis = (int) Math.round(Math.sqrt(sdev.millis / (double) size));
        sdev.accuracy = Math.sqrt(sdev.accuracy / size);
        sdev.precision = Math.sqrt(sdev.precision / size);
        sdev.recall = Math.sqrt(sdev.recall / size);
        sdev.fscore = Math.sqrt(sdev.fscore / size);
        sdev.levenshtein = Math.sqrt(sdev.levenshtein / size);
        sdev.mse = Math.sqrt(sdev.mse / size);
        sdev.psnr = Math.sqrt(sdev.psnr / size);
    }

    private static int sqr(int x)
    {
        return x * x;
    }

    private static double sqr(double x)
    {
        return x * x;
    }

    public String toCsvString(boolean isOcrMode)
    {
        StringBuilder sb = new StringBuilder(csvHeader(isOcrMode));
        for (RecognitionErrors row : rows)
            sb.append("\n").append(row.toCsvString(isOcrMode));
        return sb.toString();
    }

    public String toCsvMeanString(boolean isOcrMode, boolean addSDev)
    {
        String csv = mean.name + ";" + mean.counter + ";";
        csv += mean.millis + ";";
        if (isOcrMode)
        {
            csv += mean.error + ";";
            csv += mean.ghost + ";";
            csv += mean.orphan + ";";
        }

        csv += str(mean.accuracy) + ";";
        csv += str(mean.precision) + ";";
        csv += str(mean.recall) + ";";
        csv += str(mean.fscore) + str(sdev.fscore, addSDev) + ";";

        if (isOcrMode)
            csv += str(mean.levenshtein) + str(sdev.levenshtein, addSDev);
        else
        {
            csv += String.format("%.2f", mean.mse) + str(sdev.mse, addSDev) + ";";
            csv += String.format("%.2f", mean.psnr) + str(sdev.psnr, addSDev);
        }
        return csv;
    }

    private static String str(double v)
    {
        return String.format("%.2f", v);
    }

    public static String str(double v, boolean sdev)
    {
        return sdev ? " (" + str(v) + ")" : "";
    }

}
