package ru.avem.aaophack;

import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static ru.avem.aaophack.ACKScopeDrv.CUBA_COMMAND1;
import static ru.avem.aaophack.ACKScopeDrv.CUBA_COMMAND2;
import static ru.avem.aaophack.Utils.toHexString;

public class AULNetConnection {
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
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
    public static final boolean D = false;
    private static final String TAG = "AULNet";
    public final byte AULN_DEFUBA;
    private Set<BluetoothDevice> BTDevList;
    final BroadcastReceiver bReceiver;
    public BluetoothSocket btSocket;
    public boolean btStarted;
    public byte cUBA;
    public UsbDeviceConnection connection;
    private boolean demoMode = true;
    private int devCnt = 0;
    private boolean devInited = false;
    public UsbDevice device;
    public String devname;
    public UsbEndpoint epRDST;
    public UsbEndpoint epSNDCMD;
    public UsbEndpoint epRDDT;
    public UsbEndpoint epWRDT;
    public Utils.TANetInterface interfaceMode;
    BluetoothAdapter mBluetoothAdapter;
    private final BroadcastReceiver mUsbReceiver;
    public UsbManager manager;
    public byte outCmdCode;
    private Activity pActivity;
    public String serverIP = "192.168.0.1";
    public int serverPort = 1024;
    public String[] supportedDevs;
    public Socket tcpSocket;

    //----------------------------------------------------------------------------------------------
    public final int idEndPointRDST = 0;
    public final int idEndPointSNDCM = 1;
    public final int idEndPointRDDT = 2;
    public final int idEndPointWRDT = 3;
    public final int idEndPointPASSWORD = 4;
    public final int idEndPointUNLOCK = 5;
    public final int idEndPointCLOSE = 6;

    public static final int WRITE_FUNCTION = -37;
    public static final int _READ_FUNCTION = -38;
    public static final int CUBA_FUNCTION = -33;
    //----------------------------------------------------------------------------------------------

    public AULNetConnection(Activity activity) {
        this.interfaceMode = Utils.TANetInterface.aniAUN;
        this.tcpSocket = null;
        this.btSocket = null;
        this.btStarted = false;
        this.connection = null;
        this.supportedDevs = new String[]{"ACK-3102", "ACK-3002", "ACK-3712"};
        this.AULN_DEFUBA = 1;
        this.bReceiver = new BroadcastReceiver() {
            public void onReceive(Context var1, Intent var2) {
                String var4 = var2.getAction();
                BluetoothDevice bluetoothDevice;
                if ("android.bluetooth.device.action.FOUND".equals(var4)) {
                    bluetoothDevice = (BluetoothDevice) var2.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                    AULNetConnection.this.BTDevList.add(bluetoothDevice);
                }

                if ("android.bluetooth.adapter.action.DISCOVERY_FINISHED".equals(var4)) {
                    AULNetConnection.this.pActivity.unregisterReceiver(AULNetConnection.this.bReceiver);
                    AULNetConnection.this.mBluetoothAdapter.cancelDiscovery();
                    boolean var3 = true;
                    Iterator var5 = AULNetConnection.this.BTDevList.iterator();

                    while (var5.hasNext()) {
                        bluetoothDevice = (BluetoothDevice) var5.next();
                        if (AULNetConnection.this.isValidDevice(bluetoothDevice.getName())) {
                            AULNetConnection.this.new openBtSocketTask().execute(bluetoothDevice);
                            var3 = false;
                            break;
                        }
                    }

                    if (var3) {
                        AULNetConnection.this.Connect(true);
                    }
                }

            }
        };
        this.mUsbReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                boolean var3 = true;
                if ("com.android.example.USB_PERMISSION".equals(intent.getAction())) {
                    synchronized (this) {
                    }

                    Throwable throwable;
                    label307:
                    {
                        label313:
                        {
                            UsbDevice var34;
                            label314:
                            {
                                AULNetConnection var36;
                                try {
                                    var34 = (UsbDevice) intent.getParcelableExtra("device");
                                    if (!intent.getBooleanExtra("permission", false)) {
                                        break label314;
                                    }

                                    var36 = AULNetConnection.this;
                                } catch (Throwable var33) {
                                    throwable = var33;
                                    break label307;
                                }

                                if (var34 != null) {
                                    var3 = false;
                                }

                                try {
                                    var36.Connect(var3);
                                    break label313;
                                } catch (Throwable var32) {
                                    throwable = var32;
                                    break label307;
                                }
                            }

                            try {
                                Toast.makeText(AULNetConnection.this.pActivity.getApplicationContext(), "permission denied for device " + var34, Toast.LENGTH_LONG).show();
                                AULNetConnection.this.Connect(true);
                            } catch (Throwable var31) {
                                throwable = var31;
                                break label307;
                            }
                        }

                        label293:
                        try {
                            return;
                        } catch (Throwable var30) {
                            throwable = var30;
                            break label293;
                        }
                    }

                    while (true) {
                        Throwable throwable1 = throwable;

                        try {
                            throw throwable1;
                        } catch (Throwable var29) {
                            throwable = var29;
                            continue;
                        }
                    }
                }
            }
        };
        this.pActivity = activity;
    }

    private int requestTcp(int var1, byte[] var2, int var3) {
        int var5 = -1;
        byte var6 = 0;
        byte var4 = 0;
        boolean var8 = false;
        AULNetConnection.transferData var10 = new AULNetConnection.transferData();
        byte var21;
        switch (var1) {
            case 0:
                var21 = 5;
                var8 = true;
                var4 = 8;
                var10.hlp[0] = (byte) (var3 & 255);
                var10.hlp[1] = (byte) (var3 >> 8 & 255);
                var10.hlp[2] = (byte) (var3 >> 16 & 255);
                var10.hlp[3] = (byte) (var3 >> 24 & 255);
                break;
            case 1:
                var21 = 6;
                var4 = 8;
                var10.hlp[0] = (byte) (var3 & 255);
                var10.hlp[1] = (byte) (var3 >> 8 & 255);
                var10.hlp[2] = (byte) (var3 >> 16 & 255);
                var10.hlp[3] = (byte) (var3 >> 24 & 255);
                break;
            case 2:
                var21 = 7;
                var8 = true;
                var4 = 8;
                var10.hlp[0] = (byte) (var3 & 255);
                var10.hlp[1] = (byte) (var3 >> 8 & 255);
                var10.hlp[2] = (byte) (var3 >> 16 & 255);
                var10.hlp[3] = (byte) (var3 >> 24 & 255);
                break;
            case 3:
                var21 = 8;
                var4 = 8;
                var10.hlp[0] = (byte) (var3 & 255);
                var10.hlp[1] = (byte) (var3 >> 8 & 255);
                var10.hlp[2] = (byte) (var3 >> 16 & 255);
                var10.hlp[3] = (byte) (var3 >> 24 & 255);
                break;
            case 4:
                var21 = 0;
                break;
            case 5:
                var21 = 1;
                var4 = 8;
                break;
            case 6:
                var21 = 2;
                var4 = 4;
                break;
            default:
                var21 = var6;
        }

        int var7 = var4;
        if (!var8) {
            var7 = var4 + var3;
        }

        byte[] var11 = new byte[28];
        var11[0] = 65;
        var11[1] = 85;
        var11[2] = 76;
        var11[3] = 78;
        var11[4] = 101;
        var11[5] = 116;
        var11[6] = 67;
        var11[7] = 109;
        var11[11] = 1;
        var11[12] = (byte) (var21 & 255);
        var11[14] = 2;
        var11[24] = (byte) (var7 & 255);
        var11[25] = (byte) (var7 >> 8 & 255);
        var11[26] = (byte) (var7 >> 16 & 255);
        var11[27] = (byte) (var7 >> 24 & 255);
        var10.newBuf(var11.length + var3 + var4);
        System.arraycopy(var11, 0, var10.buf, 0, var11.length);
        if (var4 > 0) {
            System.arraycopy(var10.hlp, 0, var10.buf, var11.length, var4);
        }

        if (!var8 && var3 > 0) {
            System.arraycopy(var2, 0, var10.buf, var11.length + var4, var3);
        }

        var10.read = var8;
        var10.size = var3;
        var10.hlpsize = var4;
        toHexString(var10.buf);
        AULNetConnection.transferSocketTask var9 = new AULNetConnection.transferSocketTask();
        var9.execute(new AULNetConnection.transferData[]{var10});
        int var26 = var5;
        int var25 = var5;

        label69:
        {
            InterruptedException var28;
            label68:
            {
                ExecutionException var27;
                label67:
                {
                    TimeoutException var10000;
                    label79:
                    {
                        boolean var10001;
                        try {
                            var1 = (Integer) var9.get(1500L, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException var18) {
                            var28 = var18;
                            var10001 = false;
                            break label68;
                        } catch (ExecutionException var19) {
                            var27 = var19;
                            var10001 = false;
                            break label67;
                        } catch (TimeoutException var20) {
                            var10000 = var20;
                            var10001 = false;
                            break label79;
                        }

                        if (var8) {
                            var26 = var1;
                            var25 = var1;
                            var5 = var1;

                            System.arraycopy(var10.buf, var11.length, var2, 0, var3);
                        }

                        var26 = var1;
                        var25 = var1;
                        var5 = var1;

                        var9.cancel(true);
                        break label69;
                    }

                    TimeoutException var22 = var10000;
                    var22.printStackTrace();
                    var1 = var5;
                    break label69;
                }

                ExecutionException var23 = var27;
                var23.printStackTrace();
                var1 = var25;
                break label69;
            }

            InterruptedException var24 = var28;
            var24.printStackTrace();
            var1 = var26;
        }

        var9.cancel(true);
        return var1;
    }

    int AULNetTransfer(int idEndPoint, byte[] buffer, int length) {
        int writtenLength = 0;
        if (this.interfaceMode != Utils.TANetInterface.aniALAN && this.interfaceMode != Utils.TANetInterface.aniABT) {
            if (this.connection != null) {
                UsbEndpoint usbEndpoint;
                switch (idEndPoint) {
                    case idEndPointRDST:
                        usbEndpoint = this.epRDST;
                        break;
                    case idEndPointSNDCM:
                        usbEndpoint = this.epSNDCMD;
                        break;
                    case idEndPointRDDT:
                        usbEndpoint = this.epRDDT;
                        break;
                    case idEndPointWRDT:
                        usbEndpoint = this.epWRDT;
                        break;
                    default:
                        return length;
                }

                if (length <= 16 && idEndPoint == 3) {
                    //ПОЧЕМУ ПУСТО?
                }

                int writtenLengthBulk = this.connection.bulkTransfer(usbEndpoint, buffer, length, 0);
                if (idEndPoint != idEndPointRDST) {
                    writtenLength = writtenLengthBulk;
                    if (idEndPoint != idEndPointRDDT) {
                        return writtenLength;
                    }
                }

                writtenLength = writtenLengthBulk;
                if (length <= 16) {
                    writtenLength = writtenLengthBulk;
                }
            }
        } else {
            writtenLength = this.requestTcp(idEndPoint, buffer, length);
        }

        return writtenLength;
    }

    public boolean CheckAUNVers() {
        boolean versionOk = false;
        if (this.interfaceMode == Utils.TANetInterface.aniAUN || this.interfaceMode == Utils.TANetInterface.aniAUN2) {
            byte[] buffer = new byte[16];
            this.SelectUBA(CUBA_COMMAND2);
            if (this.ReadRegister(42, buffer) < 0) {
                buffer[0] = 0;
            }

            if (buffer[0] == 1) { // :)
                versionOk = true;
            } else {
                versionOk = false;
            }
        }

        if (versionOk) {
            this.interfaceMode = Utils.TANetInterface.aniAUN2;
        }

        return versionOk;
    }

    public void CloseDevice() {
        this.AULNetTransfer(idEndPointCLOSE, (byte[]) null, 0);
        if (this.tcpSocket != null) {
            try {
                this.tcpSocket.close();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }

        if (this.btSocket != null) {
            try {
                this.btSocket.close();
            } catch (IOException exception) {
                exception.printStackTrace();
                return;
            }
        }

    }

    boolean Connect(boolean isDeviceExcisted) {
        this.devInited = false;
        if (isDeviceExcisted) {
            this.AULNetTransfer(idEndPointCLOSE, (byte[]) null, 0);
            this.demoMode = true;
        } else {
            int bufWrittenLength = 0;
            int writtenLength;
            byte[] bufferPassword;
            if (this.interfaceMode != Utils.TANetInterface.aniALAN &&
                    this.interfaceMode != Utils.TANetInterface.aniABT) {
                UsbInterface usbInterface = this.device.getInterface(0);
                if (usbInterface.getEndpointCount() < 4) {
                    return this.Connect(true);
                }

                this.epSNDCMD = usbInterface.getEndpoint(0);
                this.epRDST = usbInterface.getEndpoint(1);
                this.epWRDT = usbInterface.getEndpoint(2);
                this.epRDDT = usbInterface.getEndpoint(3);
                this.connection = this.manager.openDevice(this.device);
                this.connection.claimInterface(usbInterface, true);
            } else {
                bufferPassword = "AULNetPass ".getBytes();
                bufferPassword[10] = 0;
                writtenLength = this.AULNetTransfer(idEndPointPASSWORD, bufferPassword, 11);
                bufWrittenLength = writtenLength;
                if (writtenLength >= 0) {
                    bufWrittenLength = this.AULNetTransfer(idEndPointUNLOCK, (byte[]) null, 0);
                }
            }

            bufferPassword = new byte[16];
            writtenLength = bufWrittenLength;
            if (bufWrittenLength >= 0) {
                writtenLength = this.ReadRegister(221, bufferPassword);
            }

            this.devname = new String(bufferPassword, 0, 8);
            if (!this.isValidDevice(this.devname)) {
                return this.Connect(true);
            }

            this.demoMode = false;
            bufWrittenLength = writtenLength;
            if (writtenLength >= 0) {
                bufWrittenLength = this.ReadRegister(222, bufferPassword);
            }

            this.devname = this.devname + " #" + new String(bufferPassword, 0, 16);
            if (bufWrittenLength >= 0) {
                this.CheckAUNVers();
            }

            if (bufWrittenLength >= 0) {
                this.ResetDev();
            }
        }

        ((Utils.IAULNetListener) this.pActivity).onANConnect(this);
        return !this.demoMode;
    }

    void Destroy() {
        this.CloseDevice();
        if (this.btStarted && this.mBluetoothAdapter.isEnabled()) {
            this.mBluetoothAdapter.disable();
        }

    }

    boolean InitConnection() {
        if (this.interfaceMode == Utils.TANetInterface.aniALAN) {
            InetSocketAddress inetSocketAddress = new InetSocketAddress(this.serverIP, this.serverPort);
            new AULNetConnection.openSocketTask().execute(inetSocketAddress);
            return true;
        } else if (this.interfaceMode == Utils.TANetInterface.aniABT) {
            return this.btFind();
        } else {
            this.manager = (UsbManager) this.pActivity.getSystemService(Context.USB_SERVICE);
            if (this.manager == null) {
                return false;
            } else {
                HashMap deviceList = this.manager.getDeviceList();
                Iterator iteratorDeviceList = deviceList.values().iterator();
                this.devCnt = 0;
                if (deviceList.isEmpty()) {
                    return false;
                } else {
                    while (iteratorDeviceList.hasNext()) {
                        ++this.devCnt;
                        this.device = (UsbDevice) iteratorDeviceList.next();
                    }

                    PendingIntent pendingIntent = PendingIntent.getBroadcast(this.pActivity, 0, new Intent("com.android.example.USB_PERMISSION"), 0);
                    IntentFilter intentFilter = new IntentFilter("com.android.example.USB_PERMISSION");
                    this.pActivity.registerReceiver(this.mUsbReceiver, intentFilter);
                    this.manager.requestPermission(this.device, pendingIntent);
                    return this.devCnt > 0;
                }
            }
        }
    }

    public boolean isValidDevice(String var1) {
        boolean isValidDevice = false;

        for (int device = 0; device < this.supportedDevs.length; ++device) {
            isValidDevice |= var1.startsWith(this.supportedDevs[device]);
        }

        return isValidDevice;
    }

    public int ReadData(byte[] buffer, byte value) {
        short length = 512;
        byte[] bufferCommand = new byte[512];
        if (this.interfaceMode != Utils.TANetInterface.aniAUN2) {
            length = 64;
        }

        bufferCommand[0] = _READ_FUNCTION;
        bufferCommand[1] = value;
        this.AULNetTransfer(idEndPointSNDCM, bufferCommand, 2);
        this.AULNetTransfer(idEndPointRDDT, bufferCommand, length);
        int lengthBuffer = -1;

        int lengthRDDT;
        for (int lengthNeed = 0; lengthNeed < buffer.length; lengthBuffer = lengthRDDT) {
            lengthRDDT = this.AULNetTransfer(idEndPointRDDT, bufferCommand, length);
            if (buffer.length - lengthNeed >= length) {
                lengthBuffer = length;
            } else {
                lengthBuffer = buffer.length - lengthNeed;
            }

            System.arraycopy(bufferCommand, 0, buffer, lengthNeed, lengthBuffer);
            if (buffer.length - lengthNeed >= length) {
                lengthBuffer = length;
            } else {
                lengthBuffer = buffer.length - lengthNeed;
            }

            lengthNeed += lengthBuffer;
        }

        return lengthBuffer;
    }

    public int ReadRegister(int command, byte[] bufferRead) {
        byte length = 1;
        byte[] bufferCommand = new byte[16];
        if (221 == command) {
            bufferCommand[0] = -35;
        } else if (222 == command) {
            bufferCommand[0] = -34;
        } else {
            bufferCommand[0] = -36;
            bufferCommand[1] = (byte) command;
            length = 2;
            if (command >> 8 != 0) {
                bufferCommand[2] = (byte) (command >> 8);
                length = 3;
                if (command >> 16 != 0) {
                    bufferCommand[3] = (byte) (command >> 16);
                    length = 4;
                    if (command >> 24 != 0) {
                        bufferCommand[4] = (byte) (command >> 24);
                        length = 5;
                    }
                }
            }
        }

        this.AULNetTransfer(idEndPointSNDCM, bufferCommand, length);
        this.AULNetTransfer(idEndPointRDST, bufferRead, 16);
        return this.AULNetTransfer(idEndPointRDST, bufferRead, 16);
    }

    public void ResetDev() {
        this.devInited = false;
        this.SelectUBA(CUBA_COMMAND1);
        (new Handler()).postDelayed(new Runnable() {
            public void run() {
                AULNetConnection.this.devInited = true;

                try {
                    TimeUnit.MILLISECONDS.sleep(50L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, 500L);
    }

    public int SelectUBA(byte cubaCommand) {
        byte[] bufferCommand = new byte[16];
        bufferCommand[0] = CUBA_FUNCTION;
        bufferCommand[1] = cubaCommand;
        this.cUBA = cubaCommand;
        return this.AULNetTransfer(idEndPointSNDCM, bufferCommand, 2);
    }

    public int WriteData(byte[] bufferWrite, byte value) {
        byte[] bufferCommand = new byte[16];
        bufferCommand[0] = _READ_FUNCTION;
        bufferCommand[1] = value;
        this.AULNetTransfer(idEndPointSNDCM, bufferCommand, 2);
        return this.AULNetTransfer(idEndPointWRDT, bufferWrite, bufferWrite.length);
    }

    public int WriteRegister(int command, byte value) {
        byte[] bufferCommand = new byte[16];
        bufferCommand[0] = WRITE_FUNCTION;
        bufferCommand[1] = (byte) command;
        bufferCommand[2] = value;
        return this.AULNetTransfer(idEndPointSNDCM, bufferCommand, 3);
    }

    public int WriteRegister(int command, byte[] commandBytes) {
        byte[] bufferCommand = new byte[16];
        bufferCommand[0] = WRITE_FUNCTION;
        bufferCommand[1] = (byte) command;
        bufferCommand[2] = commandBytes[0];
        int length = 3;
        if (command >> 8 != 0) {
            bufferCommand[3] = (byte) (command >> 8);
            bufferCommand[4] = commandBytes[1];
            length += 2;
            if (command >> 16 != 0) {
                bufferCommand[5] = (byte) (command >> 16);
                bufferCommand[6] = commandBytes[2];
                length += 2;
                if (command >> 24 != 0) {
                    bufferCommand[7] = (byte) (command >> 24);
                    bufferCommand[8] = commandBytes[3];
                    length += 2;
                }
            }
        }

        return this.AULNetTransfer(idEndPointSNDCM, bufferCommand, length);
    }

    public boolean btFind() {
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!this.mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent("android.bluetooth.adapter.action.REQUEST_ENABLE");
            this.pActivity.startActivityForResult(intent, 101);
            return false;
        } else {
            this.BTDevList = new HashSet();
            this.BTDevList.clear();
            boolean var1 = this.mBluetoothAdapter.startDiscovery();
            this.pActivity.registerReceiver(this.bReceiver, new IntentFilter("android.bluetooth.device.action.FOUND"));
            this.pActivity.registerReceiver(this.bReceiver, new IntentFilter("android.bluetooth.adapter.action.DISCOVERY_FINISHED"));
            return var1;
        }
    }

    public boolean isDemoMode() {
        return this.demoMode;
    }


    private class openBtSocketTask extends AsyncTask<BluetoothDevice, Void, BluetoothSocket> {
        private final UUID BTMODULEUUID;

        private openBtSocketTask() {
            this.BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        }

        protected BluetoothSocket doInBackground(BluetoothDevice... var1) {
            try {
                BluetoothSocket bluetoothSocket = var1[0].createRfcommSocketToServiceRecord(this.BTMODULEUUID);
                AULNetConnection.this.mBluetoothAdapter.cancelDiscovery();
                bluetoothSocket.connect();
                return bluetoothSocket;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(BluetoothSocket bluetoothSocket) {
            AULNetConnection.this.btSocket = bluetoothSocket;

            try {
                TimeUnit.MILLISECONDS.sleep(100L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            AULNetConnection aulNetConnection = AULNetConnection.this;
            boolean isNotConnected;
            if (AULNetConnection.this.btSocket == null) {
                isNotConnected = true;
            } else {
                isNotConnected = false;
            }

            aulNetConnection.Connect(isNotConnected);
        }
    }

    private class openSocketTask extends AsyncTask<InetSocketAddress, Void, Socket> {
        private openSocketTask() {
        }

        protected Socket doInBackground(InetSocketAddress... inetSocketAddresses) {
            try {
                Socket socket = new Socket();
                socket.connect(inetSocketAddresses[0], 1500);
                socket.setKeepAlive(true);
                return socket;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(Socket var1) {
            AULNetConnection.this.CloseDevice();
            AULNetConnection.this.tcpSocket = var1;
            AULNetConnection var3 = AULNetConnection.this;
            boolean isNotConnected;
            if (AULNetConnection.this.tcpSocket == null) {
                isNotConnected = true;
            } else {
                isNotConnected = false;
            }

            var3.Connect(isNotConnected);
        }
    }

    private class transferData {
        public byte[] buf;
        public byte[] hlp;
        public int hlpsize;
        public boolean read;
        public int size;

        private transferData() {
            this.hlp = new byte[8];
            this.hlpsize = 0;
        }

        public void newBuf(int size) {
            this.buf = new byte[size];
        }
    }

    private class transferSocketTask extends AsyncTask<AULNetConnection.transferData, Void, Integer> {
        private static final int AULNET_HDRSIZE = 28;

        private transferSocketTask() {
        }

        protected Integer doInBackground(AULNetConnection.transferData... transferDatas) {
            boolean isBTSocket = false;
            if (AULNetConnection.this.interfaceMode != Utils.TANetInterface.aniALAN || AULNetConnection.this.tcpSocket == null) {
                if (AULNetConnection.this.interfaceMode != Utils.TANetInterface.aniABT || AULNetConnection.this.btSocket == null) {
                    return -3;
                }

                isBTSocket = true;
            }

            UnknownHostException unknownHostException;

            label220:
            {
                IOException ioException;
                label237:
                {
                    int var4;
                    var4 = transferDatas[0].hlpsize + 28;

                    int var3 = var4;

                    if (!transferDatas[0].read) {
                        var3 = var4 + transferDatas[0].size;
                    }

                    if (transferDatas[0].buf == null) {
                        return -2;
                    }

                    OutputStream outputStream;
                    if (isBTSocket) {
                        try {
                            outputStream = AULNetConnection.this.btSocket.getOutputStream();
                        } catch (UnknownHostException e) {
                            unknownHostException = e;
                            break label220;
                        } catch (IOException e) {
                            ioException = e;
                            break label237;
                        }
                    } else {
                        try {
                            outputStream = AULNetConnection.this.tcpSocket.getOutputStream();
                        } catch (UnknownHostException e) {
                            unknownHostException = e;
                            break label220;
                        } catch (IOException e) {
                            ioException = e;
                            break label237;
                        }
                    }

                    try {
                        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
                        AULNetConnection.this.outCmdCode = transferDatas[0].buf[12];
                        dataOutputStream.write(transferDatas[0].buf, 0, var3);
                        dataOutputStream.flush();
                        toHexString(transferDatas[0].buf);
                    } catch (UnknownHostException e) {
                        unknownHostException = e;
                        break label220;
                    } catch (IOException e) {
                        ioException = e;
                        break label237;
                    }

                    InputStream var58;
                    if (isBTSocket) {
                        try {
                            var58 = AULNetConnection.this.btSocket.getInputStream();
                        } catch (UnknownHostException e) {
                            unknownHostException = e;
                            break label220;
                        } catch (IOException e) {
                            ioException = e;
                            break label237;
                        }
                    } else {
                        try {
                            var58 = AULNetConnection.this.tcpSocket.getInputStream();
                        } catch (UnknownHostException e) {
                            unknownHostException = e;
                            break label220;
                        } catch (IOException e) {
                            ioException = e;
                            break label237;
                        }
                    }

                    byte[] var12;
                    DataInputStream dataInputStream;
                    dataInputStream = new DataInputStream(var58);
                    var12 = new byte[28];

                    int var55 = 0;

                    long time;
                    time = System.currentTimeMillis();

                    do {
                        try {
                            var3 = var55 + dataInputStream.read(var12, var55, 28 - var55);
                        } catch (UnknownHostException e) {
                            unknownHostException = e;
                            break label220;
                        } catch (IOException e) {
                            ioException = e;
                            break label237;
                        }

                        if (var3 < 0) {
                            if (System.currentTimeMillis() - time >= 50L) {
                                break;
                            }
                        }

                        var55 = var3;
                    } while (var3 < 28);

                    if (var3 < 0) {
                        return -3;
                    } else {
                        label239:
                        {
                            if (var12[12] != AULNetConnection.this.outCmdCode) {
                                System.exit(-3);
                            }

                            byte var56 = var12[20];
                            byte var5 = var12[21];
                            byte var6 = var12[22];
                            byte var7 = var12[23];
                            int var8 = (var12[24] & 255) + ((var12[25] & 255) << 8) + ((var12[26] & 255) << 16) + ((var12[27] & 255) << 24);
                            if (var8 <= 0) {
                                return (var56 & 255) + ((var5 & 255) << 8) + ((var6 & 255) << 16) + ((var7 & 255) << 24);
                            }

                            var12 = new byte[var8];

                            var55 = 0;

                            do {
                                try {
                                    var3 = var55 + dataInputStream.read(var12, var55, var8 - var55);
                                } catch (UnknownHostException e) {
                                    unknownHostException = e;
                                    break label220;
                                } catch (IOException e) {
                                    ioException = e;
                                    break label239;
                                }

                                if (var3 < 0) {
                                    break;
                                }

                                var55 = var3;
                            } while (var3 < var8);

                            if (var3 < 0) {
                                return -3;
                            } else {
                                label157:
                                {
                                    if (transferDatas[0].size >= var8 && transferDatas[0].buf != null) {
                                        break label157;
                                    }
                                    return -2;
                                }

                                System.arraycopy(var12, 0, transferDatas[0].buf, 28, var8);
                                return (var56 & 255) + ((var5 & 255) << 8) + ((var6 & 255) << 16) + ((var7 & 255) << 24);
                            }
                        }
                    }
                }

                IOException ioException1 = ioException;
                ioException1.printStackTrace();
                return -1;
            }

            UnknownHostException unknownHostException1 = unknownHostException;
            unknownHostException1.printStackTrace();
            return -3;
        }

        protected void onPostExecute(Integer var1) {
        }
    }
}
