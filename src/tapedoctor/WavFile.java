/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tapedoctor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author aguyon
 */
public class WavFile {
    
    private byte[] fileBuffer;
    
    public WavFile(File file) {
        
        try {
            fileBuffer = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
        } catch (IOException ex) {
            Logger.getLogger(WavFile.class.getName()).log(Level.SEVERE, null, ex);
        }        
    }
    
    public boolean isValid() {
        boolean isRiff = ByteHelper.equalsString(fileBuffer, 0, "RIFF");
        return isRiff;
    }
    
}
