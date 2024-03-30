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
import com.demo.entity.vo.VenueOrder;
import com.demo.exception.LoginException;
import com.demo.service.OrderService;
import com.demo.service.OrderVoService;
import com.demo.service.VenueService;
import org.aspectj.weaver.ast.Or;
import org.hibernate.jdbc.Expectation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.NestedServletException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    public void testOrderManageWithUserHaveManyOrders() throws Exception {
        int userManyOrders = 27;

        //mock orders
        Order mockOrder1 = new Order(29, "yonghu", 16, 2,
                LocalDateTime.of(2020, 1, 2, 18, 16, 8),
                LocalDateTime.of(2020, 1, 24, 11, 0, 0), 3, 1500);
        Order mockOrder2 = new Order(30, "yonghu",17,2,
                LocalDateTime.of(2020,1,2,18,16,21),
                LocalDateTime.of(2020,1,25,11,0,0),3,900);
        List<Order> mockOrderList =  new ArrayList<>();
        mockOrderList.add(mockOrder1);
        mockOrderList.add(mockOrder2);

        // mock page
        Page<Order> pageOfManyOrders = new PageImpl<>(mockOrderList);

        //mock service
        Pageable order_pageable = PageRequest.of(0,5, Sort.by("orderTime").descending());
        when(orderService.findUserOrder(String.valueOf(userManyOrders), order_pageable)).thenReturn(pageOfManyOrders);

        // test user have many orders
        User user = new User();
        user.setUserID("27");
        mockMvc.perform(get("/order_manage").sessionAttr("user",user))
                .andExpect(status().isOk())
                .andExpect(view().name("order_manage"))
                .andExpect(model().attribute("total", pageOfManyOrders.getTotalPages()));
    }

    @Test
    public void testOrderManageWithUserHaveNoOrders() throws Exception {
        int userNoOrders = 19;

        Pageable order_pageable = PageRequest.of(0,5, Sort.by("orderTime").descending());
        when(orderService.findUserOrder(String.valueOf(userNoOrders),order_pageable)).thenReturn(null);

        //mock 19's orders and expected page
        List<Order> mockEmpty =  new ArrayList<>();
        Page<Order> pageOfEmptyOrders = new PageImpl<>(mockEmpty);

        // test user have no orders
        User user = new User();
        user.setUserID(String.valueOf(userNoOrders));
        mockMvc.perform(get("/order_manage").sessionAttr("user",user))
                .andExpect(status().isOk())
                .andExpect(view().name("order_manage"))
                .andExpect(model().attribute("total", pageOfEmptyOrders.getTotalPages()));
    }

    @Test
    public void testOrderManageWithoutLogin() throws Exception {
        NestedServletException exception = assertThrows(NestedServletException.class, () -> mockMvc.perform(get("/order_manage")));
        assertTrue(exception.getRootCause() instanceof LoginException);
    }

    @Test
    public void testOrderPlaceDoWithValidID() throws Exception {
        int validId = 1;

        // mock service
        Venue mockVenue = new Venue(1,"name","description"
                ,200,"","address","09:00","20:00");
        when(venueService.findByVenueID(validId)).thenReturn(mockVenue);

        // test valid id
        mockMvc.perform(get("/order_place.do").param("venueID",String.valueOf(validId)))
                .andExpect(status().isOk())
                .andExpect(view().name("order_place"))
                .andExpect(model().attribute("venue",mockVenue));
    }

    @Test
    public void testOrderPlaceDoWithInvalidID() throws Exception {
        int emptyVId = 2;
        when(venueService.findByVenueID(emptyVId)).thenReturn(null);

        // test error id
        mockMvc.perform(get("/order_place.do").param("venueID",String.valueOf(emptyVId)))
                .andExpect(status().isOk())
                .andExpect(view().name("order_place"))
                .andExpect(model().attributeDoesNotExist("venue"));
    }

    @Test
    public void testOrderPlaceDoWithStringID() throws Exception {
        mockMvc.perform(get("/order_place.do").param("venueID","nct127"))
                .andExpect(status().isBadRequest());
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
                .andExpect(status().isNotFound());
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
                .andExpect(status().isNotFound());
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

    //TODO：date有问题导致其他代码没法跑，所以相关测试都没写
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
        //TODO: 这里有问题！！ 应该抛出异常的！！
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

    @Test
    public void testModifyOrderWithStringID() throws Exception {
        mockMvc.perform(get("/modifyOrder.do").param("orderID", "nct127"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testDelOrderWithValidID() throws Exception {
        int orderID = 1;
        doNothing().when(orderService).delOrder(orderID);
        mockMvc.perform(post("/delOrder.do").param("orderID", String.valueOf(orderID)))
                .andExpect(status().isOk());
        verify(orderService).delOrder(orderID);
    }

    @Test
    public void testDelOrderWithInvalidID() throws Exception {
        // bug here
        // 错误的id，没有阻拦调用service
        int orderID = -1;
        mockMvc.perform(post("/delOrder.do").param("orderID", String.valueOf(orderID)))
                .andExpect(status().isOk());
        verify(orderService, never()).delOrder(orderID);
    }

    @Test
    public void testDelOrderWithStringID() throws Exception {
        mockMvc.perform(post("/delOrder.do").param("orderID", "nct127"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testOrderGetOrderListWithValidParam() throws Exception {
        // mock venue
        String venueName = "nct127";
        Venue mockVenue = new Venue();
        mockVenue.setVenueID(1);
        when(venueService.findByVenueName(venueName)).thenReturn(mockVenue);

        // mock date
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime ldt1 = LocalDateTime.parse("2023-03-31 00:00:00",df);
        LocalDateTime ldt2 = LocalDateTime.parse("2023-04-01 00:00:00",df);

        //mock venueOrder
        VenueOrder mockVenueOrder = new VenueOrder();
        mockVenueOrder.setVenue(mockVenue);
        List<Order> mockOrders = new ArrayList<>();
        when(orderService.findDateOrder(1,ldt1,ldt2)).thenReturn(mockOrders);
        mockVenueOrder.setOrders(mockOrders);

        mockMvc.perform(get("/order/getOrderList.do").param("venueName","nct127").param("date","2023-03-31"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"venue\":{\"venueID\":1,\"venueName\":null,\"description\":null,\"price\":0,\"picture\":null,\"address\":null,\"open_time\":null,\"close_time\":null},\"orders\":[]}"));
    }

    @Test
    public void testOrderGetOrderListWithInvalidDate() throws Exception {
        // bug here
        // 没有检测date格式

        // mock venue
        String venueName = "nct127";
        Venue mockVenue = new Venue();
        mockVenue.setVenueID(1);
        when(venueService.findByVenueName(venueName)).thenReturn(mockVenue);

        mockMvc.perform(get("/order/getOrderList.do").param("venueName", venueName).param("date","11:11"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    public void testOrderGetOrderListWithInvalidvenueName() throws Exception {
        //bug here
        //没有检测错误venue name
        String venueName = "nct127-false";
        when(venueService.findByVenueName(venueName)).thenReturn(null);

        mockMvc.perform(get("/order/getOrderList.do").param("venueName", venueName).param("date","2023-01-27"))
                .andExpect(status().isOk())
                .andExpect(content().json("{}"));
    }
}
