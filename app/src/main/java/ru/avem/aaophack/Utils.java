package ru.avem.aaophack;

class Utils {
    static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static String toHexString(byte[] bytes) {
        return toHexString(bytes, 0, false);
    }

    static String toHexString(byte[] bytes, int offset, boolean isNotFormat) {
        int localLength;
        String result = "";
        label:
        {
            if (offset > 0) {
                localLength = offset;
                if (offset <= bytes.length) {
                    break label;
                }
            }

            localLength = bytes.length;
        }

        for (offset = 0; offset < localLength; ++offset) {
            String str;
            if (!isNotFormat && bytes[offset] >= 33 && bytes[offset] <= 122) {
                str = result + (char) bytes[offset];
            } else {
                str = result + String.format("%02X", bytes[offset] & 255);
            }

            result = str;
            if ((offset & 3) == 3) {
                result = str + " ";
            }
        }

        return result;
    }

    public interface IAULNetListener {
        void onANConnect(AULNetConnection var1);
    }

    enum TANetInterface {
        aniALAN,
        aniABT,
        aniAUN,
        aniAUN2
    }


    public static double limited(double min, double value, double max) {
        if (value < min) {
            return min;
        } else {
            return Math.min(value, max);
        }
    }

    public static int limited(int min, int value, int max) {
        if (value < min) {
            return min;
        } else {
            return Math.min(value, max);
        }
    }

    public static int uByte(byte value) {
        return value & 255;
    }
}
