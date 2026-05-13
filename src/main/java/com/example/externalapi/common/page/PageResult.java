package com.example.externalapi.common.page;

import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.List;
import java.util.function.Function;

/**
 * 统一分页响应模型。
 *
 * <p>对外接口只返回该模型，不直接暴露 MyBatis-Plus 的 IPage/Page，避免 ORM 框架结构泄漏到接口协议。</p>
 */
public record PageResult<T>(
        List<T> records,
        long pageNo,
        long pageSize,
        long total,
        long pages
) {

    /**
     * 根据基础分页字段创建响应对象。
     *
     * <p>适合不依赖 MyBatis-Plus IPage 的场景。</p>
     */
    public static <T> PageResult<T> of(List<T> records, long pageNo, long pageSize, long total) {
        long pages = pageSize <= 0 ? 0 : (total + pageSize - 1) / pageSize;
        return new PageResult<>(records, pageNo, pageSize, total, pages);
    }

    /**
     * 从 MyBatis-Plus IPage 直接转换。
     *
     * <p>当 records 已经是对外 DTO 时可以使用该方法。</p>
     */
    public static <T> PageResult<T> from(IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal(), page.getPages());
    }

    /**
     * 从 MyBatis-Plus IPage 转换，并将 Entity 映射成 DTO。
     *
     * <p>推荐 Controller 对外返回 DTO，不直接返回 Entity。</p>
     */
    public static <S, T> PageResult<T> from(IPage<S> page, Function<S, T> mapper) {
        List<T> records = page.getRecords().stream().map(mapper).toList();
        return new PageResult<>(records, page.getCurrent(), page.getSize(), page.getTotal(), page.getPages());
    }
}
