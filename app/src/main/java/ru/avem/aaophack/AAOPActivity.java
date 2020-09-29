//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package ru.avem.aaophack;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.usb.UsbDevice;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.VerticalSeekBar;

import com.aktakom.aaop.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import ru.avem.aaophack.ACKScopeDrv.IACKScopeListener;
import ru.avem.aaophack.Utils.IAULNetListener;
import ru.avem.aaophack.Plot2d.Iplot2dListener;
import ru.avem.aaophack.Plot2d.Plot;

import static android.content.pm.PackageManager.GET_ACTIVITIES;
import static ru.avem.aaophack.Utils.sleep;

public class AAOPActivity extends Activity implements OnClickListener, IAULNetListener, IACKScopeListener, Iplot2dListener, OnItemSelectedListener, OnSeekBarChangeListener, OnCheckedChangeListener {
    public static final boolean D = true;
    public static final byte MAX_SENS_COUNT = 2;
    public static final String PREFS_NAME = "AOPAPrefs";
    public static final String TAG = "AOPA";
    private static final String dataFolder = "/AOPA/";
    public static String serverIP = "192.168.0.1";
    public static int serverPort = 1024;
    public static boolean tcpOn = false;
    private TextView ampp1Txt;
    private TextView ampp2Txt;
    private TextView amps1Txt;
    private TextView amps2Txt;
    private TextView appTitle;
    private int autoControl = 0;
    private ImageButton autosetBtn;
    private ImageButton btnClose;
    private ImageButton btnLoad;
    private ImageButton btnSave;
    private ImageButton btnOur;
    private boolean ch1Enabled = true;
    private boolean ch2Enabled = true;
    private Spinner cpl1Spinner;
    private Spinner cpl2Spinner;
    private LinearLayout ctrlLayout;
    private Button ctrlShowBtn;
    public int dataLenK = 1;
    private Spinner dataLenSpinner;
    protected int datawaiting = 0;
    public float fontSize;
    private TextView freq1Txt;
    private TextView freq2Txt;
    private Spinner generatorSpinner;
    private Plot2d graph1;
    private LinearLayout graphLayout;
    private ImageView imageLogo;
    private boolean keepscreen = true;
    private LinearLayout layoutBack;
    private LinearLayout layoutWork;
    private Handler mHandler = null;
    private boolean noVoidData = false;
    private VerticalSeekBar offset1SeekBar;
    private VerticalSeekBar offset2SeekBar;
    private LinearLayout ofs1Layout;
    private LinearLayout ofs2Layout;
    private ACKScopeDrv pACKScopeDrv;
    private int[] pltColors = new int[]{-65536, -16776961, -16744448, -65281, -16744320, -49152, -8355840, -9437056};
    private final float[] prefixMultipliers = new float[]{1.0E12F, 1.0E9F, 1000000.0F, 1000.0F, 1.0F, 0.001F, 1.0E-6F};
    private String[] prefixStrings;
    private double pretrgRate = 0.5D;
    private SeekBar pretrgSeekBar;
    private Spinner probe1Spinner;
    private Spinner probe2Spinner;
    private Spinner range1Spinner;
    private Spinner range2Spinner;
    private ToggleButton runBtn;
    private Spinner runmodeSpinner;
    private ProgressBar searchPrg;
    private Spinner tbSpinner;
    public Socket tcpSocket = null;
    private LinearLayout trglevelLayout;
    private VerticalSeekBar trglevelSeekBar;
    private Spinner trglogSpinner;
    private Spinner trgsrcSpinner;
    private TextView tvScale;
    private String txtDevName = "none";
    private Runnable updateTask = new Runnable() {
        public void run() {
            switch (AAOPActivity.this.datawaiting) {
                case 0:
                    AAOPActivity.this.datawaiting = 1;
                    AAOPActivity.this.pACKScopeDrv.readWaveform();
                case 1:
                default:
                    break;
                case 2:
                case 3:
                case 4:
                    AAOPActivity.this.graph1.plots[0].visible = AAOPActivity.this.ch1Enabled;
                    int vectorOffset = AAOPActivity.this.dataLenK * 12;
                    if (AAOPActivity.this.ch1Enabled) {
                        AAOPActivity.this.graph1.setData(0, AAOPActivity.this.xvalues, AAOPActivity.this.yvalues[0], vectorOffset, AAOPActivity.this.xvalues.length - vectorOffset * 2);
                    }

                    AAOPActivity.this.graph1.plots[1].visible = AAOPActivity.this.ch2Enabled;
                    if (AAOPActivity.this.ch2Enabled) {
                        AAOPActivity.this.graph1.setData(1, AAOPActivity.this.xvalues, AAOPActivity.this.yvalues[1], vectorOffset, AAOPActivity.this.xvalues.length - vectorOffset * 2);
                    }

                    AAOPActivity.this.graph1.xmarks[0].x = (float) (-100.0D + 200.0D * AAOPActivity.this.pretrgRate);
                    AAOPActivity.this.doMeas();
                    if (AAOPActivity.this.datawaiting != 3 && AAOPActivity.this.pACKScopeDrv.getTriggerMode() != 2) {
                        AAOPActivity.this.datawaiting = 1;
                        AAOPActivity.this.pACKScopeDrv.readWaveform();
                    } else {
                        AAOPActivity.this.datawaiting = 0;
                        AAOPActivity.this.StartMTimer(false);
                    }
            }

            if (AAOPActivity.this.mHandler != null) {
                AAOPActivity.this.mHandler.postDelayed(AAOPActivity.this.updateTask, 500L);
            }

        }
    };
    private int valuesCount = 0;
    private float[] xvalues;
    private float[][] yvalues;

    public AAOPActivity() {
    }

    private void AutoControl(int imageAutoSet) {
        if (imageAutoSet < 0) {
            byte autoControlByte;
            if (this.autoControl != 0) {
                autoControlByte = 0;
            } else {
                autoControlByte = 10;
            }

            this.autoControl = autoControlByte;
        } else {
            this.autoControl = imageAutoSet;
        }

        ImageButton autosetBtn = this.autosetBtn;
        if (this.autoControl != 0) {
            imageAutoSet = R.drawable.ic_autoset_on;
        } else {
            imageAutoSet = R.drawable.ic_autoset_off;
        }

        autosetBtn.setImageResource(imageAutoSet);
    }

    private float ChannelCodeToValue(int var1, int var2) {
        return (float) (100.0D * ((double) var2 - 127.5D) / 127.5D);
    }

    private void CorrectOffset(int var1, double var2, double var4, int var6) {
        int var7;
        if (var1 != 0) {
            var7 = this.offset2SeekBar.getProgress();
        } else {
            var7 = this.offset1SeekBar.getProgress();
        }

        var7 += (int) (0.0025D * (var2 + var4) * 4095.0D);
        if (var1 != 0) {
            this.offset2SeekBar.setProgress(var7);
        } else {
            this.offset1SeekBar.setProgress(var7);
        }

        if (this.pACKScopeDrv != null) {
            this.pACKScopeDrv.setOffset(var7, var1);
        }

        if (var6 == var1) {
            var1 = (int) ((0.5D - 0.005D * var2) * 4095.0D);
            this.trglevelSeekBar.setProgress(var1);
            if (this.pACKScopeDrv != null) {
                this.pACKScopeDrv.setTriggerLevel(var1, 0);
                this.pACKScopeDrv.setTriggerLevel(var1, 1);
            }
        }

    }

    private void CorrectRange(int var1, double var2) {
        int var5;
        if (var1 != 0) {
            var5 = this.range2Spinner.getSelectedItemPosition();
        } else {
            var5 = this.range1Spinner.getSelectedItemPosition();
        }

        int var4;
        if (var2 > 0.0D) {
            while (true) {
                var4 = var5;
                if (var5 <= 0) {
                    break;
                }

                if (var2 <= 2.15D) {
                    var4 = var5;
                    break;
                }

                --var5;
                var2 /= 2.15D;
            }
        } else {
            ++var5;
            var4 = var5;
            if (var5 > ACKScopeDrv.voltrangTab.length - 1) {
                var4 = ACKScopeDrv.voltrangTab.length - 1;
            }
        }

        if (var1 != 0) {
            this.range2Spinner.setSelection(var4);
        } else {
            this.range1Spinner.setSelection(var4);
        }

        if (this.pACKScopeDrv != null) {
            this.pACKScopeDrv.setRange(var4, var1);
        }

    }

    private void CorrectSampleRate(double var1) {
        int var4 = this.tbSpinner.getCount() - 1 - this.tbSpinner.getSelectedItemPosition();
        int var3;
        if (var1 > 1.0D) {
            while (true) {
                var3 = var4;
                if (var4 <= 0) {
                    break;
                }

                if (var1 <= 2.15D) {
                    var3 = var4;
                    break;
                }

                --var4;
                var1 /= 2.15D;
            }
        } else {
            var3 = var4;
            if (var1 > 0.0D) {
                while (true) {
                    var3 = var4;
                    if (var4 >= ACKScopeDrv.timebaseTab.length - 1) {
                        break;
                    }

                    var3 = var4;
                    if (var1 >= 0.46511627906976744D) {
                        break;
                    }

                    ++var4;
                    var1 *= 2.15D;
                }
            }
        }

        this.tbSpinner.setSelection(this.tbSpinner.getCount() - 1 - var3);
        if (this.pACKScopeDrv != null) {
            this.pACKScopeDrv.setSampleRate(var3);
        }

    }

    private void DefaultScopeSettings() {
        this.tbSpinner.setSelection(ACKScopeDrv.timebaseTab.length - 1);
        this.dataLenSpinner.setSelection(0);
        this.runmodeSpinner.setSelection(0);
        this.trgsrcSpinner.setSelection(0);
        this.trglogSpinner.setSelection(0);
        this.generatorSpinner.setSelection(0);
        this.range1Spinner.setSelection(ACKScopeDrv.voltrangTab.length - 1);
        this.range2Spinner.setSelection(ACKScopeDrv.voltrangTab.length - 1);
        this.cpl1Spinner.setSelection(0);
        this.cpl2Spinner.setSelection(0);
        this.probe1Spinner.setSelection(0);
        this.probe2Spinner.setSelection(0);
        this.pretrgRate = 0.5D;
        this.pretrgSeekBar.setProgress((int) (this.pretrgRate * (double) this.pretrgSeekBar.getMax()));
        int var1 = (int) ((double) (this.dataLenK * 1024) * this.pretrgRate + 0.5D);
        if (this.pACKScopeDrv != null) {
            this.pACKScopeDrv.setTrgDelay(var1);
            this.pACKScopeDrv.setPostTrgLength(this.dataLenK * 1024 - var1);
        }

        this.offset1SeekBar.setProgress(2047);
        this.offset2SeekBar.setProgress(2047);
        this.trglevelSeekBar.setProgress(2047);
        if (this.pACKScopeDrv != null) {
            this.pACKScopeDrv.setTriggerLevel(2047, 0);
            this.pACKScopeDrv.setTriggerLevel(2047, 1);
        }

    }

    private String[] GetFileNames() {
        File var1 = new File(Environment.getExternalStorageDirectory() + "/AOPA/" + "//");

        try {
            var1.mkdirs();
        } catch (SecurityException var3) {
            Log.e("AOPA", "unable to write on the sd card " + var3.toString());
        }

        String[] var4;
        if (var1.exists()) {
            var4 = var1.list(new FilenameFilter() {
                public boolean accept(File var1, String var2) {
                    var1 = new File(var1, var2);
                    return var2.contains(".csv") && var1.length() > 0L;
                }
            });
        } else {
            var4 = new String[0];
        }

        Log.i("AOPA", "Files in list: " + var4.length);
        return var4;
    }

    private int GetRFileName() {
        final String[] var1 = this.GetFileNames();
        if (var1 != null && var1.length >= 1) {
            Builder var2 = new Builder(this);
            var2.setTitle(R.string.selectfile);
            var2.setIcon(R.drawable.ic_menu_archive);
            var2.setSingleChoiceItems(var1, var1.length - 1, new android.content.DialogInterface.OnClickListener() {
                public void onClick(DialogInterface var1x, int var2) {
                    var1x.cancel();
                    AAOPActivity.this.StartMTimer(false);
                    AAOPActivity.this.LoadData("/AOPA/" + var1[var2], false);
                }
            });
            var2.setPositiveButton(this.getResources().getString(android.R.string.ok), new android.content.DialogInterface.OnClickListener() {
                public void onClick(DialogInterface var1x, int var2) {
                    AAOPActivity.this.StartMTimer(false);
                    AAOPActivity.this.LoadData("/AOPA/" + var1[var1.length - 1], false);
                }
            });
            var2.setNegativeButton(this.getResources().getString(android.R.string.cancel), new android.content.DialogInterface.OnClickListener() {
                public void onClick(DialogInterface var1, int var2) {
                }
            });
            var2.setCancelable(false);
            var2.show();
            return 1;
        } else {
            Toast.makeText(this.getApplicationContext(), R.string.err_nodatafile, Toast.LENGTH_LONG).show();
            return -1;
        }
    }

    private float GraphCodeToValue(int var1, float var2) {
        int var5 = 1;
        if (this.pACKScopeDrv != null) {
            var5 = this.pACKScopeDrv.getProbe(var1);
        }

        double var3 = (double) var5 * ACKScopeDrv.voltrangTab[this.pACKScopeDrv.getRange(var1)] / 25.0D;
        return (float) ((double) var2 * var3);
    }

    private float GraphTimeToValue(int var1, float var2) {
        double var3 = ACKScopeDrv.timebaseTab[this.pACKScopeDrv.getSampleRate()];
        return (float) ((double) var2 * var3);
    }

    private void HelpDialog() {
        this.ShowHelpLayout(true);
        Builder var3 = new Builder(this);
        String var1 = "\n" + this.getResources().getString(R.string.version) + ": ";

        label13:
        {
            String var6;
            try {
                PackageInfo var2 = this.getPackageManager().getPackageInfo(this.getPackageName(), GET_ACTIVITIES);
                var6 = var1 + var2.versionName;
            } catch (NameNotFoundException var4) {
                var4.printStackTrace();
                break label13;
            }

            var1 = var6;
        }

        var3.setTitle(R.string.about);
        var3.setMessage(this.getResources().getString(R.string.app_longname) + "\n(C) AKTAKOM, 2014" + var1);
        var3.setIcon(R.drawable.ic_launcher);
        TextView var5 = new TextView(this);
        var5.setLinksClickable(true);
        var5.setMovementMethod(LinkMovementMethod.getInstance());
        var5.setText(Html.fromHtml("<br>&nbsp;&nbsp;&nbsp;&nbsp;" + this.getResources().getString(R.string.apphelp_urihtml) + "<br><br>&nbsp;&nbsp;&nbsp;&nbsp;" + this.getResources().getString(R.string.aktakom_urihtml)));
        var3.setView(var5);
        var3.setPositiveButton(this.getResources().getString(android.R.string.ok), new android.content.DialogInterface.OnClickListener() {
            public void onClick(DialogInterface var1, int var2) {
            }
        });
        var3.show();
    }

    private void InitConnection() {
        this.searchPrg.setVisibility(View.VISIBLE);
        this.StartMTimer(false);
        String var1 = this.getResources().getString(R.string.app_longname) + "\n" + this.getResources().getString(R.string.initconnect);
        this.appTitle.setText(var1);
        if (this.pACKScopeDrv == null) {
            this.pACKScopeDrv = new ACKScopeDrv(this, tcpOn, serverPort, serverIP);
        } else {
            this.pACKScopeDrv.InitConnection();
        }
    }

    private int LoadData(String var1, boolean var2) {
        File var9 = new File(Environment.getExternalStorageDirectory(), var1);
        if (!var9.exists()) {
            if (!var2) {
                Toast.makeText(this.getApplicationContext(), "Error. Open data file failure!\tОшибка при открытии файла данных.", Toast.LENGTH_LONG).show();
            }

            return -1;
        } else {
            RandomAccessFile var10;
            try {
                var10 = new RandomAccessFile(var9, "r");
            } catch (FileNotFoundException var8) {
                var8.printStackTrace();
                if (!var2) {
                    Toast.makeText(this.getApplicationContext(), "Error. Open data file failure!\tОшибка при открытии файла данных.", Toast.LENGTH_LONG).show();
                }

                return -2;
            }

            NumberFormat var5 = NumberFormat.getInstance();
            ParsePosition var6 = new ParsePosition(0);
            String var7 = this.ReadUTFLine(var10, 2, 1);
            if (var7 == null) {
                return 0;
            } else if (var7.isEmpty()) {
                return -3;
            } else {
                var7 = this.ReadUTFLine(var10, 2, 1);
                if (var7 == null) {
                    return 0;
                } else if (var7.isEmpty()) {
                    return -3;
                } else {
                    var6.setIndex(0);
                    Number var11 = var5.parse(var7, var6);
                    if (var11 == null) {
                        return -3;
                    } else {
                        this.tbSpinner.setSelection(var11.intValue());
                        var7 = this.ReadUTFLine(var10, 2, 1);
                        if (var7 == null) {
                            return 0;
                        } else if (var7.isEmpty()) {
                            return -3;
                        } else {
                            var6.setIndex(0);
                            var11 = var5.parse(var7, var6);
                            if (var11 == null) {
                                return -3;
                            } else {
                                this.dataLenSpinner.setSelection(var11.intValue());
                                var7 = this.ReadUTFLine(var10, 2, 1);
                                if (var7 == null) {
                                    return 0;
                                } else if (var7.isEmpty()) {
                                    return -3;
                                } else {
                                    var6.setIndex(0);
                                    var11 = var5.parse(var7, var6);
                                    if (var11 == null) {
                                        return -3;
                                    } else {
                                        this.runmodeSpinner.setSelection(var11.intValue());
                                        var7 = this.ReadUTFLine(var10, 2, 1);
                                        if (var7 == null) {
                                            return 0;
                                        } else if (var7.isEmpty()) {
                                            return -3;
                                        } else {
                                            var6.setIndex(0);
                                            var11 = var5.parse(var7, var6);
                                            if (var11 == null) {
                                                return -3;
                                            } else {
                                                this.trgsrcSpinner.setSelection(var11.intValue());
                                                var7 = this.ReadUTFLine(var10, 2, 1);
                                                if (var7 == null) {
                                                    return 0;
                                                } else if (var7.isEmpty()) {
                                                    return -3;
                                                } else {
                                                    var6.setIndex(0);
                                                    var11 = var5.parse(var7, var6);
                                                    if (var11 == null) {
                                                        return -3;
                                                    } else {
                                                        this.trglogSpinner.setSelection(var11.intValue());
                                                        var7 = this.ReadUTFLine(var10, 2, 1);
                                                        if (var7 == null) {
                                                            return 0;
                                                        } else if (var7.isEmpty()) {
                                                            return -3;
                                                        } else {
                                                            var6.setIndex(0);
                                                            var11 = var5.parse(var7, var6);
                                                            if (var11 == null) {
                                                                return -3;
                                                            } else {
                                                                this.range1Spinner.setSelection(var11.intValue());
                                                                var7 = this.ReadUTFLine(var10, 2, 1);
                                                                if (var7 == null) {
                                                                    return 0;
                                                                } else if (var7.isEmpty()) {
                                                                    return -3;
                                                                } else {
                                                                    var6.setIndex(0);
                                                                    var11 = var5.parse(var7, var6);
                                                                    if (var11 == null) {
                                                                        return -3;
                                                                    } else {
                                                                        this.range2Spinner.setSelection(var11.intValue());
                                                                        var7 = this.ReadUTFLine(var10, 2, 1);
                                                                        if (var7 == null) {
                                                                            return 0;
                                                                        } else if (var7.isEmpty()) {
                                                                            return -3;
                                                                        } else {
                                                                            var6.setIndex(0);
                                                                            var11 = var5.parse(var7, var6);
                                                                            if (var11 == null) {
                                                                                return -3;
                                                                            } else {
                                                                                this.cpl1Spinner.setSelection(var11.intValue());
                                                                                var7 = this.ReadUTFLine(var10, 2, 1);
                                                                                if (var7 == null) {
                                                                                    return 0;
                                                                                } else if (var7.isEmpty()) {
                                                                                    return -3;
                                                                                } else {
                                                                                    var6.setIndex(0);
                                                                                    var11 = var5.parse(var7, var6);
                                                                                    if (var11 == null) {
                                                                                        return -3;
                                                                                    } else {
                                                                                        this.cpl2Spinner.setSelection(var11.intValue());
                                                                                        var7 = this.ReadUTFLine(var10, 2, 1);
                                                                                        if (var7 == null) {
                                                                                            return 0;
                                                                                        } else if (var7.isEmpty()) {
                                                                                            return -3;
                                                                                        } else {
                                                                                            var6.setIndex(0);
                                                                                            var11 = var5.parse(var7, var6);
                                                                                            if (var11 == null) {
                                                                                                return -3;
                                                                                            } else {
                                                                                                this.probe1Spinner.setSelection(var11.intValue());
                                                                                                var7 = this.ReadUTFLine(var10, 2, 1);
                                                                                                if (var7 == null) {
                                                                                                    return 0;
                                                                                                } else if (var7.isEmpty()) {
                                                                                                    return -3;
                                                                                                } else {
                                                                                                    var6.setIndex(0);
                                                                                                    var11 = var5.parse(var7, var6);
                                                                                                    if (var11 == null) {
                                                                                                        return -3;
                                                                                                    } else {
                                                                                                        this.probe2Spinner.setSelection(var11.intValue());
                                                                                                        var7 = this.ReadUTFLine(var10, 2, 1);
                                                                                                        if (var7 == null) {
                                                                                                            return 0;
                                                                                                        } else if (var7.isEmpty()) {
                                                                                                            return -3;
                                                                                                        } else {
                                                                                                            var6.setIndex(0);
                                                                                                            var11 = var5.parse(var7, var6);
                                                                                                            if (var11 == null) {
                                                                                                                return -3;
                                                                                                            } else {
                                                                                                                int var3 = var11.intValue();
                                                                                                                this.pretrgSeekBar.setProgress(var3);
                                                                                                                this.pretrgRate = (double) ((float) var3 / (float) this.pretrgSeekBar.getMax());
                                                                                                                var3 = (int) (0.5D + (double) (this.dataLenK * 1024) * this.pretrgRate);
                                                                                                                if (this.pACKScopeDrv != null) {
                                                                                                                    this.pACKScopeDrv.setTrgDelay(var3);
                                                                                                                    this.pACKScopeDrv.setPostTrgLength(this.dataLenK * 1024 - var3);
                                                                                                                }

                                                                                                                var7 = this.ReadUTFLine(var10, 2, 1);
                                                                                                                if (var7 == null) {
                                                                                                                    return 0;
                                                                                                                } else if (var7.isEmpty()) {
                                                                                                                    return -3;
                                                                                                                } else {
                                                                                                                    var6.setIndex(0);
                                                                                                                    var11 = var5.parse(var7, var6);
                                                                                                                    if (var11 == null) {
                                                                                                                        return -3;
                                                                                                                    } else {
                                                                                                                        var3 = var11.intValue();
                                                                                                                        this.offset1SeekBar.setProgress(var3);
                                                                                                                        var7 = this.ReadUTFLine(var10, 2, 1);
                                                                                                                        if (var7 == null) {
                                                                                                                            return 0;
                                                                                                                        } else if (var7.isEmpty()) {
                                                                                                                            return -3;
                                                                                                                        } else {
                                                                                                                            var6.setIndex(0);
                                                                                                                            var11 = var5.parse(var7, var6);
                                                                                                                            if (var11 == null) {
                                                                                                                                return -3;
                                                                                                                            } else {
                                                                                                                                var3 = var11.intValue();
                                                                                                                                this.offset2SeekBar.setProgress(var3);
                                                                                                                                var7 = this.ReadUTFLine(var10, 2, 1);
                                                                                                                                if (var7 == null) {
                                                                                                                                    return 0;
                                                                                                                                } else if (var7.isEmpty()) {
                                                                                                                                    return -3;
                                                                                                                                } else {
                                                                                                                                    var6.setIndex(0);
                                                                                                                                    var11 = var5.parse(var7, var6);
                                                                                                                                    if (var11 == null) {
                                                                                                                                        return -3;
                                                                                                                                    } else {
                                                                                                                                        var3 = var11.intValue();
                                                                                                                                        this.trglevelSeekBar.setProgress(var3);
                                                                                                                                        if (!var2) {
                                                                                                                                            var7 = this.ReadUTFLine(var10, 2, 1);
                                                                                                                                            if (var7 == null) {
                                                                                                                                                return 0;
                                                                                                                                            }

                                                                                                                                            if (var7.isEmpty()) {
                                                                                                                                                return -3;
                                                                                                                                            }

                                                                                                                                            var6.setIndex(0);
                                                                                                                                            var11 = var5.parse(var7, var6);
                                                                                                                                            if (var11 == null) {
                                                                                                                                                return -3;
                                                                                                                                            }

                                                                                                                                            var3 = var11.intValue();
                                                                                                                                            this.xvalues = new float[var3];
                                                                                                                                            this.yvalues[0] = new float[var3];

                                                                                                                                            int var4;
                                                                                                                                            for (var3 = 0; var3 < this.xvalues.length; ++var3) {
                                                                                                                                                this.xvalues[var3] = (float) (-102.4D + 204.8D * (double) (var3 + 1) / (double) this.xvalues.length);
                                                                                                                                                var7 = this.ReadUTFLine(var10, 1, 0);
                                                                                                                                                if (var7 == null) {
                                                                                                                                                    return 0;
                                                                                                                                                }

                                                                                                                                                if (var7.isEmpty()) {
                                                                                                                                                    return -3;
                                                                                                                                                }

                                                                                                                                                var6.setIndex(0);
                                                                                                                                                var11 = var5.parse(var7, var6);
                                                                                                                                                if (var11 == null) {
                                                                                                                                                    return -3;
                                                                                                                                                }

                                                                                                                                                var4 = var11.intValue();
                                                                                                                                                this.yvalues[0][var3] = this.ChannelCodeToValue(0, var4);
                                                                                                                                            }

                                                                                                                                            var7 = this.ReadUTFLine(var10, 2, 1);
                                                                                                                                            if (var7 == null) {
                                                                                                                                                return 0;
                                                                                                                                            }

                                                                                                                                            if (var7.isEmpty()) {
                                                                                                                                                return -3;
                                                                                                                                            }

                                                                                                                                            var6.setIndex(0);
                                                                                                                                            var11 = var5.parse(var7, var6);
                                                                                                                                            if (var11 == null) {
                                                                                                                                                return -3;
                                                                                                                                            }

                                                                                                                                            var3 = var11.intValue();
                                                                                                                                            this.xvalues = new float[var3];
                                                                                                                                            this.yvalues[1] = new float[var3];

                                                                                                                                            for (var3 = 0; var3 < this.xvalues.length; ++var3) {
                                                                                                                                                this.xvalues[var3] = (float) (-102.4D + 204.8D * (double) (var3 + 1) / (double) this.xvalues.length);
                                                                                                                                                var7 = this.ReadUTFLine(var10, 1, 0);
                                                                                                                                                if (var7 == null) {
                                                                                                                                                    return 0;
                                                                                                                                                }

                                                                                                                                                if (var7.isEmpty()) {
                                                                                                                                                    return -3;
                                                                                                                                                }

                                                                                                                                                var6.setIndex(0);
                                                                                                                                                var11 = var5.parse(var7, var6);
                                                                                                                                                if (var11 == null) {
                                                                                                                                                    return -3;
                                                                                                                                                }

                                                                                                                                                var4 = var11.intValue();
                                                                                                                                                this.yvalues[1][var3] = this.ChannelCodeToValue(1, var4);
                                                                                                                                            }

                                                                                                                                            this.datawaiting = 3;
                                                                                                                                            this.StartMTimer(true);
                                                                                                                                        }

                                                                                                                                        return 1;
                                                                                                                                    }
                                                                                                                                }
                                                                                                                            }
                                                                                                                        }
                                                                                                                    }
                                                                                                                }
                                                                                                            }
                                                                                                        }
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private String PrefixFormat(float var1, int var2) {
        float var3 = Math.abs(var1);
        byte var4;
        if ((double) var3 < 1.0E-9D) {
            var4 = 0;
        } else if ((double) var3 < 1.0E-6D) {
            var4 = 1;
        } else if ((double) var3 < 0.001D) {
            var4 = 2;
        } else if (var3 < 1.0F) {
            var4 = 3;
        } else if ((double) var3 < 1000.0D) {
            var4 = 4;
        } else if ((double) var3 < 1000000.0D) {
            var4 = 5;
        } else {
            var4 = 6;
        }

        if (var2 <= 0) {
            return this.prefixMultipliers[var4] * var1 + " " + this.prefixStrings[var4];
        } else {
            NumberFormat var7 = NumberFormat.getInstance();
            String var6 = "@";

            for (int var5 = 1; var5 < var2; ++var5) {
                var6 = var6 + "#";
            }

            ((DecimalFormat) var7).applyPattern(var6);
            return var7.format((double) (this.prefixMultipliers[var4] * var1)) + " " + this.prefixStrings[var4];
        }
    }

    private String ReadUTFLine(RandomAccessFile var1, int var2, int var3) {
        String var7 = null;
        byte[] var8 = new byte[256];
        int var4 = 0;

        int var5;
        do {
            try {
                var5 = var1.read();
            } catch (IOException var10) {
                var10.printStackTrace();
                var5 = -1;
            }

            if (var5 != -1) {
                int var6 = var4 + 1;
                var8[var4] = (byte) var5;
                var4 = var6;
            }

            if (var4 > 0 && var8[var4 - 1] == 10) {
                break;
            }

            if (var4 >= 256) {
                Log.d("AOPA", "Слишком длинная строка в файле");
                break;
            }
        } while (var5 != -1);

        String var11 = var7;
        if (var4 > 0) {
            var11 = new String(var8, 0, var4);
        }

        var7 = var11;
        if (var11 != null) {
            var7 = var11;
            if (var2 > 0) {
                String[] var12 = var11.substring(1, var11.length() - 3).split("\",\"");
                if (var12.length >= var2) {
                    return var12[var3];
                }

                var7 = "";
            }
        }

        return var7;
    }

    private void SaveData(String param1) {
        // $FF: Couldn't be decompiled
    }

    private void ShowHelpLayout(boolean var1) {
        byte var3 = 0;
        byte var2;
        if (var1) {
            var2 = 8;
        } else {
            var2 = 0;
        }

        this.layoutWork.setVisibility(var2);
        LinearLayout var4 = this.layoutBack;
        int var5 = var3;
        if (var1) {
            var5 = R.drawable.helpwnd;
        }

        var4.setBackgroundResource(var5);
    }

    private void StartMTimer(boolean var1) {
        Log.d("AOPA", "StartMTimer " + var1 + " [runBtn.isChecked " + this.runBtn.isChecked() + "]");
        if (this.mHandler != null) {
            this.mHandler.removeCallbacks(this.updateTask);
            this.mHandler = null;
        }

        if (var1 != this.runBtn.isChecked()) {
            this.runBtn.setChecked(var1);
        }

        if (var1) {
            this.mHandler = new Handler();
            this.mHandler.postDelayed(this.updateTask, 0L);
        }

    }

    private int ValueToChannelCode(int var1, float var2) {
        return (int) (0.5D + 127.5D * (1.0D + (double) var2 / 100.0D));
    }

    private void WriteBOM(RandomAccessFile var1) {
        try {
            var1.write(new byte[]{-17, -69, -65});
        } catch (IOException var2) {
            var2.printStackTrace();
        }
    }

    private void doAutoControl(int var1, int var2, int var3, int var4, int var5, int var6) {
        boolean var12;
        if (this.ch1Enabled && this.ch2Enabled) {
            var12 = true;
        } else {
            var12 = false;
        }

        if (this.autoControl > 0) {
            double var7 = 1.0D;
            if (var12) {
                var7 = 0.5D;
            }

            int var11 = 0;
            if (this.pACKScopeDrv != null) {
                var11 = this.pACKScopeDrv.getTriggerSource();
            }

            if (var11 != 1) {
                var6 = var3;
            }

            double var9;
            if (var6 != 0) {
                var9 = (double) this.dataLenK * 1024.0D / (double) var6;
                if (var9 < 2.0D || var9 > 6.0D) {
                    this.CorrectSampleRate(0.25D * var9);
                }
            }

            if (this.ch1Enabled) {
                boolean var15;
                if ((var12 || var1 <= -99) && var2 >= 99) {
                    var15 = true;
                } else {
                    var15 = false;
                }

                var9 = -0.5D * (double) (var2 + var1);
                byte var16;
                if (var12) {
                    var16 = 50;
                } else {
                    var16 = 0;
                }

                this.CorrectOffset(0, var9, (double) var16, var11);
                if (var15 || Math.abs(var9) < 200.0D / 4.0D) {
                    var9 = 1.0D + 1.05D * (double) (var2 - var1);
                    if (var9 < var7 * 200.0D / 2.5D) {
                        this.CorrectRange(0, var7 * 200.0D / var9);
                    } else if (var9 > var7 * 200.0D) {
                        this.CorrectRange(0, -1.0D);
                    }
                }
            }

            if (this.ch2Enabled) {
                boolean var13;
                if (var4 > -99 || !var12 && var5 < 99) {
                    var13 = false;
                } else {
                    var13 = true;
                }

                var9 = -0.5D * (double) (var5 + var4);
                byte var14;
                if (var12) {
                    var14 = -50;
                } else {
                    var14 = 0;
                }

                this.CorrectOffset(1, var9, (double) var14, var11);
                if (var13 || Math.abs(var9) < 200.0D / 4.0D) {
                    var9 = 1.0D + 1.05D * (double) (var5 - var4);
                    if (var9 < var7 * 200.0D / 2.5D) {
                        this.CorrectRange(1, var7 * 200.0D / var9);
                    } else if (var9 > var7 * 200.0D) {
                        this.CorrectRange(1, -1.0D);
                    }
                }
            }

            --this.autoControl;
            if (this.autoControl == 0) {
                this.AutoControl(0);
            }
        }

    }

    double NextIntFall(float[] var1, int var2, int var3, double var4) {
        while (var2 < var3 - 1) {
            if ((double) var1[var2] > var4 && (double) var1[var2 + 1] <= var4) {
                return (double) var2 + (var4 - (double) var1[var2]) / (double) (var1[var2 + 1] - var1[var2]);
            }

            ++var2;
        }

        return (double) var3;
    }

    double NextIntRise(float[] var1, int var2, int var3, double var4) {
        while (var2 < var3 - 1) {
            if ((double) var1[var2] < var4 && (double) var1[var2 + 1] >= var4) {
                return (double) var2 + (var4 - (double) var1[var2]) / (double) (var1[var2 + 1] - var1[var2]);
            }

            ++var2;
        }

        return (double) var3;
    }

    protected void doMeas() {
        double var7 = 0.0D;
        double var11 = 0.0D;
        double var17 = 0.0D;
        double var15 = 0.0D;
        double var9 = 0.0D;
        double var13 = 0.0D;
        float[][] var26 = new float[200][];
        Log.d("AOPA", "doMeas start...");

        int var21;
        for (var21 = 0; var21 < 200; ++var21) {
            var26[var21] = new float[2];
            var26[var21][0] = 0f;
            var26[var21][1] = 0f;
        }

        var21 = this.graph1.getVectorLength();
        this.tvScale.setText("[T]: " + this.PrefixFormat(this.GraphTimeToValue(var21, (float) (0.1D * (double) var21)), 3) + "s/d; [1]: " + this.PrefixFormat(this.GraphCodeToValue(0, 25.0F), 3) + "V/d; [2]: " + this.PrefixFormat(this.GraphCodeToValue(1, 25.0F), 3) + "V/d");
        double var1;
        double var3;
        double var5;
        int var10002;
        double var19;
        int var22;
        int var23;
        int var24;
        int var25;
        float[] var27;
        if (this.ch1Enabled && this.yvalues[0].length > 0) {
            var5 = (double) this.yvalues[0][0];
            var3 = var5;

            for (var21 = 0; var21 < this.yvalues[0].length; var5 = var7) {
                var1 = var3;
                if (var3 < (double) this.yvalues[0][var21]) {
                    var1 = (double) this.yvalues[0][var21];
                }

                var7 = var5;
                if (var5 > (double) this.yvalues[0][var21]) {
                    var7 = (double) this.yvalues[0][var21];
                }

                var22 = ACKScopeDrv.limited(0, (int) ((double) this.yvalues[0][var21] + 100.5D), 199);
                var26[var22][1] = var26[var22][0] * var26[var22][1] + this.yvalues[0][var21];
                var27 = var26[var22];
                var27[0]++;
                var27 = var26[var22];
                var27[1] /= var26[var22][0];
                ++var21;
                var3 = var1;
            }

            var25 = ACKScopeDrv.limited(1, (int) (100.5D + (var3 + var5) * 0.5D), 198);
            var22 = 0;
            var24 = 0;

            for (var21 = 0; var21 < var25 + 1; var22 = var23) {
                var23 = var22;
                if ((float) var22 < var26[var21][0]) {
                    var23 = (int) var26[var21][0];
                    var24 = var21;
                }

                ++var21;
            }

            var1 = (double) this.GraphCodeToValue(0, var26[var24][1]);
            var22 = var25 - 1;
            var24 = 0;

            for (var21 = var25 - 1; var21 < 200; var22 = var23) {
                var23 = var22;
                if ((float) var22 < var26[var21][0]) {
                    var23 = (int) var26[var21][0];
                    var24 = var21;
                }

                ++var21;
            }

            var7 = (double) this.GraphCodeToValue(0, var26[var24][1]);
            this.amps1Txt.setText("a1(sine): " + this.PrefixFormat((float) (0.5D * (double) (this.GraphCodeToValue(0, (float) var3) - this.GraphCodeToValue(0, (float) var5))), 4) + "V");
            this.ampp1Txt.setText("a1(puls): " + this.PrefixFormat((float) (var7 - var1), 4) + "V");
            var1 = 0.0D;
            var11 = (var3 + var5) * 0.5D;
            var22 = this.yvalues[0].length;
            if (var3 - var5 < 10.0D) {
                var1 = (double) var22;
            } else {
                var19 = var11 - 0.5D * (var11 - var5);
                var7 = this.NextIntRise(this.yvalues[0], 0, var22, var19);
                var9 = this.NextIntRise(this.yvalues[0], (int) var7, var22, var11);
                var7 = var9;

                for (var21 = 0; var7 < (double) (var22 - 1); ++var21) {
                    var7 = this.NextIntRise(this.yvalues[0], (int) var7, var22, var11 + 0.5D * (var3 - var11));
                    if (var7 > (double) (var22 - 1)) {
                        break;
                    }

                    var7 = this.NextIntFall(this.yvalues[0], (int) var7, var22, var19);
                    if (var7 > (double) (var22 - 1)) {
                        break;
                    }

                    var7 = this.NextIntRise(this.yvalues[0], (int) var7, var22, var11);
                    if (var7 > (double) (var22 - 1)) {
                        break;
                    }

                    var1 = var7;
                }

                if (var21 != 0) {
                    var1 = (var1 - var9) / (double) var21;
                } else {
                    var1 = (double) var22;
                }
            }

            var9 = var1;
            this.freq1Txt.setText("f1: " + this.PrefixFormat((float) (1.0D / (double) this.GraphTimeToValue(var22, (float) var1)), 4) + "Hz");
            var11 = var5;
            var7 = var3;
        } else {
            this.freq1Txt.setText("f1: --- Hz");
            this.amps1Txt.setText("a1(sine): --- V");
            this.ampp1Txt.setText("a1(puls): --- V");
        }

        for (var21 = 0; var21 < 200; ++var21) {
            var26[var21][0] = 0f;
            var26[var21][1] = 0f;
        }

        if (this.ch2Enabled && this.yvalues[1].length > 0) {
            var5 = (double) this.yvalues[1][0];
            var3 = var5;

            for (var21 = 0; var21 < this.yvalues[1].length; var5 = var13) {
                var1 = var3;
                if (var3 < (double) this.yvalues[1][var21]) {
                    var1 = (double) this.yvalues[1][var21];
                }

                var13 = var5;
                if (var5 > (double) this.yvalues[1][var21]) {
                    var13 = (double) this.yvalues[1][var21];
                }

                var22 = ACKScopeDrv.limited(0, (int) ((double) this.yvalues[1][var21] + 100.5D), 199);
                var26[var22][1] = var26[var22][0] * var26[var22][1] + this.yvalues[1][var21];
                var27 = var26[var22];
                var27[0]++;
                var27 = var26[var22];
                var27[1] /= var26[var22][0];
                ++var21;
                var3 = var1;
            }

            var25 = ACKScopeDrv.limited(1, (int) (100.5D + (var3 + var5) * 0.5D), 198);
            var22 = 0;
            var24 = 0;

            for (var21 = 0; var21 < var25 + 1; var22 = var23) {
                var23 = var22;
                if ((float) var22 < var26[var21][0]) {
                    var23 = (int) var26[var21][0];
                    var24 = var21;
                }

                ++var21;
            }

            var1 = (double) this.GraphCodeToValue(1, var26[var24][1]);
            var22 = var25 - 1;
            var24 = 0;

            for (var21 = var25 - 1; var21 < 200; var22 = var23) {
                var23 = var22;
                if ((float) var22 < var26[var21][0]) {
                    var23 = (int) var26[var21][0];
                    var24 = var21;
                }

                ++var21;
            }

            var13 = (double) this.GraphCodeToValue(1, var26[var24][1]);
            this.amps2Txt.setText("a2(sine): " + this.PrefixFormat((float) (0.5D * (double) (this.GraphCodeToValue(1, (float) var3) - this.GraphCodeToValue(1, (float) var5))), 4) + "V");
            this.ampp2Txt.setText("a2(puls): " + this.PrefixFormat((float) (var13 - var1), 4) + "V");
            var1 = 0.0D;
            var17 = (var3 + var5) * 0.5D;
            var22 = this.yvalues[1].length;
            if (var3 - var5 < 10.0D) {
                var1 = (double) var22;
            } else {
                var19 = var17 - 0.5D * (var17 - var5);
                var13 = this.NextIntRise(this.yvalues[1], 0, var22, var19);
                var15 = this.NextIntRise(this.yvalues[1], (int) var13, var22, var17);
                var13 = var15;

                for (var21 = 0; var13 < (double) (var22 - 1); ++var21) {
                    var13 = this.NextIntRise(this.yvalues[1], (int) var13, var22, var17 + 0.5D * (var3 - var17));
                    if (var13 > (double) (var22 - 1)) {
                        break;
                    }

                    var13 = this.NextIntFall(this.yvalues[1], (int) var13, var22, var19);
                    if (var13 > (double) (var22 - 1)) {
                        break;
                    }

                    var13 = this.NextIntRise(this.yvalues[1], (int) var13, var22, var17);
                    if (var13 > (double) (var22 - 1)) {
                        break;
                    }

                    var1 = var13;
                }

                if (var21 != 0) {
                    var1 = (var1 - var15) / (double) var21;
                } else {
                    var1 = (double) var22;
                }
            }

            var13 = var1;
            this.freq2Txt.setText("f2: " + this.PrefixFormat((float) (1.0D / (double) this.GraphTimeToValue(var22, (float) var1)), 4) + "Hz");
        } else {
            this.freq2Txt.setText("f2: --- Hz");
            this.amps2Txt.setText("a2(sine): --- V");
            this.ampp2Txt.setText("a2(puls): --- V");
            var3 = var17;
            var5 = var15;
        }

        if (this.autoControl > 0) {
            this.doAutoControl((int) var11, (int) var7, (int) var9, (int) var5, (int) var3, (int) var13);
        }

        Log.d("AOPA", "doMeas exit");
    }

    public void onANConnect(AULNetConnection aulNetConnection) {
        this.searchPrg.setVisibility(View.INVISIBLE);
        String headerName;
        if (aulNetConnection.isDemoMode()) {
            headerName = "AKTAKOM Oscilloscope Pro";
            this.txtDevName = this.getResources().getString(R.string.demomode);
            headerName = headerName + "\n" + this.txtDevName;
            this.appTitle.setText(headerName);
            this.StartMTimer(false);
        } else {
            String var2 = this.getResources().getString(R.string.app_longname);
            this.txtDevName = aulNetConnection.devname;
            headerName = var2 + "\n" + this.txtDevName;
            this.appTitle.setText(headerName);
            if (this.pACKScopeDrv != null) {
                if (this.pACKScopeDrv.ReadMemorySize() < 100000) {
                    ArrayList var4 = new ArrayList();
                    var4.add("1K p");
                    var4.add("10K p");
                    var4.add("50K p");
                    ArrayAdapter var5 = new ArrayAdapter(this, android.R.layout.simple_spinner_item, var4);
                    var5.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    this.dataLenSpinner.setAdapter(var5);
                    var5.notifyDataSetChanged();
                }

                if (this.LoadData("/AOPA/default.csv", true) != 1) {
                    this.DefaultScopeSettings();
                    this.AutoControl(15);
                }

                this.datawaiting = 0;
            }

            this.StartMTimer(true);
            Log.d("AOPA", "onANConnect exit");
        }
    }

    public void onCheckedChanged(CompoundButton var1, boolean var2) {
        if (var1 == this.runBtn) {
            this.StartMTimer(var2);
        }

    }

    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.btnOur:
                this.pACKScopeDrv.pAULNetConnection.InitConnection();
                return;

            case R.id.layoutBack:
                this.ShowHelpLayout(false);
                return;
            case R.id.imageLogo:
                this.startActivity(new Intent("android.intent.action.VIEW", Uri.parse(this.getResources().getString(R.string.aktakom_uri))));
                return;
            case R.id.btnClose:
                this.finish();
                return;
            case R.id.btnSave:
                this.SaveData("/AOPA/" + (new SimpleDateFormat("yyyyMMddHHmmss")).format(new Date()) + ".csv");
                return;
            case R.id.btnLoad:
                this.GetRFileName();
                return;
            case R.id.autosetBtn:
                this.AutoControl(-1);
                return;
            case R.id.btnHelp:
                this.HelpDialog();
                return;
            case R.id.ctrlShowBtn:
                boolean isNeedShow;
                isNeedShow = this.ctrlLayout.getVisibility() == View.VISIBLE;

                byte visible;
                if (isNeedShow) {
                    visible = 8;
                } else {
                    visible = 0;
                }

                this.ctrlLayout.setVisibility(visible);
                this.trglevelLayout.setVisibility(visible);
                this.ofs1Layout.setVisibility(visible);
                this.ofs2Layout.setVisibility(visible);
                Button ctrlShowBtn = this.ctrlShowBtn;
                int left;
                int right;
                if (isNeedShow) {
                    left = android.R.drawable.arrow_down_float;
                    right = android.R.drawable.arrow_down_float;
                } else {
                    left = android.R.drawable.arrow_up_float;
                    right = android.R.drawable.arrow_up_float;
                }

                ctrlShowBtn.setCompoundDrawablesWithIntrinsicBounds(left, 0, right, 0);
                return;
            default:
        }
    }

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.setContentView(R.layout.main);


        this.btnOur = (ImageButton) this.findViewById(R.id.btnOur);
        this.btnOur.setOnClickListener(this);


        this.layoutBack = (LinearLayout) this.findViewById(R.id.layoutBack);
        this.layoutBack.setOnClickListener(this);
        this.layoutWork = (LinearLayout) this.findViewById(R.id.layoutWork);
        this.trglevelLayout = (LinearLayout) this.findViewById(R.id.trglevelLayout);
        this.ofs1Layout = (LinearLayout) this.findViewById(R.id.ofs1Layout);
        this.ofs2Layout = (LinearLayout) this.findViewById(R.id.ofs2Layout);
        this.imageLogo = (ImageView) this.findViewById(R.id.imageLogo);
        this.imageLogo.setOnClickListener(this);
        this.appTitle = (TextView) this.findViewById(R.id.appTitle);
        this.freq1Txt = (TextView) this.findViewById(R.id.freq1Txt);
        this.freq2Txt = (TextView) this.findViewById(R.id.freq2Txt);
        this.amps1Txt = (TextView) this.findViewById(R.id.amps1Txt);
        this.amps2Txt = (TextView) this.findViewById(R.id.amps2Txt);
        this.ampp1Txt = (TextView) this.findViewById(R.id.ampp1Txt);
        this.ampp2Txt = (TextView) this.findViewById(R.id.ampp2Txt);
        this.tvScale = (TextView) this.findViewById(R.id.tvScale);
        this.btnClose = (ImageButton) this.findViewById(R.id.btnClose);
        this.btnClose.setOnClickListener(this);
        this.autosetBtn = (ImageButton) this.findViewById(R.id.autosetBtn);
        this.autosetBtn.setOnClickListener(this);
        this.findViewById(R.id.btnHelp).setOnClickListener(this);
        this.range1Spinner = (Spinner) this.findViewById(R.id.range1Spinner);
        this.range1Spinner.setOnItemSelectedListener(this);
        this.range2Spinner = (Spinner) this.findViewById(R.id.range2Spinner);
        this.range2Spinner.setOnItemSelectedListener(this);
        this.tbSpinner = (Spinner) this.findViewById(R.id.tbSpinner);
        this.tbSpinner.setOnItemSelectedListener(this);
        this.dataLenSpinner = (Spinner) this.findViewById(R.id.dataLenSpinner);
        this.dataLenSpinner.setOnItemSelectedListener(this);
        this.runmodeSpinner = (Spinner) this.findViewById(R.id.runmodeSpinner);
        this.runmodeSpinner.setOnItemSelectedListener(this);
        this.trgsrcSpinner = (Spinner) this.findViewById(R.id.trgsrcSpinner);
        this.trgsrcSpinner.setOnItemSelectedListener(this);
        this.trglogSpinner = (Spinner) this.findViewById(R.id.trglogSpinner);
        this.trglogSpinner.setOnItemSelectedListener(this);
        this.generatorSpinner = (Spinner) this.findViewById(R.id.generatorSpinner);
        this.generatorSpinner.setOnItemSelectedListener(this);
        this.cpl1Spinner = (Spinner) this.findViewById(R.id.cpl1Spinner);
        this.cpl1Spinner.setOnItemSelectedListener(this);
        this.cpl2Spinner = (Spinner) this.findViewById(R.id.cpl2Spinner);
        this.cpl2Spinner.setOnItemSelectedListener(this);
        this.probe1Spinner = (Spinner) this.findViewById(R.id.probe1Spinner);
        this.probe1Spinner.setOnItemSelectedListener(this);
        this.probe2Spinner = (Spinner) this.findViewById(R.id.probe2Spinner);
        this.probe2Spinner.setOnItemSelectedListener(this);
        Display display = this.getWindowManager().getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getMetrics(displayMetrics);
        this.fontSize = (float) (0.001D * (double) displayMetrics.heightPixels);
        this.offset1SeekBar = (VerticalSeekBar) this.findViewById(R.id.offset1SeekBar);
        this.offset1SeekBar.setOnSeekBarChangeListener(this);
        this.offset2SeekBar = (VerticalSeekBar) this.findViewById(R.id.offset2SeekBar);
        this.offset2SeekBar.setOnSeekBarChangeListener(this);
        this.trglevelSeekBar = (VerticalSeekBar) this.findViewById(R.id.trglevelSeekBar);
        this.trglevelSeekBar.setOnSeekBarChangeListener(this);
        this.pretrgSeekBar = (SeekBar) this.findViewById(R.id.pretrgSeekBar);
        this.pretrgSeekBar.setOnSeekBarChangeListener(this);
        this.offset1SeekBar.setMax(4095);
        this.offset2SeekBar.setMax(4095);
        this.trglevelSeekBar.setMax(4095);
        this.pretrgSeekBar.setMax(1023);
        this.graphLayout = (LinearLayout) this.findViewById(R.id.graphLayout);
        this.runBtn = (ToggleButton) this.findViewById(R.id.runBtn);
        this.runBtn.setOnCheckedChangeListener(this);
        this.searchPrg = (ProgressBar) this.findViewById(R.id.searchPrg);
        this.ctrlLayout = (LinearLayout) this.findViewById(R.id.ctrlLayout);
        this.ctrlShowBtn = (Button) this.findViewById(R.id.ctrlShowBtn);
        this.ctrlShowBtn.setOnClickListener(this);
        this.btnSave = (ImageButton) this.findViewById(R.id.btnSave);
        this.btnSave.setOnClickListener(this);
        this.btnLoad = (ImageButton) this.findViewById(R.id.btnLoad);
        this.btnLoad.setOnClickListener(this);
        this.prefixStrings = this.getResources().getStringArray(R.array.prefixList);
        SharedPreferences sharedPreferences = this.getSharedPreferences("AOPAPrefs", 0);
        serverPort = sharedPreferences.getInt("serverPort", serverPort);
        serverIP = sharedPreferences.getString("serverIP", serverIP);
        tcpOn = sharedPreferences.getBoolean("tcpOn", tcpOn);
        LayoutParams layoutParams = new LayoutParams(-1, -2, 0.5F);
        this.xvalues = new float[this.dataLenK * 1024];
        this.yvalues = new float[2][];

        int channel;
        for (channel = 0; channel < 2; ++channel) {
            this.yvalues[channel] = new float[this.dataLenK * 1024];
        }

        for (channel = 0; channel < this.xvalues.length; ++channel) {
            this.xvalues[channel] = (float) (0.001D * (double) (channel + 1 - this.xvalues.length));
        }

        this.graph1 = new Plot2d(this, 2, 101, 0);
        this.graph1.backColor = 549503231;
        this.graph1.axisColor = -12566464;
        this.graph1.setStrokeWidth(2.0F * this.fontSize);
        this.graph1.setXAxis(-100.0F, 100.0F, 0.0F, 20.0F, false);
        this.graph1.setYAxis(-100.0F, 100.0F, 0.0F, 25.0F, false);
        this.graph1.setXAutorange(200.0F, true);
        this.graph1.setYAutorange(200.0F, true);

        for (channel = 0; channel < 2; ++channel) {
            this.graph1.plots[channel].color = this.pltColors[channel];
            Plot plot = this.graph1.plots[channel];
            plot.visible = true;

            for (int numOfDots = 0; numOfDots < this.graph1.plots[channel].xvalues.length; ++numOfDots) {
                this.graph1.plots[channel].xvalues[numOfDots] = (float) (-100.0D + 200.0D * (double) numOfDots / (double) this.graph1.plots[channel].xvalues.length);
                if (channel == 0) {
                    this.graph1.plots[channel].yvalues[numOfDots] = (float) (50.0D + 20.0D * Math.sin(0.08D * (double) this.graph1.plots[channel].xvalues[numOfDots]));
                } else {
                    float[] yvalues = this.graph1.plots[channel].yvalues;
                    byte kakoitobred;
                    if ((numOfDots / 23 & 1) == 0) {
                        kakoitobred = -20;
                    } else {
                        kakoitobred = -80;
                    }

                    yvalues[numOfDots] = (float) kakoitobred;
                }
            }
        }

        this.graph1.xmarks[0].visible = true;
        this.graph1.xmarks[0].label = "T";
        channel = this.graphLayout.getChildCount();
        this.graphLayout.addView(this.graph1, channel - 2, layoutParams);
        if (this.keepscreen) {
            this.getWindow().addFlags(128);
        } else {
            this.getWindow().clearFlags(128);
        }

        this.InitConnection();
    }

    public void onDataReady(ACKScopeDrv ackScopeDrv) {
        if (this.runBtn.isChecked()) {
            int dataLength = this.pACKScopeDrv.getData1().length;
            if (dataLength > 0) {
                this.noVoidData = true;
            }

            this.xvalues = new float[dataLength];
            this.yvalues[0] = new float[dataLength];

            for (dataLength = 0; dataLength < this.xvalues.length; ++dataLength) {
                this.xvalues[dataLength] = (float) ((double) (dataLength + 1) * 204.8D / (double) this.xvalues.length - 102.4D);
                this.yvalues[0][dataLength] = this.ChannelCodeToValue(0, this.pACKScopeDrv.getData1()[dataLength] & 255);
            }

            dataLength = this.pACKScopeDrv.getData2().length;
            if (dataLength > 0) {
                this.noVoidData = true;
            }

            this.xvalues = new float[dataLength];
            this.yvalues[1] = new float[dataLength];

            for (dataLength = 0; dataLength < this.xvalues.length; ++dataLength) {
                this.xvalues[dataLength] = (float) ((double) (dataLength + 1) * 204.8D / (double) this.xvalues.length - 102.4D);
                this.yvalues[1][dataLength] = this.ChannelCodeToValue(1, this.pACKScopeDrv.getData2()[dataLength] & 255);
            }
        }

        this.datawaiting = 2;
    }

    protected void onDestroy() {
        super.onDestroy();
        this.pACKScopeDrv.pAULNetConnection.CloseDevice();
        this.StartMTimer(false);
        if (this.noVoidData) {
            this.SaveData("/AOPA/default.csv");
        }

    }

    public void onItemSelected(AdapterView<?> var1, View var2, int var3, long var4) {
        boolean var7 = false;
        boolean var8 = true;
        if (var1 == this.range1Spinner) {
            if (this.pACKScopeDrv != null) {
                this.pACKScopeDrv.setRange(var3, 0);
            }
        } else if (var1 == this.range2Spinner) {
            if (this.pACKScopeDrv != null) {
                this.pACKScopeDrv.setRange(var3, 1);
                return;
            }
        } else if (var1 == this.tbSpinner) {
            if (this.pACKScopeDrv != null) {
                this.pACKScopeDrv.setSampleRate(this.tbSpinner.getCount() - 1 - var3);
                return;
            }
        } else if (var1 == this.dataLenSpinner) {
            switch (var3) {
                case 0:
                    this.dataLenK = 1;
                    break;
                case 1:
                    this.dataLenK = 10;
                    break;
                case 2:
                    this.dataLenK = 100;
            }

            if (this.pACKScopeDrv != null && this.dataLenK * 1024 > this.pACKScopeDrv.getMemorySize()) {
                this.dataLenK = 50;
            }

            var3 = (int) (0.5D + (double) (this.dataLenK * 1024) * this.pretrgRate);
            if (this.pACKScopeDrv != null) {
                this.pACKScopeDrv.setTrgDelay(var3);
                this.pACKScopeDrv.setPostTrgLength(this.dataLenK * 1024 - var3);
                return;
            }
        } else if (var1 == this.runmodeSpinner) {
            if (this.pACKScopeDrv != null) {
                this.pACKScopeDrv.setTriggerMode(var3);
            }

            if (this.mHandler == null) {
                this.StartMTimer(this.runBtn.isChecked());
                return;
            }
        } else if (var1 == this.trgsrcSpinner) {
            if (this.pACKScopeDrv != null) {
                this.pACKScopeDrv.setTriggerSource(var3);
                return;
            }
        } else if (var1 == this.trglogSpinner) {
            if (this.pACKScopeDrv != null) {
                this.pACKScopeDrv.setTriggerLogic(var3);
                return;
            }
        } else if (var1 == this.generatorSpinner) {
            if (this.pACKScopeDrv != null) {
                this.pACKScopeDrv.setGenerator(var3);
                return;
            }
        } else if (var1 == this.cpl1Spinner) {
            if (var3 < 4) {
                var7 = var8;
            } else {
                var7 = false;
            }

            this.ch1Enabled = var7;
            if (this.ch1Enabled && this.pACKScopeDrv != null) {
                this.pACKScopeDrv.setCoupling(var3, 0);
                return;
            }
        } else if (var1 == this.cpl2Spinner) {
            if (var3 < 4) {
                var7 = true;
            }

            this.ch2Enabled = var7;
            if (this.ch2Enabled && this.pACKScopeDrv != null) {
                this.pACKScopeDrv.setCoupling(var3, 1);
                return;
            }
        } else {
            byte var6;
            if (var1 == this.probe1Spinner) {
                var6 = 1;
                if (var3 == 1) {
                    var6 = 10;
                } else if (var3 == 2) {
                    var6 = 100;
                }

                if (this.pACKScopeDrv != null) {
                    this.pACKScopeDrv.setProbe(var6, 0);
                    return;
                }
            } else if (var1 == this.probe2Spinner) {
                var6 = 1;
                if (var3 == 1) {
                    var6 = 10;
                } else if (var3 == 2) {
                    var6 = 100;
                }

                if (this.pACKScopeDrv != null) {
                    this.pACKScopeDrv.setProbe(var6, 1);
                    return;
                }
            }
        }

    }

    public void onNothingSelected(AdapterView<?> var1) {
    }

    public void onPlotDoubleTap(Plot2d var1) {
        this.pretrgRate = 0.5D;
        this.pretrgSeekBar.setProgress((int) (this.pretrgRate * (double) this.pretrgSeekBar.getMax()));
        int var2 = (int) ((double) (this.dataLenK * 1024) * this.pretrgRate + 0.5D);
        if (this.pACKScopeDrv != null) {
            this.pACKScopeDrv.setTrgDelay(var2);
            this.pACKScopeDrv.setPostTrgLength(this.dataLenK * 1024 - var2);
        }

    }

    public void onProgressChanged(SeekBar var1, int var2, boolean var3) {
        if (var1 == this.offset1SeekBar) {
            if (this.pACKScopeDrv != null) {
                this.pACKScopeDrv.setOffset(var2, 0);
            }
        } else if (var1 == this.offset2SeekBar) {
            if (this.pACKScopeDrv != null) {
                this.pACKScopeDrv.setOffset(var2, 1);
                return;
            }
        } else if (var1 == this.trglevelSeekBar) {
            if (this.pACKScopeDrv != null) {
                this.pACKScopeDrv.setTriggerLevel(var2, 0);
                this.pACKScopeDrv.setTriggerLevel(var2, 1);
                return;
            }
        } else if (var1 == this.pretrgSeekBar) {
            this.pretrgRate = (double) var2 / (double) this.pretrgSeekBar.getMax();
            var2 = (int) (0.5D + (double) (this.dataLenK * 1024) * this.pretrgRate);
            if (this.pACKScopeDrv != null) {
                this.pACKScopeDrv.setTrgDelay(var2);
                this.pACKScopeDrv.setPostTrgLength(this.dataLenK * 1024 - var2);
                return;
            }
        }

    }

    public void onRegStatusChange(ACKScopeDrv var1) {
    }

    public void onStartTrackingTouch(SeekBar var1) {
    }

    protected void onStop() {
        super.onStop();
        Editor var1 = this.getSharedPreferences("AOPAPrefs", 0).edit();
        var1.putInt("serverPort", serverPort);
        var1.putString("serverIP", serverIP);
        var1.putBoolean("tcpOn", tcpOn);
        var1.commit();
    }

    public void onStopTrackingTouch(SeekBar var1) {
    }

    public class MyAdapter extends ArrayAdapter<String> {
        public MyAdapter(Context var2, int var3, String[] var4) {
            super(var2, var3, var4);
        }

        public View getDropDownView(int var1, View var2, ViewGroup var3) {
            return super.getDropDownView(var1, var2, var3);
        }

        public View getView(int var1, View var2, ViewGroup var3) {
            TextView var4 = (TextView) super.getView(var1, var2, var3);
            var4.setTextSize(5.0F * AAOPActivity.this.fontSize);
            return var4;
        }
    }
}
