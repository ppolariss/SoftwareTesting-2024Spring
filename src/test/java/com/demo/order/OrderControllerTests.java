package com.demo.order;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.demo.controller.user.OrderController;
import com.demo.entity.Order;
import com.demo.entity.User;
import com.demo.entity.Venue;
import com.demo.entity.vo.OrderVo;
import com.demo.exception.LoginException;
import com.demo.service.OrderService;
import com.demo.service.OrderVoService;
import com.demo.service.VenueService;
import org.aspectj.weaver.ast.Or;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.NestedServletException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@WebMvcTest(OrderController.class)
public class OrderControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private OrderVoService orderVoService; // 使用 @MockBean 注解模拟 OrderVoService 类

    @MockBean
    private VenueService venueService; // 使用 @MockBean 注解模拟 OrderVoService 类

    @Test
    public void testOrderManage() throws Exception {
        //mock two types of user
        int userManyOrders = 27;
        int userNoOrders = 19;

        //mock 27's orders
        Order mockOrder1 = new Order(29, "yonghu", 16, 2,
                LocalDateTime.of(2020, 1, 2, 18, 16, 8),
                LocalDateTime.of(2020, 1, 24, 11, 0, 0), 3, 1500);
        Order mockOrder2 = new Order(30, "yonghu",17,2,
                LocalDateTime.of(2020,1,2,18,16,21),
                LocalDateTime.of(2020,1,25,11,0,0),3,900);
        List<Order> mockOrderList =  new ArrayList<>();
        mockOrderList.add(mockOrder1);
        mockOrderList.add(mockOrder2);

        //mock 19's orders
        List<Order> mockEmpty =  new ArrayList<>();

        // mock page
        Page<Order> pageOfManyOrders = new PageImpl<>(mockOrderList);
        Page<Order> pageOfEmptyOrders = new PageImpl<>(mockEmpty);

        //mock service
        Pageable order_pageable = PageRequest.of(0,5, Sort.by("orderTime").descending());
        when(orderService.findUserOrder(String.valueOf(userManyOrders), order_pageable)).thenReturn(pageOfManyOrders);
        when(orderService.findUserOrder(String.valueOf(userNoOrders),order_pageable)).thenReturn(pageOfEmptyOrders);

        // test user have many orders
        User user = new User();
        user.setUserID("27");
        mockMvc.perform(get("/order_manage").sessionAttr("user",user))
                .andExpect(status().isOk())
                .andExpect(view().name("order_manage"))
                .andExpect(model().attribute("total", pageOfManyOrders.getTotalPages()));

        // test user have no orders
        user.setUserID("19");
        mockMvc.perform(get("/order_manage").sessionAttr("user",user))
                .andExpect(status().isOk())
                .andExpect(view().name("order_manage"))
                .andExpect(model().attribute("total", pageOfEmptyOrders.getTotalPages()));

        // no test for error userid -- this method based on login

        // test no login
        NestedServletException exception = assertThrows(NestedServletException.class, () -> mockMvc.perform(get("/order_manage")));
        assertTrue(exception.getRootCause() instanceof LoginException);

    }

    @Test
    public void testOrderPlace() throws  Exception {
        // choose id
        int normalVId = 1;
        int emptyVId = 2;

        // mock service
        Venue mockVenue = new Venue(1,"name","description"
        ,200,"","address","09:00","20:00");
        Venue mockEmpty = new Venue();
        when(venueService.findByVenueID(normalVId)).thenReturn(mockVenue);
        when(venueService.findByVenueID(emptyVId)).thenReturn(mockEmpty);

        // test normal id
        mockMvc.perform(get("/order_place.do").param("venueID",String.valueOf(normalVId)))
                .andExpect(status().isOk())
                .andExpect(view().name("order_place"))
                .andExpect(model().attribute("venue",mockVenue));

        // test error id
        mockMvc.perform(get("/order_place.do").param("venueID",String.valueOf(emptyVId)))
                .andExpect(status().isOk())
                .andExpect(view().name("order_place"))
                .andExpect(model().attribute("venue",mockEmpty));

        // test error param
        mockMvc.perform(get("/order_place.do").param("venueID","nct127"))
                .andExpect(status().isBadRequest());

        // test empty param
        mockMvc.perform(get("/order_place.do").param("venueID",""))
                .andExpect(status().isBadRequest());

        //test order_place no param
        mockMvc.perform(get("/order_place"))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetOrderListWithoutLogin() throws Exception {
        NestedServletException exception = assertThrows(NestedServletException.class, () -> mockMvc.perform(get("/getOrderList.do")));
        assertTrue(exception.getRootCause() instanceof LoginException);
    }

    @Test
    public void testGetOrderListWithErrorPageMin() throws Exception {
        // bug here
        // 没有做-1输入的处理，导致PageRequest.of()失败
        mockMvc.perform(get("/getOrderList.do").param("page", "-1").sessionAttr("user", new User()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetOrderListWithErrorPageMax() throws Exception {
        // bug here
        // 没有做page越界处理，导致null对象调用
        User user = new User();
        user.setUserID("19");

        Pageable order_pageable = PageRequest.of(5-1,5, Sort.by("orderTime").descending());
        when(orderService.findUserOrder(user.getUserID(), order_pageable)).thenReturn(null);

        mockMvc.perform(get("/getOrderList.do").param("page","5").sessionAttr("user",user))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    public void testGetOrderListWithValidtPage() throws Exception{
        // mock orders
        List<Order> mockOrderList = new ArrayList<>();
        mockOrderList.add(new Order());
        Page<Order> page = new PageImpl<>(mockOrderList);

        // mock page
        Page<Order> pageOfManyOrders = new PageImpl<>(mockOrderList);

        //mock service
        int pageNormal = 0;
        Pageable orderPageableNormal = PageRequest.of(pageNormal,5, Sort.by("orderTime").descending());
        when(orderService.findUserOrder("1", orderPageableNormal)).thenReturn(pageOfManyOrders);

        // test
        User user = new User();
        user.setUserID("1");
        mockMvc.perform(get("/getOrderList.do").sessionAttr("user",user))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    public void testAddOrderWithoutLogin() throws Exception {
        //代码 date有问题！！
        mockMvc.perform(post("/addOrder.do")
                        .param("venueName", "Venue1")
                        .param("date", "2024-03-30")
                        .param("startTime", "10:00")
                        .param("hours", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof NestedServletException))
                .andExpect(result -> assertTrue(result.getResolvedException().getCause() instanceof LoginException))
                .andExpect(result -> assertEquals("请登录！", result.getResolvedException().getCause().getMessage()));
    }

    @Test
    public void testFinishOrderWithValidID() throws Exception {
        int orderID = 1;
        doNothing().when(orderService).finishOrder(orderID);
        mockMvc.perform(post("/finishOrder.do").param("orderID", String.valueOf(orderID)))
                .andExpect(status().isOk());
        verify(orderService).finishOrder(orderID);
    }

    @Test
    public void testFinishOrderWithInvalidOrderID() throws Exception {
        int orderID = -1;
        doNothing().when(orderService).finishOrder(orderID);
        mockMvc.perform(post("/finishOrder.do").param("orderID", String.valueOf(orderID)))
                .andExpect(status().isOk());
        verify(orderService).finishOrder(orderID);
    }

    @Test
    public void testModifyOrderWithValidID() throws Exception {
        int validId = 1;
        // mock orders
        Order mockOrder = new Order();
        mockOrder.setOrderID(validId);
        mockOrder.setVenueID(1);
        Venue mockVenue = new Venue();
        mockVenue.setVenueID(1);

        //mock service
        when(orderService.findById(validId)).thenReturn(mockOrder);
        when(venueService.findByVenueID(mockOrder.getVenueID())).thenReturn(mockVenue);

        // test
        mockMvc.perform(get("/modifyOrder.do").param("orderID",String.valueOf(validId)))
                .andExpect(status().isOk())
                .andExpect(view().name("order_edit"))
                .andExpect(model().attribute("order",mockOrder))
                .andExpect(model().attribute("venue",mockVenue));
    }

    @Test
    public void testModifyOrderWithInvalidID() throws Exception {
        // bug here
        // 程序中没有处理非法order id情况，可能导致null调用后续的函数
        when(orderService.findById(-1)).thenReturn(null);
        mockMvc.perform(get("/modifyOrder.do").param("orderID","-1"))
                .andExpect(status().isNotFound());
    }
}
