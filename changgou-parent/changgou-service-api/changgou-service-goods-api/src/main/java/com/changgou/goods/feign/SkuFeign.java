package com.changgou.goods.feign;

import entity.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import com.changgou.pojo.Sku;

import java.util.List;

@FeignClient(name = "goods")
@RequestMapping(value = "/sku")
public interface SkuFeign {

    /***
     * 查询Sku全部数据
     * @return
     */
    @GetMapping
    public Result<List<Sku>> findAll();
}
