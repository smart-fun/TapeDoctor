/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tapedoctor;

/**
 *
 * @author aguyon
 */
public class ByteHelper {
    
    public static boolean equalsString(byte[] buffer, int offset, String text) {
        int length = text.length();
        int max = buffer.length;
        for(int i=0; i<length; ++i) {
            int pos = i + offset;
            if (pos >= max) {
                return false;
            }
            char car = (char) buffer[pos];
            char other = text.charAt(i);
            if (car != other) {
                return false;
            }
        }
        return true;
    }
    
}
