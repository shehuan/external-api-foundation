package com.example.externalapi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.externalapi.entity.DemoOrderEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DemoOrderMapper extends BaseMapper<DemoOrderEntity> {
}
