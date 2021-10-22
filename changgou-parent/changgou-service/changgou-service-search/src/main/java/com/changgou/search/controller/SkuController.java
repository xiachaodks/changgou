package com.changgou.search.controller;

import entity.Result;
import entity.StatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.changgou.search.service.SkuService;

import java.util.Map;

@RestController
@RequestMapping(value = "/search")
@CrossOrigin
public class SkuController {

    @Autowired
    private SkuService skuService;

    /**
     * 数据导入
     * @return
     */
    @GetMapping("/import")
    public Result importSku() {
        skuService.importSku();
        return new Result(true, StatusCode.OK, "导入数据到索引库中成功！");
    }

    /**
     * 搜索
     * @param searchMap
     * @return
     */
    @GetMapping
    public Map search(@RequestParam(required = false) Map<String, String> searchMap){
        return  skuService.search(searchMap);
    }

}
