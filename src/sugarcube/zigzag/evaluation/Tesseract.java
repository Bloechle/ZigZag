package sugarcube.zigzag.evaluation;

import sugarcube.zigzag.util.ImageUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class Tesseract
{
    private static final String PATH_TO_EXE = "C:/Program Files/Tesseract-OCR/tesseract.exe ";

    public static void main(String... args)
    {
        String text = getText(ImageUtil.getDesktopPath()+"ZigZag/Mobile/color/samN10p-quarto2-book-bk1p1-foff.png", true);
        log(".main - text="+text);
    }

    public static String getText(String filepath, boolean removeTextFile)
    {

        try
        {
            //        cmd+="--print-parameters";
            String cmd = PATH_TO_EXE+" " +filepath+" "+ImageUtil.removeExtension(filepath)+" -l eng txt";
            StringBuilder output = new StringBuilder();
            log(".exec - " + cmd);

            Process p = Runtime.getRuntime().exec(cmd);

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String line = "";
            while ((line = reader.readLine()) != null)
            {
                System.out.println(line);
                output.append(line).append("\n");
                log(line);
            }

            while ((line = error.readLine()) != null)
            {
                System.out.println(line);
                log("! " + line);
            }
            p.waitFor();
            log(".exec - console: " + output);

            File textFile = new File(ImageUtil.removeExtension(filepath)+".txt");
            String ocr= ImageUtil.readText(textFile);
            if(removeTextFile)
                textFile.delete();
            return ocr;

        } catch (Exception e)
        {
            e.printStackTrace();
            log(".exec - process failed");
        }

        return "";
    }

    public static void log(String msg)
    {
        System.out.println("Tesseract"+msg);
    }
}
