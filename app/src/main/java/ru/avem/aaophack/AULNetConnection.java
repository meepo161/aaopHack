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
import android.util.Log;
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
import java.util.concurrent.TimeUnit;

import static java.sql.DriverManager.println;
import static ru.avem.aaophack.Constants.ACKScopeDrv.CUBA_COMMAND1;
import static ru.avem.aaophack.Constants.ACKScopeDrv.CUBA_COMMAND2;
import static ru.avem.aaophack.Constants.AULNetConnection.BT_ENABLE_REQUEST;
import static ru.avem.aaophack.Utils.toHexString;

public class AULNetConnection {
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    public final byte AULN_DEFUBA;
    private Set<BluetoothDevice> BTDevList;
    final BroadcastReceiver bReceiver;
    public BluetoothSocket btSocket;
    public boolean btStarted;
    public byte cUBA;
    public UsbDeviceConnection connection;
    private boolean demoMode = true;
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
        interfaceMode = Utils.TANetInterface.aniAUN;
        tcpSocket = null;
        btSocket = null;
        btStarted = false;
        connection = null;
        supportedDevs = new String[]{"ACK-3102", "ACK-3002", "ACK-3712"};
        AULN_DEFUBA = 1;
        bReceiver = new BroadcastReceiver() {
            public void onReceive(Context var1, Intent intent) {
                String bluetoothIntent = intent.getAction();
                BluetoothDevice bluetoothDevice;
                if ("android.bluetooth.device.action.FOUND".equals(bluetoothIntent)) {
                    bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                    BTDevList.add(bluetoothDevice);
                }

                if ("android.bluetooth.adapter.action.DISCOVERY_FINISHED".equals(bluetoothIntent)) {
                    pActivity.unregisterReceiver(bReceiver);
                    mBluetoothAdapter.cancelDiscovery();
                    boolean var3 = true;

                    for (BluetoothDevice value : BTDevList) {
                        bluetoothDevice = value;
                        if (isValidDevice(bluetoothDevice.getName())) {
                            new OpenBtSocketTask().execute(bluetoothDevice);
                            var3 = false;
                            break;
                        }
                    }

                    if (var3) {
                        connect(true);
                    }
                }

            }
        };
        mUsbReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                boolean isDeviceExists = true;

                UsbDevice usbDevice = (UsbDevice) intent.getParcelableExtra("device");
                if ("com.android.example.USB_PERMISSION".equals(intent.getAction())) {
                    if (intent.getBooleanExtra("permission", false)) {
                        if (usbDevice != null) {
                            isDeviceExists = false;
                        }
                        connect(isDeviceExists);
                    }
                } else {
                    Toast.makeText(pActivity.getApplicationContext(), "permission denied for device " + usbDevice, Toast.LENGTH_LONG).show();
                    connect(true);
                }
            }
        };
        pActivity = activity;
    }

    private int requestTcp(int idEndPoint, byte[] buffer, int length) {
        int idNotInitialized = -1;
        byte var6 = 0;
        byte var4 = 0;
        boolean isEndPointRead = false;
        TransferData transferData = new TransferData();
        byte var21;
        switch (idEndPoint) {
            case idEndPointRDST:
                var21 = 5;
                isEndPointRead = true;
                var4 = 8;
                transferData.hlp[0] = (byte) (length & 255);
                transferData.hlp[1] = (byte) (length >> 8 & 255);
                transferData.hlp[2] = (byte) (length >> 16 & 255);
                transferData.hlp[3] = (byte) (length >> 24 & 255);
                break;
            case idEndPointSNDCM:
                var21 = 6;
                var4 = 8;
                transferData.hlp[0] = (byte) (length & 255);
                transferData.hlp[1] = (byte) (length >> 8 & 255);
                transferData.hlp[2] = (byte) (length >> 16 & 255);
                transferData.hlp[3] = (byte) (length >> 24 & 255);
                break;
            case idEndPointRDDT:
                var21 = 7;
                isEndPointRead = true;
                var4 = 8;
                transferData.hlp[0] = (byte) (length & 255);
                transferData.hlp[1] = (byte) (length >> 8 & 255);
                transferData.hlp[2] = (byte) (length >> 16 & 255);
                transferData.hlp[3] = (byte) (length >> 24 & 255);
                break;
            case idEndPointWRDT:
                var21 = 8;
                var4 = 8;
                transferData.hlp[0] = (byte) (length & 255);
                transferData.hlp[1] = (byte) (length >> 8 & 255);
                transferData.hlp[2] = (byte) (length >> 16 & 255);
                transferData.hlp[3] = (byte) (length >> 24 & 255);
                break;
            case idEndPointPASSWORD:
                var21 = 0;
                break;
            case idEndPointUNLOCK:
                var21 = 1;
                var4 = 8;
                break;
            case idEndPointCLOSE:
                var21 = 2;
                var4 = 4;
                break;
            default:
                var21 = var6;
        }

        int var7 = var4;
        if (!isEndPointRead) {
            var7 = var4 + length;
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
        transferData.newBuf(var11.length + length + var4);
        System.arraycopy(var11, 0, transferData.buf, 0, var11.length);
        if (var4 > 0) {
            System.arraycopy(transferData.hlp, 0, transferData.buf, var11.length, var4);
        }

        if (!isEndPointRead && length > 0) {
            System.arraycopy(buffer, 0, transferData.buf, var11.length + var4, length);
        }

        transferData.read = isEndPointRead;
        transferData.size = length;
        transferData.hlpsize = var4;
        toHexString(transferData.buf);
        TransferSocketTask socketTask = new TransferSocketTask();
        socketTask.execute(transferData);

        try {
            idEndPoint = (Integer) socketTask.get(1500L, TimeUnit.MILLISECONDS);
            if (isEndPointRead) {
                System.arraycopy(transferData.buf, var11.length, buffer, 0, length);
            }
        } catch (Exception e) {
            e.printStackTrace();
            idEndPoint = idNotInitialized;
        }

        socketTask.cancel(true);
        return idEndPoint;
    }

    int AULNetTransfer(int idEndPoint, byte[] buffer, int length) {
        int writtenLength = 0;
        if (interfaceMode != Utils.TANetInterface.aniALAN && interfaceMode != Utils.TANetInterface.aniABT) {
            if (connection != null) {
                UsbEndpoint usbEndpoint;
                switch (idEndPoint) {
                    case idEndPointRDST:
                        usbEndpoint = epRDST;
                        break;
                    case idEndPointSNDCM:
                        usbEndpoint = epSNDCMD;
                        break;
                    case idEndPointRDDT:
                        usbEndpoint = epRDDT;
                        break;
                    case idEndPointWRDT:
                        usbEndpoint = epWRDT;
                        break;
                    default:
                        return length;
                }

                int writtenLengthBulk = connection.bulkTransfer(usbEndpoint, buffer, length, 0);
                Log.d("AYE1337", "numBytesWritten(1) = " + writtenLengthBulk + "/" + buffer.length);
                Log.d("AYE1337", "toHexString(1) = " + toHexString(buffer));
                if (idEndPoint != idEndPointRDST) {
                    writtenLength = writtenLengthBulk;
                    if (idEndPoint != idEndPointRDDT) {
                        return writtenLength;
                    }
                }

                writtenLength = writtenLengthBulk;
            }
        } else {
            writtenLength = requestTcp(idEndPoint, buffer, length);
        }

        return writtenLength;
    }

    public boolean checkAUNVers() {
        boolean versionOk = false;
        if (interfaceMode == Utils.TANetInterface.aniAUN || interfaceMode == Utils.TANetInterface.aniAUN2) {
            byte[] buffer = new byte[16];
            selectUBA(CUBA_COMMAND2);
            if (readRegister(42, buffer) < 0) {
                buffer[0] = 0;
            }
            versionOk = buffer[0] == 1;
        }

        if (versionOk) {
            interfaceMode = Utils.TANetInterface.aniAUN2;
        }

        return versionOk;
    }

    public void closeDevice() {
        AULNetTransfer(idEndPointCLOSE, null, 0);
        if (tcpSocket != null) {
            try {
                tcpSocket.close();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }

        if (btSocket != null) {
            try {
                btSocket.close();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    }

    boolean connect(boolean isDeviceExist) {
        devInited = false;
        if (isDeviceExist) {
            AULNetTransfer(idEndPointCLOSE, null, 0);
            demoMode = true;
        } else {
            int bufWrittenLength = 0;
            int writtenLength;
            byte[] bufferPassword;
            if (interfaceMode != Utils.TANetInterface.aniALAN &&
                    interfaceMode != Utils.TANetInterface.aniABT) {
                UsbInterface usbInterface = device.getInterface(0);
                if (usbInterface.getEndpointCount() < 4) {
                    return connect(true);
                }

                epSNDCMD = usbInterface.getEndpoint(0);
                epRDST = usbInterface.getEndpoint(1);
                epWRDT = usbInterface.getEndpoint(2);
                epRDDT = usbInterface.getEndpoint(3);
                connection = manager.openDevice(device);
                connection.claimInterface(usbInterface, true);
            } else {
                bufferPassword = "AULNetPass ".getBytes();
                bufferPassword[10] = 0;
                writtenLength = AULNetTransfer(idEndPointPASSWORD, bufferPassword, 11);
                bufWrittenLength = writtenLength;
                if (writtenLength >= 0) {
                    bufWrittenLength = AULNetTransfer(idEndPointUNLOCK, null, 0);
                }
            }

            bufferPassword = new byte[16];
            writtenLength = bufWrittenLength;
            if (bufWrittenLength >= 0) {
                writtenLength = readRegister(221, bufferPassword);
            }

            devname = new String(bufferPassword, 0, 8);
            if (!isValidDevice(devname)) {
                return connect(true);
            }

            demoMode = false;
            bufWrittenLength = writtenLength;
            if (writtenLength >= 0) {
                bufWrittenLength = readRegister(222, bufferPassword);
            }

            devname = devname + " #" + new String(bufferPassword, 0, 16);
            if (bufWrittenLength >= 0) {
                checkAUNVers();
            }

            if (bufWrittenLength >= 0) {
                resetDev();
            }
        }

        ((Utils.IAULNetListener) pActivity).onANConnect(this);
        return !demoMode;
    }

    boolean initConnection() {
        if (interfaceMode == Utils.TANetInterface.aniALAN) {
            InetSocketAddress inetSocketAddress = new InetSocketAddress(serverIP, serverPort);
            new OpenSocketTask().execute(inetSocketAddress);
            return true;
        } else if (interfaceMode == Utils.TANetInterface.aniABT) {
            return btFind();
        } else {
            manager = (UsbManager) pActivity.getSystemService(Context.USB_SERVICE);
            if (manager == null) {
                return false;
            } else {
                HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
                Iterator<UsbDevice> iteratorDeviceList = deviceList.values().iterator();
                int devCnt = 0;
                if (deviceList.isEmpty()) {
                    return false;
                } else {
                    while (iteratorDeviceList.hasNext()) {
                        ++devCnt;
                        device = iteratorDeviceList.next();
                    }

                    PendingIntent pendingIntent = PendingIntent.getBroadcast(pActivity, 0, new Intent("com.android.example.USB_PERMISSION"), 0);
                    IntentFilter intentFilter = new IntentFilter("com.android.example.USB_PERMISSION");
                    pActivity.registerReceiver(mUsbReceiver, intentFilter);
                    manager.requestPermission(device, pendingIntent);
                    return devCnt > 0;
                }
            }
        }
    }

    public boolean isValidDevice(String var1) {
        boolean isValidDevice = false;

        for (String supportedDev : supportedDevs) {
            isValidDevice |= var1.startsWith(supportedDev);
        }

        return isValidDevice;
    }

    public int readData(byte[] buffer, byte value) {
        short length = 512;
        byte[] bufferCommand = new byte[512];
        if (interfaceMode != Utils.TANetInterface.aniAUN2) {
            length = 64;
        }

        bufferCommand[0] = _READ_FUNCTION;
        bufferCommand[1] = value;
        AULNetTransfer(idEndPointSNDCM, bufferCommand, 2);
        AULNetTransfer(idEndPointRDDT, bufferCommand, length);
        int lengthBuffer = -1;

        int lengthRDDT;
        for (int lengthNeed = 0; lengthNeed < buffer.length; lengthBuffer = lengthRDDT) {
            lengthRDDT = AULNetTransfer(idEndPointRDDT, bufferCommand, length);
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

    public int readRegister(int command, byte[] bufferRead) {
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

        AULNetTransfer(idEndPointSNDCM, bufferCommand, length);
        AULNetTransfer(idEndPointRDST, bufferRead, 16);
        return AULNetTransfer(idEndPointRDST, bufferRead, 16);
    }

    public void resetDev() {
        devInited = false;
        selectUBA(CUBA_COMMAND1);
        (new Handler()).postDelayed(new Runnable() {
            public void run() {
                devInited = true;

                try {
                    TimeUnit.MILLISECONDS.sleep(50L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, 500L);
    }

    public int selectUBA(byte cubaCommand) {
        byte[] bufferCommand = new byte[16];
        bufferCommand[0] = CUBA_FUNCTION;
        bufferCommand[1] = cubaCommand;
        cUBA = cubaCommand;
        return AULNetTransfer(idEndPointSNDCM, bufferCommand, 2);
    }

    public int writeData(byte[] bufferWrite, byte value) {
        byte[] bufferCommand = new byte[16];
        bufferCommand[0] = _READ_FUNCTION;
        bufferCommand[1] = value;
        AULNetTransfer(idEndPointSNDCM, bufferCommand, 2);
        return AULNetTransfer(idEndPointWRDT, bufferWrite, bufferWrite.length);
    }

    public int writeRegister(int command, byte value) {
        byte[] bufferCommand = new byte[16];
        bufferCommand[0] = WRITE_FUNCTION;
        bufferCommand[1] = (byte) command;
        bufferCommand[2] = value;
        return AULNetTransfer(idEndPointSNDCM, bufferCommand, 3);
    }

    public int writeRegister(int command, byte[] commandBytes) {
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

        return AULNetTransfer(idEndPointSNDCM, bufferCommand, length);
    }

    public boolean btFind() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent("android.bluetooth.adapter.action.REQUEST_ENABLE");
            pActivity.startActivityForResult(intent, BT_ENABLE_REQUEST);
            return false;
        } else {
            BTDevList = new HashSet<>();
            BTDevList.clear();
            boolean var1 = mBluetoothAdapter.startDiscovery();
            pActivity.registerReceiver(bReceiver, new IntentFilter("android.bluetooth.device.action.FOUND"));
            pActivity.registerReceiver(bReceiver, new IntentFilter("android.bluetooth.adapter.action.DISCOVERY_FINISHED"));
            return var1;
        }
    }

    public boolean isDemoMode() {
        return demoMode;
    }


    private class OpenBtSocketTask extends AsyncTask<BluetoothDevice, Void, BluetoothSocket> {
        private final UUID BTMODULEUUID;

        private OpenBtSocketTask() {
            BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        }

        protected BluetoothSocket doInBackground(BluetoothDevice... var1) {
            try {
                BluetoothSocket bluetoothSocket = var1[0].createRfcommSocketToServiceRecord(BTMODULEUUID);
                mBluetoothAdapter.cancelDiscovery();
                bluetoothSocket.connect();
                return bluetoothSocket;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(BluetoothSocket bluetoothSocket) {
            btSocket = bluetoothSocket;

            try {
                TimeUnit.MILLISECONDS.sleep(100L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            AULNetConnection aulNetConnection = AULNetConnection.this;
            boolean isNotConnected;
            isNotConnected = btSocket == null;

            aulNetConnection.connect(isNotConnected);
        }
    }

    private class OpenSocketTask extends AsyncTask<InetSocketAddress, Void, Socket> {
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

        protected void onPostExecute(Socket socket) {
            closeDevice();
            tcpSocket = socket;
            AULNetConnection aulNetConnection = AULNetConnection.this;
            boolean isNotConnected;
            isNotConnected = tcpSocket == null;

            aulNetConnection.connect(isNotConnected);
        }
    }

    private class TransferData {
        public byte[] buf;
        public byte[] hlp;
        public int hlpsize;
        public boolean read;
        public int size;

        private TransferData() {
            hlp = new byte[8];
            hlpsize = 0;
        }

        public void newBuf(int size) {
            buf = new byte[size];
        }
    }

    private class TransferSocketTask extends AsyncTask<TransferData, Void, Integer> {
        private static final int AULNET_HDRSIZE = 28;

        protected Integer doInBackground(TransferData... TransferData) {
            boolean isBTSocket = false;
            if (interfaceMode != Utils.TANetInterface.aniALAN || tcpSocket == null) {
                if (interfaceMode != Utils.TANetInterface.aniABT || btSocket == null) {
                    return -3;
                }

                isBTSocket = true;
            }

            int var4 = TransferData[0].hlpsize + 28;
            int length = var4;
            if (!TransferData[0].read) {
                length = var4 + TransferData[0].size;
            }

            if (TransferData[0].buf == null) {
                return -2;
            } else {
                byte[] buffer;
                byte buffer20;
                byte buffer21;
                byte buffer22;
                byte buffer23;
                int bufferSize;
                label1:
                {
                    UnknownHostException unknownHostException;
                    label2:
                    {
                        IOException ioException;
                        label3:
                        {
                            OutputStream outputStream;
                            if (isBTSocket) {
                                try {
                                    outputStream = btSocket.getOutputStream();
                                } catch (UnknownHostException e) {
                                    unknownHostException = e;
                                    break label2;
                                } catch (IOException e) {
                                    ioException = e;
                                    break label3;
                                }
                            } else {
                                try {
                                    outputStream = tcpSocket.getOutputStream();
                                } catch (UnknownHostException e) {
                                    unknownHostException = e;
                                    break label2;
                                } catch (IOException e) {
                                    ioException = e;
                                    break label3;
                                }
                            }

                            try {
                                DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
                                outCmdCode = TransferData[0].buf[12];
                                dataOutputStream.write(TransferData[0].buf, 0, length);
                                dataOutputStream.flush();
                                Utils.toHexString(TransferData[0].buf);
                            } catch (UnknownHostException e) {
                                unknownHostException = e;
                                break label2;
                            } catch (IOException e) {
                                ioException = e;
                                break label3;
                            }

                            InputStream inputStream;
                            if (isBTSocket) {
                                try {
                                    inputStream = btSocket.getInputStream();
                                } catch (UnknownHostException e) {
                                    unknownHostException = e;
                                    break label2;
                                } catch (IOException e) {
                                    ioException = e;
                                    break label3;
                                }
                            } else {
                                try {
                                    inputStream = tcpSocket.getInputStream();
                                } catch (UnknownHostException e) {
                                    unknownHostException = e;
                                    break label2;
                                } catch (IOException e) {
                                    ioException = e;
                                    break label3;
                                }
                            }

                            DataInputStream dataInputStream = new DataInputStream(inputStream);
                            buffer = new byte[28];
                            int offset = 0;
                            long time = System.currentTimeMillis();

                            do {
                                try {
                                    length = offset + dataInputStream.read(buffer, offset, 28 - offset);
                                } catch (UnknownHostException e) {
                                    unknownHostException = e;
                                    break label2;
                                } catch (IOException e) {
                                    ioException = e;
                                    break label3;
                                }

                                if (length < 0 && System.currentTimeMillis() - time >= 50L) {
                                    break;
                                }

                                offset = length;
                            } while (length < 28);

                            if (length < 0) {
                                return -3;
                            }

                            if (buffer[12] != outCmdCode) {
                                System.exit(-3);
                            }

                            buffer20 = buffer[20];
                            buffer21 = buffer[21];
                            buffer22 = buffer[22];
                            buffer23 = buffer[23];
                            bufferSize = (buffer[24] & 255) + ((buffer[25] & 255) << 8) + ((buffer[26] & 255) << 16) + ((buffer[27] & 255) << 24);
                            if (bufferSize <= 0) {
                                return (buffer20 & 255) + ((buffer21 & 255) << 8) + ((buffer22 & 255) << 16) + ((buffer23 & 255) << 24);
                            }

                            buffer = new byte[bufferSize];
                            offset = 0;

                            while (true) {
                                try {
                                    length = offset + dataInputStream.read(buffer, offset, bufferSize - offset);
                                } catch (UnknownHostException e) {
                                    unknownHostException = e;
                                    break label2;
                                } catch (IOException e) {
                                    ioException = e;
                                    break;
                                }

                                if (length < 0) {
                                    break label1;
                                }

                                offset = length;
                                if (length >= bufferSize) {
                                    break label1;
                                }
                            }
                        }

                        ioException.printStackTrace();
                        return -1;
                    }

                    unknownHostException.printStackTrace();
                    return -3;
                }

                if (length < 0) {
                    return -3;
                } else if (TransferData[0].size >= bufferSize && TransferData[0].buf != null) {
                    System.arraycopy(buffer, 0, TransferData[0].buf, 28, bufferSize);
                    return (buffer20 & 255) + ((buffer21 & 255) << 8) + ((buffer22 & 255) << 16) + ((buffer23 & 255) << 24);
                } else {
                    return -2;
                }
            }
        }
    }
}
