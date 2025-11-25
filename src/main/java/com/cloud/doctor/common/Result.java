package com.cloud.doctor.common;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code; // 200:成功, 500:失败
    private String msg;   // 提示信息
    private T data;       // 具体的 VO 对象

    // 1. 成功，带数据 (查到了数据)
    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.msg = "操作成功";
        r.data = data;
        return r;
    }

    // 2. 成功，不带数据 (注册/修改成功) -> 这就是你问的“没有返回”
    public static <T> Result<T> success() {
        Result<T> r = new Result<>();
        r.code = 200;
        r.msg = "操作成功";
        r.data = null;
        return r;
    }

    // 3. 失败 (系统报错)
    public static <T> Result<T> error(String msg) {
        Result<T> r = new Result<>();
        r.code = 500;
        r.msg = msg;
        return r;
    }
}
