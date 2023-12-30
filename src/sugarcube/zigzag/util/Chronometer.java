package sugarcube.zigzag.util;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public class Chronometer
{
    private final String name;
    private final LinkedHashMap<String, Long> times = new LinkedHashMap<>();
    private long start_ms = 0;
    private long last_ms = 0;
    private StringBuilder csv;
    private String message;

    public Chronometer(String name)
    {
        this.name = name;
        this.csv = new StringBuilder();
        csv.append("filename").append(";").append("millis").append("\n");
    }

    public void reset(String message)
    {
        this.message = message;
        times.clear();
        start_ms = System.currentTimeMillis();
        last_ms = start_ms;
    }

    public void addTime(String name)
    {
        long time = System.currentTimeMillis();
        times.put(name, time - last_ms);
        last_ms = time;
    }

    public long elapsedTime()
    {
        return last_ms - start_ms;
    }

    public void stop(File imageFile)
    {
        this.last_ms = System.currentTimeMillis();
        if (imageFile != null)
            csv.append(imageFile.getName()).append(";").append(elapsedTime()).append("\n");
    }

    public void writeProcessingTimes(File filePath)
    {
        ImageUtil.writeText(filePath, csv.toString());
    }

    public void printTimes()
    {
        System.out.println("\n" + name + " · " + elapsedTime() + "ms · " + message);
        for (Map.Entry<String, Long> entry : times.entrySet())
        {
            long millis = entry.getValue();
            //if (millis > 0)
            System.out.println(" ▪ " + entry.getKey() + " · " + millis + "ms");
        }
    }


}
