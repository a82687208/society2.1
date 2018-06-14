package com.example.basemodel;

/**
 * Created by Administrator on 2018/2/25/025.
 */

public class Value {
    public static final String BLUE = "官方蓝";
    public static final String PINK = "粉色";
    public static final String GREEN = "绿色";
    public static final String RED = "红";
    public static final String[] COLOR_NAME = { BLUE, PINK, GREEN };

    private static COLOR colorUI;

    public enum COLOR {
        BLUE(0xff3691f6, Value.BLUE), PINK(0xfff36bf8, Value.PINK),
        //GREEN(0xff29d856, Value.GREEN),
        GREEN(0xff40c362, Value.GREEN),
        RED(0xffff4141, Value.RED);

        private int color;
        private String name;

        COLOR(int color, String name) {
            this.color = color;
            this.name = name;
        }

        public int toValue() {
            return color;
        }

        public String toName() {
            return name();
        }
    }

    static {
        colorUI = COLOR.BLUE;
    }

    public static void setColorUI(COLOR color) {
        colorUI = color;
    }

    public static void setColorUI(String themeName) {
        COLOR[] colors = COLOR.values();
        for (int i = 0; i < COLOR_NAME.length; ++i)
            if (themeName.equals(COLOR_NAME[i]))
                colorUI = colors[i];
    }

    public static COLOR getColorUI() {
        return colorUI;
    }
}
