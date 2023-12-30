package sugarcube.zigzag.evaluation;


import sugarcube.zigzag.util.Box2D;

import java.text.Normalizer;

public class OCRSymbol
{
    public Box2D box;
    public String symbol;
    public float confidence;

    public OCRSymbol match;

    public OCRSymbol(String symbol, float confidence, Box2D box)
    {
        this.symbol = symbol;
        this.confidence = confidence;
        this.box = box;
    }

    public void upsample()
    {
        if (box != null)
            box = new Box2D(2 * box.x0, 2 * box.x1, 2 * box.y0, 2 * box.y1);
    }

    public void downsample()
    {
        if (box != null)
            box = new Box2D(box.x0 / 2, box.x1 / 2, box.y0 / 2, box.y1 / 2);
    }

    public void normalize()
    {
        symbol = Normalizer.normalize(symbol, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "").toLowerCase();
    }

    public boolean isMatchCorrect()
    {
        return match != null && match.symbol.equals(symbol);
    }


}
