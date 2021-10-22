package com.changgou.search.service;

import java.util.Map;

public interface SkuService {


    /**
     * 导入数据到索引库中
     */
    void importSku();

    /***
     * 条件搜索
     * @param searchMap
     * @return
     */
    Map search(Map<String, String> searchMap);
}
