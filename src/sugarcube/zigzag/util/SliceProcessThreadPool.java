package sugarcube.zigzag.util;

public class SliceProcessThreadPool {
    public static final SliceProcessThreadPool EMPTY_THREAD_POOL = new SliceProcessThreadPool(0);
    private final ProcessThread[] threads;

    public SliceProcessThreadPool(int size) {
        threads = new ProcessThread[size];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new ProcessThread(i);
        }
    }

    public void execute(int size, SliceProcessable task) {
        execute(size, true, task);
    }

    public void execute(int size, boolean doSlice, SliceProcessable task) {
        int numThreads = Math.max(threads.length, 1);
        int sliceSize = doSlice ? size / numThreads : 1;
        for (int i = 0; i < (doSlice ? numThreads : size); i++) {
            int threadIndex = i;
            if (threadIndex < threads.length) {
                final int minIncluded = threadIndex * sliceSize;
                final int maxExcluded = minIncluded + sliceSize;
                threads[threadIndex].execute(() -> task.run(threadIndex, minIncluded, threadIndex == threads.length - 1 ? size : maxExcluded));
            } else {
                task.run(threadIndex, 0, size);
            }
        }
        waitForCompletion();
    }

    public int size() {
        return threads.length;
    }


    public void waitForCompletion() {
        if (threads.length == 0) return;
        boolean inProgress;
        do {
            inProgress = false;
            try {
                for (ProcessThread thread : threads)
                    if (!thread.isDone()) {
                        inProgress = true;
                        break;
                    }
                Thread.sleep(1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } while (inProgress);
    }

    public void kill() {
        for (ProcessThread thread : threads) thread.kill();
    }
}
