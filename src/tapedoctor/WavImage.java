/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tapedoctor;

import java.util.ArrayList;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 *
 * @author aguyon
 */
public class WavImage extends Canvas {
    
    private WavFile wavFile;
    
    public WavImage(int width, int height, WavFile wavFile) {
        super(width, height);
        
        this.wavFile = wavFile;
    }
    
    public void draw(double offsetValue, double zoomValue) {
        GraphicsContext gc = getGraphicsContext2D();
        
        double width = getWidth();
        double height = getHeight();
        double midHeight = height / 2;
        
        gc.beginPath();        
        
        // Background
        gc.setFill(Color.WHITE);
        gc.fillRect(0,0,width,height);
        
        // Horizontal line
        gc.setStroke(Color.LIGHTGREY);
        gc.setLineWidth(1.0);
        gc.moveTo(0, midHeight);
        gc.lineTo(width, midHeight);
        
        gc.setStroke(Color.BLACK);
        gc.moveTo(0, midHeight);
        
        double stepZoom0 = wavFile.getNumSamples() / (double)width;
        double stepZoom100 = 1;
        double step = (stepZoom100 * zoomValue) + (stepZoom0 * (1 - zoomValue));
        
        double samplesOnScreen = step * width;
        double remainingSamples = wavFile.getNumSamples() - samplesOnScreen;
        final double offset = offsetValue * remainingSamples / 100;
        
        double position = offset;
        for(int x=0; x<width; ++x) {
            double value = wavFile.getSampleValue((int) position);
            position += step;
            double y = midHeight - (value * height);
            gc.lineTo(x, y);
            
            if (wavFile.isHighPeak((int) position)) {
                gc.lineTo(x, 0);
                gc.moveTo(x, y);
            } else if (wavFile.isLowPeak((int) position)) {
                gc.lineTo(x, height);
                gc.moveTo(x, y);
            }
            
        }
        
        gc.stroke();
        
        gc.closePath();
        
        int offsetStart = (int) offset;
        int offsetEnd = (int) (offsetStart + (step * width));
        showErrors(gc, offsetStart, offsetEnd);
        
    }
    
    private void showErrors(GraphicsContext gc, int offsetStart, int offsetEnd) {
        
        ArrayList<WavFile.MissingBitInfo> missingBits = wavFile.getMissingBits();
        
        double screenWidth = getWidth();
        double offsetWidth = offsetEnd - offsetStart;
        double pixelsPerOffset = screenWidth / offsetWidth;
        
        if (missingBits.size() > 0) {
            gc.beginPath(); 
            gc.setStroke(Color.RED);
            
            for(WavFile.MissingBitInfo info : missingBits) {
                int start = info.offsetStart;
                if (start >= offsetEnd) {
                    continue;
                }
                int end = info.offsetEnd;
                if (end <= offsetStart) {
                    continue;
                }
                
                double leftOffset = start-offsetStart;
                double leftPixel = leftOffset * pixelsPerOffset;

                double rightOffset = end-offsetStart;
                double rightPixel = rightOffset * pixelsPerOffset;
                
                gc.moveTo(leftPixel, 0);
                gc.lineTo(rightPixel, 10);
               
            }

            gc.stroke();
            gc.closePath();
        }
    }
    
}
