package com.hamani.suntech.pushman;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.hamani.suntech.pushman.common.Globals;
import com.hamani.suntech.pushman.common.HttpSuntechService;
import com.hamani.suntech.pushman.common.SharedData;
import com.hamani.suntech.pushman.data.PushItem;
import com.hamani.suntech.pushman.data.ReplyItem;
import com.hamani.suntech.pushman.data.RestPush;
import com.hamani.suntech.pushman.data.RestReply;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DetailActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "DetailAct";

    Button mBtnSettingsCatch;
    Button mBtnSettingsCancel;
    Button mBtnSettingsComplete;
    Button mBtnSettingsBack;

    private SharedData sd;
    private int list_pos;    // 리스트에서 선택한 위치값
    private String push_idx;    // 리스트에서 선택한 값의 DB idx 값
    private ArrayList<PushItem> listData;   // 본문로딩을 위한 임시변수
    private PushItem item;		// 실제 화면에 표시될 데이터

    private String content_status = "";

    private SwipeRefreshLayout mSwipe;

    private TextView parent;
    private TextView factory;
    private TextView line;
    private TextView model;
    private TextView machine_no;
    private TextView mac_addr;
    private TextView worker;
    private TextView date;
    private TextView time;
    private TextView content;
    private ImageView status;
    private TextView status_msg;
    private TextView tv_reply_cnt;

    private ReplyAdapter adapter;;
    private ArrayList<ReplyItem> replyData;		// 실제 화면에 표시될 데이터
    private int reply_cnt = 0;      // 워크리스트 갯수

    private ListView myPushReply;
    private TextView mEmptyMessage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        setTitle(R.string.title_detail);

        Bundle extras = getIntent().getExtras();

        if (extras == null) {
            Toast.makeText(getApplicationContext(), "The parameter was not passed(1).", Toast.LENGTH_LONG).show();
            finish();
        } else {
            list_pos = extras.getInt("pos");
            push_idx = extras.getString("idx");
        }

        if (push_idx == null || push_idx.equals("")) {
            Toast.makeText(getApplicationContext(), "The parameter was not passed(2).", Toast.LENGTH_LONG).show();
            finish();
        }

        sd = new SharedData(this);

        adapter = new ReplyAdapter();
        listData  = new ArrayList<PushItem>();
        replyData = new ArrayList<ReplyItem>();		// 댓글리스트저장

        parent = findViewById(R.id.txt_parent);
        factory = findViewById(R.id.txt_factory);
        line = findViewById(R.id.txt_line);
        model = findViewById(R.id.txt_model);
        machine_no = findViewById(R.id.txt_machine_no);
        mac_addr = findViewById(R.id.txt_mac_addr);
        worker = findViewById(R.id.txt_worker);
        date = findViewById(R.id.txt_date);
        time = findViewById(R.id.txt_time);
        content = findViewById(R.id.txt_content);
        status = findViewById(R.id.status);
        status_msg = findViewById(R.id.txt_status_msg);
        tv_reply_cnt = findViewById(R.id.txt_reply_cnt);

        mBtnSettingsCatch = findViewById(R.id.btn_settings_catch);
        mBtnSettingsBack = findViewById(R.id.btn_settings_back);
        mBtnSettingsCancel = findViewById(R.id.btn_settings_cancel);
        mBtnSettingsComplete = findViewById(R.id.btn_settings_complete);

        mBtnSettingsCatch.setOnClickListener(this);
        mBtnSettingsBack.setOnClickListener(this);
        mBtnSettingsCancel.setOnClickListener(this);
        mBtnSettingsComplete.setOnClickListener(this);

        mEmptyMessage = findViewById(R.id.empty_message);

        myPushReply = findViewById(R.id.list_reply);
        myPushReply.setAdapter(adapter);
        myPushReply.setEmptyView(mEmptyMessage);

        mSwipe = (SwipeRefreshLayout) findViewById(R.id.swipe_layout);
        mSwipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // refresh
                loadData();
                mSwipe.setRefreshing(false);
            }
        });

        loadData();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_settings_catch:
                catchConfirm("Do you want to handle this device?", "R");
                break;
            case R.id.btn_settings_complete:
                catchConfirm("Do you want to finish repairing the device?", "Y");
                break;
            case R.id.btn_settings_cancel:
                catchConfirm("Are you sure you want to cancel the selected equipment repair job?", "W");
                break;
            case R.id.btn_settings_back:
                goBack();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        goBack();
        super.onBackPressed();
    }

    public void catchConfirm(String message, final String change_status) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm").setMessage(message)
                .setIcon(R.drawable.warning32)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        catchContent(change_status);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
    }

    public void catchContent(String change_status) {
        String device_id = sd.getString(Globals.PREFS_DEVICE_ID);
        String addr = sd.getString(Globals.PREFS_SERVER_ADDR);
        String port = sd.getString(Globals.PREFS_SERVER_PORT);

        if (port != null && !port.equals("") && !port.equals("80")) {
            addr = addr + ":" + port;
        }
        addr = "http://" + addr + "/";

        CatchAsyncTask task = new CatchAsyncTask();
        task.execute(addr, device_id, push_idx, change_status);
    }

    public void loadData() {
        String device_id = sd.getString(Globals.PREFS_DEVICE_ID);
        String addr = sd.getString(Globals.PREFS_SERVER_ADDR);
        String port = sd.getString(Globals.PREFS_SERVER_PORT);

        if (port != null && !port.equals("") && !port.equals("80")) {
            addr = addr + ":" + port;
        }
        addr = "http://" + addr + "/";

        DetailAsyncTask task = new DetailAsyncTask();
        task.execute(addr, device_id, push_idx);
    }

    public void showData(String result) {
        if (result.equals(Globals.STATUS_USING_OK)) {
            if (item != null) {
                parent.setText(item.getParent());
                factory.setText(item.getFactory());
                line.setText(item.getLine());
                model.setText(item.getModel());
                machine_no.setText(item.getMachine_no());
                mac_addr.setText(item.getMac_addr());
                worker.setText(item.getWorker());
                date.setText(item.getReg_date());
                time.setText(item.getReg_time());
                content.setText(item.getText());
                content_status = item.getStatus();
                status_msg.setText(item.getStatus_msg());

                if (item.getReply_cnt().equals("0")) {
                    tv_reply_cnt.setVisibility(View.GONE);
                } else {
                    tv_reply_cnt.setText(item.getReply_cnt());
                    tv_reply_cnt.setVisibility(View.VISIBLE);
                }

                if (content_status.equals("W")) {   // 대기
                    mBtnSettingsCatch.setVisibility(View.VISIBLE);
                    mBtnSettingsCancel.setVisibility(View.GONE);
                    mBtnSettingsComplete.setVisibility(View.GONE);

                    status.setImageResource(R.drawable.warning48);

                } else if (content_status.equals("R")) {    // 진행중
                    if (item.getOwner().equals("1")) {      // 내가 작업중
                        mBtnSettingsCatch.setVisibility(View.GONE);
                        mBtnSettingsCancel.setVisibility(View.VISIBLE);
                        mBtnSettingsComplete.setVisibility(View.VISIBLE);
                    } else {    // 다른사람이 작업중
                        mBtnSettingsCatch.setVisibility(View.GONE);
                        mBtnSettingsCancel.setVisibility(View.GONE);
                        mBtnSettingsComplete.setVisibility(View.GONE);
                    }

                    status.setImageResource(R.drawable.running48);

                } else {    // 완료
                    mBtnSettingsCatch.setVisibility(View.GONE);
                    mBtnSettingsCancel.setVisibility(View.GONE);
                    mBtnSettingsComplete.setVisibility(View.GONE);

                    status.setImageResource(0);
                }
                loadReply();
            } else {
                parent.setText("");
                factory.setText("");
                line.setText("");
                model.setText("");
                machine_no.setText("");
                mac_addr.setText("");
                date.setText("");
                time.setText("");
                content.setText("");
                status_msg.setText("");
                tv_reply_cnt.setVisibility(View.GONE);
                status.setImageResource(0);

                mBtnSettingsCatch.setVisibility(View.GONE);
                mBtnSettingsCancel.setVisibility(View.GONE);
                mBtnSettingsComplete.setVisibility(View.GONE);
            }
        } else {
            Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
            Log.e(TAG, result);
        }
    }

    public class DetailAsyncTask extends AsyncTask<String, Void, String> {

        ProgressDialog dialog = new ProgressDialog(DetailActivity.this);

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
            if (result != null) showData(result);
        }

        @Override
        protected String doInBackground(String... strings) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(strings[0])
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            HttpSuntechService service = retrofit.create(HttpSuntechService.class);

            try {
                final Call<RestPush> call = service.getData(Globals.GET_DATA, strings[1], strings[2]);
                Response<RestPush> response = call.execute();
                if (response.isSuccessful()) {
                    String code = response.body().getCode();
                    if (code.equals(Globals.STATUS_USING_OK)) {
                        listData = response.body().getItems();
                        if (listData.size() > 0) {
                            item = listData.get(0);
                        } else {
                            item = null;
                        }
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

    public class CatchAsyncTask extends AsyncTask<String, Void, String> {

        ProgressDialog dialog = new ProgressDialog(DetailActivity.this);

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
            if (result != null) showData(result);
        }

        @Override
        protected String doInBackground(String... strings) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(strings[0])
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            HttpSuntechService service = retrofit.create(HttpSuntechService.class);

            try {
                final Call<RestPush> call = service.setData(Globals.SET_DATA, strings[1], strings[2], strings[3]);
                Response<RestPush> response = call.execute();
                if (response.isSuccessful()) {
                    String code = response.body().getCode();
                    if (code.equals(Globals.STATUS_USING_OK)) {
                        listData = response.body().getItems();
                        if (listData.size() > 0) {
                            item = listData.get(0);
                        } else {
                            item = null;
                        }
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

    public void loadReply() {
        String device_id = sd.getString(Globals.PREFS_DEVICE_ID);
        String addr = sd.getString(Globals.PREFS_SERVER_ADDR);
        String port = sd.getString(Globals.PREFS_SERVER_PORT);

        if (port != null && !port.equals("") && !port.equals("80")) {
            addr = addr + ":" + port;
        }
        addr = "http://" + addr + "/";

        ReplyAsyncTask mytask = new ReplyAsyncTask();
        mytask.execute(addr, device_id, push_idx, "1");
    }

    public void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            // pre-condition
            return;
        }

        int count = listAdapter.getCount();
        int total = 0;
        int width = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.AT_MOST);
        for (int i = 0; i < count; i++) {
            View item = listAdapter.getView(i, null, listView);
            item.measure(0, 0);
            total += item.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = total + (listView.getDividerHeight() * (count - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }

    public class ReplyAsyncTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (result != null) {
                if (!result.equals(Globals.STATUS_USING_OK)) {
                    replyData.clear();
                    Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
                    Log.e(TAG, result);
                }
            }
            adapter.notifyDataSetChanged();
            setListViewHeightBasedOnChildren(myPushReply);
        }

        @Override
        protected String doInBackground(String... strings) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(strings[0])
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            HttpSuntechService service = retrofit.create(HttpSuntechService.class);

            try {
                final Call<RestReply> call = service.getReply(Globals.GET_REPLY, strings[1], strings[2], strings[3]);
                Response<RestReply> response = call.execute();
                if (response.isSuccessful()) {
                    String code = response.body().getCode();
                    if (code.equals(Globals.STATUS_USING_OK)) {
                        replyData = response.body().getItems();
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

    public class ReplyAdapter extends BaseAdapter {

        private ReplyItem item;

        @Override
        public int getCount() {
            return (replyData == null) ? 0 : replyData.size();
        }

        @Override
        public Object getItem(int position)
        {
            return replyData.get(position);
        }

        @Override
        public long getItemId(int position)
        {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            item = replyData.get(position);
            ReplyViewHolder holder;

            if (convertView == null) {
                convertView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.activity_detail_reply_row, parent, false);
                holder = new ReplyViewHolder();
                holder.worker = (TextView) convertView.findViewById(R.id.worker);
                holder.date = (TextView) convertView.findViewById(R.id.date);
                holder.time = (TextView) convertView.findViewById(R.id.time);
                holder.message = (TextView) convertView.findViewById(R.id.message);
                holder.status = (ImageView) convertView.findViewById(R.id.status);
                convertView.setTag(holder);
            } else {
                holder = (ReplyViewHolder) convertView.getTag();
            }

            if (item != null) {
                holder.worker.setText(item.getWorker());
                holder.date.setText(item.getReg_date());
                holder.time.setText(item.getReg_time());
                holder.message.setText(item.getStatus_msg());
                String status = item.getStatus();
                if (status.equals("W")) {
                    holder.status.setImageResource(R.drawable.warning32);
                } else if (status.equals("R")) {
                    holder.status.setImageResource(R.drawable.running32);
                } else {
                    holder.status.setImageResource(R.drawable.complete32);
                }
            }
            return convertView;
        }
    }

    private static class ReplyViewHolder {
        public TextView worker;
        public TextView date;
        public TextView time;
        public TextView message;
        public ImageView status;
    }

    public void goBack() {
        Intent i = new Intent();
        i.putExtra("list_pos", list_pos);
        i.putExtra("push_idx", push_idx);
        i.putExtra("reply_cnt", item.getReply_cnt());
        i.putExtra("changed_status", item.getStatus());
        setResult(RESULT_OK, i);
        finish();
    }
}
