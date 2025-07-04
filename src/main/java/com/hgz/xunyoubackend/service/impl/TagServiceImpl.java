package com.hgz.xunyoubackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hgz.xunyoubackend.model.domain.Tag;
import com.hgz.xunyoubackend.mapper.TagMapper;
import com.hgz.xunyoubackend.service.TagService;
import org.springframework.stereotype.Service;

/**
* @author 32438
* @description 针对表【tag(标签)】的数据库操作Service实现
* @createDate 2025-07-05 06:45:39
*/
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService{

}




