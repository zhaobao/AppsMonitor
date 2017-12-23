package timeline.lizimumu.com.t.ui;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.Explode;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
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

import timeline.lizimumu.com.t.GlideApp;
import timeline.lizimumu.com.t.R;
import timeline.lizimumu.com.t.data.AppItem;
import timeline.lizimumu.com.t.data.DataManager;
import timeline.lizimumu.com.t.database.DbExecutor;
import timeline.lizimumu.com.t.service.AppService;
import timeline.lizimumu.com.t.util.AppUtil;
import timeline.lizimumu.com.t.util.PreferenceManager;

public class MainActivity extends AppCompatActivity {

    private Switch mSwitch;
    private TextView mSwitchText;
    private RecyclerView mList;
    private DataManager mManager;
    private MyAdapter mAdapter;
    private AlertDialog mDialog;
    private SwipeRefreshLayout mSwipe;
    private long mTotal;
    private int mDay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // https://guides.codepath.com/android/Shared-Element-Activity-Transition
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        getWindow().setExitTransition(new Explode());

        setContentView(R.layout.activity_main);

        mSwitch = findViewById(R.id.enable_switch);
        mSwitchText = findViewById(R.id.enable_text);
        mManager = new DataManager();
        mAdapter = new MyAdapter();

        mList = findViewById(R.id.list);
        mList.setLayoutManager(new LinearLayoutManager(this));
        mList.setAdapter(mAdapter);

        initLayout();
        initEvents();
        initSpinner();

        if (mManager.hasPermission(getApplicationContext())) process();
    }

    private void initLayout() {
        mSwipe = findViewById(R.id.swipe_refresh);
        if (mManager.hasPermission(getApplicationContext())) {
            mSwitchText.setText(R.string.enable_apps_monitoring);
            mSwitch.setVisibility(View.GONE);
            mSwipe.setEnabled(true);
        } else {
            mSwitchText.setText(R.string.enable_apps_monitor);
            mSwitch.setVisibility(View.VISIBLE);
            mSwitch.setChecked(false);
            mSwipe.setEnabled(false);
        }
    }

    private void initSpinner() {
        if (mManager.hasPermission(getApplicationContext())) {
            Spinner spinner = findViewById(R.id.spinner);
            spinner.setVisibility(View.VISIBLE);
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                    R.array.duration, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    if (mDay != i) {
                        int[] values = getResources().getIntArray(R.array.duration_int);
                        mDay = values[i];
                        process();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });
        }
    }

    private void initEvents() {
        if (!mManager.hasPermission(getApplicationContext())) {
            mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (b) {
                        if (!DataManager.detectFunctionValid(getApplicationContext())) {
                            Toast.makeText(MainActivity.this, R.string.not_support, Toast.LENGTH_LONG).show();
                            compoundButton.setChecked(false);
                        } else {
                            Intent intent = new Intent(MainActivity.this, AppService.class);
                            intent.putExtra(AppService.SERVICE_ACTION, AppService.SERVICE_ACTION_CHECK);
                            startService(intent);
                        }
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
        mSwipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                process();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mManager.hasPermission(getApplicationContext())) {
            mSwitch.setChecked(false);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (mManager.hasPermission(this)) {
            mSwipe.setEnabled(true);
            mSwitch.setVisibility(View.GONE);
            initSpinner();
            process();
        }
    }

    private void process() {
        if (mManager.hasPermission(getApplicationContext())) {
            mList.setVisibility(View.INVISIBLE);
            new MyAsyncTask().execute(PreferenceManager.getInstance().getInt(PreferenceManager.PREF_LIST_SORT), mDay);
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
                if (mManager.hasPermission(getApplicationContext())) {
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
                } else {
                    return false;
                }
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
                    new SimpleDateFormat("yyyy.MM.dd · HH:mm:ss", Locale.getDefault()).format(new Date(item.mEventTime)),
                    item.mCount,
                    getResources().getString(R.string.times_only))
            );
            if (mTotal > 0) {
                holder.mProgress.setProgress((int) (item.mUsageTime * 100 / mTotal));
            } else {
                holder.mProgress.setProgress(0);
            }
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
            private ProgressBar mProgress;

            MyViewHolder(View itemView) {
                super(itemView);
                mName = itemView.findViewById(R.id.app_name);
                mUsage = itemView.findViewById(R.id.app_usage);
                mTime = itemView.findViewById(R.id.app_time);
                mIcon = itemView.findViewById(R.id.app_image);
                mProgress = itemView.findViewById(R.id.progressBar);
                itemView.setOnCreateContextMenuListener(this);
            }

            @SuppressLint("RestrictedApi")
            void setOnClickListener(final AppItem item) {
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                        intent.putExtra(DetailActivity.EXTRA_PACKAGE_NAME, item.mPackageName);
                        intent.putExtra(DetailActivity.EXTRA_DAY, mDay);
                        ActivityOptionsCompat options = ActivityOptionsCompat.
                                makeSceneTransitionAnimation(MainActivity.this, mIcon, "profile");
                        startActivityForResult(intent, 1, options.toBundle());
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
        protected void onPreExecute() {
            super.onPreExecute();
            mSwipe.setRefreshing(true);
        }

        @Override
        protected List<AppItem> doInBackground(Integer... integers) {
            return mManager.getApps(getApplicationContext(), integers[0], integers[1]);
        }

        @Override
        protected void onPostExecute(List<AppItem> appItems) {
            mList.setVisibility(View.VISIBLE);
            mTotal = 0;
            for (AppItem item : appItems) {
                if (item.mUsageTime <= 0) continue;
                mTotal += item.mUsageTime;
            }
            mSwitchText.setText(String.format(getResources().getString(R.string.total), AppUtil.formatMilliSeconds(mTotal)));
            mSwipe.setRefreshing(false);
            mAdapter.updateData(appItems);
        }
    }
}
