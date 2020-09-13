package com.qingcheng.service.order;

import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.order.Order;
import com.qingcheng.pojo.order.Orders;

import java.util.List;
import java.util.Map;

/**
 * order业务逻辑层
 */
public interface OrderService {


    public List<Order> findAll();


    public PageResult<Order> findPage(int page, int size);


    public List<Order> findList(Map<String,Object> searchMap);


    public PageResult<Order> findPage(Map<String,Object> searchMap, int page, int size);


    public Order findById(String id);

    public void add(Order order);


    public void update(Order order);


    public void delete(String id);

    /**
     * 根据id查找订单列表
     * @param id
     */
    public Orders findOrdersById(String id);

    /**
     * 批量发货
     * @param orders
     */
    public void batchSend(List<Order> orders);

    /**
     * 订单超时处理逻辑
     */
    public void orderTimeOutLogic();

    /**
     * 合并订单
     * @param order1
     * @param order2
     */
    public void merge(String order1,String order2);

    /**
     * 拆分订单
     * @param id
     * @param num
     */
    public void split(String id,String num);

}
