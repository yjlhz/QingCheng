package com.qingcheng.service.goods;

import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.goods.Brand;

import java.util.List;
import java.util.Map;

public interface BrandService {
    public List<Brand> findAll();
    public PageResult<Brand> findPage(int page, int size);
    public List<Brand> findList(Map<String,Object> searchMap);
    public PageResult<Brand> findPage(Map<String,Object> searchMap,int page, int size);
    public Brand findById(Integer id);
    public void add(Brand brand);
}
