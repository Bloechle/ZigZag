package zig.zag.process;

public interface SliceProcessable {
    void run(int index, int minIncluded, int maxExcluded);
}