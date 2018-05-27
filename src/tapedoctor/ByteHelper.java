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

    public static int getInt2(byte[] buffer, int offset) {
        return (int) getInt(buffer, offset, 2);
    }
        
    public static long getInt4(byte[] buffer, int offset) {
        return getInt(buffer, offset, 4);
    }
    
    private static long getInt(byte[] buffer, int offset, int numBytes) {
        long result = 0;
        for(int i=0; i<numBytes; ++i) {
            int value = buffer[offset + numBytes - 1 - i];
            if (value < 0) {
                value += 256;
            }
            result = (result << 8) + value;
        }
        return result;
    }
    
}
