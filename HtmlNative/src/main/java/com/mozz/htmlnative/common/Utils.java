package com.mozz.htmlnative.common;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.TypedValue;

import com.mozz.htmlnative.attrs.AttrApplyException;
import com.mozz.htmlnative.attrs.PixelValue;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Yang Tao, 17/2/24.
 */

public final class Utils {

    private static final String TAG = Utils.class.getSimpleName();

    private Utils() {
    }

    private static float screenDensity = -1.f;

    public static void init(Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        updateScreenDensity(density);
    }

    public static void updateScreenDensity(float density) {
        screenDensity = density;
    }

    public static void closeQuietly(@Nullable Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
                // do nothing
            }
        }
    }

    public static int toInt(Object object) throws AttrApplyException {
        if (object instanceof Integer) {
            return (int) object;
        } else {
            try {
                int i = Integer.valueOf(object.toString());
                return i;
            } catch (NumberFormatException e) {
                throw new AttrApplyException("can't read int from " + object);
            }
        }
    }

    public static float toFloat(Object object) throws AttrApplyException {
        if (object instanceof Float) {
            return (float) object;
        } else {
            try {
                String fStr = object.toString();
                boolean isPercentage = false;
                if (fStr.endsWith("%")) {
                    fStr = fStr.substring(0, fStr.length() - 1);
                    isPercentage = true;
                }
                float f = Float.valueOf(fStr);

                return isPercentage ? f / 100 : f;
            } catch (NumberFormatException e) {
                throw new AttrApplyException("can't read float from " + object);
            }
        }
    }

    public static PixelValue toPixel(Object object) throws AttrApplyException {
        int unit = TypedValue.COMPLEX_UNIT_PX;
        if (object instanceof String) {
            String string = (String) object;

            StringBuilder unitString = new StringBuilder();
            int i = string.length() - 1;
            for (; i > 0; i--) {
                char c = string.charAt(i);
                if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                    unitString.append(c);
                } else {
                    break;
                }
            }

            try {
                unit = getUnit(unitString.reverse().toString());
            } catch (AttrApplyException e) {
            }

            float value = toFloat(string.substring(0, i + 1));
            return new PixelValue(value, unit);

        } else {
            return new PixelValue(toFloat(object), unit);
        }
    }

    public static int getUnit(String s) throws AttrApplyException {
        switch (s.toLowerCase()) {
            case "px":
                return TypedValue.COMPLEX_UNIT_PX;
            case "dp":
            case "dip":
                return TypedValue.COMPLEX_UNIT_DIP;
            case "sp":
                return TypedValue.COMPLEX_UNIT_SP;
            case "em":
                return PixelValue.EM;
            default:
                return PixelValue.UNSET;

        }
    }

    public static float getPercent(String s) throws AttrApplyException {
        if (s.endsWith("%")) {
            return toInt(s.substring(0, s.length() - 2)) / 100.f;
        } else {
            throw new AttrApplyException("not a percent format " + s);
        }
    }

    public static PixelValue[] pixelGroups(String ss) throws AttrApplyException {
        String[] single = ss.split(" ");

        PixelValue[] pixelValues = new PixelValue[single.length];

        int i = 0;

        for (String s : single) {
            String trimS = s.trim();

            pixelValues[i++] = toPixel(trimS);
        }

        return pixelValues;
    }

    public static float px(Object object) throws AttrApplyException {
        if (object instanceof String) {
            String s = (String) object;

            if (s.endsWith("px")) {
                return toFloat(s.substring(0, s.length() - 2));
            } else {
                return toFloat(s);
            }
        } else {
            return toFloat(object);
        }
    }


    public static boolean toBoolean(Object object) throws AttrApplyException {
        if (object instanceof Boolean) {
            return (boolean) object;
        } else {
            throw new AttrApplyException("can't read boolean from " + object);
        }
    }

    public static int color(@NonNull Object colorObj) throws AttrApplyException {
        String colorString = colorObj.toString().trim();
        if (colorString.length() == 0) {
            throw new AttrApplyException("empty color string for parse");
        }

        // handle the #* like color
        if (colorString.charAt(0) == '#') {

            // handle the #000000 like color string
            if (colorString.length() > 4) {
                try {
                    return Color.parseColor(colorString);
                } catch (IllegalArgumentException e) {
                    throw new AttrApplyException(e);
                }


            } else if (colorString.length() == 4) {
                long color = 0;
                for (int i = 0; i < 3; i++) {
                    char c = colorString.charAt(i + 1);
                    int cI;
                    if (c >= 'a' && c <= 'z') {
                        cI = c - 'a' + 10;
                    } else if (c >= 'A' && c <= 'Z') {
                        cI = c - 'A' + 10;
                    } else if (c >= '0' && c <= '9') {
                        cI = c - '0';
                    } else {
                        throw new AttrApplyException("unknown color string " + colorString);
                    }

                    color |= (cI * 16 + cI) << (3 - i - 1) * 8;
                }

                return (int) (color | 0x00000000ff000000);
            } else {
                throw new AttrApplyException("unknown color string " + colorString);
            }

        } else {
            /**
             handle the color like 'red', 'green' ect. see {@link https://www.w3.org/TR/CSS2/syndata
            .html#tokenization}
             */
            try {
                return Color.parseColor(colorString);
            } catch (IllegalArgumentException e) {
                throw new AttrApplyException("can't read color from " + colorString, e);
            }
        }
    }


    public static Map<String, String> parseStyle(@NonNull String styleString) {
        Map<String, String> pas = new HashMap<>();
        StringBuilder sb = new StringBuilder();
        String key = null;
        for (int i = 0; i < styleString.length(); i++) {
            char c = styleString.charAt(i);

            if (c == ';') {
                pas.put(key, sb.toString());
                sb.setLength(0);
            } else if (c == ':') {
                key = sb.toString();
                sb.setLength(0);
            } else {
                if (c == ' ' || c == '\r' || c == '\n' || c == '\t' || c == '\f' || c == '\b') {
                    continue;
                }
                sb.append(c);
            }
        }

        if (key != null) {
            pas.put(key, sb.toString());
        }

        return pas;
    }

    public static float dp2px(float px) {
        if (screenDensity == -1.f) {
            throw new IllegalStateException("you must call init() first");
        }
        return (int) (screenDensity * px + 0.5f);
    }

    public static float px2dp(float dp) {
        if (screenDensity == -1.f) {
            throw new IllegalStateException("you must call init() first");
        }
        return dp / screenDensity;
    }

    public static int em2px(float em) {
        return (int) (em * 16.f);
    }

    public static float px2em(int px) {
        return px / 16.f;
    }
}
