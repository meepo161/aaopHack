package ru.avem.aaophack;

public class Constants {
    public static class ACKScopeDrv {
        public static final int ANY_EDGE = 2;
        public static final int CPL_50_OHM = 3;
        public static final int CPL_AC = 1;
        public static final int CPL_DC = 0;
        public static final int CPL_GND = 2;
        public static final boolean D = false;
        public static final int LEADING_EDGE = 0;
        public static final int MAX_DACCODE = 4095;
        private static final int MIN_POSTTRG_LENGTH = 2;
        private static final int NEED_RESET_OFFS = 2;
        private static final int NEED_RESET_RANGE = 1;
        private static final int NEED_RESET_TIME = 8;
        private static final int NEED_RESET_TRG = 4;
        public static final byte RSTATUS_POSTTRIGGER = 2;
        public static final byte RSTATUS_READDATA = 3;
        public static final byte RSTATUS_ROLL = 4;
        public static final byte RSTATUS_UNKNOWN = 0;
        public static final byte RSTATUS_WAITTRIGGER = 1;
        public static final int SRC_EXTERNAL = 1;
        public static final int SRC_INTERNAL = 0;
        public static final byte STATUS_CARRY_WRITEADDR = 32;
        public static final byte STATUS_COUNTER_DONE = 1;
        public static final byte STATUS_DELAY_END = 4;
        public static final byte STATUS_POSTTRIGGER = 8;
        public static final byte STATUS_REGISTRATION = 2;
        public static final byte STATUS_ROLL_MODE = 16;
        public static final int TRAILING_EDGE = 1;
        public static final int TRIGGER_AUTO = 0;
        public static final int TRIGGER_NORMAL = 1;
        public static final int TRIGGER_SCROLL = 3;
        public static final int TRIGGER_SINGLE = 2;
        public static final byte CUBA_COMMAND2 = (byte) 2;
        public static final byte CUBA_COMMAND1 = (byte) 1;
    }

    public static class AULNetConnection {

        public static final int AULNCMDEP_CLOSE = 6;
        public static final int AULNCMDEP_GETDEVCOUNT = 8;
        public static final int AULNCMDEP_GETUSBNAME = 9;
        public static final int AULNCMDEP_ISOPEN = 7;
        public static final int AULNCMDEP_OPEN = 5;
        public static final int AULNCMDEP_READDATA = 2;
        public static final int AULNCMDEP_READSTATUS = 0;
        public static final int AULNCMDEP_REQACCESS = 4;
        public static final int AULNCMDEP_SENDCOMMAND = 1;
        public static final int AULNCMDEP_WRITEDATA = 3;
        public static final int BT_ENABLE_REQUEST = 101;
    }
}
