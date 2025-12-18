package com.hmdp.dto;

import lombok.Data;

import java.util.List;

/**
 * 滚动分页结果封装类
 * 用于Feed流的滚动分页查询
 *
 * 为什么要滚动分页？
 * - 普通分页（LIMIT offset, count）在新数据插入时会导致重复/遗漏
 * - 滚动分页基于时间戳，不受新数据影响
 *
 * @author Nisson
 */
@Data
public class ScrollResult {
    private List<?> list;     // 查询结果列表
    private Long minTime;     // 本次查询的最小时间戳（下次查询的max参数）
    private Integer offset;   // 偏移量（处理相同时间戳的情况）
}
