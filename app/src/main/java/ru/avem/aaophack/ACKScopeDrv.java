package ru.avem.aaophack;

import android.app.Activity;
import android.os.AsyncTask;

import ru.avem.aaophack.Utils.TANetInterface;

import java.util.concurrent.TimeUnit;

import static ru.avem.aaophack.Constants.ACKScopeDrv.*;
import static ru.avem.aaophack.Utils.limited;
import static ru.avem.aaophack.Utils.sleep;
import static ru.avem.aaophack.Utils.uByte;

public class ACKScopeDrv {
    public static final double[] timebaseTab = new double[]{1.0E-8D, 2.0E-8D, 5.0E-8D, 1.0E-7D, 2.0E-7D, 5.0E-7D, 1.0E-6D, 2.0E-6D, 5.0E-6D, 1.0E-5D, 2.0E-5D, 5.0E-5D, 1.0E-4D, 2.0E-4D, 5.0E-4D, 0.001D};
    public static final double[] voltrangTab = new double[]{0.01D, 0.02D, 0.05D, 0.1D, 0.2D, 0.5D, 1.0D, 2.0D, 5.0D, 10.0D};

    private int clockSource;
    public int connectState;
    private int[] coupling = new int[2];
    private byte[] data1;
    private byte[] data2;
    final byte[][] gainCode;
    private int generator;
    private boolean lockCtrl = false;
    private int memorySize;
    private int needReset = 0;
    private int[] offset = new int[2];
    public AULNetConnection pAULNetConnection;
    private Activity pActivity;
    private int postTrgLength;
    private int[] probe = new int[]{1, 1};
    private int[] range = new int[2];
    public int regStatus;
    private int sampleRate;
    private boolean isScroll;
    private boolean isStart;
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
        gainCode = new byte[][]{var7, {-110, 50}, var8, {-125, 35}, {-126, 34}, var9, var10, var11, {1, 1}, var6};
        pActivity = activity;

        for (int channel = 0; channel < 2; ++channel) {
            range[channel] = voltrangTab.length - 1;
            coupling[channel] = 0;
            offset[channel] = 2047;
            triggerLevel[channel] = 2040;
            probe[channel] = 1;
        }

        triggerLogic = 0;
        triggerMode = 0;
        triggerSource = 0;
        sampleRate = 4;
        clockSource = 0;
        generator = 0;
        isScroll = false;
        isStart = false;
        postTrgLength = 512;
        trgDelay = 512;
        memorySize = 65536;
        pAULNetConnection = new AULNetConnection(pActivity);
        AULNetConnection aulNetConnection = pAULNetConnection;
        TANetInterface taNetInterface;
        if (tcpOn) {
            taNetInterface = TANetInterface.aniALAN;
        } else {
            taNetInterface = TANetInterface.aniAUN;
        }

        aulNetConnection.interfaceMode = taNetInterface;
        pAULNetConnection.serverPort = serverPort;
        pAULNetConnection.serverIP = serverIP;
        connectState = 1;
        initConnection();
    }

    private int CheckRegistrateState(double var1, double var3) {
        byte var5 = 0;
        if ((needReset & 1) != 0) {
            writeChannelControl(0);
        }

        if ((needReset & 256) != 0) {
            writeChannelControl(1);
        }

        if ((needReset & 2) != 0) {
            setOffset(offset[0], 0);
        }

        if ((needReset & 512) != 0) {
            setOffset(offset[1], 1);
        }

        if ((needReset & 4) != 0) {
            setTriggerLevel(triggerLevel[0], 0);
        }

        if ((needReset & 1024) != 0) {
            setTriggerLevel(triggerLevel[1], 1);
        }

        if ((needReset & 8) != 0) {
            setSampleRate(sampleRate);
        }

        if ((needReset & 8) != 0 || (needReset & 4) != 0) {
            writeLowCmd();
        }

        if (0.001D * (double) System.currentTimeMillis() - var1 > var3) {
            var5 = 1;
        }

        needReset = 0;
        return var5;
    }

    private boolean isLock() {
        return lockCtrl;
    }

    private void setNeedReset(int var1) {
        if (var1 != 0) {
            needReset |= var1;
        } else {
            needReset = 0;
        }
    }

    private void setRegStatus(int regStatus) {
        if (this.regStatus != regStatus) {
            this.regStatus = regStatus;
            ((ACKScopeDrv.IACKScopeListener) pActivity).onRegStatusChange(this);
        }
    }

    int getChannelsControl() {
        pAULNetConnection.selectUBA(CUBA_COMMAND2);
        byte[] buffer = new byte[16];
        pAULNetConnection.readRegister(4881, buffer);
        int leftUByte = uByte(buffer[0]);
        int rightUByte = uByte(buffer[1]);
        pAULNetConnection.getClass();
        pAULNetConnection.selectUBA(CUBA_COMMAND1);
        return (leftUByte & 3 | (leftUByte & 4) << 1 | (leftUByte & 8) << 1 | (leftUByte & 16) << 3 | leftUByte & 32 | (leftUByte & 64) >> 4) ^ 252
                | ((rightUByte & 3 | (rightUByte & 4) << 1 | (rightUByte & 8) << 1 | (rightUByte & 16) << 1 | (rightUByte & 32) << 1 | (rightUByte & 64) >> 4) ^ 252) << 8;
    }

    public void initConnection() {
        if (!pAULNetConnection.initConnection()) {
            pAULNetConnection.connect(true);
        }

    }

    int readMemorySize() {
        byte[] buffer = new byte[16];
        pAULNetConnection.readRegister(8, buffer);
        switch (buffer[0] & 15) {
            case 0:
                memorySize = 65536;
                break;
            case 1:
                memorySize = 131072;
                break;
            case 2:
                memorySize = 262144;
                break;
            case 3:
                memorySize = 524288;
                break;
            case 4:
                memorySize = 1048576;
                break;
            default:
                memorySize = 65536;
        }

        return memorySize;
    }

    int writeChannelControl(int var1) {
        byte var3 = gainCode[range[var1]][var1];
        int var2 = var3;
        if (2 == coupling[var1]) {
            byte var6;
            if (var1 != 0) {
                var6 = 64;
            } else {
                var6 = 32;
            }

            var2 = var3 | var6;
        }

        int var8 = var2;
        if (1 == coupling[var1]) {
            var8 = var2 | 8;
        }

        var2 = var8;
        if (3 == coupling[var1]) {
            var2 = var8 | 4;
        }

        pAULNetConnection.selectUBA(CUBA_COMMAND2);
        byte var5;
        if (var1 == 0) {
            var5 = 16;
        } else {
            var5 = 18;
        }

        byte var7 = var5;
        pAULNetConnection.writeRegister(var7, (byte) (var2 ^ 236));
        AULNetConnection var4 = pAULNetConnection;
        pAULNetConnection.getClass();
        return var4.selectUBA(CUBA_COMMAND1);
    }

    int WriteChannelsControl() {
        byte var4 = 0;
        int channelsControlMask = getChannelsControl();
        byte var3 = 0;
        byte var2 = gainCode[range[0]][0];
        int FF = var2;
        if (2 == coupling[0]) {
            FF = var2 | 32;
        }

        int var8 = FF;
        if (1 == coupling[0]) {
            var8 = FF | 8;
        }

        FF = var8;
        if (3 == coupling[0]) {
            FF = var8 | 4;
        }

        int var6 = FF ^ 236;
        FF = var3;
        if (var6 != (channelsControlMask & 255)) {
            FF = 1;
        }

        byte var9 = gainCode[range[1]][1];
        var8 = var9;
        if (2 == coupling[1]) {
            var8 = var9 | 64;
        }

        int var10 = var8;
        if (1 == coupling[1]) {
            var10 = var8 | 8;
        }

        var8 = var10;
        if (3 == coupling[1]) {
            var8 = var10 | 4;
        }

        var10 = var8 ^ 236;
        var8 = FF;
        if (var10 != (channelsControlMask & 255)) {
            var8 = FF | 2;
        }

        FF = var4;
        if (var8 != 0) {
            pAULNetConnection.selectUBA(CUBA_COMMAND2);
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

            pAULNetConnection.writeRegister(command, var7);
            AULNetConnection var12 = pAULNetConnection;
            pAULNetConnection.getClass();
            FF = var12.selectUBA(CUBA_COMMAND1);
        }

        return FF;
    }

    int writeLowCmd() {
        byte var3;
        if (generator != 0) {
            var3 = 32;
        } else {
            var3 = 0;
        }

        byte var2 = (byte) var3;
        byte var1 = var2;
        if (isScroll) {
            if (isStart) {
                var1 = (byte) (var2 | 16);
            }
        }

        var2 = var1;
        if (triggerLogic == 1) {
            var2 = (byte) (var1 | 8);
        }

        if (triggerMode == 0) {
            var1 = (byte) (var2 | 6);
        } else {
            switch (triggerSource) {
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
        if (isStart) {
            if (!isScroll) {
                var2 = (byte) (var1 | 1);
            }
        }

        var1 = var2;
        if (clockSource == 1) {
            var1 = (byte) (var2 | 64);
        }

        return pAULNetConnection.writeRegister(12, var1);
    }

    public void ackReset() {
        setTrgDelay(trgDelay);
        setPostTrgLength(postTrgLength);
        setOffset(offset[0], 0);
        setOffset(offset[1], 1);
        setTriggerLevel(triggerLevel[0], 0);
        setTriggerLevel(triggerLevel[1], 1);
        WriteChannelsControl();
        setSampleRate(sampleRate);
        writeLowCmd();
    }

    public byte[] getData1() {
        return data1;
    }

    public byte[] getData2() {
        return data2;
    }

    public int getMemorySize() {
        return memorySize;
    }

    public int getProbe(int var1) {
        return probe[var1];
    }

    public int getRange(int var1) {
        return range[var1];
    }

    public int getReadAddress() {
        byte[] buffer = new byte[16];
        pAULNetConnection.readRegister(459522, buffer);
        return (uByte(buffer[0]) + (uByte(buffer[1]) << 8) + (uByte(buffer[2]) << 16)) % memorySize;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public byte getStatus() {
        byte[] buffer = new byte[16];
        pAULNetConnection.readRegister(4, buffer);
        return buffer[0];
    }

    public int getTriggerMode() {
        return triggerMode;
    }

    public int getTriggerSource() {
        return triggerSource;
    }

    public int getWriteAddress() {
        byte[] buffer = new byte[16];
        pAULNetConnection.readRegister(393472, buffer);
        return (uByte(buffer[0]) + (uByte(buffer[1]) << 8) + (uByte(buffer[2]) << 16)) % memorySize;
    }

    public int readRAM(int var1, int var2) {
        if (var1 == 0) {
            data1 = new byte[var2];
            pAULNetConnection.readData(data1, (byte) -113);
        } else {
            data2 = new byte[var2];
            pAULNetConnection.readData(data2, (byte) -97);
        }

        return 0;
    }

    public int readWaveform() {
        new ReadWaveformTask().execute(this);
        return 0;
    }

    public void setCoupling(int value, int var2) {
        value = limited(0, value, 3);
        coupling[var2] = value;
        if (isLock()) {
            setNeedReset(1 << var2 * 8);
        } else {
            writeChannelControl(var2);
        }
    }

    public void setGenerator(int var1) {
        var1 = limited(0, var1, 2);
        generator = var1;
        byte var3 = (byte) sampleRate;
        byte var2 = var3;
        if (2 == var1) {
            var2 = (byte) (var3 | 16);
        }

        if (isLock()) {
            setNeedReset(8);
        } else {
            pAULNetConnection.writeRegister(11, var2);
            writeLowCmd();
        }
    }

    public void setOffset(int var1, int var2) {
        int var4 = limited(0, var1, 4095);
        offset[var2] = var4;
        if (isLock()) {
            setNeedReset(2 << var2 * 8);
        } else {
            pAULNetConnection.selectUBA(CUBA_COMMAND2);
            byte var6;
            if (var2 == 0) {
                var6 = 21;
            } else {
                var6 = 25;
            }

            byte var7 = (byte) var6;
            byte var3 = (byte) (4095 - var4 >> 8 & 255);
            pAULNetConnection.writeRegister(var7, var3);
            if (var2 == 0) {
                var6 = 20;
            } else {
                var6 = 24;
            }

            var7 = (byte) var6;
            var3 = (byte) (4095 - var4 & 255);
            pAULNetConnection.writeRegister(var7, var3);
            AULNetConnection var5 = pAULNetConnection;
            pAULNetConnection.getClass();
            var5.selectUBA(CUBA_COMMAND1);
        }
    }

    public void setPostTrgLength(int var1) {
        var1 = limited(2, var1, memorySize);
        postTrgLength = var1;
        int var2 = memorySize - (var1 - 2);
        var1 = var2;
        if (var2 != 0) {
            var1 = var2 - 1;
        }

        setReadAddress(var1);
        getReadAddress();
    }

    public void setProbe(int var1, int var2) {
        int var3 = var1;
        if (var1 <= 0) {
            var3 = 1;
        }

        probe[var2] = var3;
    }

    public void setRange(int var1, int var2) {
        var1 = limited(0, var1, voltrangTab.length - 1);
        range[var2] = var1;
        if (isLock()) {
            setNeedReset(1 << var2 * 8);
        } else {
            writeChannelControl(var2);
        }
    }

    public int setReadAddress(int var1) {
        byte[] var2 = new byte[16];
        var2[2] = (byte) (var1 >> 16 & 255);
        var2[1] = (byte) (var1 >> 8 & 255);
        var2[0] = (byte) (var1 & 255);
        return pAULNetConnection.writeRegister(459522, var2);
    }

    public void setSampleRate(int var1) {
        int var2 = limited(0, var1, timebaseTab.length - 1);
        sampleRate = var2;
        var1 = var2;
        if (2 == generator) {
            var1 = var2 | 16;
        }

        if (isLock()) {
            setNeedReset(8);
        } else {
            pAULNetConnection.writeRegister(11, (byte) var1);
        }
    }

    public void setScroll(boolean var1) {
        isScroll = var1;
        if (isLock()) {
            setNeedReset(4);
        } else {
            writeLowCmd();
        }
    }

    public void setStart(boolean var1) {
        isStart = var1;
        if (isLock()) {
            setNeedReset(4);
        } else {
            writeLowCmd();
        }
    }

    public void setTrgDelay(int var1) {
        var1 = limited(0, var1, memorySize);
        trgDelay = var1;
        int var2 = memorySize - var1;
        var1 = var2;
        if (var2 != 0) {
            var1 = var2 - 1;
        }

        setWriteAddress(var1);
    }

    public void setTriggerLevel(int var1, int var2) {
        int var3 = limited(0, var1, 4095);
        triggerLevel[var2] = var3;
        if (isLock()) {
            setNeedReset(4 << var2 * 8);
        } else {
            pAULNetConnection.selectUBA(CUBA_COMMAND2);
            short var5;
            if (var2 == 0) {
                var5 = 5655;
            } else {
                var5 = 6683;
            }

            byte[] var4 = new byte[16];
            var4[0] = (byte) (var3 >> 8 & 255);
            var4[1] = (byte) (var3 & 255);
            pAULNetConnection.writeRegister(var5, var4);
            AULNetConnection var6 = pAULNetConnection;
            pAULNetConnection.getClass();
            var6.selectUBA(CUBA_COMMAND1);
        }
    }

    public void setTriggerLogic(int var1) {
        triggerLogic = limited(0, var1, 2);
        if (isLock()) {
            setNeedReset(4);
        } else {
            writeLowCmd();
        }
    }

    public void setTriggerMode(int var1) {
        triggerMode = limited(0, var1, 3);
        if (isLock()) {
            setNeedReset(4);
        } else {
            writeLowCmd();
        }
    }

    public void setTriggerSource(int var1) {
        triggerSource = limited(0, var1, 1);
        if (isLock()) {
            setNeedReset(4);
        } else {
            writeLowCmd();
        }
    }

    public int setWriteAddress(int var1) {
        byte[] var2 = new byte[16];
        var2[2] = (byte) (var1 >> 16 & 255);
        var2[1] = (byte) (var1 >> 8 & 255);
        var2[0] = (byte) (var1 & 255);
        return pAULNetConnection.writeRegister(393472, var2);
    }

    public void startNormal() {
        isStart = true;
        int var1 = triggerMode;
        triggerMode = 1;
        writeLowCmd();
        triggerMode = var1;
    }

    public interface IACKScopeListener {
        void onDataReady(ACKScopeDrv var1);

        void onRegStatusChange(ACKScopeDrv var1);
    }

    private class ReadWaveformTask extends AsyncTask<ACKScopeDrv, Void, Integer> {
        protected Integer doInBackground(ACKScopeDrv... ackScopeDrvs) {
            int lengthDelay = ackScopeDrvs[0].postTrgLength + ackScopeDrvs[0].trgDelay;
            ackScopeDrvs[0].isStart = false;
            ackScopeDrvs[0].ackReset();
            ackScopeDrvs[0].startNormal();
            ackScopeDrvs[0].setRegStatus(1);
            double currentTimeMillis = (double) System.currentTimeMillis();

            byte cuba1;
            do {
                try {
                    TimeUnit.MILLISECONDS.sleep(50L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                ackScopeDrvs[0].lockCtrl = true;
                byte status = ackScopeDrvs[0].getStatus();
                ackScopeDrvs[0].lockCtrl = false;
                cuba1 = (byte) (status & 14);
            } while (CheckRegistrateState(0.001D * currentTimeMillis, 0.2D) == 0 && (cuba1 & 2) == 0 && cuba1 != 0);

            double _currentTimeMicros = 0.001D * (double) System.currentTimeMillis();
            double sampleRate = ACKScopeDrv.timebaseTab[ackScopeDrvs[0].sampleRate];
            double _yRate = (double) (ackScopeDrvs[0].trgDelay + 100) * sampleRate;
            double _newYRate = _yRate;
            boolean isTriggerModeOn = true;
            byte cuba2 = cuba1;
            currentTimeMillis = _currentTimeMicros;
            if (_yRate < 0.2D) {
                _newYRate = 0.2D;
                currentTimeMillis = _currentTimeMicros;
            }

            byte cuba3;
            do {
                sleep(50L);
                cuba3 = pAULNetConnection.cUBA;
                pAULNetConnection.getClass();
                if (cuba3 != 1) {
                    cuba3 = cuba2;
                } else {
                    ackScopeDrvs[0].lockCtrl = true;
                    cuba3 = ackScopeDrvs[0].getStatus();
                    ackScopeDrvs[0].lockCtrl = false;
                    cuba1 = (byte) (cuba3 & 14);
                    if ((cuba1 & 8) != 0) {
                        ackScopeDrvs[0].setRegStatus(2);
                    }

                    boolean localTriggerMode = isTriggerModeOn;
                    _currentTimeMicros = currentTimeMillis;
                    if (isTriggerModeOn
                            && (ackScopeDrvs[0].triggerMode == 0)
                            && ((cuba1 & 4) != 0)
                            && ((cuba1 & 8) == 0)
                            && (CheckRegistrateState(currentTimeMillis, _newYRate) != 0)) {
                        ACKScopeDrv ackScopeDrv = ackScopeDrvs[0];
                        localTriggerMode = false;
                        ackScopeDrv.setTriggerMode(0);
                        _currentTimeMicros = 0.001D * (double) System.currentTimeMillis();
                    }

                    isTriggerModeOn = localTriggerMode;
                    cuba3 = cuba1;
                    currentTimeMillis = _currentTimeMicros;
                    if (!localTriggerMode) {
                        if (CheckRegistrateState(_currentTimeMicros, (double) (lengthDelay * 2) * sampleRate) != 0) {
                            cuba3 = 0;
                            currentTimeMillis = _currentTimeMicros;
                        }
                    }
                }

                cuba2 = cuba3;
            } while ((cuba3 & 6) != 0);

            ackScopeDrvs[0].setStart(false);
            ackScopeDrvs[0].lockCtrl = true;
            ackScopeDrvs[0].setRegStatus(3);
            int _lastAddress = ackScopeDrvs[0].getWriteAddress() - lengthDelay;
            int startAddress = _lastAddress;
            if (_lastAddress < 0) {
                startAddress = _lastAddress + ackScopeDrvs[0].memorySize;
            }

            ackScopeDrvs[0].setReadAddress(startAddress);
            ackScopeDrvs[0].getReadAddress();

            ackScopeDrvs[0].readRAM(0, lengthDelay);
            ackScopeDrvs[0].setReadAddress(startAddress);
            startAddress = ackScopeDrvs[0].readRAM(1, lengthDelay);
            ackScopeDrvs[0].lockCtrl = false;
            ackScopeDrvs[0].setRegStatus(0);
            ((ACKScopeDrv.IACKScopeListener) pActivity).onDataReady(ackScopeDrvs[0]);
            return startAddress;
        }
    }
}
