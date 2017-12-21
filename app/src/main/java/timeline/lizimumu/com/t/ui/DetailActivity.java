package timeline.lizimumu.com.t.ui;

import android.annotation.SuppressLint;
import android.app.usage.UsageEvents;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import timeline.lizimumu.com.t.GlideApp;
import timeline.lizimumu.com.t.R;
import timeline.lizimumu.com.t.data.AppItem;
import timeline.lizimumu.com.t.data.DataManager;
import timeline.lizimumu.com.t.database.DbExecutor;
import timeline.lizimumu.com.t.util.AppUtil;

public class DetailActivity extends AppCompatActivity {

    public static final String EXTRA_PACKAGE_NAME = "package_name";

    private MyAdapter mAdapter;
    private TextView mTime;
    private String mPackageName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.detail);
        }

        Intent intent = getIntent();
        if (intent != null) {
            mPackageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
            // icon
            ImageView imageView = findViewById(R.id.icon);
            GlideApp.with(this)
                    .load(AppUtil.getPackageIcon(this, mPackageName))
                    .transition(new DrawableTransitionOptions().crossFade())
                    .into(imageView);
            // name
            TextView name = findViewById(R.id.name);
            name.setText(AppUtil.parsePackageName(getPackageManager(), mPackageName));
            // time
            mTime = findViewById(R.id.time);
            // action
            final Button button = findViewById(R.id.open);
            final Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage(mPackageName);
            if (LaunchIntent == null) {
                button.setClickable(false);
                button.setAlpha(0.5f);
            } else {
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivity(LaunchIntent);
                    }
                });
            }
            // list
            RecyclerView mList = findViewById(R.id.list);
            mList.setLayoutManager(new LinearLayoutManager(this));
            mAdapter = new MyAdapter();
            mList.setAdapter(mAdapter);
            // load
            new MyAsyncTask(this).execute(mPackageName);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.ignore:
                if (!TextUtils.isEmpty(mPackageName)) {
                    DbExecutor.getInstance().insertItem(mPackageName);
                    setResult(1);
                    Toast.makeText(this, R.string.ignore_success, Toast.LENGTH_SHORT).show();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {

        private List<AppItem> mData;

        MyAdapter() {
            mData = new ArrayList<>();
        }

        void setData(List<AppItem> data) {
            mData = data;
            notifyDataSetChanged();
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_detail, parent, false);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            AppItem item = mData.get(position);
            String time = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault()).format(new Date(item.mEventTime));
            String desc;
            if (item.mEventType == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                desc = String.format(Locale.getDefault(), "%s %s %s", time, formatEventType(item.mEventType), AppUtil.formatMilliSeconds(item.mUsageTime));
            } else {
                desc = String.format(Locale.getDefault(), "%s %s", time, formatEventType(item.mEventType));
            }
            holder.mEvent.setText(desc);
        }

        private String formatEventType(int event) {
            switch (event) {
                case 1:
                    return "open";
                case 2:
                    return "close";
                case 7:
                    return "using";
                default:
                    return "none";
            }
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder {

            TextView mEvent;

            MyViewHolder(View itemView) {
                super(itemView);
                mEvent = itemView.findViewById(R.id.event);
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    class MyAsyncTask extends AsyncTask<String, Void, List<AppItem>> {

        private WeakReference<Context> mContext;

        MyAsyncTask(Context context) {
            mContext = new WeakReference<>(context);
        }

        @Override
        protected List<AppItem> doInBackground(String... strings) {
            return new DataManager().getTargetAppTimeline(mContext.get(), strings[0]);
        }

        @Override
        protected void onPostExecute(List<AppItem> appItems) {
            if (mContext.get() != null) {
                long duration = 0;
                for (AppItem item : appItems) {
                    duration += item.mUsageTime;
                }
                mTime.setText(String.format(getResources().getString(R.string.times), AppUtil.formatMilliSeconds(duration), appItems.get(appItems.size() - 1).mCount));
                mAdapter.setData(appItems);
            }
        }
    }

}
