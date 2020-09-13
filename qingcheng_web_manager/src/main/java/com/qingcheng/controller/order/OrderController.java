package com.qingcheng.controller.order;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.entity.PageResult;
import com.qingcheng.entity.Result;
import com.qingcheng.pojo.order.Order;
import com.qingcheng.pojo.order.Orders;
import com.qingcheng.service.order.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Reference
    private OrderService orderService;

    @GetMapping("/findAll")
    public List<Order> findAll(){
        return orderService.findAll();
    }

    @GetMapping("/findPage")
    public PageResult<Order> findPage(int page, int size){
        return orderService.findPage(page, size);
    }

    @PostMapping("/findList")
    public List<Order> findList(@RequestBody Map<String,Object> searchMap){
        return orderService.findList(searchMap);
    }

    @PostMapping("/findPage")
    public PageResult<Order> findPage(@RequestBody Map<String,Object> searchMap, int page, int size){
        return  orderService.findPage(searchMap,page,size);
    }

    @GetMapping("/findById")
    public Order findById(String id){
        return orderService.findById(id);
    }


    @PostMapping("/add")
    public Result add(@RequestBody Order order){
        orderService.add(order);
        return new Result();
    }

    @PostMapping("/update")
    public Result update(@RequestBody Order order){
        orderService.update(order);
        return new Result();
    }

    @GetMapping("/delete")
    public Result delete(String id){
        orderService.delete(id);
        return new Result();
    }

    @GetMapping("/findOrdersById")
    public Orders findOrdersById(String id){
        return orderService.findOrdersById(id);
    }

    @PostMapping("/batchSend")
    public Result batchSend(@RequestBody List<Order> orders){
        orderService.batchSend(orders);
        return new Result();
    }

    @GetMapping("/merge")
    public Result merge(String order1,String order2){
        orderService.merge(order1,order2);
        return new Result();
    }

    @PostMapping("/split")
    public Result split(@RequestBody Map<String,String> map){
        orderService.split(map.get("id"),map.get("num"));
        return new Result();
    }


}
