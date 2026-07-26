package com.yinyu.common.result;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.util.List;
import java.util.function.Function;

/**
 * 统一分页响应体（对齐 api.md 1.3）
 */
@Data
public class PageResult<T> {

    private long pageNum;
    private long pageSize;
    private long total;
    private long pages;
    private List<T> list;

    public static <T> PageResult<T> of(long pageNum, long pageSize, long total, List<T> list) {
        PageResult<T> r = new PageResult<>();
        r.pageNum = pageNum;
        r.pageSize = pageSize;
        r.total = total;
        r.pages = pageSize == 0 ? 0 : (total + pageSize - 1) / pageSize;
        r.list = list;
        return r;
    }

    /** 由 MyBatis-Plus Page 转换，可选做元素映射 */
    public static <E, T> PageResult<T> from(Page<E> page, Function<List<E>, List<T>> mapper) {
        return of(page.getCurrent(), page.getSize(), page.getTotal(), mapper.apply(page.getRecords()));
    }
}
