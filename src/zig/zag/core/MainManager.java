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
                printUsage();
            }
        } catch (Exception e) {
            ImageUtil.printStackTrace(e);
            printUsage();
        }
    }

    public static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java Main [-size <size>] [-percent <percent>] [-mode <mode>] [-threads <threads>] [-exit <true/false>] -input <inputFilePath> [-output <outputFilePath>] [-debug <true/false>]");
        System.out.println("    -input <inputFilePath>   : Path to the input image file (required)");
        System.out.println("    -output <outputFilePath> : Path to the output image file (optional, default is inputFilePath with 'ZZ' postfix)");
        System.out.println("    -size <size>             : Window size (optional, default is 30)");
        System.out.println("    -percent <percent>       : Mean weight for historical documents (optional, default is 90)");
        System.out.println("    -mode <mode>             : Processing mode (optional, default is 1, 0=binary, 1=binary upsampled, 2=binary antialiased, 3=gray, 4=color)");
        System.out.println("    -threads <threads>       : Number of threads for processing (optional, default is half the number of available processors)");
        System.out.println("    -lossless <true/false>   : Enable or disable lossless compression (optional, default is true)");
        System.out.println("    -debug <true/false>      : Enable or disable debug mode (optional, default is false)");
        System.out.println("    -exit <true/false>       : Whether to exit the application after processing (optional, default is false)");
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
        int percent = argsParser.getInt("-percent", 90);
        int mode = argsParser.getInt("-mode", ImageFilter.MODE_BINARY_UPSAMPLED);
        int threads = argsParser.getInt("-threads", Math.max(1, Runtime.getRuntime().availableProcessors() / 2));
        boolean debug = argsParser.getBoolean("-debug", false);
        boolean lossless = argsParser.getBoolean("-lossless", true);
        String inputFilePath = argsParser.get("-input");
        String outputFilePath = argsParser.get("-output", inputFilePath.replaceFirst("\\.(?=[^.]+$)", "_ZZ."));

        printConfiguration(size, percent, mode, threads, debug, lossless, inputFilePath, outputFilePath);

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

    private static void printConfiguration(int size, int percent, int mode, int threads, boolean debug, boolean lossless, String inputFilePath, String outputFilePath) {
        System.out.println("Configuration:");
        System.out.println("  Size: " + size);
        System.out.println("  Percent: " + percent);
        System.out.println("  Mode: " + mode);
        System.out.println("  Threads: " + threads);
        System.out.println("  Debug: " + debug);
        System.out.println("  Lossless: " + lossless);
        System.out.println("  Input File: " + inputFilePath);
        System.out.println("  Output File: " + outputFilePath);
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
