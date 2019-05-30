package com.hamani.suntech.pushman;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.hamani.suntech.pushman.common.CheckCertification;
import com.hamani.suntech.pushman.common.DeviceUuidFactory;
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

public class NoticeActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "DetailAct";

    private SharedData sd;
    private CheckCertification cert;
    private NoticeAdapter adapter;

    Button mBtnSettingsBack;
    private SwipeRefreshLayout mSwipe;
    private ListView myPushNotice;
    private TextView mEmptyMessage;

    private ArrayList<NoticeItem> saveData;       // 임시 저장용
    private ArrayList<NoticeItem> listData;		// 실제 화면에 표시될 데이터

    private boolean lastItemVisible = false;    // 리스트뷰의 마지막 row 가 노출되었는지 확인
    private boolean loadingNow = false;         // 현재 데이터를 로딩중인지 확인
    private boolean end_of_data = false;        // 마지막 데이터까지 로딩이 끝났는지 확인
    private int current_list_page = 1;          // 노출페이지

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notice);

        setTitle(R.string.title_notice);

        sd = new SharedData(this);
        adapter = new NoticeAdapter();
        cert = new CheckCertification(getApplicationContext());
        saveData = new ArrayList<NoticeItem>();     // 임시저장용
        listData = new ArrayList<NoticeItem>();		// 댓글리스트저장

        mBtnSettingsBack = findViewById(R.id.btn_notice_back);
        mBtnSettingsBack.setOnClickListener(this);

        mEmptyMessage = findViewById(R.id.empty_notice_message);

        myPushNotice = findViewById(R.id.list_notice);
        myPushNotice.setAdapter(adapter);
        myPushNotice.setEmptyView(mEmptyMessage);

        myPushNotice.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                position -= myPushNotice.getHeaderViewsCount();
                Intent intent = new Intent(NoticeActivity.this, NoticeDetailActivity.class);
                intent.putExtra("pos", position);
                intent.putExtra("idx", listData.get(position).getIdx());
                startActivity(intent);
            }
        });

        myPushNotice.setOnScrollListener(new AbsListView.OnScrollListener() {
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

        mSwipe = (SwipeRefreshLayout) findViewById(R.id.swipe_notice_layout);
        mSwipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadData(1);
                mSwipe.setRefreshing(false);
            }
        });
        mSwipe.setOnChildScrollUpCallback(new SwipeRefreshLayout.OnChildScrollUpCallback() {
            @Override
            public boolean canChildScrollUp(@NonNull SwipeRefreshLayout parent, @Nullable View child) {
                return myPushNotice.canScrollVertically(-1);
            }
        });

        int status = checkCert();

        if (status != CheckCertification.NO_SERVER_ADDR) {
            loadData(1);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_notice_back:
                finish();
        }
    }

    public int checkCert() {
        int status = cert.check();

        if (status == CheckCertification.NO_DEVICE_ID) {
            DeviceUuidFactory factory = new DeviceUuidFactory(this);
            String device_id = factory.getDeviceUuid().toString();
            if (device_id != null) {
                status = CheckCertification.NO_SERVER_ADDR;
            }
        }
        return status;
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

        NoticeAsyncTask mytask = new NoticeAsyncTask();
        mytask.execute(addr, device_id, Integer.toString(page));
    }

    public class NoticeAsyncTask extends AsyncTask<String, Void, String> {
        ProgressDialog dialog = new ProgressDialog(NoticeActivity.this);

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
            if (!result.equals(Globals.STATUS_USING_OK)) {
                listData.clear();
            }
            adapter.notifyDataSetChanged();
            if (current_list_page==1) {
                myPushNotice.setSelection(0);
            }
            loadingNow = false;
        }

        @Override
        protected String doInBackground(String... strings) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(strings[0])
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            HttpSuntechService service = retrofit.create(HttpSuntechService.class);

            try {
                final Call<RestNotice> call = service.getNoticeList(Globals.GET_NOTICE_LIST, strings[1], strings[2]);
                Response<RestNotice> response = call.execute();
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

    public class NoticeAdapter extends BaseAdapter {

        private NoticeItem item;

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
            NoticeViewHolder holder;

            if (convertView == null) {
                convertView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.activity_notice_row, parent, false);
                holder = new NoticeViewHolder();
                holder.title = (TextView) convertView.findViewById(R.id.title);
                holder.text = (TextView) convertView.findViewById(R.id.text);
                holder.datetime = (TextView) convertView.findViewById(R.id.datetime);
                convertView.setTag(holder);
            } else {
                holder = (NoticeViewHolder) convertView.getTag();
            }

            if (item != null) {
                holder.title.setText(item.getTitle());
                holder.text.setText(item.getText());
                holder.datetime.setText(item.getReg_datetime());
            }
            return convertView;
        }
    }

    private static class NoticeViewHolder {
        public TextView title;
        public TextView text;
        public TextView datetime;
    }
}
