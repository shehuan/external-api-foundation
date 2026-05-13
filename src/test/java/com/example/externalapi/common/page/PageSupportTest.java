package com.example.externalapi.common.page;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 验证项目分页模型和 MyBatis-Plus 分页对象之间的转换规则。
 */
class PageSupportTest {

    @Test
    void pageQueryShouldConvertToMybatisPage() {
        PageQuery query = new PageQuery();
        query.setPageNo(3);
        query.setPageSize(20);

        Page<String> page = query.toMybatisPage();

        assertEquals(3, page.getCurrent());
        assertEquals(20, page.getSize());
    }

    @Test
    void pageUtilsShouldConvertQueryToMybatisPage() {
        PageQuery query = new PageQuery();
        query.setPageNo(2);
        query.setPageSize(50);

        Page<String> page = PageUtils.toMybatisPage(query);

        assertEquals(2, page.getCurrent());
        assertEquals(50, page.getSize());
    }

    @Test
    void pageResultShouldCalculatePages() {
        PageResult<String> result = PageResult.of(List.of("a", "b"), 2, 10, 21);

        assertEquals(List.of("a", "b"), result.records());
        assertEquals(2, result.pageNo());
        assertEquals(10, result.pageSize());
        assertEquals(21, result.total());
        assertEquals(3, result.pages());
    }

    @Test
    void pageResultShouldMapFromMybatisPage() {
        Page<Integer> page = new Page<>(1, 2);
        page.setRecords(List.of(1, 2));
        page.setTotal(5);

        PageResult<String> result = PageResult.from(page, value -> "item-" + value);

        assertEquals(List.of("item-1", "item-2"), result.records());
        assertEquals(1, result.pageNo());
        assertEquals(2, result.pageSize());
        assertEquals(5, result.total());
        assertEquals(3, result.pages());
    }
}
