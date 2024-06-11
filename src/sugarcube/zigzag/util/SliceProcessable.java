package sugarcube.zigzag.util;

public interface SliceProcessable {
    void run(int index, int minIncluded, int maxExcluded);
}