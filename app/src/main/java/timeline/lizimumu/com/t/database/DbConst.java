package timeline.lizimumu.com.t.database;

import android.provider.BaseColumns;

/**
 * App Table Schema
 * Created by zb on 18/12/2017.
 */

final class DbConst {

    static final String DATABASE_NAME = "timeline";

    private DbConst() {
    }

    static class TableApp implements BaseColumns {
        static final String TABLE_NAME = "ignore";
        static final String FIELD_PACKAGE_NAME = "package_name";
        static final String FIELD_CREATE_TIME = "created_time";
    }

}
