package com.hamani.suntech.pushman.data;

import java.util.ArrayList;

public class RestPush {
    String code;
    String msg;
    String total_cnt;
    String warning_cnt;
    String working_cnt;
    String complete_cnt;
    String end_of_data;
    ArrayList<PushItem> items;

    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }
    public void setMsg(String msg) {
        this.msg = msg;
    }

    public ArrayList<PushItem> getItems() {
        return items;
    }

    public void setItems(ArrayList<PushItem> items) {
        this.items = items;
    }

    public String getWarning_cnt() {
        return warning_cnt;
    }

    public void setWarning_cnt(String warning_cnt) {
        this.warning_cnt = warning_cnt;
    }

    public String getWorking_cnt() {
        return working_cnt;
    }

    public void setWorking_cnt(String working_cnt) {
        this.working_cnt = working_cnt;
    }

    public String getComplete_cnt() {
        return complete_cnt;
    }

    public void setComplete_cnt(String complete_cnt) {
        this.complete_cnt = complete_cnt;
    }

    public String getEnd_of_data() {
        return end_of_data;
    }

    public void setEnd_of_data(String end_of_data) {
        this.end_of_data = end_of_data;
    }

    public String getTotal_cnt() {
        return total_cnt;
    }

    public void setTotal_cnt(String total_cnt) {
        this.total_cnt = total_cnt;
    }
}
