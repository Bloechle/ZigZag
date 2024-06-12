package zig.zag.core;

import zig.zag.ZigZag;
import zig.zag.util.ArgsParser;
import zig.zag.util.ImageUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainManager {
    private static final List<String[]> processingResults = new ArrayList<>();

    public static void main(String... args) {
        ArgsParser argsParser = new ArgsParser(args);

        try {
            if (argsParser.has("-input")) {
                processImage(argsParser);
            } else {
                argsParser.printUsage();
            }
        } catch (Exception e) {
            ImageUtil.printStackTrace(e);
            argsParser.printUsage();
        }
    }

    private static void processDirectory(File inputDir, File outputDir, int size, int percent, int mode, int threads, boolean lossless, boolean debug) {
        if (!outputDir.exists() && outputDir.mkdirs()) {
            System.out.println("Directory created: " + outputDir);
        }

        File[] imageFiles = inputDir.listFiles();
        if (imageFiles != null) {
            for (File file : imageFiles) {
                if (file.isDirectory()) {
                    processDirectory(file, new File(outputDir, file.getName()), size, percent, mode, threads, lossless, debug);
                } else {
                    String fileName = file.getName().toLowerCase();
                    if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png") || fileName.endsWith(".bmp")) {
                        String outputFilePath = new File(outputDir, file.getName().replaceFirst("\\.[^.]+$", lossless ? ".png" : ".jpg")).getPath();
                        long processingTime = processSingleImage(size, percent, mode, threads, lossless, debug, file.getPath(), outputFilePath);
                        processingResults.add(new String[]{"\""+file.getPath()+"\"", Long.toString(processingTime)});
                    }
                }
            }
        }
    }

    private static void processImage(ArgsParser argsParser) {
        int size = argsParser.getInt("-size", 30);
        int percent = argsParser.getInt("-percent", 100);
        int mode = argsParser.getInt("-mode", ImageFilter.MODE_BINARY_UPSAMPLED);
        int threads = argsParser.getInt("-threads", Math.max(1, Runtime.getRuntime().availableProcessors() / 2));
        boolean debug = argsParser.getBoolean("-debug", false);
        boolean lossless = argsParser.getBoolean("-lossless", true);
        String inputFilePath = argsParser.get("-input");
        String outputFilePath = argsParser.get("-output", inputFilePath.replaceFirst("\\.(?=[^.]+$)", "_ZZ."));

        File inputFile = new File(inputFilePath);
        File outputFile = new File(outputFilePath);

        if (inputFile.isDirectory()) {
            processDirectory(inputFile, outputFile, size, percent, mode, threads, lossless, debug);
            generateCSVReport(outputFile.getPath());
        } else {
            processSingleImage(size, percent, mode, threads, lossless, debug, inputFilePath, outputFilePath);
        }

        if (argsParser.getBoolean("-exit", false)) {
            System.exit(0);
        }
    }

    private static long processSingleImage(int size, int percent, int mode, int threads, boolean lossless, boolean debug, String inputFilePath, String outputFilePath) {
        ZigZag zigzag = new ZigZag(size, percent, mode, threads);
        zigzag.losslessEnabled = lossless;
        zigzag.debugEnabled = debug;

        zigzag.applyFilter(inputFilePath, outputFilePath);
        zigzag.dispose();
        return zigzag.getProcessingTime();
    }

    private static void generateCSVReport(String outputDirPath) {
        File csvFile = new File(outputDirPath, "processing_report.csv");
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.append("FilePath;ElapsedMillis\n");
            for (String[] record : processingResults) {
                writer.append(String.join(";", record)).append("\n");
            }
            System.out.println("CSV report generated: " + csvFile.getPath());
        } catch (IOException e) {
            ImageUtil.printStackTrace(e);
        }
    }
}
