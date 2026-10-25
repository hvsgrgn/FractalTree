import javax.swing.*;
import java.awt.*;
import java.util.concurrent.*;

//TODO:  import only those that we truly need to save size + what is awt?



public class FractalTree extends Canvas {
    //we are using this to make the drawing slower to witness it when given "true" as console parameter
    private static boolean slowMode;
    private static BlockingQueue<DrawCommand> drawQueue;
    private static ExecutorService executor;
    //common key for protecting the task counter from simultaneous manipulation
    private static final Object counterLock = new Object();
    private static int taskCounter = 0;

    // TODO: move class into a separate file
    //this class stores the parameters for the given level of the drawing
    static class DrawCommand {
        int x1, y1, x2, y2;
        Color color;

        DrawCommand(int x1, int y1, int x2, int y2, Color color) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.color = color;
        }
    }

    //this function handles the calculation of the tree levels
    public static void makeFractalTree(int x, int y, int angle, int height) {
        if (slowMode) {
            try {Thread.sleep(100);} //we make it slower with sending the thread to sleep for 0.1 secs each level
            catch (InterruptedException ie) {ie.printStackTrace();} //TODO: make this warning go away
        }

        //we update the stopping condition
        if (height == 0) {
            decrementCounter();
            return;
        }

        //we calculate the ending coordinates
        int x2 = x + (int)(Math.cos(Math.toRadians(angle)) * height * 8);
        int y2 = y + (int)(Math.sin(Math.toRadians(angle)) * height * 8); //TODO: understand this algorythm
        //for the last 5 levels, we switch from black branches to green ones
        Color color = (height < 5) ? Color.GREEN : Color.BLACK;

        // We add the drawing command to the execution queue
        try {
            drawQueue.put(new DrawCommand(x, y, x2, y2, color));
        } catch (InterruptedException e) {
            e.printStackTrace(); //TODO: warning
        }

        // First recursive call via ExecutorService //TODO: understand this as well
        incrementCounter(); //we increase the number of tasks with one, as we just enlisted another drawing command
        executor.submit(() -> makeFractalTree(x2, y2, angle - 20, height - 1));

        // Second recursive call directly this time:
        makeFractalTree(x2, y2, angle + 20, height - 1);
    }

    //Safe incrementation of the total task count with a common key
    private static void incrementCounter() {
        synchronized (counterLock) {
            taskCounter++;
        }
    }

    //safe decrementation of the total task count with a common key
    private static void decrementCounter() {
        synchronized (counterLock) {
            taskCounter--;
            if (taskCounter == 0) { //we notify the lock when we ran out of tasks to execute
                counterLock.notify();
            }
        }
    }

    //the isolated ETD code responsible exclusively for drawing processes
    @Override
    public void paint(Graphics g) {
        while (true) { //infinite loop TODO: check why it says this will always end in an exception
            try {
                DrawCommand cmd = drawQueue.poll(100, TimeUnit.MILLISECONDS);//one-by one we execute the drawing commands
                if (cmd != null) {
                    g.setColor(cmd.color);
                    g.drawLine(cmd.x1, cmd.y1, cmd.x2, cmd.y2);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //the main thread will tie the others together
    public static void main(String args[]) {
        //slow mode should only be interpreted if we have arguments at all and parsing it into a boolean is successful
        slowMode = args.length != 0 && Boolean.parseBoolean(args[0]);

        // Making a thread-safe queue with the maximum capacity of 1000
        drawQueue = new LinkedBlockingQueue<>(1000);

        // Making a threadpool with a maximum capacity of 128 as required
        executor = Executors.newFixedThreadPool(Math.min(128, Runtime.getRuntime().availableProcessors() * 2));

        // Initializing graphic elements
        FractalTree tree = new FractalTree();
        JFrame frame = new JFrame(); //TODO: what is this?
        frame.setSize(800, 600); //making a canvas for the tree to be drawn onto
        frame.setVisible(true);
        frame.add(tree); //adding the tree object

        // The first task being executed
        incrementCounter();
        executor.submit(() -> makeFractalTree(390, 480, -90, 10));

        //TODO: is this busy waiting?
        synchronized (counterLock) {
            while (taskCounter > 0) {
                try {
                    counterLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        // ExecutorService shutting down
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS); //let's wait until all tasks are completed or 10 secs
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // We give feedback on the console about the main thread ending
        System.out.println("Main has finished");
    }
}
