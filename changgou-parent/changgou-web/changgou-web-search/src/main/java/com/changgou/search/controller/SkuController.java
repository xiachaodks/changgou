package com.changgou.search.controller;

import com.changgou.search.feign.SkuFeign;
import com.changgou.pojo.SkuInfo;
import entity.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
 
@Controller
@RequestMapping("/search")
public class SkuController {

    @Autowired
    private SkuFeign skuFeign;

    @GetMapping("/list")
    public String search(@RequestParam(required = false) Map searchMap, Model model) {
        Map resultMap = skuFeign.search(searchMap);
        model.addAttribute("result", resultMap);

        //将条件存储，用于数据回显
        model.addAttribute("searchMap", searchMap);

        //计算分页
        Page<SkuInfo> pageInfo = new Page<>(
            Long.parseLong(resultMap.get("totalPages").toString()),
            Integer.parseInt(resultMap.get("pageNumber").toString())+1,
            Integer.parseInt(resultMap.get("pageSize").toString())
        );

        //存储分页信息
        model.addAttribute("pageInfo",pageInfo);

        //获取上次请求地址
        String url[] = getUrl(searchMap);
        model.addAttribute("url",url[0]);
        model.addAttribute("sortUrl",url[1]);
        return "search";
    }

    /**
     * URL组装
     *
     * @param searchMap
     * @return
     */
    public static String[] getUrl(Map searchMap) {
        String url = "/search/list";
        String sortUrl = "/search/list";
        if (searchMap != null && searchMap.size() > 0) {
            url += "?";
            sortUrl += "?";
            for (Object entry : searchMap.keySet()) {
                //key是搜索的条件对象
                String key = (String) entry;

                //跳过分页参数
                if(key.equalsIgnoreCase("pageNum")){
                    continue;
                }

                //value是搜索的值
                String value = (String) searchMap.get(entry);
                url += key + "=" + value + "&";

                //跳过排序参数
                if(key.equalsIgnoreCase("sortField")||key.equalsIgnoreCase("sortRule")){
                    continue;
                }

                sortUrl += key + "=" + value + "&";
            }
            //去掉最后一个&
            url = url.substring(0, url.length() - 1);
            sortUrl = sortUrl.substring(0, sortUrl.length() - 1);
        }

        return new String[]{url,sortUrl};
    }


}
