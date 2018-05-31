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
public class ZX81CharSet {
    
    private static int CHARSET_OFFSET = 11;
    private static String CHARSET = "\"L$:?()><=+-*/;,.0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    
    public static char getAsciiChar(int zxchar) {
        zxchar &= 0x7F;             // bit 7 not used
        if (zxchar >= 0x40) {       // same but video inverse
            zxchar -= 0x40;
        }
        if (zxchar < CHARSET_OFFSET) {
            return ' ';
        } else {
            return CHARSET.charAt(zxchar - CHARSET_OFFSET);
        }
    }
    
}
