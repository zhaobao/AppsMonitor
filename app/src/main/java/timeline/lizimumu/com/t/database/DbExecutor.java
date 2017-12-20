package timeline.lizimumu.com.t.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import timeline.lizimumu.com.t.data.AppItem;

/**
 * Database Entry
 * Created by zb on 18/12/2017.
 */

public class DbExecutor {

    private static DbExecutor sInstance;
    private static DbHelper mHelper;

    private DbExecutor() {
    }

    public static void init(Context context) {
        mHelper = new DbHelper(context);
        sInstance = new DbExecutor();
    }

    public static DbExecutor getInstance() {
        return sInstance;
    }

    public void insertItem(AppItem item) {
        if (!exists(item.mPackageName)) {
            ContentValues values = new ContentValues();
            values.put(DbConst.TableApp.FIELD_PACKAGE_NAME, item.mPackageName);
            values.put(DbConst.TableApp.FIELD_CREATE_TIME, System.currentTimeMillis());
            mHelper.getWritableDatabase().insert(DbConst.TableApp.TABLE_NAME, null, values);
        }
    }

    public void insertItem(String packageName) {
        if (!exists(packageName)) {
            ContentValues values = new ContentValues();
            values.put(DbConst.TableApp.FIELD_PACKAGE_NAME, packageName);
            values.put(DbConst.TableApp.FIELD_CREATE_TIME, System.currentTimeMillis());
            mHelper.getWritableDatabase().insert(DbConst.TableApp.TABLE_NAME, null, values);
        }
    }

    public void deleteItem(IgnoreItem item) {
        if (exists(item.mPackageName)) {
            mHelper.getWritableDatabase().delete(
                    DbConst.TableApp.TABLE_NAME,
                    DbConst.TableApp.FIELD_PACKAGE_NAME + " = ?",
                    new String[]{item.mPackageName}
            );
        }
    }

    public List<IgnoreItem> getAllItems() {
        Cursor cursor = null;
        List<IgnoreItem> items = new ArrayList<>();
        try {
            String[] columns = {
                    DbConst.TableApp._ID,
                    DbConst.TableApp.FIELD_PACKAGE_NAME,
                    DbConst.TableApp.FIELD_CREATE_TIME,
            };
            String orderBy = DbConst.TableApp.FIELD_CREATE_TIME + " DESC";
            cursor = mHelper.getReadableDatabase().query(
                    DbConst.TableApp.TABLE_NAME,
                    columns,
                    null,
                    null,
                    null,
                    null,
                    orderBy);
            while (cursor.moveToNext()) {
                items.add(cursorToItem(cursor));
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return items;
    }

    private boolean exists(String packageName) {
        SQLiteDatabase database = mHelper.getWritableDatabase();
        Cursor cursor = null;
        try {
            cursor = database.query(DbConst.TableApp.TABLE_NAME,
                    new String[]{DbConst.TableApp._ID},
                    DbConst.TableApp.FIELD_PACKAGE_NAME + " = ?",
                    new String[]{packageName},
                    null,
                    null,
                    null);
            if (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndex(DbConst.TableApp._ID));
                return id > 0;
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return false;
    }

    private IgnoreItem cursorToItem(Cursor cursor) {
        IgnoreItem item = new IgnoreItem();
        item.mPackageName = cursor.getString(cursor.getColumnIndex(DbConst.TableApp.FIELD_PACKAGE_NAME));
        item.mCreated = cursor.getLong(cursor.getColumnIndex(DbConst.TableApp.FIELD_CREATE_TIME));
        return item;
    }
}
