package sugarcube.zigzag.util;


public class ProcessThread implements Runnable
{
    private final Thread thread;
    private Runnable runnable;

    private boolean isProcessing = false;
    private boolean doKill = false;

    public final int index;

    public ProcessThread()
    {
        this(-1);
    }

    public ProcessThread(int index)
    {
        this.index = index;
        thread = new Thread(this, "ProcessThread");
        thread.setDaemon(false);
        thread.setPriority(8);
        thread.start();
    }

    public void kill()
    {
        doKill = true;
    }

    public void execute(Runnable runnable)
    {
        this.runnable = runnable;
    }

    public void run()
    {
        while (!doKill)
        {
            try
            {
                Runnable currentRunnable = runnable;
                if (currentRunnable != null)
                {
                    isProcessing = true;
                    runnable = null;
                    currentRunnable.run();
                    isProcessing = false;
                }
                Thread.sleep(1);
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        isProcessing = false;
    }

    public boolean isProcessing()
    {
        return isProcessing;
    }

    public boolean isDone()
    {
        return runnable == null && !isProcessing;
    }
}
