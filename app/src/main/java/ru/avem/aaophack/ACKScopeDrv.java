package ru.avem.aaophack;

import android.app.Activity;
import android.os.AsyncTask;

import ru.avem.aaophack.Utils.TANetInterface;

import java.util.concurrent.TimeUnit;

import static ru.avem.aaophack.Utils.sleep;

public class ACKScopeDrv {
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
    public static final double[] timebaseTab = new double[]{1.0E-8D, 2.0E-8D, 5.0E-8D, 1.0E-7D, 2.0E-7D, 5.0E-7D, 1.0E-6D, 2.0E-6D, 5.0E-6D, 1.0E-5D, 2.0E-5D, 5.0E-5D, 1.0E-4D, 2.0E-4D, 5.0E-4D, 0.001D};
    public static final double[] voltrangTab = new double[]{0.01D, 0.02D, 0.05D, 0.1D, 0.2D, 0.5D, 1.0D, 2.0D, 5.0D, 10.0D};
    public static final byte CUBA_COMMAND2 = (byte) 2;
    public static final byte CUBA_COMMAND1 = (byte) 1;
    private int clockSource;
    public int connectState;
    private int[] coupling = new int[2];
    private byte[] data1;
    private byte[] data2;
    private boolean filterEnable;
    final byte[][] gainCode;
    private int generator;
    private boolean lockCtrl = false;
    private int memorySize;
    private int minRange = 0;
    private int needReset = 0;
    private int[] offset = new int[2];
    public AULNetConnection pAULNetConnection;
    private Activity pActivity;
    private int postTrgLength;
    private int[] probe = new int[]{1, 1};
    private int[] range = new int[2];
    public int regStatus;
    private int sampleRate;
    private boolean scroll;
    private boolean start;
    private int trgDelay;
    private int[] triggerLevel = new int[2];
    private int triggerLogic;
    private int triggerMode;
    private int triggerSource;

    public ACKScopeDrv(Activity activity, boolean tcpOn, int serverPort, String serverIP) {
        byte[] var7 = new byte[]{-109, 51};
        byte[] var8 = new byte[]{-111, 49};
        byte[] var9 = new byte[]{-127, 33};
        byte[] var10 = new byte[]{3, 3};
        byte[] var11 = new byte[]{2, 2};
        byte[] var6 = new byte[2];
        this.gainCode = new byte[][]{var7, {-110, 50}, var8, {-125, 35}, {-126, 34}, var9, var10, var11, {1, 1}, var6};
        this.pActivity = activity;

        for (int channel = 0; channel < 2; ++channel) {
            this.range[channel] = voltrangTab.length - 1;
            this.coupling[channel] = 0;
            this.offset[channel] = 2047;
            this.triggerLevel[channel] = 2040;
            this.probe[channel] = 1;
        }

        this.triggerLogic = 0;
        this.triggerMode = 0;
        this.triggerSource = 0;
        this.sampleRate = 4;
        this.clockSource = 0;
        this.filterEnable = false;
        this.generator = 0;
        this.scroll = false;
        this.start = false;
        this.postTrgLength = 512;
        this.trgDelay = 512;
        this.memorySize = 65536;
        this.pAULNetConnection = new AULNetConnection(this.pActivity);
        AULNetConnection aulNetConnection = this.pAULNetConnection;
        TANetInterface taNetInterface;
        if (tcpOn) {
            taNetInterface = TANetInterface.aniALAN;
        } else {
            taNetInterface = TANetInterface.aniAUN;
        }

        aulNetConnection.interfaceMode = taNetInterface;
        this.pAULNetConnection.serverPort = serverPort;
        this.pAULNetConnection.serverIP = serverIP;
        this.connectState = 1;
        this.InitConnection();
    }

    private int CheckRegistrateState(double var1, double var3) {
        byte var5 = 0;
        if ((this.needReset & 1) != 0) {
            this.WriteChannelControl(0);
        }

        if ((this.needReset & 256) != 0) {
            this.WriteChannelControl(1);
        }

        if ((this.needReset & 2) != 0) {
            this.setOffset(this.offset[0], 0);
        }

        if ((this.needReset & 512) != 0) {
            this.setOffset(this.offset[1], 1);
        }

        if ((this.needReset & 4) != 0) {
            this.setTriggerLevel(this.triggerLevel[0], 0);
        }

        if ((this.needReset & 1024) != 0) {
            this.setTriggerLevel(this.triggerLevel[1], 1);
        }

        if ((this.needReset & 8) != 0) {
            this.setSampleRate(this.sampleRate);
        }

        if ((this.needReset & 8) != 0 || (this.needReset & 4) != 0) {
            this.WriteLowCmd();
        }

        if (0.001D * (double) System.currentTimeMillis() - var1 > var3) {
            var5 = 1;
        }

        this.needReset = 0;
        return var5;
    }

    private boolean isLock() {
        return this.lockCtrl;
    }

    public static double limited(double first, double value, double last) { // не стал прям разбираться
        if (value < first) {
            return first;
        } else {
            return value > last ? last : value;
        }
    }

    public static int limited(int var0, int var1, int var2) { // не стал прям разбираться
        if (var1 < var0) {
            return var0;
        } else {
            return var1 > var2 ? var2 : var1;
        }
    }

    private void setNeedReset(int var1) {
        if (var1 != 0) {
            this.needReset |= var1;
        } else {
            this.needReset = 0;
        }
    }

    private void setRegStatus(int regStatus) {
        if (this.regStatus != regStatus) {
            this.regStatus = regStatus;
            ((ACKScopeDrv.IACKScopeListener) this.pActivity).onRegStatusChange(this);
        }

    }

    public static int uByte(byte value) {
        return value & 255;
    }

    int GetChannelsControl() {
        this.pAULNetConnection.SelectUBA(CUBA_COMMAND2);
        byte[] buffer = new byte[16];
        this.pAULNetConnection.ReadRegister(4881, buffer);
        int leftUByte = uByte(buffer[0]);
        int rightUByte = uByte(buffer[1]);
        this.pAULNetConnection.getClass();
        this.pAULNetConnection.SelectUBA(CUBA_COMMAND1);
        return (leftUByte & 3 | (leftUByte & 4) << 1 | (leftUByte & 8) << 1 | (leftUByte & 16) << 3 | leftUByte & 32 | (leftUByte & 64) >> 4) ^ 252
                | ((rightUByte & 3 | (rightUByte & 4) << 1 | (rightUByte & 8) << 1 | (rightUByte & 16) << 1 | (rightUByte & 32) << 1 | (rightUByte & 64) >> 4) ^ 252) << 8;
    }

    public void InitConnection() {
        if (!this.pAULNetConnection.InitConnection()) {
            this.pAULNetConnection.Connect(true);
        }

    }

    int ReadMemorySize() {
        byte[] buffer = new byte[16];
        this.pAULNetConnection.ReadRegister(8, buffer);
        switch (buffer[0] & 15) {
            case 0:
                this.memorySize = 65536;
                break;
            case 1:
                this.memorySize = 131072;
                break;
            case 2:
                this.memorySize = 262144;
                break;
            case 3:
                this.memorySize = 524288;
                break;
            case 4:
                this.memorySize = 1048576;
                break;
            default:
                this.memorySize = 65536;
        }

        return this.memorySize;
    }

    int WriteChannelControl(int var1) {
        byte var3 = this.gainCode[this.range[var1]][var1];
        int var2 = var3;
        if (2 == this.coupling[var1]) {
            byte var6;
            if (var1 != 0) {
                var6 = 64;
            } else {
                var6 = 32;
            }

            var2 = var3 | var6;
        }

        int var8 = var2;
        if (1 == this.coupling[var1]) {
            var8 = var2 | 8;
        }

        var2 = var8;
        if (3 == this.coupling[var1]) {
            var2 = var8 | 4;
        }

        this.pAULNetConnection.SelectUBA(CUBA_COMMAND2);
        byte var5;
        if (var1 == 0) {
            var5 = 16;
        } else {
            var5 = 18;
        }

        byte var7 = (byte) var5;
        this.pAULNetConnection.WriteRegister(var7, (byte) (var2 ^ 236));
        AULNetConnection var4 = this.pAULNetConnection;
        this.pAULNetConnection.getClass();
        return var4.SelectUBA(CUBA_COMMAND1);
    }

    int WriteChannelsControl() {
        byte var4 = 0;
        int channelsControlMask = this.GetChannelsControl();
        byte var3 = 0;
        byte var2 = this.gainCode[this.range[0]][0];
        int FF = var2;
        if (2 == this.coupling[0]) {
            FF = var2 | 32;
        }

        int var8 = FF;
        if (1 == this.coupling[0]) {
            var8 = FF | 8;
        }

        FF = var8;
        if (3 == this.coupling[0]) {
            FF = var8 | 4;
        }

        int var6 = FF ^ 236;
        FF = var3;
        if (var6 != (channelsControlMask & 255)) {
            FF = 1;
        }

        byte var9 = this.gainCode[this.range[1]][1];
        var8 = var9;
        if (2 == this.coupling[1]) {
            var8 = var9 | 64;
        }

        int var10 = var8;
        if (1 == this.coupling[1]) {
            var10 = var8 | 8;
        }

        var8 = var10;
        if (3 == this.coupling[1]) {
            var8 = var10 | 4;
        }

        var10 = var8 ^ 236;
        var8 = FF;
        if (var10 != (channelsControlMask & 255)) {
            var8 = FF | 2;
        }

        FF = var4;
        if (var8 != 0) {
            this.pAULNetConnection.SelectUBA(CUBA_COMMAND2);
            byte[] var7 = new byte[16];
            short command = 0;
            switch (var8) {
                case 1:
                    command = 16;
                    var7[0] = (byte) (var6 & 255);
                    break;
                case 2:
                    command = 18;
                    var7[0] = (byte) (var10 & 255);
                    break;
                case 3:
                    command = 4624;
                    var7[0] = (byte) (var6 & 255);
                    var7[1] = (byte) (var10 & 255);
            }

            this.pAULNetConnection.WriteRegister(command, var7);
            AULNetConnection var12 = this.pAULNetConnection;
            this.pAULNetConnection.getClass();
            FF = var12.SelectUBA(CUBA_COMMAND1);
        }

        return FF;
    }

    int WriteLowCmd() {
        byte var3;
        if (this.generator != 0) {
            var3 = 32;
        } else {
            var3 = 0;
        }

        byte var2 = (byte) var3;
        byte var1 = var2;
        if (this.scroll) {
            var1 = var2;
            if (this.start) {
                var1 = (byte) (var2 | 16);
            }
        }

        var2 = var1;
        if (this.triggerLogic == 1) {
            var2 = (byte) (var1 | 8);
        }

        if (this.triggerMode == 0) {
            var1 = (byte) (var2 | 6);
        } else {
            switch (this.triggerSource) {
                case 1:
                    var1 = (byte) (var2 | 2);
                    break;
                case 2:
                case 3:
                default:
                    var1 = var2;
                    break;
                case 4:
                    var1 = (byte) (var2 | 4);
            }
        }

        var2 = var1;
        if (this.start) {
            var2 = var1;
            if (!this.scroll) {
                var2 = (byte) (var1 | 1);
            }
        }

        var1 = var2;
        if (this.clockSource == 1) {
            var1 = (byte) (var2 | 64);
        }

        return this.pAULNetConnection.WriteRegister(12, var1);
    }

    public void ackReset() {
        this.setTrgDelay(this.trgDelay);
        this.setPostTrgLength(this.postTrgLength);
        this.setOffset(this.offset[0], 0);
        this.setOffset(this.offset[1], 1);
        this.setTriggerLevel(this.triggerLevel[0], 0);
        this.setTriggerLevel(this.triggerLevel[1], 1);
        this.WriteChannelsControl();
        this.setSampleRate(this.sampleRate);
        this.WriteLowCmd();
    }

    public int getClockSource() {
        return this.clockSource;
    }

    public int getConnectState() {
        return this.connectState;
    }

    public int getCoupling(int var1) {
        return this.coupling[var1];
    }

    public byte[] getData1() {
        return this.data1;
    }

    public byte[] getData2() {
        return this.data2;
    }

    public boolean getFilterEnable() {
        return this.filterEnable;
    }

    public int getGenerator() {
        return this.generator;
    }

    public int getMemorySize() {
        return this.memorySize;
    }

    public int getMinRange() {
        return this.minRange;
    }

    public int getOffset(int var1) {
        return this.offset[var1];
    }

    public int getPostTrgLength() {
        return this.postTrgLength;
    }

    public int getProbe(int var1) {
        return this.probe[var1];
    }

    public int getRange(int var1) {
        return this.range[var1];
    }

    public int getReadAddress() {
        byte[] buffer = new byte[16];
        this.pAULNetConnection.ReadRegister(459522, buffer);
        return (uByte(buffer[0]) + (uByte(buffer[1]) << 8) + (uByte(buffer[2]) << 16)) % this.memorySize;
    }

    public int getSampleRate() {
        return this.sampleRate;
    }

    public boolean getScroll() {
        return this.scroll;
    }

    public boolean getStart() {
        return this.start;
    }

    public byte getStatus() {
        byte[] buffer = new byte[16];
        this.pAULNetConnection.ReadRegister(4, buffer);
        return buffer[0];
    }

    public int getTrgDelay() {
        return this.trgDelay;
    }

    public int getTriggerLevel(int var1) {
        return this.triggerLevel[var1];
    }

    public int getTriggerLogic() {
        return this.triggerLogic;
    }

    public int getTriggerMode() {
        return this.triggerMode;
    }

    public int getTriggerSource() {
        return this.triggerSource;
    }

    public int getWriteAddress() {
        byte[] buffer = new byte[16];
        this.pAULNetConnection.ReadRegister(393472, buffer);
        return (uByte(buffer[0]) + (uByte(buffer[1]) << 8) + (uByte(buffer[2]) << 16)) % this.memorySize;
    }

    public int readRAM(int var1, int var2) {
        if (var1 == 0) {
            this.data1 = new byte[var2];
            this.pAULNetConnection.ReadData(this.data1, (byte) -113);
        } else {
            this.data2 = new byte[var2];
            this.pAULNetConnection.ReadData(this.data2, (byte) -97);
        }

        return 0;
    }

    public int readWaveform() {
        new ReadWaveformTask().execute(this);
        return 0;
    }

    public void setClockSource(int var1) {
        this.clockSource = limited(0, var1, 1);
        if (this.isLock()) {
            this.setNeedReset(4);
        } else {
            this.WriteLowCmd();
        }
    }

    public void setConnectState(int var1) {
        this.connectState = var1;
    }

    public void setCoupling(int var1, int var2) {
        var1 = limited(0, var1, 3);
        this.coupling[var2] = var1;
        if (this.isLock()) {
            this.setNeedReset(1 << var2 * 8);
        } else {
            this.WriteChannelControl(var2);
        }
    }

    public void setFilterEnable(boolean var1) {
        this.filterEnable = var1;
    }

    public void setGenerator(int var1) {
        var1 = limited(0, var1, 2);
        this.generator = var1;
        byte var3 = (byte) this.sampleRate;
        byte var2 = var3;
        if (2 == var1) {
            var2 = (byte) (var3 | 16);
        }

        if (this.isLock()) {
            this.setNeedReset(8);
        } else {
            this.pAULNetConnection.WriteRegister(11, var2);
            this.WriteLowCmd();
        }
    }

    public void setOffset(int var1, int var2) {
        int var4 = limited(0, var1, 4095);
        this.offset[var2] = var4;
        if (this.isLock()) {
            this.setNeedReset(2 << var2 * 8);
        } else {
            this.pAULNetConnection.SelectUBA(CUBA_COMMAND2);
            byte var6;
            if (var2 == 0) {
                var6 = 21;
            } else {
                var6 = 25;
            }

            byte var7 = (byte) var6;
            byte var3 = (byte) (4095 - var4 >> 8 & 255);
            this.pAULNetConnection.WriteRegister(var7, var3);
            if (var2 == 0) {
                var6 = 20;
            } else {
                var6 = 24;
            }

            var7 = (byte) var6;
            var3 = (byte) (4095 - var4 & 255);
            this.pAULNetConnection.WriteRegister(var7, var3);
            AULNetConnection var5 = this.pAULNetConnection;
            this.pAULNetConnection.getClass();
            var5.SelectUBA(CUBA_COMMAND1);
        }
    }

    public void setPostTrgLength(int var1) {
        var1 = limited(2, var1, this.memorySize);
        this.postTrgLength = var1;
        int var2 = this.memorySize - (var1 - 2);
        var1 = var2;
        if (var2 != 0) {
            var1 = var2 - 1;
        }

        this.setReadAddress(var1);
        this.getReadAddress();
    }

    public void setProbe(int var1, int var2) {
        int var3 = var1;
        if (var1 <= 0) {
            var3 = 1;
        }

        this.probe[var2] = var3;
    }

    public void setRange(int var1, int var2) {
        var1 = limited(0, var1, voltrangTab.length - 1);
        this.range[var2] = var1;
        if (this.isLock()) {
            this.setNeedReset(1 << var2 * 8);
        } else {
            this.WriteChannelControl(var2);
        }
    }

    public int setReadAddress(int var1) {
        byte[] var2 = new byte[16];
        var2[2] = (byte) (var1 >> 16 & 255);
        var2[1] = (byte) (var1 >> 8 & 255);
        var2[0] = (byte) (var1 & 255);
        return this.pAULNetConnection.WriteRegister(459522, var2);
    }

    public void setSampleRate(int var1) {
        int var2 = limited(0, var1, timebaseTab.length - 1);
        this.sampleRate = var2;
        var1 = var2;
        if (2 == this.generator) {
            var1 = var2 | 16;
        }

        if (this.isLock()) {
            this.setNeedReset(8);
        } else {
            this.pAULNetConnection.WriteRegister(11, (byte) var1);
        }
    }

    public void setScroll(boolean var1) {
        this.scroll = var1;
        if (this.isLock()) {
            this.setNeedReset(4);
        } else {
            this.WriteLowCmd();
        }
    }

    public void setStart(boolean var1) {
        this.start = var1;
        if (this.isLock()) {
            this.setNeedReset(4);
        } else {
            this.WriteLowCmd();
        }
    }

    public void setTrgDelay(int var1) {
        var1 = limited(0, var1, this.memorySize);
        this.trgDelay = var1;
        int var2 = this.memorySize - var1;
        var1 = var2;
        if (var2 != 0) {
            var1 = var2 - 1;
        }

        this.setWriteAddress(var1);
    }

    public void setTriggerLevel(int var1, int var2) {
        int var3 = limited(0, var1, 4095);
        this.triggerLevel[var2] = var3;
        if (this.isLock()) {
            this.setNeedReset(4 << var2 * 8);
        } else {
            this.pAULNetConnection.SelectUBA(CUBA_COMMAND2);
            short var5;
            if (var2 == 0) {
                var5 = 5655;
            } else {
                var5 = 6683;
            }

            byte[] var4 = new byte[16];
            var4[0] = (byte) (var3 >> 8 & 255);
            var4[1] = (byte) (var3 & 255);
            this.pAULNetConnection.WriteRegister(var5, var4);
            AULNetConnection var6 = this.pAULNetConnection;
            this.pAULNetConnection.getClass();
            var6.SelectUBA(CUBA_COMMAND1);
        }
    }

    public void setTriggerLogic(int var1) {
        this.triggerLogic = limited(0, var1, 2);
        if (this.isLock()) {
            this.setNeedReset(4);
        } else {
            this.WriteLowCmd();
        }
    }

    public void setTriggerMode(int var1) {
        this.triggerMode = limited(0, var1, 3);
        if (this.isLock()) {
            this.setNeedReset(4);
        } else {
            this.WriteLowCmd();
        }
    }

    public void setTriggerSource(int var1) {
        this.triggerSource = limited(0, var1, 1);
        if (this.isLock()) {
            this.setNeedReset(4);
        } else {
            this.WriteLowCmd();
        }
    }

    public int setWriteAddress(int var1) {
        byte[] var2 = new byte[16];
        var2[2] = (byte) (var1 >> 16 & 255);
        var2[1] = (byte) (var1 >> 8 & 255);
        var2[0] = (byte) (var1 & 255);
        return this.pAULNetConnection.WriteRegister(393472, var2);
    }

    public void startNormal() {
        this.start = true;
        int var1 = this.triggerMode;
        this.triggerMode = 1;
        this.WriteLowCmd();
        this.triggerMode = var1;
    }

    public interface IACKScopeListener {
        void onDataReady(ACKScopeDrv var1);

        void onRegStatusChange(ACKScopeDrv var1);
    }

    private class ReadWaveformTask extends AsyncTask<ACKScopeDrv, Void, Integer> {
        private ReadWaveformTask() {
        }

        protected Integer doInBackground(ACKScopeDrv... ackScopeDrvs) {
            int lengthDelay = ackScopeDrvs[0].postTrgLength + ackScopeDrvs[0].trgDelay;
            ackScopeDrvs[0].start = false;
            ackScopeDrvs[0].ackReset();
            boolean var13 = true;
            ackScopeDrvs[0].startNormal();
            ackScopeDrvs[0].setRegStatus(1);
            double currentTimeMillis = (double) System.currentTimeMillis();

            byte kakaytoMaska;
            do {
                try {
                    TimeUnit.MILLISECONDS.sleep(50L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                ackScopeDrvs[0].lockCtrl = true;
                byte status = ackScopeDrvs[0].getStatus();
                ackScopeDrvs[0].lockCtrl = false;
                kakaytoMaska = (byte) (status & 14);
            } while (ACKScopeDrv.this.CheckRegistrateState(0.001D * currentTimeMillis, 0.2D) == 0 && (kakaytoMaska & 2) == 0 && kakaytoMaska != 0);

            double _currentTimeMicros = 0.001D * (double) System.currentTimeMillis();
            double sampleRate = ACKScopeDrv.timebaseTab[ackScopeDrvs[0].sampleRate];
            double _yRate = (double) (ackScopeDrvs[0].trgDelay + 100) * sampleRate;
            double _newYRate = _yRate;
            boolean boolHZ = var13;
            byte newCuba = kakaytoMaska;
            currentTimeMillis = _currentTimeMicros;
            if (_yRate < 0.2D) {
                _newYRate = 0.2D;
                currentTimeMillis = _currentTimeMicros;
                newCuba = kakaytoMaska;
                boolHZ = var13;
            }

            byte cuba;
            do {
                sleep(50L);
                cuba = ACKScopeDrv.this.pAULNetConnection.cUBA;
                ACKScopeDrv.this.pAULNetConnection.getClass();
                if (cuba != 1) {
                    cuba = newCuba;
                } else {
                    ackScopeDrvs[0].lockCtrl = true;
                    cuba = ackScopeDrvs[0].getStatus();
                    ackScopeDrvs[0].lockCtrl = false;
                    kakaytoMaska = (byte) (cuba & 14);
                    if ((kakaytoMaska & 8) != 0) {
                        ackScopeDrvs[0].setRegStatus(2);
                    }

                    boolean var23 = boolHZ;
                    _currentTimeMicros = currentTimeMillis;
                    if (boolHZ) {
                        var23 = boolHZ;
                        _currentTimeMicros = currentTimeMillis;
                        if (ackScopeDrvs[0].triggerMode == 0) {
                            var23 = boolHZ;
                            _currentTimeMicros = currentTimeMillis;
                            if ((kakaytoMaska & 4) != 0) {
                                var23 = boolHZ;
                                _currentTimeMicros = currentTimeMillis;
                                if ((kakaytoMaska & 8) == 0) {
                                    var23 = boolHZ;
                                    _currentTimeMicros = currentTimeMillis;
                                    if (ACKScopeDrv.this.CheckRegistrateState(currentTimeMillis, _newYRate) != 0) {
                                        ACKScopeDrv ackScopeDrv = ackScopeDrvs[0];
                                        var23 = false;
                                        ackScopeDrv.setTriggerMode(0);
                                        _currentTimeMicros = 0.001D * (double) System.currentTimeMillis();
                                    }
                                }
                            }
                        }
                    }

                    boolHZ = var23;
                    cuba = kakaytoMaska;
                    currentTimeMillis = _currentTimeMicros;
                    if (!var23) {
                        boolHZ = var23;
                        cuba = kakaytoMaska;
                        currentTimeMillis = _currentTimeMicros;
                        if (ACKScopeDrv.this.CheckRegistrateState(_currentTimeMicros, (double) (lengthDelay * 2) * sampleRate) != 0) {
                            cuba = 0;
                            boolHZ = var23;
                            currentTimeMillis = _currentTimeMicros;
                        }
                    }
                }

                newCuba = cuba;
            } while ((cuba & 6) != 0);

            ackScopeDrvs[0].setStart(false);
            ackScopeDrvs[0].lockCtrl = true;
            ackScopeDrvs[0].setRegStatus(3);
            int _lastAddress = ackScopeDrvs[0].getWriteAddress() - lengthDelay;
            int startAddress = _lastAddress;
            if (_lastAddress < 0) {
                startAddress = _lastAddress + ackScopeDrvs[0].memorySize;
            }

            ackScopeDrvs[0].setReadAddress(startAddress);
            if (ackScopeDrvs[0].getReadAddress() != startAddress) {
            }

            ackScopeDrvs[0].readRAM(0, lengthDelay);
            ackScopeDrvs[0].setReadAddress(startAddress);
            startAddress = ackScopeDrvs[0].readRAM(1, lengthDelay);
            ackScopeDrvs[0].lockCtrl = false;
            ackScopeDrvs[0].setRegStatus(0);
            ((ACKScopeDrv.IACKScopeListener) ACKScopeDrv.this.pActivity).onDataReady(ackScopeDrvs[0]);
            return startAddress;
        }

        protected void onPostExecute(Integer integer) {
        }
    }
}

