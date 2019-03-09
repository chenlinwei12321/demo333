package com.shuwen.crawler.scheduler.web.filter;

/**
 * Created by gbshine on 2018/4/12.
 */
public class SsoResult<T> {
    private int code;
    private T data;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
