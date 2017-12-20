package timeline.lizimumu.com.t;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import timeline.lizimumu.com.t.data.AppItem;
import timeline.lizimumu.com.t.data.DataManager;
import timeline.lizimumu.com.t.database.DbExecutor;
import timeline.lizimumu.com.t.ui.DetailActivity;
import timeline.lizimumu.com.t.ui.SettingsActivity;
import timeline.lizimumu.com.t.util.AppUtil;
import timeline.lizimumu.com.t.util.PreferenceManager;

public class MainActivity extends AppCompatActivity {

    private static long CHECK_INTERVAL = 400;
    private Switch mSwitch;
    private TextView mSwitchText;
    private ProgressBar mProgress;
    private RecyclerView mList;
    private DataManager mManager;
    private MyAdapter mAdapter;
    private AlertDialog mDialog;

    private Handler mHandler = new Handler();
    private Runnable mTask = new Runnable() {
        @Override
        public void run() {
            if (!mManager.hasPermission(getApplicationContext())) {
                mHandler.postDelayed(this, CHECK_INTERVAL);
            } else {
                mHandler.removeCallbacks(mTask);
                PreferenceManager.getInstance().putBoolean(PreferenceManager.PREF_MONITOR_ON, true);
                Toast.makeText(MainActivity.this, R.string.grant_success, Toast.LENGTH_SHORT).show();
                process();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSwitch = findViewById(R.id.enable_switch);
        mSwitchText = findViewById(R.id.enable_text);
        mProgress = findViewById(R.id.progress);
        mManager = new DataManager();
        mAdapter = new MyAdapter();

        mList = findViewById(R.id.list);
        mList.setLayoutManager(new LinearLayoutManager(this));
        mList.setAdapter(mAdapter);

        initHeader();
        initEvents();

        process();
    }

    private void initHeader() {
        if (mManager.hasPermission(getApplicationContext())) {
            mSwitchText.setText(R.string.enable_apps_monitoring);
            if (PreferenceManager.getInstance().getBoolean(PreferenceManager.PREF_MONITOR_ON)) {
                mSwitch.setChecked(true);
            } else {
                mSwitch.setChecked(false);
            }
        } else {
            mSwitchText.setText(R.string.enable_apps_monitor);
            mSwitch.setChecked(false);
            mProgress.setVisibility(View.GONE);
        }
    }

    private void initEvents() {
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    if (!mManager.hasPermission(getApplicationContext())) {
                        mManager.requestPermission(getApplicationContext());
                        mHandler.postDelayed(mTask, CHECK_INTERVAL);
                    } else {
                        PreferenceManager.getInstance().putBoolean(PreferenceManager.PREF_MONITOR_ON, true);
                        process();
                    }
                } else {
                    PreferenceManager.getInstance().putBoolean(PreferenceManager.PREF_MONITOR_ON, false);
                    mAdapter.clear();
                }
            }
        });
        findViewById(R.id.enable).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSwitch.performClick();
            }
        });
    }

    private void process() {
        if (mManager.hasPermission(getApplicationContext())) {
            mProgress.setVisibility(View.VISIBLE);
            mList.setVisibility(View.INVISIBLE);
            new MyAsyncTask().execute(PreferenceManager.getInstance().getInt(PreferenceManager.PREF_LIST_SORT));
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.ignore:
                DbExecutor.getInstance().insertItem(mAdapter.getItemInfoByPosition(item.getOrder()));
                process();
                Toast.makeText(this, R.string.ignore_success, Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                mAdapter.clear();
                process();
                return true;
            case R.id.settings:
                startActivityForResult(new Intent(MainActivity.this, SettingsActivity.class), 1);
                return true;
            case R.id.sort:
                mDialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.sort)
                        .setSingleChoiceItems(R.array.sort, PreferenceManager.getInstance().getInt(PreferenceManager.PREF_LIST_SORT), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                PreferenceManager.getInstance().putInt(PreferenceManager.PREF_LIST_SORT, i);
                                process();
                                mDialog.dismiss();
                            }
                        })
                        .create();
                mDialog.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode > 0) process();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mTask);
        if (mDialog != null) mDialog.dismiss();
    }

    class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {

        private List<AppItem> mData;

        MyAdapter() {
            super();
            mData = new ArrayList<>();
        }

        void clear() {
            mData = new ArrayList<>();
            notifyDataSetChanged();
        }

        void updateData(List<AppItem> data) {
            mData = data;
            notifyDataSetChanged();
        }

        AppItem getItemInfoByPosition(int position) {
            if (mData.size() > position) {
                return mData.get(position);
            }
            return null;
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list, parent, false);
            return new MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            AppItem item = getItemInfoByPosition(position);
            holder.mName.setText(item.mName);
            holder.mUsage.setText(AppUtil.formatMilliSeconds(item.mUsageTime));
            holder.mTime.setText(String.format(Locale.getDefault(),
                    "%s · %d %s",
                    new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault()).format(new Date(item.mEventTime)),
                    item.mCount,
                    getResources().getString(R.string.times_only))
            );
            GlideApp.with(MainActivity.this)
                    .load(AppUtil.getPackageIcon(MainActivity.this, item.mPackageName))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .transition(new DrawableTransitionOptions().crossFade())
                    .into(holder.mIcon);
            holder.setOnClickListener(item);
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {

            private TextView mName;
            private TextView mUsage;
            private TextView mTime;
            private ImageView mIcon;

            MyViewHolder(View itemView) {
                super(itemView);
                mName = itemView.findViewById(R.id.app_name);
                mUsage = itemView.findViewById(R.id.app_usage);
                mTime = itemView.findViewById(R.id.app_time);
                mIcon = itemView.findViewById(R.id.app_image);
                itemView.setOnCreateContextMenuListener(this);
            }

            void setOnClickListener(final AppItem item) {
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                        intent.putExtra(DetailActivity.EXTRA_PACKAGE_NAME, item.mPackageName);
                        startActivityForResult(intent, 1);
                    }
                });
            }

            @Override
            public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
                contextMenu.add(0, R.id.ignore, getAdapterPosition(), getResources().getString(R.string.ignore));
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    class MyAsyncTask extends AsyncTask<Integer, Void, List<AppItem>> {

        @Override
        protected List<AppItem> doInBackground(Integer... integers) {
            return mManager.getApps(getApplicationContext(), integers[0]);
        }

        @Override
        protected void onPostExecute(List<AppItem> appItems) {
            mProgress.setVisibility(View.GONE);
            mAdapter.updateData(appItems);
            mList.setVisibility(View.VISIBLE);
            long total = 0;
            for (AppItem item : appItems) {
                total += item.mUsageTime;
            }
            mSwitchText.setText(String.format(getResources().getString(R.string.total), AppUtil.formatMilliSeconds(total)));
        }
    }
}
