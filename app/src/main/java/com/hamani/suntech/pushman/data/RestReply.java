package com.hamani.suntech.pushman.data;

import java.util.ArrayList;

public class RestReply {
    String code;
    String msg;
    ArrayList<ReplyItem> items;

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

    public ArrayList<ReplyItem> getItems() {
        return items;
    }

    public void setItems(ArrayList<ReplyItem> items) {
        this.items = items;
    }
}
