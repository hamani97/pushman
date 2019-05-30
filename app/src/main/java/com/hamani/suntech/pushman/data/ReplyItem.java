package com.hamani.suntech.pushman.data;

public class ReplyItem {
    String idx;
    String worker_idx;
    String worker;          // 작업자
    String status;          // W:작업취소, R:작업잡음, Y:완료
    String status_msg;
    String reg_date;
    String reg_time;

    public String getIdx() {
        return idx;
    }

    public void setIdx(String idx) {
        this.idx = idx;
    }

    public String getWorker_idx() {
        return worker_idx;
    }

    public void setWorker_idx(String worker_idx) {
        this.worker_idx = worker_idx;
    }

    public String getWorker() {
        return worker;
    }

    public void setWorker(String worker) {
        this.worker = worker;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus_msg() {
        return status_msg;
    }

    public void setStatus_msg(String status_msg) {
        this.status_msg = status_msg;
    }

    public String getReg_date() {
        return reg_date;
    }

    public void setReg_date(String reg_date) {
        this.reg_date = reg_date;
    }

    public String getReg_time() {
        return reg_time;
    }

    public void setReg_time(String reg_time) {
        this.reg_time = reg_time;
    }
}
