package com.example.externalapi.common.page;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 分页工具类。
 *
 * <p>保留该工具类是为了给不方便继承 PageQuery 的场景使用，例如内部任务或组合查询对象。</p>
 */
public final class PageUtils {

    private PageUtils() {
    }

    public static <T> Page<T> toMybatisPage(PageQuery query) {
        return new Page<>(query.getPageNo(), query.getPageSize());
    }
}
