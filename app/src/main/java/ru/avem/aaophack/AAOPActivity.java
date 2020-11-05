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
import static ru.avem.aaophack.Utils.limited;

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
    private Plot2d plot2d;
    private boolean isKeepScreen = true;
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
    private ProgressBar searchProgressBar;
    private Spinner tbSpinner;
    private LinearLayout trglevelLayout;
    private VerticalSeekBar trglevelSeekBar;
    private Spinner trglogSpinner;
    private Spinner trgsrcSpinner;
    private TextView tvScale;

    private Runnable updateTask = new Runnable() {
        public void run() {
            switch (datawaiting) {
                case 0:
                    datawaiting = 1;
                    pACKScopeDrv.readWaveform();
                case 1:
                default:
                    break;
                case 2:
                case 3:
                case 4:
                    plot2d.plots[0].visible = ch1Enabled;
                    int vectorOffset = dataLenK * 12;
                    if (ch1Enabled) {
                        plot2d.setData(0, xvalues, yvalues[0], vectorOffset, xvalues.length - vectorOffset * 2);
                    }

                    plot2d.plots[1].visible = ch2Enabled;
                    if (ch2Enabled) {
                        plot2d.setData(1, xvalues, yvalues[1], vectorOffset, xvalues.length - vectorOffset * 2);
                    }

                    plot2d.xmarks[0].x = (float) (-100.0D + 200.0D * pretrgRate);
                    doMeas();
                    if (datawaiting != 3 && pACKScopeDrv.getTriggerMode() != 2) {
                        datawaiting = 1;
                        pACKScopeDrv.readWaveform();
                    } else {
                        datawaiting = 0;
                        startMTimer(false);
                    }
            }

            if (mHandler != null) {
                mHandler.postDelayed(updateTask, 500L);
            }

        }
    };
    private float[] xvalues = new float[dataLenK * 1024];
    private float[][] yvalues = new float[2][];

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.main);

        LinearLayout graphLayout = viewInitialization();

        prefixStrings = getResources().getStringArray(R.array.prefixList);

        SharedPreferences sharedPreferences = getSharedPreferences("AOPAPrefs", 0);
        serverPort = sharedPreferences.getInt("serverPort", serverPort);
        serverIP = sharedPreferences.getString("serverIP", serverIP);
        tcpOn = sharedPreferences.getBoolean("tcpOn", tcpOn);

        initPlot(graphLayout);

        if (isKeepScreen) {
            getWindow().addFlags(128);
        } else {
            getWindow().clearFlags(128);
        }

        initConnection();
    }

    private void initPlot(LinearLayout graphLayout) {
        int channel;
        for (channel = 0; channel < 2; ++channel) {
            yvalues[channel] = new float[dataLenK * 1024];
        }

        for (channel = 0; channel < xvalues.length; ++channel) {
            xvalues[channel] = (float) (0.001D * (double) (channel + 1 - xvalues.length));
        }

        plot2d = new Plot2d(this, 2, 101, 0);
        plot2d.backColor = 549503231;
        plot2d.axisColor = -12566464;
        plot2d.setStrokeWidth(2.0F * fontSize);
        plot2d.setXAxis(-100.0F, 100.0F, 0.0F, 20.0F, false);
        plot2d.setYAxis(-100.0F, 100.0F, 0.0F, 25.0F, false);
        plot2d.setXAutorange(200.0F, true);
        plot2d.setYAutorange(200.0F, true);

        for (channel = 0; channel < 2; ++channel) {
            plot2d.plots[channel].color = pltColors[channel];
            Plot plot = plot2d.plots[channel];
            plot.visible = true;

            for (int numOfDots = 0; numOfDots < plot2d.plots[channel].xvalues.length; ++numOfDots) {
                plot2d.plots[channel].xvalues[numOfDots] = (float) (-100.0D + 200.0D * (double) numOfDots / (double) plot2d.plots[channel].xvalues.length);
                if (channel == 0) {
                    plot2d.plots[channel].yvalues[numOfDots] = (float) (50.0D + 20.0D * Math.sin(0.08D * (double) plot2d.plots[channel].xvalues[numOfDots]));
                } else {
                    float[] yvalues = plot2d.plots[channel].yvalues;
                    byte kakoitobred;
                    if ((numOfDots / 23 & 1) == 0) {
                        kakoitobred = -20;
                    } else {
                        kakoitobred = -80;
                    }

                    yvalues[numOfDots] = kakoitobred;
                }
            }
        }

        plot2d.xmarks[0].visible = true;
        plot2d.xmarks[0].label = "T";
        channel = graphLayout.getChildCount();
        graphLayout.addView(plot2d, channel - 2, new LayoutParams(-1, -2, 0.5F));
    }

    private LinearLayout viewInitialization() {
        ImageButton btnOur = findViewById(R.id.btnOur);
        btnOur.setOnClickListener(this);

        layoutBack = findViewById(R.id.layoutBack);
        layoutBack.setOnClickListener(this);
        layoutWork = findViewById(R.id.layoutWork);
        trglevelLayout = findViewById(R.id.trglevelLayout);
        ofs1Layout = findViewById(R.id.ofs1Layout);
        ofs2Layout = findViewById(R.id.ofs2Layout);
        ImageView imageLogo = findViewById(R.id.imageLogo);
        imageLogo.setOnClickListener(this);
        appTitle = findViewById(R.id.appTitle);
        freq1Txt = findViewById(R.id.freq1Txt);
        freq2Txt = findViewById(R.id.freq2Txt);
        amps1Txt = findViewById(R.id.amps1Txt);
        amps2Txt = findViewById(R.id.amps2Txt);
        ampp1Txt = findViewById(R.id.ampp1Txt);
        ampp2Txt = findViewById(R.id.ampp2Txt);
        tvScale = findViewById(R.id.tvScale);
        ImageButton btnClose = findViewById(R.id.btnClose);
        btnClose.setOnClickListener(this);
        autosetBtn = findViewById(R.id.autosetBtn);
        autosetBtn.setOnClickListener(this);
        findViewById(R.id.btnHelp).setOnClickListener(this);
        range1Spinner = findViewById(R.id.range1Spinner);
        range1Spinner.setOnItemSelectedListener(this);
        range2Spinner = findViewById(R.id.range2Spinner);
        range2Spinner.setOnItemSelectedListener(this);
        tbSpinner = findViewById(R.id.tbSpinner);
        tbSpinner.setOnItemSelectedListener(this);
        dataLenSpinner = findViewById(R.id.dataLenSpinner);
        dataLenSpinner.setOnItemSelectedListener(this);
        runmodeSpinner = findViewById(R.id.runmodeSpinner);
        runmodeSpinner.setOnItemSelectedListener(this);
        trgsrcSpinner = findViewById(R.id.trgsrcSpinner);
        trgsrcSpinner.setOnItemSelectedListener(this);
        trglogSpinner = findViewById(R.id.trglogSpinner);
        trglogSpinner.setOnItemSelectedListener(this);
        generatorSpinner = findViewById(R.id.generatorSpinner);
        generatorSpinner.setOnItemSelectedListener(this);
        cpl1Spinner = findViewById(R.id.cpl1Spinner);
        cpl1Spinner.setOnItemSelectedListener(this);
        cpl2Spinner = findViewById(R.id.cpl2Spinner);
        cpl2Spinner.setOnItemSelectedListener(this);
        probe1Spinner = findViewById(R.id.probe1Spinner);
        probe1Spinner.setOnItemSelectedListener(this);
        probe2Spinner = findViewById(R.id.probe2Spinner);
        probe2Spinner.setOnItemSelectedListener(this);
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getMetrics(displayMetrics);
        fontSize = (float) (0.001D * (double) displayMetrics.heightPixels);
        offset1SeekBar = findViewById(R.id.offset1SeekBar);
        offset1SeekBar.setOnSeekBarChangeListener(this);
        offset2SeekBar = findViewById(R.id.offset2SeekBar);
        offset2SeekBar.setOnSeekBarChangeListener(this);
        trglevelSeekBar = findViewById(R.id.trglevelSeekBar);
        trglevelSeekBar.setOnSeekBarChangeListener(this);
        pretrgSeekBar = findViewById(R.id.pretrgSeekBar);
        pretrgSeekBar.setOnSeekBarChangeListener(this);
        offset1SeekBar.setMax(4095);
        offset2SeekBar.setMax(4095);
        trglevelSeekBar.setMax(4095);
        pretrgSeekBar.setMax(1023);
        LinearLayout graphLayout = findViewById(R.id.graphLayout);
        runBtn = findViewById(R.id.runBtn);
        runBtn.setOnCheckedChangeListener(this);
        searchProgressBar = findViewById(R.id.searchPrg);
        ctrlLayout = findViewById(R.id.ctrlLayout);
        ctrlShowBtn = findViewById(R.id.ctrlShowBtn);
        ctrlShowBtn.setOnClickListener(this);
        ImageButton btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(this);
        ImageButton btnLoad = findViewById(R.id.btnLoad);
        btnLoad.setOnClickListener(this);
        return graphLayout;
    }

    private void autoControl(int imageAutoSet) {
        if (imageAutoSet < 0) {
            byte autoControlByte;
            if (autoControl != 0) {
                autoControlByte = 0;
            } else {
                autoControlByte = 10;
            }

            autoControl = autoControlByte;
        } else {
            autoControl = imageAutoSet;
        }

        if (autoControl != 0) {
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
            var7 = offset2SeekBar.getProgress();
        } else {
            var7 = offset1SeekBar.getProgress();
        }

        var7 += (int) (0.0025D * (var2 + var4) * 4095.0D);
        if (var1 != 0) {
            offset2SeekBar.setProgress(var7);
        } else {
            offset1SeekBar.setProgress(var7);
        }

        if (pACKScopeDrv != null) {
            pACKScopeDrv.setOffset(var7, var1);
        }

        if (var6 == var1) {
            var1 = (int) ((0.5D - 0.005D * var2) * 4095.0D);
            trglevelSeekBar.setProgress(var1);
            if (pACKScopeDrv != null) {
                pACKScopeDrv.setTriggerLevel(var1, 0);
                pACKScopeDrv.setTriggerLevel(var1, 1);
            }
        }

    }

    private void CorrectRange(int var1, double var2) {
        int var5;
        if (var1 != 0) {
            var5 = range2Spinner.getSelectedItemPosition();
        } else {
            var5 = range1Spinner.getSelectedItemPosition();
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
            range2Spinner.setSelection(var4);
        } else {
            range1Spinner.setSelection(var4);
        }

        if (pACKScopeDrv != null) {
            pACKScopeDrv.setRange(var4, var1);
        }

    }

    private void CorrectSampleRate(double var1) {
        int var4 = tbSpinner.getCount() - 1 - tbSpinner.getSelectedItemPosition();
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

        tbSpinner.setSelection(tbSpinner.getCount() - 1 - var3);
        if (pACKScopeDrv != null) {
            pACKScopeDrv.setSampleRate(var3);
        }

    }

    private void DefaultScopeSettings() {
        tbSpinner.setSelection(ACKScopeDrv.timebaseTab.length - 1);
        dataLenSpinner.setSelection(0);
        runmodeSpinner.setSelection(0);
        trgsrcSpinner.setSelection(0);
        trglogSpinner.setSelection(0);
        generatorSpinner.setSelection(0);
        range1Spinner.setSelection(ACKScopeDrv.voltrangTab.length - 1);
        range2Spinner.setSelection(ACKScopeDrv.voltrangTab.length - 1);
        cpl1Spinner.setSelection(0);
        cpl2Spinner.setSelection(0);
        probe1Spinner.setSelection(0);
        probe2Spinner.setSelection(0);
        pretrgRate = 0.5D;
        pretrgSeekBar.setProgress((int) (pretrgRate * (double) pretrgSeekBar.getMax()));
        int var1 = (int) ((double) (dataLenK * 1024) * pretrgRate + 0.5D);
        if (pACKScopeDrv != null) {
            pACKScopeDrv.setTrgDelay(var1);
            pACKScopeDrv.setPostTrgLength(dataLenK * 1024 - var1);
        }

        offset1SeekBar.setProgress(2047);
        offset2SeekBar.setProgress(2047);
        trglevelSeekBar.setProgress(2047);
        if (pACKScopeDrv != null) {
            pACKScopeDrv.setTriggerLevel(2047, 0);
            pACKScopeDrv.setTriggerLevel(2047, 1);
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

    private int getRFileName() {
        final String[] fileNames = GetFileNames();
        if (fileNames.length >= 1) {
            Builder builder = new Builder(this);
            builder.setTitle(R.string.selectfile);
            builder.setIcon(R.drawable.ic_menu_archive);
            builder.setSingleChoiceItems(fileNames, fileNames.length - 1, new android.content.DialogInterface.OnClickListener() {
                public void onClick(DialogInterface var1x, int var2) {
                    var1x.cancel();
                    startMTimer(false);
                    loadData("/AOPA/" + fileNames[var2], false);
                }
            });
            builder.setPositiveButton(getResources().getString(android.R.string.ok), new android.content.DialogInterface.OnClickListener() {
                public void onClick(DialogInterface var1x, int var2) {
                    startMTimer(false);
                    loadData("/AOPA/" + fileNames[fileNames.length - 1], false);
                }
            });
            builder.setNegativeButton(getResources().getString(android.R.string.cancel), new android.content.DialogInterface.OnClickListener() {
                public void onClick(DialogInterface var1, int var2) {
                }
            });
            builder.setCancelable(false);
            builder.show();
            return 1;
        } else {
            Toast.makeText(getApplicationContext(), R.string.err_nodatafile, Toast.LENGTH_LONG).show();
            return -1;
        }
    }

    private float GraphCodeToValue(int var1, float var2) {
        int var5 = 1;
        if (pACKScopeDrv != null) {
            var5 = pACKScopeDrv.getProbe(var1);
        }

        double var3 = (double) var5 * ACKScopeDrv.voltrangTab[pACKScopeDrv.getRange(var1)] / 25.0D;
        return (float) ((double) var2 * var3);
    }

    private float GraphTimeToValue(int var1, float var2) {
        double var3 = ACKScopeDrv.timebaseTab[pACKScopeDrv.getSampleRate()];
        return (float) ((double) var2 * var3);
    }

    private void helpDialog() {
        showHelpLayout(true);
        Builder builder = new Builder(this);
        String version = "\n" + getResources().getString(R.string.version) + ": ";

        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), GET_ACTIVITIES);
            version += packageInfo.versionName;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        builder.setTitle(R.string.about);
        builder.setMessage(getResources().getString(R.string.app_longname) + "\n(C) AKTAKOM, 2014" + version);
        builder.setIcon(R.drawable.ic_launcher);
        TextView textView = new TextView(this);
        textView.setLinksClickable(true);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setText(Html.fromHtml("<br>&nbsp;&nbsp;&nbsp;&nbsp;" + getResources().getString(R.string.apphelp_urihtml) + "<br><br>&nbsp;&nbsp;&nbsp;&nbsp;" + getResources().getString(R.string.aktakom_urihtml)));
        builder.setView(textView);
        builder.setPositiveButton(getResources().getString(android.R.string.ok), new android.content.DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int which) {
            }
        });
        builder.show();
    }

    private void initConnection() {
        searchProgressBar.setVisibility(View.VISIBLE);
        startMTimer(false);
        String longName = getResources().getString(R.string.app_longname) + "\n" + getResources().getString(R.string.initconnect);
        appTitle.setText(longName);
        if (pACKScopeDrv == null) {
            pACKScopeDrv = new ACKScopeDrv(this, tcpOn, serverPort, serverIP);
        } else {
            pACKScopeDrv.initConnection();
        }
    }

    private int loadData(String path, boolean var2) {
        File var9 = new File(Environment.getExternalStorageDirectory(), path);
        if (!var9.exists()) {
            if (!var2) {
                Toast.makeText(getApplicationContext(), "Error. Open data file failure!\tОшибка при открытии файла данных.", Toast.LENGTH_LONG).show();
            }

            return -1;
        } else {
            RandomAccessFile randomAccessFile;
            try {
                randomAccessFile = new RandomAccessFile(var9, "r");
            } catch (FileNotFoundException var8) {
                var8.printStackTrace();
                if (!var2) {
                    Toast.makeText(getApplicationContext(), "Error. Open data file failure!\tОшибка при открытии файла данных.", Toast.LENGTH_LONG).show();
                }

                return -2;
            }

            NumberFormat var5 = NumberFormat.getInstance();
            ParsePosition var6 = new ParsePosition(0);
            String var7 = ReadUTFLine(randomAccessFile, 2, 1);
            if (var7 == null) {
                return 0;
            } else if (var7.isEmpty()) {
                return -3;
            } else {
                var7 = ReadUTFLine(randomAccessFile, 2, 1);
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
                        tbSpinner.setSelection(var11.intValue());
                        var7 = ReadUTFLine(randomAccessFile, 2, 1);
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
                                dataLenSpinner.setSelection(var11.intValue());
                                var7 = ReadUTFLine(randomAccessFile, 2, 1);
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
                                        runmodeSpinner.setSelection(var11.intValue());
                                        var7 = ReadUTFLine(randomAccessFile, 2, 1);
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
                                                trgsrcSpinner.setSelection(var11.intValue());
                                                var7 = ReadUTFLine(randomAccessFile, 2, 1);
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
                                                        trglogSpinner.setSelection(var11.intValue());
                                                        var7 = ReadUTFLine(randomAccessFile, 2, 1);
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
                                                                range1Spinner.setSelection(var11.intValue());
                                                                var7 = ReadUTFLine(randomAccessFile, 2, 1);
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
                                                                        range2Spinner.setSelection(var11.intValue());
                                                                        var7 = ReadUTFLine(randomAccessFile, 2, 1);
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
                                                                                cpl1Spinner.setSelection(var11.intValue());
                                                                                var7 = ReadUTFLine(randomAccessFile, 2, 1);
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
                                                                                        cpl2Spinner.setSelection(var11.intValue());
                                                                                        var7 = ReadUTFLine(randomAccessFile, 2, 1);
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
                                                                                                probe1Spinner.setSelection(var11.intValue());
                                                                                                var7 = ReadUTFLine(randomAccessFile, 2, 1);
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
                                                                                                        probe2Spinner.setSelection(var11.intValue());
                                                                                                        var7 = ReadUTFLine(randomAccessFile, 2, 1);
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
                                                                                                                pretrgSeekBar.setProgress(var3);
                                                                                                                pretrgRate = (double) ((float) var3 / (float) pretrgSeekBar.getMax());
                                                                                                                var3 = (int) (0.5D + (double) (dataLenK * 1024) * pretrgRate);
                                                                                                                if (pACKScopeDrv != null) {
                                                                                                                    pACKScopeDrv.setTrgDelay(var3);
                                                                                                                    pACKScopeDrv.setPostTrgLength(dataLenK * 1024 - var3);
                                                                                                                }

                                                                                                                var7 = ReadUTFLine(randomAccessFile, 2, 1);
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
                                                                                                                        offset1SeekBar.setProgress(var3);
                                                                                                                        var7 = ReadUTFLine(randomAccessFile, 2, 1);
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
                                                                                                                                offset2SeekBar.setProgress(var3);
                                                                                                                                var7 = ReadUTFLine(randomAccessFile, 2, 1);
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
                                                                                                                                        trglevelSeekBar.setProgress(var3);
                                                                                                                                        if (!var2) {
                                                                                                                                            var7 = ReadUTFLine(randomAccessFile, 2, 1);
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
                                                                                                                                            xvalues = new float[var3];
                                                                                                                                            yvalues[0] = new float[var3];

                                                                                                                                            int var4;
                                                                                                                                            for (var3 = 0; var3 < xvalues.length; ++var3) {
                                                                                                                                                xvalues[var3] = (float) (-102.4D + 204.8D * (double) (var3 + 1) / (double) xvalues.length);
                                                                                                                                                var7 = ReadUTFLine(randomAccessFile, 1, 0);
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
                                                                                                                                                yvalues[0][var3] = ChannelCodeToValue(0, var4);
                                                                                                                                            }

                                                                                                                                            var7 = ReadUTFLine(randomAccessFile, 2, 1);
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
                                                                                                                                            xvalues = new float[var3];
                                                                                                                                            yvalues[1] = new float[var3];

                                                                                                                                            for (var3 = 0; var3 < xvalues.length; ++var3) {
                                                                                                                                                xvalues[var3] = (float) (-102.4D + 204.8D * (double) (var3 + 1) / (double) xvalues.length);
                                                                                                                                                var7 = ReadUTFLine(randomAccessFile, 1, 0);
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
                                                                                                                                                yvalues[1][var3] = ChannelCodeToValue(1, var4);
                                                                                                                                            }

                                                                                                                                            datawaiting = 3;
                                                                                                                                            startMTimer(true);
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
            return prefixMultipliers[var4] * var1 + " " + prefixStrings[var4];
        } else {
            NumberFormat var7 = NumberFormat.getInstance();
            String var6 = "@";

            for (int var5 = 1; var5 < var2; ++var5) {
                var6 = var6 + "#";
            }

            ((DecimalFormat) var7).applyPattern(var6);
            return var7.format((double) (prefixMultipliers[var4] * var1)) + " " + prefixStrings[var4];
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

    private void saveData(String path) {
    }

    private void showHelpLayout(boolean var1) {
        byte var3 = 0;
        byte var2;
        if (var1) {
            var2 = 8;
        } else {
            var2 = 0;
        }

        layoutWork.setVisibility(var2);
        LinearLayout var4 = layoutBack;
        int var5 = var3;
        if (var1) {
            var5 = R.drawable.helpwnd;
        }

        var4.setBackgroundResource(var5);
    }

    private void startMTimer(boolean b) {
        Log.d("AOPA", "StartMTimer " + b + " [runBtn.isChecked " + runBtn.isChecked() + "]");
        if (mHandler != null) {
            mHandler.removeCallbacks(updateTask);
            mHandler = null;
        }

        if (b != runBtn.isChecked()) {
            runBtn.setChecked(b);
        }

        if (b) {
            mHandler = new Handler();
            mHandler.postDelayed(updateTask, 0L);
        }

    }

    private int valueToChannelCode(int var1, float var2) {
        return (int) (0.5D + 127.5D * (1.0D + (double) var2 / 100.0D));
    }

    private void writeBOM(RandomAccessFile var1) {
        try {
            var1.write(new byte[]{-17, -69, -65});
        } catch (IOException var2) {
            var2.printStackTrace();
        }
    }

    private void doAutoControl(int var1, int var2, int var3, int var4, int var5, int var6) {
        boolean var12;
        var12 = ch1Enabled && ch2Enabled;

        if (autoControl > 0) {
            double var7 = 1.0D;
            if (var12) {
                var7 = 0.5D;
            }

            int var11 = 0;
            if (pACKScopeDrv != null) {
                var11 = pACKScopeDrv.getTriggerSource();
            }

            if (var11 != 1) {
                var6 = var3;
            }

            double var9;
            if (var6 != 0) {
                var9 = (double) dataLenK * 1024.0D / (double) var6;
                if (var9 < 2.0D || var9 > 6.0D) {
                    CorrectSampleRate(0.25D * var9);
                }
            }

            if (ch1Enabled) {
                boolean var15;
                var15 = (var12 || var1 <= -99) && var2 >= 99;

                var9 = -0.5D * (double) (var2 + var1);
                byte var16;
                if (var12) {
                    var16 = 50;
                } else {
                    var16 = 0;
                }

                CorrectOffset(0, var9, (double) var16, var11);
                if (var15 || Math.abs(var9) < 200.0D / 4.0D) {
                    var9 = 1.0D + 1.05D * (double) (var2 - var1);
                    if (var9 < var7 * 200.0D / 2.5D) {
                        CorrectRange(0, var7 * 200.0D / var9);
                    } else if (var9 > var7 * 200.0D) {
                        CorrectRange(0, -1.0D);
                    }
                }
            }

            if (ch2Enabled) {
                boolean var13;
                var13 = var4 <= -99 && (var12 || var5 >= 99);

                var9 = -0.5D * (double) (var5 + var4);
                byte var14;
                if (var12) {
                    var14 = -50;
                } else {
                    var14 = 0;
                }

                CorrectOffset(1, var9, (double) var14, var11);
                if (var13 || Math.abs(var9) < 200.0D / 4.0D) {
                    var9 = 1.0D + 1.05D * (double) (var5 - var4);
                    if (var9 < var7 * 200.0D / 2.5D) {
                        CorrectRange(1, var7 * 200.0D / var9);
                    } else if (var9 > var7 * 200.0D) {
                        CorrectRange(1, -1.0D);
                    }
                }
            }

            --autoControl;
            if (autoControl == 0) {
                autoControl(0);
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

        var21 = plot2d.getVectorLength();
        tvScale.setText("[T]: " + PrefixFormat(GraphTimeToValue(var21, (float) (0.1D * (double) var21)), 3) + "s/d; [1]: " + PrefixFormat(GraphCodeToValue(0, 25.0F), 3) + "V/d; [2]: " + PrefixFormat(GraphCodeToValue(1, 25.0F), 3) + "V/d");
        double var1;
        double var3;
        double var5;
        double var19;
        int var22;
        int var23;
        int var24;
        int var25;
        float[] var27;
        if (ch1Enabled && yvalues[0].length > 0) {
            var5 = (double) yvalues[0][0];
            var3 = var5;

            for (var21 = 0; var21 < yvalues[0].length; var5 = var7) {
                var1 = var3;
                if (var3 < (double) yvalues[0][var21]) {
                    var1 = (double) yvalues[0][var21];
                }

                var7 = var5;
                if (var5 > (double) yvalues[0][var21]) {
                    var7 = (double) yvalues[0][var21];
                }

                var22 = limited(0, (int) ((double) yvalues[0][var21] + 100.5D), 199);
                var26[var22][1] = var26[var22][0] * var26[var22][1] + yvalues[0][var21];
                var27 = var26[var22];
                var27[0]++;
                var27 = var26[var22];
                var27[1] /= var26[var22][0];
                ++var21;
                var3 = var1;
            }

            var25 = limited(1, (int) (100.5D + (var3 + var5) * 0.5D), 198);
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

            var1 = (double) GraphCodeToValue(0, var26[var24][1]);
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

            var7 = (double) GraphCodeToValue(0, var26[var24][1]);
            amps1Txt.setText("a1(sine): " + PrefixFormat((float) (0.5D * (double) (GraphCodeToValue(0, (float) var3) - GraphCodeToValue(0, (float) var5))), 4) + "V");
            ampp1Txt.setText("a1(puls): " + PrefixFormat((float) (var7 - var1), 4) + "V");
            var1 = 0.0D;
            var11 = (var3 + var5) * 0.5D;
            var22 = yvalues[0].length;
            if (var3 - var5 < 10.0D) {
                var1 = (double) var22;
            } else {
                var19 = var11 - 0.5D * (var11 - var5);
                var7 = NextIntRise(yvalues[0], 0, var22, var19);
                var9 = NextIntRise(yvalues[0], (int) var7, var22, var11);
                var7 = var9;

                for (var21 = 0; var7 < (double) (var22 - 1); ++var21) {
                    var7 = NextIntRise(yvalues[0], (int) var7, var22, var11 + 0.5D * (var3 - var11));
                    if (var7 > (double) (var22 - 1)) {
                        break;
                    }

                    var7 = NextIntFall(yvalues[0], (int) var7, var22, var19);
                    if (var7 > (double) (var22 - 1)) {
                        break;
                    }

                    var7 = NextIntRise(yvalues[0], (int) var7, var22, var11);
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
            freq1Txt.setText("f1: " + PrefixFormat((float) (1.0D / (double) GraphTimeToValue(var22, (float) var1)), 4) + "Hz");
            var11 = var5;
            var7 = var3;
        } else {
            freq1Txt.setText("f1: --- Hz");
            amps1Txt.setText("a1(sine): --- V");
            ampp1Txt.setText("a1(puls): --- V");
        }

        for (var21 = 0; var21 < 200; ++var21) {
            var26[var21][0] = 0f;
            var26[var21][1] = 0f;
        }

        if (ch2Enabled && yvalues[1].length > 0) {
            var5 = (double) yvalues[1][0];
            var3 = var5;

            for (var21 = 0; var21 < yvalues[1].length; var5 = var13) {
                var1 = var3;
                if (var3 < (double) yvalues[1][var21]) {
                    var1 = (double) yvalues[1][var21];
                }

                var13 = var5;
                if (var5 > (double) yvalues[1][var21]) {
                    var13 = (double) yvalues[1][var21];
                }

                var22 = limited(0, (int) ((double) yvalues[1][var21] + 100.5D), 199);
                var26[var22][1] = var26[var22][0] * var26[var22][1] + yvalues[1][var21];
                var27 = var26[var22];
                var27[0]++;
                var27 = var26[var22];
                var27[1] /= var26[var22][0];
                ++var21;
                var3 = var1;
            }

            var25 = limited(1, (int) (100.5D + (var3 + var5) * 0.5D), 198);
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

            var1 = (double) GraphCodeToValue(1, var26[var24][1]);
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

            var13 = (double) GraphCodeToValue(1, var26[var24][1]);
            amps2Txt.setText("a2(sine): " + PrefixFormat((float) (0.5D * (double) (GraphCodeToValue(1, (float) var3) - GraphCodeToValue(1, (float) var5))), 4) + "V");
            ampp2Txt.setText("a2(puls): " + PrefixFormat((float) (var13 - var1), 4) + "V");
            var1 = 0.0D;
            var17 = (var3 + var5) * 0.5D;
            var22 = yvalues[1].length;
            if (var3 - var5 < 10.0D) {
                var1 = (double) var22;
            } else {
                var19 = var17 - 0.5D * (var17 - var5);
                var13 = NextIntRise(yvalues[1], 0, var22, var19);
                var15 = NextIntRise(yvalues[1], (int) var13, var22, var17);
                var13 = var15;

                for (var21 = 0; var13 < (double) (var22 - 1); ++var21) {
                    var13 = NextIntRise(yvalues[1], (int) var13, var22, var17 + 0.5D * (var3 - var17));
                    if (var13 > (double) (var22 - 1)) {
                        break;
                    }

                    var13 = NextIntFall(yvalues[1], (int) var13, var22, var19);
                    if (var13 > (double) (var22 - 1)) {
                        break;
                    }

                    var13 = NextIntRise(yvalues[1], (int) var13, var22, var17);
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
            freq2Txt.setText("f2: " + PrefixFormat((float) (1.0D / (double) GraphTimeToValue(var22, (float) var1)), 4) + "Hz");
        } else {
            freq2Txt.setText("f2: --- Hz");
            amps2Txt.setText("a2(sine): --- V");
            ampp2Txt.setText("a2(puls): --- V");
            var3 = var17;
            var5 = var15;
        }

        if (autoControl > 0) {
            doAutoControl((int) var11, (int) var7, (int) var9, (int) var5, (int) var3, (int) var13);
        }

        Log.d("AOPA", "doMeas exit");
    }

    public void onANConnect(AULNetConnection aulNetConnection) {
        searchProgressBar.setVisibility(View.INVISIBLE);
        String headerName;
        String txtDevName = "none";
        if (aulNetConnection.isDemoMode()) {
            headerName = "AKTAKOM Oscilloscope Pro";
            txtDevName = getResources().getString(R.string.demomode);
            headerName = headerName + "\n" + txtDevName;
            appTitle.setText(headerName);
            startMTimer(false);
        } else {
            String var2 = getResources().getString(R.string.app_longname);
            txtDevName = aulNetConnection.devname;
            headerName = var2 + "\n" + txtDevName;
            appTitle.setText(headerName);
            if (pACKScopeDrv != null) {
                if (pACKScopeDrv.readMemorySize() < 100000) {
                    ArrayList<String> list = new ArrayList<>();
                    list.add("1K p");
                    list.add("10K p");
                    list.add("50K p");
                    ArrayAdapter<String> var5 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list);
                    var5.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    dataLenSpinner.setAdapter(var5);
                    var5.notifyDataSetChanged();
                }
                System.out.println("111111111111");
                if (loadData("/AOPA/default.csv", true) != 1) {
                    DefaultScopeSettings();
                    autoControl(15);
                }

                datawaiting = 0;
            }

            startMTimer(true);
            Log.d("AOPA", "onANConnect exit");
        }
    }

    public void onCheckedChanged(CompoundButton compoundButton, boolean var2) {
        if (compoundButton == runBtn) {
            startMTimer(var2);
        }

    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnOur:
                pACKScopeDrv.pAULNetConnection.initConnection();
                return;
            case R.id.layoutBack:
                showHelpLayout(false);
                return;
            case R.id.imageLogo:
                startActivity(new Intent("android.intent.action.VIEW", Uri.parse(getResources().getString(R.string.aktakom_uri))));
                return;
            case R.id.btnClose:
                finish();
                return;
            case R.id.btnSave:
                saveData("/AOPA/" + (new SimpleDateFormat("yyyyMMddHHmmss")).format(new Date()) + ".csv");
                return;
            case R.id.btnLoad:
                getRFileName();
                return;
            case R.id.autosetBtn:
                autoControl(-1);
                return;
            case R.id.btnHelp:
                helpDialog();
                return;
            case R.id.ctrlShowBtn:
                boolean isNeedShow;
                isNeedShow = ctrlLayout.getVisibility() == View.VISIBLE;

                byte visible;
                if (isNeedShow) {
                    visible = 8;
                } else {
                    visible = 0;
                }

                ctrlLayout.setVisibility(visible);
                trglevelLayout.setVisibility(visible);
                ofs1Layout.setVisibility(visible);
                ofs2Layout.setVisibility(visible);
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

    public void onDataReady(ACKScopeDrv ackScopeDrv) {
        if (runBtn.isChecked()) {
            int dataLength = pACKScopeDrv.getData1().length;
            if (dataLength > 0) {
                noVoidData = true;
            }

            xvalues = new float[dataLength];
            yvalues[0] = new float[dataLength];

            for (dataLength = 0; dataLength < xvalues.length; ++dataLength) {
                xvalues[dataLength] = (float) ((double) (dataLength + 1) * 204.8D / (double) xvalues.length - 102.4D);
                yvalues[0][dataLength] = ChannelCodeToValue(0, pACKScopeDrv.getData1()[dataLength] & 255);
            }

            dataLength = pACKScopeDrv.getData2().length;
            if (dataLength > 0) {
                noVoidData = true;
            }

            xvalues = new float[dataLength];
            yvalues[1] = new float[dataLength];

            for (dataLength = 0; dataLength < xvalues.length; ++dataLength) {
                xvalues[dataLength] = (float) ((double) (dataLength + 1) * 204.8D / (double) xvalues.length - 102.4D);
                yvalues[1][dataLength] = ChannelCodeToValue(1, pACKScopeDrv.getData2()[dataLength] & 255);
            }
        }

        datawaiting = 2;
    }

    protected void onDestroy() {
        super.onDestroy();
        pACKScopeDrv.pAULNetConnection.closeDevice();
        startMTimer(false);
        if (noVoidData) {
            saveData("/AOPA/default.csv");
        }

    }

    public void onItemSelected(AdapterView<?> var1, View var2, int indexNewValue, long var4) {
        boolean var7 = false;
        boolean var8 = true;
        if (var1 == range1Spinner) {
            if (pACKScopeDrv != null) {
                pACKScopeDrv.setRange(indexNewValue, 0);
            }
        } else if (var1 == range2Spinner) {
            if (pACKScopeDrv != null) {
                pACKScopeDrv.setRange(indexNewValue, 1);
            }
        } else if (var1 == tbSpinner) {
            if (pACKScopeDrv != null) {
                pACKScopeDrv.setSampleRate(tbSpinner.getCount() - 1 - indexNewValue);
            }
        } else if (var1 == dataLenSpinner) {
            switch (indexNewValue) {
                case 0:
                    dataLenK = 1;
                    break;
                case 1:
                    dataLenK = 10;
                    break;
                case 2:
                    dataLenK = 100;
            }

            if (pACKScopeDrv != null && dataLenK * 1024 > pACKScopeDrv.getMemorySize()) {
                dataLenK = 50;
            }

            indexNewValue = (int) (0.5D + (double) (dataLenK * 1024) * pretrgRate);
            if (pACKScopeDrv != null) {
                pACKScopeDrv.setTrgDelay(indexNewValue);
                pACKScopeDrv.setPostTrgLength(dataLenK * 1024 - indexNewValue);
            }
        } else if (var1 == runmodeSpinner) {
            if (pACKScopeDrv != null) {
                pACKScopeDrv.setTriggerMode(indexNewValue);
            }

            if (mHandler == null) {
                startMTimer(runBtn.isChecked());
            }
        } else if (var1 == trgsrcSpinner) {
            if (pACKScopeDrv != null) {
                pACKScopeDrv.setTriggerSource(indexNewValue);
            }
        } else if (var1 == trglogSpinner) {
            if (pACKScopeDrv != null) {
                pACKScopeDrv.setTriggerLogic(indexNewValue);
            }
        } else if (var1 == generatorSpinner) {
            if (pACKScopeDrv != null) {
                pACKScopeDrv.setGenerator(indexNewValue);
            }
        } else if (var1 == cpl1Spinner) {
            var7 = indexNewValue < 4;

            ch1Enabled = var7;
            if (ch1Enabled && pACKScopeDrv != null) {
                pACKScopeDrv.setCoupling(indexNewValue, 0);
            }
        } else if (var1 == cpl2Spinner) {
            if (indexNewValue < 4) {
                var7 = true;
            }

            ch2Enabled = var7;
            if (ch2Enabled && pACKScopeDrv != null) {
                pACKScopeDrv.setCoupling(indexNewValue, 1);
            }
        } else {
            byte var6;
            if (var1 == probe1Spinner) {
                var6 = 1;
                if (indexNewValue == 1) {
                    var6 = 10;
                } else if (indexNewValue == 2) {
                    var6 = 100;
                }

                if (pACKScopeDrv != null) {
                    pACKScopeDrv.setProbe(var6, 0);
                }
            } else if (var1 == probe2Spinner) {
                var6 = 1;
                if (indexNewValue == 1) {
                    var6 = 10;
                } else if (indexNewValue == 2) {
                    var6 = 100;
                }

                if (pACKScopeDrv != null) {
                    pACKScopeDrv.setProbe(var6, 1);
                }
            }
        }

    }

    public void onNothingSelected(AdapterView<?> var1) {
    }

    public void onPlotDoubleTap(Plot2d var1) {
        pretrgRate = 0.5D;
        pretrgSeekBar.setProgress((int) (pretrgRate * (double) pretrgSeekBar.getMax()));
        int var2 = (int) ((double) (dataLenK * 1024) * pretrgRate + 0.5D);
        if (pACKScopeDrv != null) {
            pACKScopeDrv.setTrgDelay(var2);
            pACKScopeDrv.setPostTrgLength(dataLenK * 1024 - var2);
        }

    }

    public void onProgressChanged(SeekBar var1, int var2, boolean var3) {
        if (var1 == offset1SeekBar) {
            if (pACKScopeDrv != null) {
                pACKScopeDrv.setOffset(var2, 0);
            }
        } else if (var1 == offset2SeekBar) {
            if (pACKScopeDrv != null) {
                pACKScopeDrv.setOffset(var2, 1);
            }
        } else if (var1 == trglevelSeekBar) {
            if (pACKScopeDrv != null) {
                pACKScopeDrv.setTriggerLevel(var2, 0);
                pACKScopeDrv.setTriggerLevel(var2, 1);
            }
        } else if (var1 == pretrgSeekBar) {
            pretrgRate = (double) var2 / (double) pretrgSeekBar.getMax();
            var2 = (int) (0.5D + (double) (dataLenK * 1024) * pretrgRate);
            if (pACKScopeDrv != null) {
                pACKScopeDrv.setTrgDelay(var2);
                pACKScopeDrv.setPostTrgLength(dataLenK * 1024 - var2);
            }
        }
    }

    public void onRegStatusChange(ACKScopeDrv var1) {
    }

    public void onStartTrackingTouch(SeekBar var1) {
    }

    protected void onStop() {
        super.onStop();
        Editor editor = getSharedPreferences("AOPAPrefs", 0).edit();
        editor.putInt("serverPort", serverPort);
        editor.putString("serverIP", serverIP);
        editor.putBoolean("tcpOn", tcpOn);
        editor.apply();
    }

    public void onStopTrackingTouch(SeekBar var1) {
    }

    public class MyAdapter extends ArrayAdapter<String> {
        public MyAdapter(Context var2, int var3, String[] var4) {
            super(var2, var3, var4);
        }

        public View getDropDownView(int position, View view, ViewGroup viewGroup) {
            return super.getDropDownView(position, view, viewGroup);
        }

        public View getView(int position, View view, ViewGroup viewGroup) {
            TextView var4 = (TextView) super.getView(position, view, viewGroup);
            var4.setTextSize(5.0F * fontSize);
            return var4;
        }
    }
}
