package sugarcube.zigzag.evaluation;

import sugarcube.zigzag.util.ImageUtil;

import java.io.File;

public class DatasetNormalization
{

    public static void main(String... args)
    {
        String datasetPath = "C:/Projects/ZigZag/eval/Wezut/dataset/";

        int mode = 2;

        switch (mode)
        {
            case 0:
                //convert
                ImageUtil.converFolderToPng(datasetPath, ".bmp", true);
                break;
            case 1:
                //rename
                for (File file : ImageUtil.listFiles(new File(datasetPath), ".png"))
                {
                    String oldName = file.getName();
                    String newName = file.getName().replace("ls", "las");

                    if (!oldName.equals(newName))
                    {
                        System.out.println(oldName + "->" + newName);
                        file.renameTo(new File(file.getParent(), newName));
                    }
                }
                break;
            case 2:
                //write OCR ground-truth images
                writeImageWithOcr(datasetPath, "_.jpg", false);
                break;
            default:
                ImageUtil.deleteFiles(new File(datasetPath), "_ocr");
                break;
        }

    }

    private static void writeImageWithOcr(String datasetPath, String suffix, boolean normalizeSymbols)
    {
        File[] images = ImageUtil.listImageFiles(new File(datasetPath));
        for (int i = 0; i < images.length; i++)
            if(!images[i].getPath().contains("_ocr"))
            {
                OCRPage page = OCRPage.read(ImageUtil.removeExtension(images[i].getPath()) + ".ocr");
                if (normalizeSymbols)
                    page.normalizeSymbols();
                page.writeOcrDebugImage(images[i], suffix);
                System.out.println((i + 1) + "/" + images.length + ") " + images[i]);
            }
    }
}
