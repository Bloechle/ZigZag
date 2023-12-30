package sugarcube.zigzag.evaluation;

public class RecognitionErrors
{
    public String name;
    public int counter, millis;
    public int error, ghost, orphan;
    public double accuracy, precision, recall, fscore, mse, psnr, levenshtein;


    public String toCsvString(boolean isOcrMode)
    {
        if (isOcrMode)
            return name + ";" + counter + ";" + millis + ";" + error + ";" + ghost + ";" + orphan + ";" + String.format("%.2f", accuracy) + String.format("%.2f", precision) + ";" + String.format("%.2f", recall) + ";" + String.format("%.2f", fscore) + ";" + String.format("%.2f", levenshtein);
        else
            return name + ";" + counter + ";" + millis + ";" + String.format("%.2f", accuracy) + ";" + String.format("%.2f", precision) + ";" + String.format("%.2f", recall) + ";" + String.format("%.2f", fscore) + ";" + String.format("%.2f", mse) + ";" + String.format("%.2f", psnr);
    }
}
