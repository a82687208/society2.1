package com.gdptc.society.tools;

/**
 * Created by Administrator on 2018/3/28/028.
 */

public class MathUtil {
    public static int getNumberLength(int n) {
        int length = 0;
        while (n > 0) {
            n = n / 10;
            length++;
        }
        return length;
    }
}
