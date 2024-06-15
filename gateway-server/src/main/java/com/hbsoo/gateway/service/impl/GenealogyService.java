package com.hbsoo.gateway.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hbsoo.database.config.DataSource;
import com.hbsoo.gateway.entity.Genealogy;
import com.hbsoo.gateway.mapper.GenealogyMapper;
import com.hbsoo.gateway.service.IGenealogyService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * Created by zun.wei on 2024/6/15.
 */
@Service
public class GenealogyService extends ServiceImpl<GenealogyMapper, Genealogy> implements IGenealogyService {

    @Resource
    GenealogyMapper genealogyMapper;


    @Override
    @DataSource(dsName = "SLAVE")
    public List<Genealogy> listAll() {
        return list();
    }

}
