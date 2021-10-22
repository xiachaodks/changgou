package com.changgou.search.dao;

import com.changgou.pojo.SkuInfo;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface SkuEsMapper extends ElasticsearchRepository<SkuInfo, Long> {
}

