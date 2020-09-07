package com.qingcheng.service.impl;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingcheng.dao.*;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.goods.*;
import com.qingcheng.service.goods.SpuService;
import com.qingcheng.util.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.*;

@Service(interfaceClass = SpuService.class)
public class SpuServiceImpl implements SpuService {

    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private CategoryBrandMapper categoryBrandMapper;

    @Autowired
    private AuditLogMapper auditLogMapper;

    /**
     * 返回全部记录
     * @return
     */
    public List<Spu> findAll() {
        return spuMapper.selectAll();
    }

    /**
     * 分页查询
     * @param page 页码
     * @param size 每页记录数
     * @return 分页结果
     */
    public PageResult<Spu> findPage(int page, int size) {
        PageHelper.startPage(page,size);
        Page<Spu> spus = (Page<Spu>) spuMapper.selectAll();
        return new PageResult<Spu>(spus.getTotal(),spus.getResult());
    }

    /**
     * 条件查询
     * @param searchMap 查询条件
     * @return
     */
    public List<Spu> findList(Map<String, Object> searchMap) {
        Example example = createExample(searchMap);
        return spuMapper.selectByExample(example);
    }

    /**
     * 分页+条件查询
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    public PageResult<Spu> findPage(Map<String, Object> searchMap, int page, int size) {
        PageHelper.startPage(page,size);
        Example example = createExample(searchMap);
        Page<Spu> spus = (Page<Spu>) spuMapper.selectByExample(example);
        return new PageResult<Spu>(spus.getTotal(),spus.getResult());
    }

    /**
     * 根据Id查询
     * @param id
     * @return
     */
    public Spu findById(String id) {
        return spuMapper.selectByPrimaryKey(id);
    }

    /**
     * 新增
     * @param spu
     */
    public void add(Spu spu) {
        spuMapper.insert(spu);
    }

    /**
     * 修改
     * @param spu
     */
    public void update(Spu spu) {
        spuMapper.updateByPrimaryKeySelective(spu);
    }

    /**
     *  删除
     * @param id
     */
    public void delete(String id) {
        spuMapper.deleteByPrimaryKey(id);
    }

    /**
     * 保存商品
     * @param goods
     */

    @Transactional
    @Override
    public void saveGoods(Goods goods) {

        //保存一个spu的信息
        //获取要保存的spu信息
        Spu spu = goods.getSpu();

        if (spu.getId() == null){//新增商品
            //用雪花算法生成id，便于分片管理
            spu.setId(idWorker.nextId()+"");
            //持久化到数据库
            spuMapper.insert(spu);
            //获取当前时间
        }else {//修改商品
            //删除之前的sku列表
            Example example = new Example(Sku.class);
            Example.Criteria criteria = example.createCriteria();
            criteria.andEqualTo("spuId",spu.getId());
            skuMapper.deleteByExample(example);
            //执行spu修改
            spuMapper.updateByPrimaryKeySelective(spu);

        }

        Date date = new Date();
        //查分类对象
        Category category = categoryMapper.selectByPrimaryKey(spu.getCategory3Id());

        //保存sku列表的信息
        //获取sku的全部信息
        List<Sku> skuList = goods.getSkuList();
        //循环读取sku的信息并插入数据库
        for (Sku sku : skuList){
            if (sku.getId() == null){//新增情况
                //雪花算法得到id
                sku.setId(idWorker.nextId()+"");
                sku.setCreateTime(date);//创建日期
            }

            //设置spuid
            sku.setSpuId(spu.getId());
            //sku名称=spu名称+规格值列表（空格分隔）
            String name = spu.getName();
            //判断商品是否有规格参数
            if (sku.getSpec() == null || "".equals(sku.getSpec())){
                sku.setSpec("{}");
            }
            //将取到的字符串转成map
            Map<String,String> specMap = JSON.parseObject(sku.getSpec(), Map.class);
            for (String value:specMap.values()){
                name +=" " + value;
            }
            //设置名称
            sku.setName(name);
            sku.setUpdateTime(date);//修改日期
            sku.setCategoryId(spu.getCategory3Id());//分类id
            sku.setCategoryName(category.getName());//分类名称
            sku.setCommentNum(0);//评论数初始值设置为0
            sku.setSaleNum(0);//销售数量初始值设为0

            //持久化到数据库
            skuMapper.insert(sku);
        }

        //建立分类和品牌的关联
        CategoryBrand categoryBrand = new CategoryBrand();
        categoryBrand.setCategoryId(spu.getCategory3Id());
        categoryBrand.setBrandId(spu.getBrandId());
        int count = categoryBrandMapper.selectCount(categoryBrand);
        //如果为0则说明之前没有这个品牌和分类的商品，添加数据，如存在，则不需要往关联表中添加
        if (count == 0){
            categoryBrandMapper.insert(categoryBrand);
        }

    }

    @Override
    public Goods findGoodsById(String id) {
        //查询spu
        Spu spu = spuMapper.selectByPrimaryKey(id);
        //查询sku
        Example example = new Example(Sku.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("spuId",id);
        List<Sku> skuList = skuMapper.selectByExample(example);
        //封装为组合实体类
        Goods goods = new Goods();
        goods.setSpu(spu);
        goods.setSkuList(skuList);
        return goods;

    }

    /**
     * 商品审核
     * @param id
     * @param status
     * @param message
     */
    @Override
    @Transactional
    public void audit(String id, String status, String message) {
        //根据id获取spu
        Spu spu = spuMapper.selectByPrimaryKey(id);
        spu.setStatus(status);
        if ("1".equals(status)){//审核通过的设置上下架状态
            spu.setIsMarketable("1");//审核通过自动上架
        }
        spuMapper.updateByPrimaryKey(spu);

        //记录商品审核记录
        AuditLog auditLog = new AuditLog();
        auditLog.setId(String.valueOf(idWorker.nextId()));
        auditLog.setName(spu.getName());
        auditLog.setStatus(status);
        auditLog.setAuditTime(new Date());
        auditLog.setAuditAdmin("lhz");
        auditLog.setAuditMessage(message);

        //插入数据库
        auditLogMapper.insert(auditLog);


    }

    /**
     * 商品下架
     * @param id
     */
    @Override
    public void pull(String id) {
        //修改状态
        Spu spu = new Spu();
        spu.setId(id);
        spu.setIsMarketable("0");
        spuMapper.updateByPrimaryKeySelective(spu);
        //记录商品信息


    }

    /**
     * 商品上架
     * @param id
     */
    @Override
    public void put(String id) {
        //修改状态
        Spu spu = spuMapper.selectByPrimaryKey(id);
        if ( !"1".equals(spu.getStatus()) ){
            throw new RuntimeException("此商品未通过审核!");
        }
        spu.setIsMarketable("1");
        spuMapper.updateByPrimaryKeySelective(spu);

        //记录商品日志


    }

    /**
     * 批量上架
     * @param ids
     * @return
     */
    @Override
    public int putMany(String[] ids) {

        //修改状态
        Spu spu = new Spu();
        spu.setIsMarketable("1");

        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andIn("id", Arrays.asList(ids));
        criteria.andEqualTo("isMarketabel","0");//下架的
        criteria.andEqualTo("status","1");//审核通过的
        return spuMapper.updateByExampleSelective(spu,example);

    }

    @Override
    public void restoreGoods(String id) {
        Spu spu = spuMapper.selectByPrimaryKey(id);
        //将是否是删除状态设置为0
        spu.setIsDelete("0");
        spuMapper.updateByPrimaryKeySelective(spu);
    }

    @Override
    public void deleteGoods(String id) {
        Spu spu = spuMapper.selectByPrimaryKey(id);
        //将是否是删除状态设置为0
        spu.setIsDelete("1");
        spuMapper.updateByPrimaryKeySelective(spu);
    }

    @Override
    public List<Goods> findDeleteGoods() {
        ArrayList<Goods> goods = new ArrayList<>();

        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("isDelete","1");

        List<Spu> spus = spuMapper.selectByExample(example);
        for (Spu spu : spus) {
            Goods goods1 = new Goods();
            goods1.setSpu(spu);

            List<Sku> skus = skuMapper.selectByExample(spu.getId());
            goods1.setSkuList(skus);

            goods.add(goods1);
        }
        return goods;
    }

    /**
     * 构建查询条件
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap){
        Example example=new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        if(searchMap!=null){
            // 主键
            if(searchMap.get("id")!=null && !"".equals(searchMap.get("id"))){
                criteria.andLike("id","%"+searchMap.get("id")+"%");
            }
            // 货号
            if(searchMap.get("sn")!=null && !"".equals(searchMap.get("sn"))){
                criteria.andLike("sn","%"+searchMap.get("sn")+"%");
            }
            // SPU名
            if(searchMap.get("name")!=null && !"".equals(searchMap.get("name"))){
                criteria.andLike("name","%"+searchMap.get("name")+"%");
            }
            // 副标题
            if(searchMap.get("caption")!=null && !"".equals(searchMap.get("caption"))){
                criteria.andLike("caption","%"+searchMap.get("caption")+"%");
            }
            // 图片
            if(searchMap.get("image")!=null && !"".equals(searchMap.get("image"))){
                criteria.andLike("image","%"+searchMap.get("image")+"%");
            }
            // 图片列表
            if(searchMap.get("images")!=null && !"".equals(searchMap.get("images"))){
                criteria.andLike("images","%"+searchMap.get("images")+"%");
            }
            // 售后服务
            if(searchMap.get("saleService")!=null && !"".equals(searchMap.get("saleService"))){
                criteria.andLike("saleService","%"+searchMap.get("saleService")+"%");
            }
            // 介绍
            if(searchMap.get("introduction")!=null && !"".equals(searchMap.get("introduction"))){
                criteria.andLike("introduction","%"+searchMap.get("introduction")+"%");
            }
            // 规格列表
            if(searchMap.get("specItems")!=null && !"".equals(searchMap.get("specItems"))){
                criteria.andLike("specItems","%"+searchMap.get("specItems")+"%");
            }
            // 参数列表
            if(searchMap.get("paraItems")!=null && !"".equals(searchMap.get("paraItems"))){
                criteria.andLike("paraItems","%"+searchMap.get("paraItems")+"%");
            }
            // 是否上架
            if(searchMap.get("isMarketable")!=null && !"".equals(searchMap.get("isMarketable"))){
                criteria.andLike("isMarketable","%"+searchMap.get("isMarketable")+"%");
            }
            // 是否启用规格
            if(searchMap.get("isEnableSpec")!=null && !"".equals(searchMap.get("isEnableSpec"))){
                criteria.andLike("isEnableSpec","%"+searchMap.get("isEnableSpec")+"%");
            }
            // 是否删除
            if(searchMap.get("isDelete")!=null && !"".equals(searchMap.get("isDelete"))){
                criteria.andLike("isDelete","%"+searchMap.get("isDelete")+"%");
            }
            // 审核状态
            if(searchMap.get("status")!=null && !"".equals(searchMap.get("status"))){
                criteria.andLike("status","%"+searchMap.get("status")+"%");
            }

            // 品牌ID
            if(searchMap.get("brandId")!=null ){
                criteria.andEqualTo("brandId",searchMap.get("brandId"));
            }
            // 一级分类
            if(searchMap.get("category1Id")!=null ){
                criteria.andEqualTo("category1Id",searchMap.get("category1Id"));
            }
            // 二级分类
            if(searchMap.get("category2Id")!=null ){
                criteria.andEqualTo("category2Id",searchMap.get("category2Id"));
            }
            // 三级分类
            if(searchMap.get("category3Id")!=null ){
                criteria.andEqualTo("category3Id",searchMap.get("category3Id"));
            }
            // 模板ID
            if(searchMap.get("templateId")!=null ){
                criteria.andEqualTo("templateId",searchMap.get("templateId"));
            }
            // 运费模板id
            if(searchMap.get("freightId")!=null ){
                criteria.andEqualTo("freightId",searchMap.get("freightId"));
            }
            // 销量
            if(searchMap.get("saleNum")!=null ){
                criteria.andEqualTo("saleNum",searchMap.get("saleNum"));
            }
            // 评论数
            if(searchMap.get("commentNum")!=null ){
                criteria.andEqualTo("commentNum",searchMap.get("commentNum"));
            }

        }
        return example;
    }

}
