package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 统一响应结果封装类
 * 前后端交互的统一数据格式
 *
 * 字段说明：
 * - success：请求是否成功
 * - errorMsg：错误信息（失败时返回）
 * - data：响应数据（成功时返回）
 * - total：总数（分页查询时返回）
 *
 * @author Nisson
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result {
    private Boolean success;
    private String errorMsg;
    private Object data;
    private Long total;

    /**
     * 成功响应（无数据）
     */
    public static Result ok(){
        return new Result(true, null, null, null);
    }

    /**
     * 成功响应（带数据）
     */
    public static Result ok(Object data){
        return new Result(true, null, data, null);
    }

    /**
     * 成功响应（分页数据）
     */
    public static Result ok(List<?> data, Long total){
        return new Result(true, null, data, total);
    }

    /**
     * 失败响应
     */
    public static Result fail(String errorMsg){
        return new Result(false, errorMsg, null, null);
    }
}
