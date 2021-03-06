package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingcheng.dao.*;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.order.*;
import com.qingcheng.service.order.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;
import com.qingcheng.util.IdWorker;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service(interfaceClass = OrderService.class)
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private OrderLogMapper orderLogMapper;

    @Autowired
    private ReturnOrderMapper returnOrderMapper;

    @Autowired
    private OrderConfigMapper orderConfigMapper;

    @Autowired
    private IdWorker idWorker;


    /**
     * 返回全部记录
     * @return
     */
    public List<Order> findAll() {
        return orderMapper.selectAll();
    }

    /**
     * 分页查询
     * @param page 页码
     * @param size 每页记录数
     * @return 分页结果
     */
    public PageResult<Order> findPage(int page, int size) {
        PageHelper.startPage(page,size);
        Page<Order> orders = (Page<Order>) orderMapper.selectAll();
        return new PageResult<Order>(orders.getTotal(),orders.getResult());
    }

    /**
     * 条件查询
     * @param searchMap 查询条件
     * @return
     */
    public List<Order> findList(Map<String, Object> searchMap) {
        Example example = createExample(searchMap);
        return orderMapper.selectByExample(example);
    }

    /**
     * 分页+条件查询
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    public PageResult<Order> findPage(Map<String, Object> searchMap, int page, int size) {
        PageHelper.startPage(page,size);
        Example example = createExample(searchMap);
        Page<Order> orders = (Page<Order>) orderMapper.selectByExample(example);
        return new PageResult<Order>(orders.getTotal(),orders.getResult());
    }

    /**
     * 根据Id查询
     * @param id
     * @return
     */
    public Order findById(String id) {
        return orderMapper.selectByPrimaryKey(id);
    }

    /**
     * 新增
     * @param order
     */
    public void add(Order order) {
        orderMapper.insert(order);
    }

    /**
     * 修改
     * @param order
     */
    public void update(Order order) {
        orderMapper.updateByPrimaryKeySelective(order);
    }

    /**
     *  删除
     * @param id
     */
    public void delete(String id) {
        orderMapper.deleteByPrimaryKey(id);
    }

    /**
     * 根据id查找订单列表
     * @param id
     */
    @Override
    @Transactional
    public Orders findOrdersById(String id) {
        //新建order对象
        Orders orders = new Orders();
        //根据id查找对应的订单
        Order order = orderMapper.selectByPrimaryKey(id);
        //将订单放到orders中
        orders.setOrder(order);
        //作条件查询，新建example
        Example example = new Example(OrderItem.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("orderId",id);
        //查询出该id订单下的订单详细
        List<OrderItem> orderItemList = orderItemMapper.selectByExample(example);
        orders.setOrderItemList(orderItemList);
        //返回包含订单和订单详细的orders
        return orders;
    }

    /**
     * 批量发货
     * @param orders
     */
    @Override
    @Transactional
    public void batchSend(List<Order> orders) {
        IdWorker idWorker = new IdWorker();

        Date date = new Date();
        for (Order order : orders) {
            //判断物流单号和物流公司是否为空
            if (order.getShippingName()==null){
                //如果为空则抛出异常
                throw  new RuntimeException("请选择物流公司!!");
            }

            if (order.getShippingCode()==null){
                throw new RuntimeException("物流单号不能为空!!");
            }
        }

        for (Order order : orders) {
            //如果都不为空 则更新订单状态、发货状态、发货时间,添加物流公司和物流单号
            order.setConsignTime(date);//发货时间
            order.setConsignStatus("1");//发货状态
            order.setOrderStatus("2");//订单状态
            orderMapper.updateByPrimaryKey(order);

            //记录订单日志
            OrderLog orderLog = new OrderLog();
            orderLog.setId(String.valueOf(idWorker.nextId()));      //分布式id
            orderLog.setOperater("yjlhz");  //操作员
            orderLog.setOperateTime(date);      //操作时间
            orderLog.setOrderId(Long.valueOf(order.getId())); //订单号
            orderLog.setOrderStatus("2");       //订单状态
            orderLog.setPayStatus("1");     //付款状态
            orderLog.setConsignStatus("2");    //发货状态
            orderLog.setRemarks("该订单已完成");    //备注

            orderLogMapper.insert(orderLog);
        }

    }

    @Override
    @Transactional
    public void orderTimeOutLogic() {
        //订单超时未付款自动关闭
        //查询超时时间
        OrderConfig orderConfig = orderConfigMapper.selectByPrimaryKey(1);
        Integer orderTimeout = orderConfig.getOrderTimeout();//超时时间,分钟
        LocalDateTime localTime = LocalDateTime.now().minusMinutes(orderTimeout);//得到超时时间点
        //条件查询
        Example example = new Example(Order.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andLessThan("createTime",localTime);//创建时间小于超时时间证明已超时
        criteria.andEqualTo("orderStatus","0");//0代表未付款的
        criteria.andEqualTo("isDelete","0");//0代表未删除的
        List<Order> orders = orderMapper.selectByExample(example);
        //处理查询出来的超时订单
        for (Order order : orders){
            OrderLog orderLog = new OrderLog();
            orderLog.setId(idWorker.nextId()+"");
            orderLog.setOperater("system");//系统自动操作
            orderLog.setOperateTime(new Date());//系统自动操作的时间
            orderLog.setOrderStatus("4");
            orderLog.setPayStatus(order.getConsignStatus());
            orderLog.setRemarks("订单超时，自动关闭!");
            orderLog.setOrderId(Long.valueOf(order.getId()));
            orderLogMapper.insert(orderLog);//保存到数据库订单日志中
            order.setOrderStatus("4");
            order.setCloseTime(new Date());//关闭日期
            orderMapper.updateByPrimaryKeySelective(order);//更新到数据库

        }

    }

    /**
     * 合并订单
     * @param order1
     * @param order2
     */
    @Override
    @Transactional
    public void merge(String order1, String order2) {
        Order majorOrder = orderMapper.selectByPrimaryKey(order1);//根据id查询用户选择的主订单
        Order subordinateOrder = orderMapper.selectByPrimaryKey(order2);//根据id查询用户选择的从订单
        majorOrder.setTotalMoney(majorOrder.getTotalMoney() + subordinateOrder.getTotalMoney());//将总金额相加
        majorOrder.setTotalNum(majorOrder.getTotalNum() + subordinateOrder.getTotalNum());//将数量相加
        majorOrder.setPayMoney(majorOrder.getPreMoney() + subordinateOrder.getPreMoney());
        orderMapper.updateByPrimaryKeySelective(majorOrder);
        //做条件查询
        Example example = new Example(OrderItem.class);
        Example.Criteria criteria = example.createCriteria();
        //将从订单的orderItem查询出来
        criteria.andEqualTo("orderId",order2);
        List<OrderItem> orderItems = orderItemMapper.selectByExample(example);
        //将从订单的orderItem的orderId设置成主订单id
        for (OrderItem orderItem : orderItems){
            orderItem.setOrderId(order1);
            orderItemMapper.updateByPrimaryKeySelective(orderItem);
        }
        //将从订单逻辑删除
        subordinateOrder.setIsDelete("1");
        orderMapper.updateByPrimaryKeySelective(subordinateOrder);
    }

    /**
     * 拆分订单
     * @param id
     * @param num
     */
    @Override
    public void split(String id, String num) {
        //条件查询
        Example example = new Example(OrderItem.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("orderId",id);
        int count = orderItemMapper.selectCountByExample(example);
        if (count <= Integer.valueOf(num)){
            throw new RuntimeException("拆分数量要小于订单详细的数量!");
        }
        Order newOrder = new Order();//新建一个订单，作为拆分出来的新订单
        newOrder.setId(idWorker.nextId()+"");//雪花算法设置新订单id

        List<OrderItem> orderItems = orderItemMapper.selectByExample(example);//查询详细订单列表
        OrderItem[] orderItems1 = (OrderItem[])orderItems.toArray();
        //将i个放到新订单中
        for (int i = 0; i < Integer.valueOf(num);i++){
            //更新orderId
            orderItems1[i].setOrderId(newOrder.getId());
            orderItemMapper.updateByPrimaryKeySelective(orderItems1[i]);
        }
        orderMapper.insert(newOrder);

    }


    /**
     * 构建查询条件
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap){
        Example example=new Example(Order.class);
        Example.Criteria criteria = example.createCriteria();
        if(searchMap!=null){
            //根据id数组查询
            if (searchMap.get("ids") != null){
                criteria.andIn("id", Arrays.asList((String[])searchMap.get("ids")));
            }
            // 订单id
            if(searchMap.get("id")!=null && !"".equals(searchMap.get("id"))){
                criteria.andLike("id","%"+searchMap.get("id")+"%");
            }
            // 支付类型，1、在线支付、0 货到付款
            if(searchMap.get("payType")!=null && !"".equals(searchMap.get("payType"))){
                criteria.andLike("payType","%"+searchMap.get("payType")+"%");
            }
            // 物流名称
            if(searchMap.get("shippingName")!=null && !"".equals(searchMap.get("shippingName"))){
                criteria.andLike("shippingName","%"+searchMap.get("shippingName")+"%");
            }
            // 物流单号
            if(searchMap.get("shippingCode")!=null && !"".equals(searchMap.get("shippingCode"))){
                criteria.andLike("shippingCode","%"+searchMap.get("shippingCode")+"%");
            }
            // 用户名称
            if(searchMap.get("username")!=null && !"".equals(searchMap.get("username"))){
                criteria.andLike("username","%"+searchMap.get("username")+"%");
            }
            // 买家留言
            if(searchMap.get("buyerMessage")!=null && !"".equals(searchMap.get("buyerMessage"))){
                criteria.andLike("buyerMessage","%"+searchMap.get("buyerMessage")+"%");
            }
            // 是否评价
            if(searchMap.get("buyerRate")!=null && !"".equals(searchMap.get("buyerRate"))){
                criteria.andLike("buyerRate","%"+searchMap.get("buyerRate")+"%");
            }
            // 收货人
            if(searchMap.get("receiverContact")!=null && !"".equals(searchMap.get("receiverContact"))){
                criteria.andLike("receiverContact","%"+searchMap.get("receiverContact")+"%");
            }
            // 收货人手机
            if(searchMap.get("receiverMobile")!=null && !"".equals(searchMap.get("receiverMobile"))){
                criteria.andLike("receiverMobile","%"+searchMap.get("receiverMobile")+"%");
            }
            // 收货人地址
            if(searchMap.get("receiverAddress")!=null && !"".equals(searchMap.get("receiverAddress"))){
                criteria.andLike("receiverAddress","%"+searchMap.get("receiverAddress")+"%");
            }
            // 订单来源：1:web，2：app，3：微信公众号，4：微信小程序  5 H5手机页面
            if(searchMap.get("sourceType")!=null && !"".equals(searchMap.get("sourceType"))){
                criteria.andLike("sourceType","%"+searchMap.get("sourceType")+"%");
            }
            // 交易流水号
            if(searchMap.get("transactionId")!=null && !"".equals(searchMap.get("transactionId"))){
                criteria.andLike("transactionId","%"+searchMap.get("transactionId")+"%");
            }
            // 订单状态
            if(searchMap.get("orderStatus")!=null && !"".equals(searchMap.get("orderStatus"))){
                criteria.andLike("orderStatus","%"+searchMap.get("orderStatus")+"%");
            }
            // 支付状态
            if(searchMap.get("payStatus")!=null && !"".equals(searchMap.get("payStatus"))){
                criteria.andLike("payStatus","%"+searchMap.get("payStatus")+"%");
            }
            // 发货状态
            if(searchMap.get("consignStatus")!=null && !"".equals(searchMap.get("consignStatus"))){
                criteria.andLike("consignStatus","%"+searchMap.get("consignStatus")+"%");
            }
            // 是否删除
            if(searchMap.get("isDelete")!=null && !"".equals(searchMap.get("isDelete"))){
                criteria.andLike("isDelete","%"+searchMap.get("isDelete")+"%");
            }

            // 数量合计
            if(searchMap.get("totalNum")!=null ){
                criteria.andEqualTo("totalNum",searchMap.get("totalNum"));
            }
            // 金额合计
            if(searchMap.get("totalMoney")!=null ){
                criteria.andEqualTo("totalMoney",searchMap.get("totalMoney"));
            }
            // 优惠金额
            if(searchMap.get("preMoney")!=null ){
                criteria.andEqualTo("preMoney",searchMap.get("preMoney"));
            }
            // 邮费
            if(searchMap.get("postFee")!=null ){
                criteria.andEqualTo("postFee",searchMap.get("postFee"));
            }
            // 实付金额
            if(searchMap.get("payMoney")!=null ){
                criteria.andEqualTo("payMoney",searchMap.get("payMoney"));
            }

        }
        return example;
    }

}
