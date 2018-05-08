package timeline.lizimumu.com.t.util;

import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import timeline.lizimumu.com.t.AppConst;

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static CrashHandler INSTANCE = new CrashHandler();
    private Thread.UncaughtExceptionHandler mDefaultHandler;
    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault());

    private CrashHandler() {
    }

    public static CrashHandler getInstance() {
        return INSTANCE;
    }

    public void init() {
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    public void uncaughtException(Thread thread, Throwable ex) {
        if (!handleException(ex) && mDefaultHandler != null) {
            mDefaultHandler.uncaughtException(thread, ex);
        } else {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }

    private boolean handleException(Throwable ex) {
        if (ex == null) return false;
        saveCrashInfo2File(ex);
        return true;
    }

    private void saveCrashInfo2File(Throwable ex) {
        StringBuffer sb = new StringBuffer();
        Writer writer = new StringWriter();
        PrintWriter pw = new PrintWriter(writer);
        ex.printStackTrace(pw);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(pw);
            cause = cause.getCause();
        }
        pw.close();
        String result = writer.toString();
        sb.append(result);
        long timestamp = System.currentTimeMillis();
        String time = format.format(new Date());
        String fileName = "crash-" + time + "-" + timestamp + ".log";
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            try {
                File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + AppConst.LOG_DIR);
                boolean ok = true;
                if (!dir.exists()) {
                    ok = dir.mkdirs();
                }
                if (ok) {
                    FileOutputStream fos = new FileOutputStream(new File(dir, fileName));
                    fos.write(sb.toString().getBytes());
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
