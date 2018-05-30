/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tapedoctor;

import java.util.ArrayList;
import javafx.event.EventHandler;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

/**
 *
 * @author aguyon
 */
public class WavImage extends Canvas {
    
    private WavFile wavFile;
    private double displayOffset = 0;
    private double displayZoom = 0;
    
    public WavImage(int width, int height, WavFile wavFile) {
        super(width, height);
        
        this.wavFile = wavFile;
        
        this.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                /*double x = event.getX();
                double stepZoom0 = wavFile.getNumSamples() / (double)width;
                double stepZoom100 = 1;
                double step = (stepZoom100 * displayZoom) + (stepZoom0 * (1 - displayZoom));
                double samplesOnScreen = step * width;
                displayOffset += x * samplesOnScreen / width;
                displayZoom = 1;*/
                
                ArrayList<WavFile.MissingBitInfo> missingBits = wavFile.getMissingBits();
                if (!missingBits.isEmpty()) {
                    WavFile.MissingBitInfo firstError = missingBits.get(0);
                    displayOffset = firstError.offsetStart - (width / 2);
                    displayZoom = 1;
                }
                
                draw();
            }
            
        });
    }
    
    // 0-100
    public void setOffsetPercent(double offsetPercent) {
        double width = getWidth();
        double stepZoom0 = wavFile.getNumSamples() / (double)width;
        double stepZoom100 = 1;
        double step = (stepZoom100 * displayZoom) + (stepZoom0 * (1 - displayZoom));
        double samplesOnScreen = step * width;
        double remainingSamples = wavFile.getNumSamples() - samplesOnScreen;
        final double offset = offsetPercent * remainingSamples / 100;
        displayOffset = offset;
    }
    
    // 0-1
    public void setZoom(double zoomValue) {
        displayZoom = zoomValue;
    }
    
    public void draw() {
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
        double step = (stepZoom100 * displayZoom) + (stepZoom0 * (1 - displayZoom));
        
        double samplesOnScreen = step * width;
        double remainingSamples = wavFile.getNumSamples() - samplesOnScreen;
        //final double offset = displayOffset * remainingSamples / 100;
        final double offset = displayOffset;
        
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
        double HalfHeight = getHeight() / 2;
        
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
                double pixelWidth = rightPixel - leftPixel;
                
                gc.setFill(new Color(1,0,0, 0.3));
                gc.fillRect(leftPixel-1, 0, pixelWidth+2, HalfHeight*2);               
            }
            //gc.stroke();
            gc.closePath();
            
            // display Bytes
            if (displayZoom >= 0.99) {
                gc.beginPath();
                gc.setFill(new Color(1,1,0, 0.3));
                
                ArrayList<WavFile.ByteInfo> bytes = wavFile.getBytes();
                for(WavFile.ByteInfo byteInfo : bytes) {
                    double leftOffset = byteInfo.offsetStart - offsetStart;
                    double leftPixel = leftOffset * pixelsPerOffset;
                    
                    double rightOffset = byteInfo.offsetEnd - offsetStart;
                    double rightPixel = rightOffset * pixelsPerOffset;
                    
                    double pixelWidth = rightPixel - leftPixel;
                    if (leftPixel + pixelWidth <= 0) {
                        continue;
                    }
                    if (leftPixel > screenWidth) {
                        continue;
                    }
                    gc.fillRect(leftPixel, HalfHeight * 1.6, pixelWidth, HalfHeight*2); 
                }
                
                gc.closePath();
            }
            
            // display bits
            if (displayZoom >= 0.99) {
                gc.beginPath();
                gc.setFill(new Color(0,0,1, 0.3));
                
                ArrayList<WavFile.BitInfo> bits = wavFile.getBits();
                for(WavFile.BitInfo bitInfo : bits) {
                    double leftOffset = bitInfo.offsetStart - offsetStart;
                    double leftPixel = leftOffset * pixelsPerOffset;
                    
                    double rightOffset = bitInfo.offsetEnd - offsetStart;
                    double rightPixel = rightOffset * pixelsPerOffset;
                    
                    double pixelWidth = rightPixel - leftPixel;
                    if (leftPixel + pixelWidth <= 0) {
                        continue;
                    }
                    if (leftPixel > screenWidth) {
                        continue;
                    }
                    gc.fillRect(leftPixel, HalfHeight * 1.8, pixelWidth, HalfHeight*2); 
                }                
                
                gc.closePath();
                
            }            
        }
    }
    
}
