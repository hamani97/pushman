package com.hamani.suntech.pushman;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.hamani.suntech.pushman.common.CheckCertification;
import com.hamani.suntech.pushman.common.DeviceUuidFactory;
import com.hamani.suntech.pushman.common.Globals;
import com.hamani.suntech.pushman.common.HttpSuntechService;
import com.hamani.suntech.pushman.common.SharedData;
import com.hamani.suntech.pushman.data.PushItem;
import com.hamani.suntech.pushman.data.RestPush;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainAct";

    private static final int REQUEST_SETTING_ID  = 100;
    private static final int REQUEST_DETAIL_ID = 101;

    private SharedData sd;
    private CheckCertification cert;

    private SwipeRefreshLayout mSwipe;
    private PushAdapter adapter;
    private ArrayList<PushItem> saveData;       // 임시 저장용
    private ArrayList<PushItem> listData;		// 실제 화면에 표시될 데이터
    private ListView myPushList;
    private LinearLayout mLayoutMessage;
    private LinearLayout mLayoutCount;

    private TextView menu_title;
    private ImageView status_title;

    private TextView mTextMessageTitle;
    private TextView mTextMessage;

    private String all_count = "0";
    private String warning_count = "0";
    private String working_count = "0";
    private String complete_count = "0";

    private TextView mTextWarningCount;
    private TextView mTextWorkingCount;
    private TextView mTextCompleteCount;

    private LinearLayout mButtonWarning;
    private LinearLayout mButtonWorking;
    private LinearLayout mButtonComplete;

    private boolean lastItemVisible = false;    // 리스트뷰의 마지막 row 가 노출되었는지 확인
    private boolean loadingNow = false;         // 현재 데이터를 로딩중인지 확인
    private boolean end_of_data = false;        // 마지막 데이터까지 로딩이 끝났는지 확인
    private int current_list_page = 1;          // 노출페이지
    private String menu_kind = "";              // :All, W:Warning list, R:Running list, Y:Complete list, S:Settings

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    buttonChange("");
                    //mTextMessage.setText(R.string.title_home);
                    return true;
                case R.id.navigation_mywork:
                    buttonChange("MYJOB");
                    //mTextMessage.setText(R.string.title_messages);
                    return true;
                case R.id.navigation_settings:
                    goSettingsPage();
                    //mTextMessage.setText(R.string.title_settings);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
/*
        BottomNavigationMenuView menu = (BottomNavigationMenuView) navigation.getChildAt(0);
        for (int i=0; i<menu.getChildCount(); i++) {
            final View iconView = menu.getChildAt(i).findViewById(android.support.design.R.id.icon);
            final ViewGroup.LayoutParams params = iconView.getLayoutParams();
            final DisplayMetrics metrics = getResources().getDisplayMetrics();
            params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, metrics);
            params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, metrics);
            iconView.setLayoutParams(params);
        }
*/
        sd = new SharedData(this);
        adapter = new PushAdapter();
        cert = new CheckCertification(getApplicationContext());
        listData = new ArrayList<PushItem>();		// 댓글리스트저장
        saveData = new ArrayList<PushItem>();       // 임시저장용

        mLayoutMessage = (LinearLayout) findViewById(R.id.layout_message);
        mTextMessageTitle = (TextView) findViewById(R.id.message_title);
        mTextMessage = (TextView) findViewById(R.id.message);

        final View header = getLayoutInflater().inflate(R.layout.activity_main_push_header, null, false);

        mLayoutCount = (LinearLayout) header.findViewById(R.id.layout_count);

        mTextWarningCount = (TextView) header.findViewById(R.id.txt_warning_count);
        mTextWorkingCount = (TextView) header.findViewById(R.id.txt_working_count);
        mTextCompleteCount = (TextView) header.findViewById(R.id.txt_complete_count);

        mButtonWarning = (LinearLayout) header.findViewById(R.id.btn_ll_warning);
        mButtonWorking = (LinearLayout) header.findViewById(R.id.btn_ll_working);
        mButtonComplete = (LinearLayout) header.findViewById(R.id.btn_ll_complete);

        mButtonWarning.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (warning_count.equals("0")) {
                    Toast.makeText(getApplicationContext(), R.string.no_data, Toast.LENGTH_LONG).show();
                } else {
                    buttonChange("W");
                }
            }
        });
        mButtonWorking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (working_count.equals("0")) {
                    Toast.makeText(getApplicationContext(), R.string.no_data, Toast.LENGTH_LONG).show();
                } else {
                    buttonChange("R");
                }
            }
        });
        mButtonComplete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (complete_count.equals("0")) {
                    Toast.makeText(getApplicationContext(), R.string.no_data, Toast.LENGTH_LONG).show();
                } else {
                    buttonChange("Y");
                }
            }
        });

        myPushList = (ListView) findViewById(R.id.list_push);
        //myPushList.addHeaderView(header);
        myPushList.addHeaderView(header, null, false);
        myPushList.setAdapter(adapter);
        myPushList.setEmptyView(mLayoutMessage);

        // 리스트 헤더에 표시할 위젯
        menu_title = (TextView) header.findViewById(R.id.menu_title);
        status_title = (ImageView) header.findViewById(R.id.status_title);

        myPushList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                position -= myPushList.getHeaderViewsCount();
                Log.e(TAG, "position : " + position);
                Log.e(TAG, "idx : " + listData.get(position).getIdx());
                Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                intent.putExtra("pos", position);
                intent.putExtra("idx", listData.get(position).getIdx());
                startActivityForResult(intent, REQUEST_DETAIL_ID);
            }
        });
        myPushList.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if(scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE && lastItemVisible && loadingNow==false && end_of_data==false) {
                    loadData(current_list_page+1);
                }
            }
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                lastItemVisible = (totalItemCount > 0 && firstVisibleItem + visibleItemCount >= totalItemCount);
            }
        });

        mSwipe = (SwipeRefreshLayout) findViewById(R.id.swipe_layout);
        mSwipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // refresh
                loadData(1);
                mSwipe.setRefreshing(false);
            }
        });
        mSwipe.setOnChildScrollUpCallback(new SwipeRefreshLayout.OnChildScrollUpCallback() {
            @Override
            public boolean canChildScrollUp(@NonNull SwipeRefreshLayout parent, @Nullable View child) {
                return myPushList.canScrollVertically(-1);
//                return false;
            }
        });

        int status = checkCert();

        if (status == CheckCertification.NO_SERVER_ADDR) {
            goSettingsPage();
        } else {
            loadData(1);
        }

        // push
        Bundle extra = getIntent().getExtras();
        if (extra != null) {
            String idx = extra.getString("idx", "0").toString();
            if (idx != null && idx != "0") {
                String kind = extra.getString("kind", "DEVICE").toString();
                Intent intent;
                if (kind != null && kind.equals("NOTICE")) {
                    intent = new Intent(MainActivity.this, NoticeDetailActivity.class);
                    intent.putExtra("pos", -1);
                    intent.putExtra("idx", idx);
                    startActivity(intent);
                } else {
                    intent = new Intent(MainActivity.this, DetailActivity.class);
                    intent.putExtra("pos", -1);
                    intent.putExtra("idx", idx);
                    startActivityForResult(intent, REQUEST_DETAIL_ID);
                }
            }
        }
    }

    public void buttonChange(String kind) {

        mTextWarningCount.setTextColor(Color.parseColor("#555555"));
        mTextWarningCount.setTypeface(null, Typeface.NORMAL);
        mTextWorkingCount.setTextColor(Color.parseColor("#555555"));
        mTextWorkingCount.setTypeface(null, Typeface.NORMAL);
        mTextCompleteCount.setTextColor(Color.parseColor("#555555"));
        mTextCompleteCount.setTypeface(null, Typeface.NORMAL);

        if (kind.equals("W")) {
            status_title.setImageResource(R.drawable.ic_warning_black_48dp);
            menu_title.setText(R.string.show_warning);

            mTextWarningCount.setTextColor(Color.parseColor("#618ec6"));
            mTextWarningCount.setTypeface(null, Typeface.BOLD);

        } else if (kind.equals("R")) {
            status_title.setImageResource(R.drawable.running48);
            menu_title.setText(R.string.show_working);

            mTextWorkingCount.setTextColor(Color.parseColor("#618ec6"));
            mTextWorkingCount.setTypeface(null, Typeface.BOLD);

        } else if (kind.equals("Y")) {
            status_title.setImageResource(R.drawable.ic_complete_black_48dp);
            menu_title.setText(R.string.show_complete);

            mTextCompleteCount.setTextColor(Color.parseColor("#618ec6"));
            mTextCompleteCount.setTypeface(null, Typeface.BOLD);

        } else if (kind.equals("MYJOB")) {
            status_title.setImageResource(R.drawable.ic_person_black_48dp);
            menu_title.setText(R.string.show_yourjob);

        } else {
            status_title.setImageResource(R.drawable.ic_list_black_48dp);
            menu_title.setText(R.string.show_all);
        }

        if (kind.equals("MYJOB")) {
            mLayoutCount.setVisibility(View.GONE);
        } else {
            mLayoutCount.setVisibility(View.VISIBLE);
        }
        loadData(1, kind);
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Notice").setMessage(R.string.alert_msg_quit)
                .setIcon(R.drawable.warning32)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        android.os.Process.killProcess(android.os.Process.myPid());
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
//        super.onBackPressed();
    }

    public void goSettingsPage() {
        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivityForResult(intent, REQUEST_SETTING_ID);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (data!=null) {
                switch(requestCode) {
                    case REQUEST_DETAIL_ID:
                        int list_pos = data.getIntExtra("list_pos", -1);
                        String push_idx = data.getStringExtra("push_idx");
                        String reply_cnt = data.getStringExtra("reply_cnt");
                        String changed_status = data.getStringExtra("changed_status");
                        if (list_pos >= 0) {
                            listData.get(list_pos).setReply_cnt(reply_cnt);
                            listData.get(list_pos).setStatus(changed_status);
                            adapter.notifyDataSetChanged();
                        }
                        Log.e(TAG, list_pos+" : "+push_idx+" : "+changed_status);
                        break;
                    case REQUEST_SETTING_ID:
                        loadData(1);
                        break;
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public int checkCert() {
        int status = cert.check();

        if (status == CheckCertification.NO_DEVICE_ID) {
            DeviceUuidFactory factory = new DeviceUuidFactory(this);
            String device_id = factory.getDeviceUuid().toString();
            if (device_id == null) {
                viewMessageLayout(R.string.unavailable, R.string.no_device_id);
            } else {
                viewMessageLayout(R.string.cannot_be_use, R.string.no_server_addr);
                status = CheckCertification.NO_SERVER_ADDR;
            }
        } else if (status == CheckCertification.NO_SERVER_ADDR) {
            viewMessageLayout(R.string.cannot_be_use, R.string.no_server_addr);
        }
        return status;
    }

    public void checkResponse(String status) {
        if (status.equals(Globals.STATUS_USING_OK)) {
            viewMessageLayout(R.string.no_data, R.string.no_string);
        } else if (status.equals(Globals.STATUS_USING_STOP)) {
            viewMessageLayout(R.string.cannot_be_use, R.string.use_stop);
        } else if (status.equals(Globals.STATUS_USING_WAIT)) {
            viewMessageLayout(R.string.wait_for_approval, R.string.use_wait);
        } else if (status.equals(Globals.STATUS_USING_WAIT_CONNECT)) {
            viewMessageLayout(R.string.wait_for_approval, R.string.use_wait_connect);
        } else if (status.equals(Globals.STATUS_NO_MEMBERS)) {
            viewMessageLayout(R.string.wait_for_approval, R.string.use_wait);
        } else {
            viewMessageLayout(R.string.no_data, R.string.no_string);
            Toast.makeText(getApplicationContext(), status, Toast.LENGTH_LONG).show();
            Log.e(TAG, status);
        }
    }

    public void viewMessageLayout(int title, int desc) {
        mTextMessageTitle.setText(title);
        mTextMessage.setText(desc);
    }

    public void loadData(int page, String kind) {
        menu_kind = kind;
        this.loadData(page);
    }
    public void loadData(int page) {

        loadingNow = true;

        if (page == 1) listData.clear();

        int status = checkCert();
        if (status == CheckCertification.NO_DEVICE_ID || status == CheckCertification.NO_SERVER_ADDR) {
            listData.clear();
            adapter.notifyDataSetChanged();
            return;
        }

        String device_id = sd.getString(Globals.PREFS_DEVICE_ID);
        String addr = sd.getString(Globals.PREFS_SERVER_ADDR);
        String port = sd.getString(Globals.PREFS_SERVER_PORT);

        if (port != null && !port.equals("") && !port.equals("80")) {
            addr = addr + ":" + port;
        }
        addr = "http://" + addr + "/";

        String filter = "ALL";

        Boolean yn = sd.getBoolean(Globals.PREFS_ALL_DATA_YN);
        if (yn==null || yn==false) {
            filter = "MY";
        }

        MyAsyncTask mytask = new MyAsyncTask();
        mytask.execute(addr, device_id, Integer.toString(page), menu_kind, filter);
    }

    public class MyAsyncTask extends AsyncTask<String, Void, String> {

        ProgressDialog dialog = new ProgressDialog(MainActivity.this);

        @Override
        protected void onPreExecute() {
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setMessage("Loading...");
            dialog.show();
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            dialog.dismiss();

            if (result == null) result = "";
            checkResponse(result);
            if (!result.equals(Globals.STATUS_USING_OK)) {
                listData.clear();
            }
            adapter.notifyDataSetChanged();
            if (current_list_page==1) {
                myPushList.setSelection(0);
            }
            loadingNow = false;

            mTextWarningCount.setText(warning_count);
            mTextWorkingCount.setText(working_count);
            mTextCompleteCount.setText(complete_count);
        }

        @Override
        protected String doInBackground(String... strings) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(strings[0])
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            HttpSuntechService service = retrofit.create(HttpSuntechService.class);

            try {
                final Call<RestPush> call = service.getList(Globals.GET_LIST, strings[1], strings[2], strings[3], strings[4]);
                Response<RestPush> response = call.execute();
                if (response.isSuccessful()) {
                    String code = response.body().getCode();
                    if (code.equals(Globals.STATUS_USING_OK)) {
                        saveData = response.body().getItems();
                        int cnt = saveData.size();
                        if (cnt > 0) {
                            current_list_page = Integer.parseInt(strings[2]);
                            for (int i = 0; i < cnt; i++) {
                                listData.add(saveData.get(i));
                            }
                        }
                        if (response.body().getEnd_of_data().equals("T")) {
                            end_of_data = true;
                        } else {
                            end_of_data = false;
                        }
                        warning_count = response.body().getWarning_cnt();
                        working_count = response.body().getWorking_cnt();
                        complete_count = response.body().getComplete_cnt();

                        saveData.clear();
                        return code;
                    } else if (code.equals(Globals.STATUS_USING_STOP) ||
                            code.equals(Globals.STATUS_USING_WAIT) ||
                            code.equals(Globals.STATUS_USING_WAIT_CONNECT) ||
                            code.equals(Globals.STATUS_NO_MEMBERS)) {
                        return code;
                    } else {
                        return response.body().getMsg() + "(" + code + ")";
                    }
                } else {
                    return response.message();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return e.getMessage();
            }
        }
    }

    public class PushAdapter extends BaseAdapter {

        private PushItem item;

        @Override
        public int getCount() {
            return (listData == null) ? 0 : listData.size();
        }

        @Override
        public Object getItem(int position) {
            return listData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            item = listData.get(position);
            PushViewHolder holder;

            if (convertView == null) {
                convertView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.activity_main_push_row, parent, false);
                holder = new PushViewHolder();
//                holder.parent = (TextView) convertView.findViewById(R.id.parent);
                holder.factory = (TextView) convertView.findViewById(R.id.factory);
                holder.machine_no = (TextView) convertView.findViewById(R.id.machine_no);
                holder.datetime = (TextView) convertView.findViewById(R.id.datetime);
                holder.text = (TextView) convertView.findViewById(R.id.text);
                holder.status = (ImageView) convertView.findViewById(R.id.status);
                holder.reply_cnt = (TextView) convertView.findViewById(R.id.reply_cnt);
                convertView.setTag(holder);
            } else {
                holder = (PushViewHolder) convertView.getTag();
            }

            if (item != null) {
                holder.factory.setText(item.getParent() + ", " + item.getFactory() + ", " + item.getLine() + ", " + item.getModel());
                holder.machine_no.setText(item.getMachine_no());
                holder.text.setText(item.getText());
                holder.datetime.setText(item.getReg_date()+" @ "+item.getWorker());

                if (item.getReply_cnt().equals("0")) {
                    holder.reply_cnt.setVisibility(View.GONE);
                } else {
                    holder.reply_cnt.setText(item.getReply_cnt());
                    holder.reply_cnt.setVisibility(View.VISIBLE);
                }
                String status = item.getStatus();
                if (status.equals("W")) {
                    holder.status.setImageResource(R.drawable.warning48);
                } else if (status.equals("R")) {
                    holder.status.setImageResource(R.drawable.running48);
                } else if (status.equals("Y")) {
                    holder.status.setImageResource(R.drawable.ic_complete_green_48dp);
                } else {
                    holder.status.setImageResource(0);
                }
            }
            return convertView;
        }
    }

    private static class PushViewHolder {
//        public TextView parent;
        public TextView factory;
        public TextView machine_no;
        public TextView datetime;
        public TextView text;
        public ImageView status;
        public TextView reply_cnt;
    }
}
