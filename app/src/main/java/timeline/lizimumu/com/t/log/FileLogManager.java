package timeline.lizimumu.com.t.log;


import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Log to file
 * Created by zb on 02/01/2018.
 */

public class FileLogManager {

    private static final String PATH_NAME = "666";
    private static final String ALARM_LOG = "alarm.log";
    private static final String LOG_PATH = Environment.getExternalStorageDirectory() + File.separator + PATH_NAME;
    private static final String LOG_FILE = LOG_PATH + File.separator + ALARM_LOG;
    private static FileLogManager mInstance;

    private FileLogManager() {
    }

    public static void init() {
        mInstance = new FileLogManager();
        new Thread(new Runnable() {
            @Override
            public void run() {
                File file = new File(LOG_PATH);
                if (!file.exists()) {
                    file.mkdirs();
                }
                try {
                    new File(LOG_FILE).createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).run();
    }

    public static FileLogManager getInstance() {
        return mInstance;
    }

    public void log(String message) {

        FileOutputStream outputStream = null;
        PrintWriter writer = null;
        try {
            outputStream = new FileOutputStream(LOG_FILE, true);
            writer = new PrintWriter(outputStream);
            writer.write(message);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) writer.close();
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
