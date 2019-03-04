package org.team5.app.main;

import org.team5.app.dataprocessing.CSVReader;
import org.team5.app.dataprocessing.DataPoint;
import org.team5.app.gui.SwingUI;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ProcessingThread implements Runnable {

    public BlockingQueue<DataPoint> buffer;

    public ProcessingThread(BlockingQueue<DataPoint> buffer) {
        this.buffer = buffer;
    }

    /**
     * This thread fetches the message rates from the blocking queue buffer.
     * A BlockingQueue is a queue that additionally supports operations that wait for
     * the queue to become non-empty when retrieving an element, and wait for space to become available
     * in the queue when storing an element.
     * <p>
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     */
    @Override
    public void run() {

        SwingUI.uploadButton.setEnabled(false);
        SwingUI.textArea.setText(null);

        long sumProcessTime = 0;
        long sumMessageRates = 0;
        long sleepTime = 100; //in millisecond
        int progressBarUpdater = 0;
        
        boolean primed = false;
        double processTime = 2d*(0.000000001d); //Change this to the input from the window
        DataAnalyzer analyzer = new DataAnalyzer();
        ArrayBlockingQueue<double[]> bufferedMessages = new ArrayBlockingQueue<double[]>(10000000);
        
        try {

            DataPoint messageRate;
            SimClock clock = new SimClock(processTime);
            
            messageRate = buffer.take();
            double[] message = new double[]{0, 0};
            //Note: value of -1 marks the end of the buffer content.
            while (messageRate.getValue() != -1) {

                long startTime = System.nanoTime(); //Get nano time just before removing message data from buffer
                //-------------------------------
                // Do some processing around at this point.
                
                //First startup
                if(!primed){
                    clock.setTime(messageRate.getTimeIn());
                    bufferedMessages.add(new double[]{messageRate.getValue(), clock.getTime()});
                    primed = true;
                }
                //Updating clock
               
                clock.update();
                if(clock.isNextMinute()){ //If it's a new minute update the message rate
                    messageRate = buffer.take();
                }
                //Adding messages to the buffered data based on the current rate
                if(clock.isNextSecond()){
                    bufferedMessages.add(new double[]{messageRate.getValue(), clock.getTime()});
                }
               
                //Updating Data
                //Update the current set of messages once the program gets through that backlog
                if(message[0] <= 0){
                    if(bufferedMessages.peek() != null){message = bufferedMessages.remove();}
                }
                else{
                    message[0]--;
                }
                if(message[0] < 0){
                    break;
                }
                
                //Recording Stats
                analyzer.writeData(message[1], clock.getTime());
                
                // Let's just simulate work time with Thread.sleep()
                //---------------------------------
                // End simulation
                //---------------------------------

                long timeNow = System.nanoTime(); //get current nano time after the processing above
                long estimatedTime = timeNow - startTime; //This gives time spent per message data

                sumProcessTime += estimatedTime;

                progressBarUpdater++;

                SwingUI.updateProgressBar(progressBarUpdater);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        SwingUI.textArea.append("Total latency (ns): " + sumProcessTime+"\n");
        SwingUI.textArea.append("No of Messages: " + sumMessageRates+"\n");
        SwingUI.textArea.append("Average latency (ns): " + (double) sumProcessTime / sumMessageRates+"\n");
        SwingUI.textArea.append("Throughput (Messages/sec): " + (double) sumMessageRates / (sumProcessTime *1e-9)+"\n");
        SwingUI.textArea.append(analyzer.printStats());
        SwingUI.uploadButton.setEnabled(true);
    }
}
