package com.example.externalapi.common.page;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 统一分页请求基类。
 *
 * <p>业务分页查询 DTO 可以继承该类，让接口请求参数保持扁平结构，例如：</p>
 * <pre>
 * {
 *   "pageNo": 1,
 *   "pageSize": 10,
 *   "status": 1
 * }
 * </pre>
 *
 * <p>Controller 层接收 PageQuery 子类，Service/Mapper 内部再转换成 MyBatis-Plus 的 Page。</p>
 */
public class PageQuery {

    /**
     * 页码从 1 开始，符合大多数外部接口调用习惯。
     */
    @Min(value = 1, message = "pageNo must be greater than or equal to 1")
    private long pageNo = 1;

    /**
     * 单页数量设置上限，避免调用方一次查询过多数据影响数据库和接口稳定性。
     */
    @Min(value = 1, message = "pageSize must be greater than or equal to 1")
    @Max(value = 500, message = "pageSize must be less than or equal to 500")
    private long pageSize = 10;

    public long getPageNo() {
        return pageNo;
    }

    public void setPageNo(long pageNo) {
        this.pageNo = pageNo;
    }

    public long getPageSize() {
        return pageSize;
    }

    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * 转换为 MyBatis-Plus 分页对象。
     *
     * <p>该转换只应在 Service/Mapper 内部使用，不建议把 MyBatis-Plus Page 暴露到 Controller 返回值。</p>
     */
    public <T> Page<T> toMybatisPage() {
        return new Page<>(pageNo, pageSize);
    }
}
