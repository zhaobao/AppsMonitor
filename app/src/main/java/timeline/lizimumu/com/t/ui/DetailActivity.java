package timeline.lizimumu.com.t.ui;

import android.annotation.SuppressLint;
import android.app.usage.UsageEvents;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import timeline.lizimumu.com.t.util.BitmapUtil;

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
            Drawable icon = AppUtil.getPackageIcon(this, mPackageName);
            GlideApp.with(this)
                    .load(icon)
                    .transition(new DrawableTransitionOptions().crossFade())
                    .into(imageView);
            // name
            TextView name = findViewById(R.id.name);
            name.setText(AppUtil.parsePackageName(getPackageManager(), mPackageName));
            // time
            mTime = findViewById(R.id.time);
            // action
            final Button mOpenButton = findViewById(R.id.open);
            final Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage(mPackageName);
            if (LaunchIntent == null) {
                mOpenButton.setClickable(false);
                mOpenButton.setAlpha(0.5f);
            } else {
                mOpenButton.setOnClickListener(new View.OnClickListener() {
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
            // color
            final int defaultButtonFilterColor = getResources().getColor(R.color.colorPrimary);
            Bitmap bitmap = BitmapUtil.drawableToBitmap(AppUtil.getPackageIcon(DetailActivity.this, mPackageName));
            Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(Palette palette) {
                    Palette.Swatch swatch = palette.getVibrantSwatch(); // 获取最欢快明亮的颜色！
                    int color = defaultButtonFilterColor;
                    if (swatch != null) {
                        color = swatch.getRgb();
                    }
                    try {
                        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(color));
                        Window window = getWindow();
                        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                        window.setStatusBarColor(color);
                    } catch (Exception e) {
                        // ignore
                    }
                    mOpenButton.getBackground().setColorFilter(color, PorterDuff.Mode.MULTIPLY);
                }
            });
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
            String desc = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault()).format(new Date(item.mEventTime));
            if (item.mEventType == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                holder.mLayout.setPadding(dpToPx(16), 0, dpToPx(16), dpToPx(4));
            } else if (item.mEventType == -1) {
                holder.mLayout.setPadding(dpToPx(16), dpToPx(4), dpToPx(16), dpToPx(4));
                desc = AppUtil.formatMilliSeconds(item.mUsageTime);
            } else if (item.mEventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                holder.mLayout.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), 0);
            }
            holder.mEvent.setText(String.format("%s %s", getPrefix(item.mEventType), desc));
        }

        private String getPrefix(int event) {
            switch (event) {
                case 1:
                    return "┌";
                case 2:
                    return "└";
                case 7:
                    return "├";
                default:
                    return "├";
            }
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder {

            TextView mEvent;
            TextView mSign;
            LinearLayout mLayout;

            MyViewHolder(View itemView) {
                super(itemView);
                mEvent = itemView.findViewById(R.id.event);
                mSign = itemView.findViewById(R.id.sign);
                mLayout = itemView.findViewById(R.id.layout);
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
                List<AppItem> newList = new ArrayList<>();
                long duration = 0;
                for (AppItem item : appItems) {
                    if (item.mEventType == UsageEvents.Event.USER_INTERACTION || item.mEventType == UsageEvents.Event.NONE) {
                        continue;
                    }
                    duration += item.mUsageTime;
                    if (item.mEventType == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                        AppItem newItem = item.copy();
                        newItem.mEventType = -1;
                        newList.add(newItem);
                    }
                    newList.add(item);
                }
                mTime.setText(String.format(getResources().getString(R.string.times), AppUtil.formatMilliSeconds(duration), appItems.get(appItems.size() - 1).mCount));
                mAdapter.setData(newList);
            }
        }
    }

    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }


}
