package sugarcube.zigzag.util;

import java.util.HashMap;
import java.util.Map;

public class ArgsParser {
    private final Map<String, String> argsMap = new HashMap<>();

    public ArgsParser(String[] args) {
        parseArgs(args);
    }

    private void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-")) {
                String key = args[i];
                if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                    String value = parseValue(args, ++i);
                    argsMap.put(key, value);
                } else {
                    argsMap.put(key, "true");
                }
            }
        }
    }

    private String parseValue(String[] args, int index) {
        StringBuilder value = new StringBuilder(args[index]);
        if (value.charAt(0) == '"') {
            value = new StringBuilder(value.substring(1));
            while (index + 1 < args.length && !args[index + 1].endsWith("\"")) {
                value.append(" ").append(args[++index]);
            }
            if (index + 1 < args.length && args[index + 1].endsWith("\"")) {
                value.append(" ").append(args[++index], 0, args[index].length() - 1);
            }
        }
        return value.toString();
    }

    public boolean has(String key) {
        return argsMap.containsKey(key);
    }

    public String get(String key) {
        return argsMap.get(key);
    }

    public int getInt(String key) {
        return Integer.parseInt(argsMap.get(key));
    }

    public int getInt(String key, int defaultValue) {
        return argsMap.containsKey(key) ? Integer.parseInt(argsMap.get(key)) : defaultValue;
    }

    public String get(String key, String defaultValue) {
        return argsMap.getOrDefault(key, defaultValue);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return argsMap.containsKey(key) ? Boolean.parseBoolean(argsMap.get(key)) : defaultValue;
    }

    public void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java Main [-size <size>] [-percent <percent>] [-mode <mode>] [-threads <threads>] [-exit <true/false>] -input <inputFilePath> [-output <outputFilePath>] [-debug <true/false>] [-printTimes <true/false>]");
        System.out.println("    -input <inputFilePath>   : Path to the input image file (required)");
        System.out.println("    -output <outputFilePath> : Path to the output image file (optional, default is inputFilePath with 'ZZ' postfix)");
        System.out.println("    -size <size>             : Size for processing (optional, default is 30)");
        System.out.println("    -percent <percent>       : Percent for processing (optional, default is 100)");
        System.out.println("    -mode <mode>             : Mode for processing (optional, default is 1)");
        System.out.println("    -threads <threads>       : Number of threads for processing (optional, default is half the number of available processors)");
        System.out.println("    -debug <true/false>      : Enable or disable debug mode (optional, default is false)");
        System.out.println("    -printTimes <true/false> : Enable or disable printing times (optional, default is false)");
        System.out.println("    -exit <true/false>       : Whether to exit the application after processing (optional, default is false)");
    }
}
