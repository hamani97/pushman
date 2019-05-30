package com.hamani.suntech.pushman;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.hamani.suntech.pushman.common.Globals;
import com.hamani.suntech.pushman.common.HttpSuntechService;
import com.hamani.suntech.pushman.common.SharedData;
import com.hamani.suntech.pushman.data.NoticeItem;
import com.hamani.suntech.pushman.data.RestNotice;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NoticeDetailActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "DetailAct";

    Button mBtnSettingsBack;

    private SharedData sd;
    private int list_pos;    // 리스트에서 선택한 위치값
    private String push_idx;    // 리스트에서 선택한 값의 DB idx 값
    private ArrayList<NoticeItem> listData;   // 본문로딩을 위한 임시변수
    private NoticeItem item;		// 실제 화면에 표시될 데이터

    private SwipeRefreshLayout mSwipe;

    private TextView title;
    private TextView text;
    private TextView regdate;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notice_detail);

        setTitle(R.string.title_notice);

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

        listData  = new ArrayList<NoticeItem>();

        title = findViewById(R.id.txt_notice_title);
        text = findViewById(R.id.txt_notice_text);
        regdate = findViewById(R.id.txt_notice_regdate);

        mBtnSettingsBack = findViewById(R.id.btn_notice_detail_back);
        mBtnSettingsBack.setOnClickListener(this);

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
            case R.id.btn_notice_detail_back:
                finish();
        }
    }

    public void loadData() {
        String device_id = sd.getString(Globals.PREFS_DEVICE_ID);
        String addr = sd.getString(Globals.PREFS_SERVER_ADDR);
        String port = sd.getString(Globals.PREFS_SERVER_PORT);

        if (port != null && !port.equals("") && !port.equals("80")) {
            addr = addr + ":" + port;
        }
        addr = "http://" + addr + "/";

        NoticeDetailAsyncTask task = new NoticeDetailAsyncTask();
        task.execute(addr, device_id, push_idx);
    }

    public void showData(String result) {
        if (result.equals(Globals.STATUS_USING_OK)) {
            if (item != null) {
                title.setText(item.getTitle());
                text.setText(item.getText());
                regdate.setText(item.getReg_datetime());
            } else {
                title.setText("");
                text.setText("");
                regdate.setText("");
            }
        } else {
            Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
            Log.e(TAG, result);
        }
    }

    public class NoticeDetailAsyncTask extends AsyncTask<String, Void, String> {

        ProgressDialog dialog = new ProgressDialog(NoticeDetailActivity.this);

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
                final Call<RestNotice> call = service.getNoticeData(Globals.GET_NOTICE_DATA, strings[1], strings[2]);
                Response<RestNotice> response = call.execute();
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
}
