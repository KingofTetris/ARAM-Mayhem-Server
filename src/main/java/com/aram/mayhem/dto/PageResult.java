package com.aram.mayhem.dto;

import lombok.Data;

import java.util.List;

/**
 * 通用分页结果包装类
 *
 * 数据流向：Service → Controller → 前端
 * 用途：统一所有分页接口的返回格式
 *
 * @param <T> 分页数据类型
 */
@Data
public class PageResult<T> {

    /** 总记录数 */
    private long total;

    /** 当前页码（从1开始） */
    private int page;

    /** 每页数量 */
    private int size;

    /** 当前页数据列表 */
    private List<T> records;

    /**
     * 构造分页结果
     *
     * @param total   总记录数
     * @param page    当前页码
     * @param size    每页数量
     * @param records 当前页数据列表
     */
    public PageResult(long total, int page, int size, List<T> records) {
        this.total = total;
        this.page = page;
        this.size = size;
        this.records = records;
    }
}
