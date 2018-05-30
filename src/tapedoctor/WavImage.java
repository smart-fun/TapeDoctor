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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 *
 * @author aguyon
 */
public class WavImage extends Canvas {
    
    private WavFile wavFile;
    private double displayOffset = 0;
    private double displayZoom = 0;
    private int currentError = -1;
    
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
                    ++currentError;
                    if (currentError >= missingBits.size()) {
                        currentError = 0;
                    }
                    jumpToCurrentError(missingBits);
                }
            }
            
        });
    }
    
    public void jumpToPreviousError() {
        ArrayList<WavFile.MissingBitInfo> missingBits = wavFile.getMissingBits();
        if (!missingBits.isEmpty()) {
            --currentError;
            if (currentError < 0) {
                currentError = missingBits.size() - 1;
            }
            jumpToCurrentError(missingBits);
        }
    }
    public void jumpToNextError() {
        ArrayList<WavFile.MissingBitInfo> missingBits = wavFile.getMissingBits();
        if (!missingBits.isEmpty()) {
            ++currentError;
            if (currentError >= missingBits.size()) {
                currentError =  0;
            }
            jumpToCurrentError(missingBits);
        }
    }
    
    private void jumpToCurrentError(ArrayList<WavFile.MissingBitInfo> missingBits) {
        WavFile.MissingBitInfo firstError = missingBits.get(currentError);
        displayOffset = firstError.offsetStart - (getWidth() * 0.4);
        displayZoom = 1;
        draw();
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
                gc.moveTo(x, 0);
                gc.lineTo(x, Math.max(midHeight * 0.15, y));
            } else if (wavFile.isLowPeak((int) position)) {
                gc.moveTo(x, height);
                gc.lineTo(x, Math.min(height * 0.9, y));
            }
            gc.moveTo(x, y);
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
            
            // display missing bits areas
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
                
                boolean displayDetails = (displayZoom >= 0.99);
                
                if (displayDetails) {
                    gc.setFill(Color.BLACK);
                    gc.setFont(Font.font("Arial", FontWeight.BOLD, 12));
                    gc.fillText(info.offsetStart + " -> " + info.offsetEnd, leftPixel + 20, HalfHeight * 0.1);
                }

                if (info.forcedValues.isEmpty()) {
                    gc.setFill(new Color(1,0,0, 0.3));
                } else {
                    if (displayDetails) {
                        int value = info.forcedValues.get(0);
                        gc.setFill(Color.BLACK);
                        gc.fillText("Guessed is " + value, leftPixel + 20, HalfHeight * 0.2);
                    }
                    gc.setFill(new Color(0,1,0, 0.3));
                }                                        
                
                gc.fillRect(leftPixel-1, 0, pixelWidth+2, HalfHeight*2);


            }
            //gc.stroke();
            gc.closePath();
            
            // display Bytes
            if (displayZoom >= 0.99) {
                gc.beginPath();
                
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
                    gc.setFill(new Color(1,1,0, 0.3));
                    gc.fillRect(leftPixel, HalfHeight * 1.6, pixelWidth, HalfHeight*2);
                    
                    gc.setFill(Color.BLACK);
                    gc.setFont(Font.font("Arial", FontWeight.BOLD, 12));
                    gc.fillText("" + byteInfo.value, leftPixel + 20, HalfHeight * 1.75);
                }
                
                gc.closePath();
            }
            
            // display bits
            if (displayZoom >= 0.99) {
                gc.beginPath();
                
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
                    
                    gc.setFill(new Color(0,0,1, 0.3));
                    gc.fillRect(leftPixel, HalfHeight * 1.8, pixelWidth, HalfHeight*2); 
                    
                    gc.setFill(Color.BLACK);
                    gc.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
                    gc.fillText("" + bitInfo.value, leftPixel + 20, HalfHeight * 1.95);

                }                
                
                gc.closePath();
                
            }            
        }
    }
    
}
