package sugarcube.zigzag.util;

public class ProcessThread implements Runnable {
    private final Thread thread;
    private Runnable task;
    private boolean isProcessing = false;
    private boolean terminate = false;
    public final int index;

    public ProcessThread(int index) {
        this.index = index;
        thread = new Thread(this, "ProcessThread-" + index);
        thread.setDaemon(true);
        thread.setPriority(Thread.NORM_PRIORITY + 1);
        thread.start();
    }

    public ProcessThread() {
        this(-1);
    }

    public void kill() {
        terminate = true;
    }

    public void execute(Runnable task) {
        this.task = task;
    }

    @Override
    public void run() {
        while (!terminate) {
            try {
                if (task != null) {
                    isProcessing = true;
                    task.run();
                    task = null;
                    isProcessing = false;
                }
                Thread.sleep(1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        isProcessing = false;
    }

    public boolean isProcessing() {
        return isProcessing;
    }

    public boolean isDone() {
        return task == null && !isProcessing;
    }
}
