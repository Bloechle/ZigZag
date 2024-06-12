package zig.zag.util;

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

    public int getInt(String key, int defaultValue) {
        return argsMap.containsKey(key) ? Integer.parseInt(argsMap.get(key)) : defaultValue;
    }

    public String get(String key, String defaultValue) {
        return argsMap.getOrDefault(key, defaultValue);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return argsMap.containsKey(key) ? Boolean.parseBoolean(argsMap.get(key)) : defaultValue;
    }

}
