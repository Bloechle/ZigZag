package sugarcube.zigzag.util;

public class SliceProcessThreadPool
{
    public static final SliceProcessThreadPool EMPTY_THREAD_POOL = new SliceProcessThreadPool(0);

    private final ProcessThread[] threads;

    public SliceProcessThreadPool(int size)
    {
        threads = new ProcessThread[size];
        for (int i = 0; i < threads.length; i++)
            threads[i] = new ProcessThread(i);
    }

    public void execute(int size, SliceProcessable runnable)
    {
        execute(size, true, runnable);
    }

    public void execute(int size, boolean doSlice, SliceProcessable runnable)
    {
        int nbOfThreads = Math.max(threads.length, 1);
        int sliceSize = doSlice ? size / nbOfThreads : 1;
        for (int i = 0; i < (doSlice ? nbOfThreads : size); i++)
        {
            int threadIndex =i;
            if (threadIndex < threads.length)
            {
                final int minIncluded = threadIndex * sliceSize;
                final int maxExcluded = minIncluded + sliceSize;
                threads[threadIndex].execute(() -> runnable.run(threadIndex, minIncluded, threadIndex == threads.length - 1 ? size : maxExcluded));
            } else
                runnable.run(threadIndex, 0, size);
        }
        waitUntilProcessingDone();
    }

    public int size()
    {
        return threads.length;
    }

    public int nbOfThreads()
    {
        return threads.length;
    }

    public void waitUntilProcessingDone()
    {
        if (threads.length == 0)
            return;
        boolean doLoop;
        do
        {
            doLoop = false;
            try
            {
                for (ProcessThread thread : threads)
                    if (!thread.isDone())
                    {
                        doLoop = true;
                        break;
                    }
                Thread.sleep(1);
            } catch (Exception e)
            {
                e.printStackTrace();
            }

        } while (doLoop);
    }

    public void kill()
    {
        for (ProcessThread thread : threads) thread.kill();
    }
}
