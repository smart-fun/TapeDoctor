/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tapedoctor;

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
    
    public void draw(double zoomValue) {
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
        int position = 0;
        for(int x=0; x<width; ++x) {
            double value = wavFile.getSampleValue(position);
            position += step;
            double y = midHeight + (value * midHeight);
            gc.lineTo(x, y);
        }
        
        gc.stroke();
        
        gc.closePath();
        
    }
    
}
