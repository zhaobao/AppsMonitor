package timeline.lizimumu.com.t.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * DB Helper
 * Created by zb on 18/12/2017.
 */

public class DbHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = DbConst.DATABASE_NAME;

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + DbConst.TableApp.TABLE_NAME + " (" +
                    DbConst.TableApp._ID + " INTEGER PRIMARY KEY," +
                    DbConst.TableApp.FIELD_PACKAGE_NAME + " TEXT," +
                    DbConst.TableApp.FIELD_CREATE_TIME + " INTEGER)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + DbConst.TableApp.TABLE_NAME;

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
