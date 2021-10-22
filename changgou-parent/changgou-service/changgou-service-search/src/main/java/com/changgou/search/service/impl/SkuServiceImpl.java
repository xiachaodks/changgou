package com.changgou.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.changgou.goods.feign.SkuFeign;
import com.changgou.search.dao.SkuEsMapper;
import com.changgou.pojo.SkuInfo;
import com.changgou.search.service.SkuService;
import entity.Result;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.changgou.pojo.Sku;

import java.util.*;

@Service
public class SkuServiceImpl implements SkuService {

    @Autowired
   private SkuFeign skuFeign;

    @Autowired
    private SkuEsMapper skuEsMapper;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    /**
     * 导入数据进索引库
     */
    @Override
    public void importSku() {
        //Feign调用，查询List<Sku>
        Result<List<Sku>> skuResult = skuFeign.findAll();
        //将List<Sku>转换为List<SkuInfo>
        List<SkuInfo> skuInfoList = JSON.parseArray(JSON.toJSONString(skuResult.getData()), SkuInfo.class);

        //循环当前SkuInfoList
        for (SkuInfo skuInfo : skuInfoList) {
            //获取Spec->Map(String)->Map类型
            String spec = skuInfo.getSpec();
            Map specMap = JSON.parseObject(spec, Map.class);
            //生成动态域,只需将该域存入一个Map<String,Object>对象中即可,该Map<String,Object>的key会自动生成一个域
            skuInfo.setSpecMap(specMap);
        }
        //调用Dao实现数据批量导入
        skuEsMapper.saveAll(skuInfoList);
    }

    /**
     * 多条件搜索
     *
     * @param searchMap
     * @return
     */
    @Override
    public Map search(Map<String, String> searchMap) {
        //搜索条件构建
        NativeSearchQueryBuilder nativeSearchQueryBuilder = buildBasicQuery(searchMap);

        //集合搜索查询
        HashMap<String, Object> resultMap = searchList(nativeSearchQueryBuilder);

        /*//分类分组查询实现
        List<String> categoryList = getStringsCategoryList(nativeSearchQueryBuilder);
        resultMap.put("categoryList", categoryList);

        //品牌分组查询实现
        List<String> brandList = getStringsBrandList(nativeSearchQueryBuilder);
        resultMap.put("brandList", brandList);

        //规格查询
        Map<String, Set<String>> specList = getStringsSpecList(nativeSearchQueryBuilder);
        resultMap.put("specList", specList);*/

        //分组搜索实现
        Map<String, Object> groupMap = searchGroupList(nativeSearchQueryBuilder, searchMap);
        resultMap.putAll(groupMap);
        return resultMap;
    }


    /**
     * 搜索条件构建
     *
     * @param searchMap
     * @return
     */
    private NativeSearchQueryBuilder buildBasicQuery(Map<String, String> searchMap) {
        //构建搜索对象
        //NativeSearchQueryBuilder:搜索条件构建对象
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();

        //组合查询对象
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        String keywords = "";
        if (searchMap != null && searchMap.size() > 0) {
            //根据关键词搜索
            keywords = searchMap.get("keywords");
            if (!StringUtils.isEmpty(keywords)) {
                //如果关键词不为空，搜索
                //nativeSearchQueryBuilder.withQuery(QueryBuilders.queryStringQuery(keywords).field("name"));
                boolQueryBuilder.must(QueryBuilders.queryStringQuery(keywords).field("name"));
            }

            //输入了分类 -->category
            if (!StringUtils.isEmpty(searchMap.get("category"))) {
                boolQueryBuilder.must(QueryBuilders.termQuery("categoryName", searchMap.get("category")));
            }

            //输入了品牌 -->brand
            if (!StringUtils.isEmpty(searchMap.get("brand"))) {
                boolQueryBuilder.must(QueryBuilders.termQuery("brandName", searchMap.get("brand")));
            }

            //规格过滤查询 spec_机身内存=16G&spec_网络=联通3G&spec_颜色=白
            for (Map.Entry<String, String> entry : searchMap.entrySet()) {
                String key = entry.getKey();
                //以spec_开头的，表示规格筛选查询
                if (key.startsWith("spec_")) {
                    //规格查询条件
                    String value = entry.getValue();
                    //spec_网络-->网络
                    boolQueryBuilder.must(QueryBuilders.termQuery("specMap." + key.substring(5) + ".keyword", value));
                }
            }

            //价格区间过滤查询
            //price: 0-500元 500-1000元 1000-2000元 2000-3000元 3000-4000元 4000元以上
            String price = searchMap.get("price");
            if (!StringUtils.isEmpty(price)) {
                //去掉中文元和以上  0-500 500-1000 1000-2000 2000-3000 3000-4000 4000
                price = price.replace("元", "").replace("以上", "");
                //根据-分割 [0,500] [500,1000] [1000,2000] [2000,3000] [3000,4000] [4000]
                String[] prices = price.split("-");
                if (prices != null && prices.length > 0) {
                    //prices[0]!=null price>prices[0]
                    //拼接条件
                    boolQueryBuilder.must(QueryBuilders.rangeQuery("price").gt(Integer.parseInt(prices[0])));
                    //prices[1]!=null price<=prices[0]
                    if (prices.length == 2) {
                        //price<=prices[0]
                        boolQueryBuilder.must(QueryBuilders.rangeQuery("price").lte(Integer.parseInt(prices[1])));
                    }
                }
            }

            //构建分页查询
            //不传分页参数，默认第一页
            Integer pageNum = 1;
            Integer pageSize = 20;//默认查询的数据条数
            if (!StringUtils.isEmpty(searchMap.get("pageNum"))) {
                try {
                    if (pageNum <= 0)
                        pageNum = 1;
                    pageNum = Integer.parseInt(searchMap.get("pageNum"));
                } catch (Exception e) {
                    e.printStackTrace();
                    pageNum = 1;
                }
            }
            nativeSearchQueryBuilder.withPageable(PageRequest.of(pageNum - 1, pageSize));

            //构建排序查询
            String sortField = searchMap.get("sortField");//指定排序的域
            String sortRule = searchMap.get("sortRule");//指定排序的规则 ASC/DESC
            if (!StringUtils.isEmpty(sortField) && !StringUtils.isEmpty(sortRule)) {
                nativeSearchQueryBuilder.withSort(SortBuilders.fieldSort(sortField)
                        .order(SortOrder.valueOf(sortRule)));
            }
        }
        if (searchMap == null || searchMap.size() == 0){
            searchMap.put("keywords","");
            boolQueryBuilder.must(QueryBuilders.queryStringQuery(keywords).field("name"));
        }

        //将boolQueryBuilder填充给nativeSearchQueryBuilder
        nativeSearchQueryBuilder.withQuery(boolQueryBuilder);
        return nativeSearchQueryBuilder;
    }

    /**
     * 分组查询:分类分组、品牌分组、规格分组
     *
     * @param nativeSearchQueryBuilder
     * @return
     */
    private Map<String, Object> searchGroupList(NativeSearchQueryBuilder nativeSearchQueryBuilder, Map<String, String> searchMap) {
        /**
         * 查询分组集合
         * .addAggregation():添加聚合查询操作
         * .terms():取别名
         *.field():根据那个域分组
         */
        //设置分组条件
        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("skuCategoryGroup").field("categoryName").size(50));

        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("skuBrandGroup").field("brandName").size(50));

        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("skuSpecGroup").field("spec.keyword").size(50));

        AggregatedPage<SkuInfo> aggregatedPage = elasticsearchTemplate.queryForPage(nativeSearchQueryBuilder.build(), SkuInfo.class);

        //获取分组数据
        //定义一个Map，存储所有分组结果

        Map<String, Object> groupResultMap = new HashMap<>();

        if (searchMap==null||StringUtils.isEmpty(searchMap.get("category"))) {
            StringTerms categoryTerms = aggregatedPage.getAggregations().get("skuCategoryGroup");
            //获取分类分组集合数据
            List<String> categoryList = getGroupList(categoryTerms);
            groupResultMap.put("categoryList", categoryList);
        }
        if (searchMap==null||StringUtils.isEmpty(searchMap.get("brand"))) {
            StringTerms brandTerms = aggregatedPage.getAggregations().get("skuBrandGroup");
            //获取品牌分组集合数据
            List<String> brandList = getGroupList(brandTerms);
            groupResultMap.put("brandList", brandList);
        }
        StringTerms specTerms = aggregatedPage.getAggregations().get("skuSpecGroup");
        //获取规格分组集合数据
        List<String> specList = getGroupList(specTerms);
        Map<String, Set<String>> specMap = putAllSpec(specList);
        groupResultMap.put("specList", specMap);

        return groupResultMap;
    }

    /**
     * 获取分组集合数据
     *
     * @param stringTerms
     * @return
     */
    private List<String> getGroupList(StringTerms stringTerms) {
        List<String> groupList = new ArrayList<String>();
        for (StringTerms.Bucket bucket : stringTerms.getBuckets()) {
            String fieldName = bucket.getKeyAsString();//其中的一个域名字
            groupList.add(fieldName);
        }
        return groupList;
    }

    /**
     * 集合搜索查询
     *
     * @param nativeSearchQueryBuilder
     * @return
     */
    private HashMap<String, Object> searchList(NativeSearchQueryBuilder nativeSearchQueryBuilder) {
        //高亮配置
        HighlightBuilder.Field field = new HighlightBuilder.Field("name");//指定高亮域
        //前缀 <span style="color:red;">
        field.preTags("<span style=\"color:red;\">");
        //后缀 </span>
        field.postTags("</span>");
        //添加高亮
        nativeSearchQueryBuilder.withHighlightFields(field);


        //执行搜索
        //skuInfos:搜索结果集的封装
        //AggregatedPage<SkuInfo> skuInfos = elasticsearchTemplate.queryForPage(nativeSearchQueryBuilder.build(), SkuInfo.class);

        //添加高亮
        AggregatedPage<SkuInfo> skuInfos = elasticsearchTemplate.queryForPage(
                nativeSearchQueryBuilder.build(),//搜索条件
                SkuInfo.class,                  //转换的类型字节码
                new SearchResultMapper() {//将搜索到的数据封装到该对象中
                    @Override
                    public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
                        //存储所有转换后的高亮数据对象
                        List<T> list = new ArrayList<>();
                        //执行查询，获取所有数据[高亮数据和非高亮数据]
                        for (SearchHit hit : response.getHits()) {
                            //分析结果集，获取非高亮数据
                            SkuInfo skuInfo = JSON.parseObject(hit.getSourceAsString(), SkuInfo.class);

                            //分析结果集，获取高亮数据-->只有某个域的高亮数据
                            HighlightField highlightField = hit.getHighlightFields().get("name");

                            if (highlightField != null && highlightField.getFragments() != null) {
                                //读取高亮数据
                                Text[] fragments = highlightField.getFragments();
                                StringBuffer buffer = new StringBuffer();
                                for (Text fragment : fragments) {
                                    buffer.append(fragment.toString());
                                }
                                //将非高亮数据替换成高亮数据
                                skuInfo.setName(buffer.toString());
                            }
                            list.add((T) skuInfo);
                        }
                        //将数据返回
                        /**
                         * 搜索的集合数据(携带高亮)：List<T> content
                         * 分页对象信息：Pageable pageable
                         * 搜索记录的总条数：long total
                         */
                        return new AggregatedPageImpl<T>(list, pageable, response.getHits().getTotalHits());
                    }
                });


        //获取数据结果集
        List<SkuInfo> contents = skuInfos.getContent();
        //分页参数总记录数
        long totalElements = skuInfos.getTotalElements();
        Pageable pageable = skuInfos.getPageable();
        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();
        //总页数
        int totalPages = skuInfos.getTotalPages();

        //封装一个Map存储所有返回数据
        HashMap<String, Object> resultMap = new HashMap<String, Object>();

        //分页数据
        resultMap.put("pageNumber",pageNumber);
        resultMap.put("pageSize",pageSize);

        resultMap.put("rows", contents);
        resultMap.put("totalElements", totalElements);
        resultMap.put("totalPages", totalPages);
        return resultMap;
    }

    /**
     * 分类分组查询
     *
     * @param nativeSearchQueryBuilder
     * @return
     */
    private List<String> getStringsCategoryList(NativeSearchQueryBuilder nativeSearchQueryBuilder) {
        /**
         * 分组查询分类集合
         * .addAggregation():添加聚合查询操作
         * .terms():取别名
         *.field():根据那个域分组
         */

        //设置分组条件  商品分类
        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("skuCategoryGroup").field("categoryName").size(50));

        AggregatedPage<SkuInfo> aggregatedPage = elasticsearchTemplate.queryForPage(nativeSearchQueryBuilder.build(), SkuInfo.class);

        //获取分组数据
        StringTerms stringTerms = aggregatedPage.getAggregations().get("skuCategoryGroup");

        List<String> categoryList = new ArrayList<String>();
        for (StringTerms.Bucket bucket : stringTerms.getBuckets()) {
            String categoryName = bucket.getKeyAsString();//其中的一个分类名字
            categoryList.add(categoryName);
        }
        return categoryList;
    }

    /**
     * 品牌分组查询
     *
     * @param nativeSearchQueryBuilder
     * @return
     */
    private List<String> getStringsBrandList(NativeSearchQueryBuilder nativeSearchQueryBuilder) {

        /**
         * 品牌查询分类集合
         * .addAggregation():添加聚合查询操作
         * .terms():取别名
         *.field():根据那个域分组
         */

        //设置分组条件  商品品牌
        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("skuBrandGroup").field("brandName").size(50));

        AggregatedPage<SkuInfo> aggregatedPage = elasticsearchTemplate.queryForPage(nativeSearchQueryBuilder.build(), SkuInfo.class);

        //获取分组数据
        StringTerms stringTerms = aggregatedPage.getAggregations().get("skuBrandGroup");

        List<String> brandList = new ArrayList<String>();
        for (StringTerms.Bucket bucket : stringTerms.getBuckets()) {
            String brandName = bucket.getKeyAsString();//其中的一个品牌名字
            brandList.add(brandName);
        }
        return brandList;
    }

    /**
     * 规格分组查询
     *
     * @param nativeSearchQueryBuilder
     * @return
     */
    private Map<String, Set<String>> getStringsSpecList(NativeSearchQueryBuilder nativeSearchQueryBuilder) {

        /**
         * 规格查询分类集合
         * .addAggregation():添加聚合查询操作
         * .terms():取别名
         *.field():根据那个域分组
         */

        //设置分组条件  商品规格     keyword:不分词
        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("skuSpecGroup").field("spec.keyword").size(50));

        AggregatedPage<SkuInfo> aggregatedPage = elasticsearchTemplate.queryForPage(nativeSearchQueryBuilder.build(), SkuInfo.class);

        //获取分组数据
        StringTerms stringTerms = aggregatedPage.getAggregations().get("skuSpecGroup");

        List<String> specList = new ArrayList<String>();
        for (StringTerms.Bucket bucket : stringTerms.getBuckets()) {
            String specName = bucket.getKeyAsString();
            specList.add(specName);
        }

        Map<String, Set<String>> allSpecMap = putAllSpec(specList);

        return allSpecMap;
    }

    /**
     * 合并specList
     *
     * @param specList
     * @return
     */
    private Map<String, Set<String>> putAllSpec(List<String> specList) {
        Map<String, Set<String>> allSpecMap = new HashMap<>();
        //循环specList
        for (String spec : specList) {
            //将每个JSON字符串转为Map集合
            Map<String, String> specMap = JSON.parseObject(spec, Map.class);
            //将每个Map对象转换为Map<String,Set<String>>对象
            for (Map.Entry<String, String> entry : specMap.entrySet()) {
                String key = entry.getKey();//规格名字
                String value = entry.getValue();//规格值
                //根据key从allSpecMap中获取specSet
                Set<String> specSet = allSpecMap.get(key);
                if (specSet == null) {
                    //之前allSpecMap中没有该规格
                    specSet = new HashSet<>();
                }
                //将当前规格加入到Set集合中
                specSet.add(value);
                //将数据存入到allSpecMap中
                allSpecMap.put(key, specSet);
            }
        }
        return allSpecMap;
    }

}
