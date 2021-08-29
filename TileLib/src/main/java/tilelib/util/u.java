/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tilelib.util;

/**
 *
 * @author joshua.marinacci@sun.com
 */
public class u {
    private static long time;

    public static void p(Exception ex) {
        p(ex.getMessage());
        ex.printStackTrace();
    }

    public static void p(String string) {
        System.out.println(string);
    }

    public static void startTimer() {
        time = System.currentTimeMillis();
    }

    public static void stopTimer() {
        long current = System.currentTimeMillis();
        System.out.println("elapsed time: " + (current-time)+" msec");
    }

}
