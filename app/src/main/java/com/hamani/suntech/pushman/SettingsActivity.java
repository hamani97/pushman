package com.hamani.suntech.pushman;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.hamani.suntech.pushman.common.Globals;
import com.hamani.suntech.pushman.common.HttpSuntechService;
import com.hamani.suntech.pushman.common.SharedData;
import com.hamani.suntech.pushman.data.RestData;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "SettingsAct";

    private SharedData sd;

    String addr;
    String name;
    String empno;
    String port;
    Boolean show_all_yn;

    private EditText editUserName;
    private EditText editUserEmpno;
    private EditText editServerAddr;
    private EditText editServerPort;

    private Switch sw_all_data;

    private TextView textVersionCode;

    LinearLayout mBtnNotice;
    Button mBtnSettingsSave;
    Button mBtnSettingsBack;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setTitle(R.string.title_settings);

        mBtnNotice = findViewById(R.id.btn_ll_notice);
        mBtnSettingsSave = findViewById(R.id.btn_settings_save);
        mBtnSettingsBack = findViewById(R.id.btn_settings_back);

        mBtnNotice.setOnClickListener(this);
        mBtnSettingsSave.setOnClickListener(this);
        mBtnSettingsBack.setOnClickListener(this);

        editUserName = findViewById(R.id.edit_user_name);
        editUserEmpno = findViewById(R.id.edit_user_empno);
        editServerAddr = findViewById(R.id.edit_server_ip);
        editServerPort = findViewById(R.id.edit_server_port);

        sw_all_data = findViewById(R.id.sw_all_data);

        textVersionCode = findViewById(R.id.text_version);

        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            textVersionCode.setText(pi.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        sd = new SharedData(getApplicationContext());

        editUserName.setText(sd.getString(Globals.PREFS_USER_NAME));
        editUserEmpno.setText(sd.getString(Globals.PREFS_USER_EMPNO));
        editServerAddr.setText(sd.getString(Globals.PREFS_SERVER_ADDR));
        editServerPort.setText(sd.getString(Globals.PREFS_SERVER_PORT));

        show_all_yn = sd.getBoolean(Globals.PREFS_ALL_DATA_YN);
        if (show_all_yn==null || show_all_yn==false) {
            show_all_yn = false;
            sw_all_data.setChecked(false);
        } else {
            sw_all_data.setChecked(true);
        }

        sw_all_data.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                show_all_yn = isChecked;
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_ll_notice:
                Intent intent = new Intent(SettingsActivity.this, NoticeActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_settings_save:
                saveData();
                break;
            case R.id.btn_settings_back:
                setResult(RESULT_CANCELED);
                finish();
                break;
        }
    }

    public void saveData() {

        String device_id = sd.getString(Globals.PREFS_DEVICE_ID);

        if (device_id == null || device_id.equals("")) {
            Toast.makeText(getApplicationContext(), "This device is not available. no device-id", Toast.LENGTH_LONG).show();
            return;
        }

        String token = sd.getString(Globals.PREFS_PUSH_TOKEN);
        if (token == null || token.equals("")) {
            //token = FirebaseInstanceId.getInstance().getToken();
            token = "";
        }

        addr = editServerAddr.getText().toString().trim();
        name = editUserName.getText().toString().trim();
        empno = editUserEmpno.getText().toString().trim();
        port = editServerPort.getText().toString().trim();

        String brand = Build.BRAND;
        String model = Build.MODEL;
        String version = Build.VERSION.RELEASE;

        if (addr == null || addr.equals("")) {
            Toast.makeText(getApplicationContext(), "The server address is not set.", Toast.LENGTH_LONG).show();
            return;
        }

        String addr2 = addr;

        if (!port.equals("") && !port.equals("80")) {
            addr2 = addr2 + ":" + port;
        }

        // 서버 주소 조립
        addr2 = "http://" + addr2 + "/";

        SaveAsyncTask task = new SaveAsyncTask();
        task.execute(addr2, device_id, token, brand, model, version, name, empno);
    }

    public class SaveAsyncTask extends AsyncTask<String, Void, String> {

        ProgressDialog dialog = new ProgressDialog(SettingsActivity.this);

        @Override
        protected void onPreExecute() {
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setMessage("Please wait...");
            dialog.show();
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String response_code) {
            super.onPostExecute(response_code);
            dialog.dismiss();

            if (response_code != null) {
                if (response_code.equals(Globals.STATUS_USING_OK)) {
                    // SD 에 저장
                    sd.setString(Globals.PREFS_USER_NAME, name);
                    sd.setString(Globals.PREFS_USER_EMPNO, empno);
                    sd.setString(Globals.PREFS_SERVER_ADDR, addr);
                    sd.setString(Globals.PREFS_SERVER_PORT, port);
                    sd.setBoolean(Globals.PREFS_ALL_DATA_YN, show_all_yn);

                    okBack();
                } else {
                    Toast.makeText(getApplicationContext(), response_code, Toast.LENGTH_LONG).show();
                    Log.e(TAG, response_code);
                }
            }
        }

        @Override
        protected String doInBackground(String... strings) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(strings[0])
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            HttpSuntechService service = retrofit.create(HttpSuntechService.class);

            try {
                final Call<RestData> call = service.addDevice(Globals.PUSH_USER_ADD, strings[1], strings[2], strings[3], strings[4], strings[5], strings[6], strings[7]);
                Response<RestData> response = call.execute();
                if (response.isSuccessful()) {
                    String response_code = response.body().getCode();
                    if (response_code.equals(Globals.STATUS_USING_OK) ||
                            response_code.equals(Globals.STATUS_NO_MEMBERS) ||
                            response_code.equals(Globals.STATUS_USING_WAIT) ||
                            response_code.equals(Globals.STATUS_USING_STOP) ||
                            response_code.equals(Globals.STATUS_USING_WAIT_CONNECT)) {
                        return Globals.STATUS_USING_OK;
                    } else {
                        return response.body().getMsg() + "(" + response_code + ")";
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

    public void okBack() {
        Intent i = new Intent();
        setResult(RESULT_OK, i);
        finish();
    }
}
