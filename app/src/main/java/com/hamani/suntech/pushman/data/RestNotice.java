package com.hamani.suntech.pushman.data;

import java.util.ArrayList;

public class RestNotice {
    String code;
    String msg;
    String end_of_data;
    ArrayList<NoticeItem> items;

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

    public String getEnd_of_data() {
        return end_of_data;
    }

    public void setEnd_of_data(String end_of_data) {
        this.end_of_data = end_of_data;
    }

    public ArrayList<NoticeItem> getItems() {
        return items;
    }

    public void setItems(ArrayList<NoticeItem> items) {
        this.items = items;
    }
}
