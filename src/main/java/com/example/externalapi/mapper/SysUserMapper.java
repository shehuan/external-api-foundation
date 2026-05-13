package com.example.externalapi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.externalapi.entity.SysUserEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 用户信息 Mapper 接口。
 * </p>
 *
 * @author codex
 * @since 2026-05-07
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUserEntity> {

}
