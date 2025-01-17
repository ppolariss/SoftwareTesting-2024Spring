package com.demo.order;

import static com.demo.service.OrderService.STATE_FINISH;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.demo.controller.user.OrderController;
import com.demo.dao.OrderDao;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.NestedServletException;

import javax.persistence.EntityNotFoundException;
import javax.swing.text.html.parser.Entity;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


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

    @MockBean
    private OrderDao orderDao;
    private MockHttpServletRequest request;
    @BeforeEach
    public void setUp() {
        // 倒数第二个isadmin字段标识身份，0-用户，1-管理员
        User user = new User(127, "userID", "userName", "userPassword", "user@example.com", "15649851625", 0, "userPic");

        request = new MockHttpServletRequest();
        Objects.requireNonNull(request.getSession()).setAttribute("user", user);
    }

    @Test
    public void testOrderManageWithManyPage() throws Exception {
        List<Order> orders = IntStream.range(0,7)
                .mapToObj(i -> new Order())
                .collect(Collectors.toList());

        Pageable order_pageable = PageRequest.of(0,5, Sort.by("orderTime").descending());
        Page<Order> pageWithOrders = new PageImpl<>(new ArrayList<>(), order_pageable, orders.size());

        when(orderService.findUserOrder(anyString(),any())).thenReturn(pageWithOrders);

        // test user have many orders
        mockMvc.perform(get("/order_manage").session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("order_manage"))
                .andExpect(model().attribute("total", 2));
    }
    @Test
    public void testOrderManageWithEmptyPage() throws Exception {
        // 页面为空

        Pageable order_pageable = PageRequest.of(0,5, Sort.by("orderTime").descending());
        when(orderService.findUserOrder(anyString(), any())).thenReturn(new PageImpl<>(Collections.emptyList(), order_pageable,0));

        // test user have no orders
        mockMvc.perform(get("/order_manage").session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("order_manage"))
                .andExpect(model().attribute("total", 0));
    }
    @Test
    public void testOrderManageWithoutLogin() throws Exception {
        Pageable order_pageable= PageRequest.of(0,5, Sort.by("time").descending());
        when(orderService.findUserOrder("1",order_pageable))
                .thenReturn(new PageImpl<>(Collections.emptyList(),order_pageable,0));

        MockHttpSession session = new MockHttpSession();
        //bug here
        mockMvc.perform(get("/order_manage").session(session))
                .andExpect(status().isUnauthorized());

    }


    @Test
    public void testOrderPlaceDoWithValidID() throws Exception {
        int validId = 1;

        // mock service
        Venue mockVenue = new Venue();
        mockVenue.setVenueID(1);
        when(venueService.findByVenueID(validId)).thenReturn(mockVenue);

        // test valid id
        mockMvc.perform(get("/order_place.do").param("venueID",String.valueOf(validId)).session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("order_place"))
                .andExpect(model().attribute("venue",mockVenue));
    }
    @Test
    public void testOrderPlaceDoWithEmptyContentID() throws Exception {
        int emptyVId = 2;
        when(venueService.findByVenueID(emptyVId)).thenThrow(EntityNotFoundException.class);

        mockMvc.perform(get("/order_place.do").param("venueID",String.valueOf(emptyVId)).session((MockHttpSession) request.getSession()))
                .andExpect(status().isNotFound())
                .andExpect(view().name("order_place"));
    }
    @Test
    public void testOrderPlaceDoWithEmptyParam() throws Exception {
        mockMvc.perform(get("/order_place.do").session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testOrderPlaceDoWithStringID() throws Exception {
        mockMvc.perform(get("/order_place.do").param("venueID","nct127").session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testOrderPlaceDoWithInvalidID() throws Exception {
        mockMvc.perform(get("/order_place.do").param("venueID","-1").session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testOrderPlaceDoWithoutLogin() throws Exception{
        mockMvc.perform(get("/order_place.do").param("venueID","1"))
                .andExpect(status().isUnauthorized());
    }


    @Test
    public void testOrderPlace() throws Exception {
        mockMvc.perform(get("/order_place").session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk());
    }
    @Test
    public void testOrderPlaceWithoutLogin() throws Exception {
        mockMvc.perform(get("/order_place"))
                .andExpect(status().isUnauthorized());
    }


    @Test
    public void testGetOrderListWithValidPage() throws Exception{
        // mock orders
        List<Order> mockOrderList = new ArrayList<>();
        mockOrderList.add(new Order());

        // mock page
        Page<Order> pageOfManyOrders = new PageImpl<>(mockOrderList);

        //mock service
        int pageNormal = 1;
        Pageable orderPageableNormal = PageRequest.of(pageNormal,5, Sort.by("orderTime").descending());
        when(orderService.findUserOrder(any(), any())).thenReturn(pageOfManyOrders);

        // test
        mockMvc.perform(get("/getOrderList.do").session((MockHttpSession) request.getSession()).param("page","2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
    @Test
    public void testGetOrderListWithEmptyParam() throws Exception{
        // mock orders
        List<Order> mockOrderList = new ArrayList<>();
        mockOrderList.add(new Order());

        // mock page
        Page<Order> pageOfManyOrders = new PageImpl<>(mockOrderList);

        //mock service
        Pageable orderPageableNormal = PageRequest.of(0,5, Sort.by("orderTime").descending());
        when(orderService.findUserOrder(any(),any())).thenReturn(pageOfManyOrders);

        // test
        User user = new User();
        user.setUserID("1");
        mockMvc.perform(get("/getOrderList.do").session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
    @Test
    public void testGetOrderListWithPageNotPositive() throws Exception {
        // bug here
        mockMvc.perform(get("/getOrderList.do").param("page", "-1").session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testGetOrderListWithEmptyPage() throws Exception {
        Pageable order_pageable= PageRequest.of(0,5, Sort.by("time").descending());
        when(orderService.findUserOrder(any(),any()))
                .thenReturn(new PageImpl<>(Collections.emptyList(),order_pageable,0));

        User user = new User();
        user.setUserID("1");
        mockMvc.perform(get("/getOrderList.do").param("page", "1").session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0))); // 直接检查响应体是否为一个空数组
    }
    @Test
    public void testGetOrderListWithPageIsNotNum() throws Exception {
        User user = new User();
        user.setUserID("nct127");
        mockMvc.perform(get("/getOrderList.do").param("page","jw").session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testGetOrderListWithoutLogin() throws Exception {
        Pageable order_pageable= PageRequest.of(0,5, Sort.by("time").descending());
        when(orderService.findUserOrder(any(),any()))
                .thenReturn(new PageImpl<>(Collections.emptyList(),order_pageable,0));

        //bug here
        mockMvc.perform(get("/getOrderList.do").param("page","1"))
                .andExpect(status().isUnauthorized());
    }


    @Test
    public void testAddOrderWithSuccess() throws Exception {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime ldt = LocalDateTime.parse("2024-03-30 10:00:00",df);
        doNothing().when(orderService).submit(anyString(),any(LocalDateTime.class),anyInt(),anyString());


        mockMvc.perform(post("/addOrder.do")
                        .param("venueName", "Venue1")
                        .param("date", "2024-03-30")
                        .param("startTime", "10:00")
                        .param("hours", "2")
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("order_manage"));
        verify(orderService).submit("Venue1",ldt,2,"1");
    }
    @Test
    public void testAddOrderWithErrorDateTime() throws Exception {
        // 都是自带解析 格式和内容错误检查一种即可
        User user = new User();
        user.setUserID("1");
        mockMvc.perform(post("/addOrder.do")
                        .param("venueName", "Venue1")
                        .param("date", "2024-33-33")
                        .param("startTime", "10:00")
                        .param("hours", "2")
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testAddOrderWithErrorFloatHours() throws Exception {
        mockMvc.perform(post("/addOrder.do")
                        .param("venueName", "Venue1")
                        .param("date", "2024-03-30")
                        .param("startTime", "10:00")
                        .param("hours", "1.5")
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testAddOrderWithErrorNegativeHours() throws Exception {
        doNothing().when(orderService).submit(anyString(),any(LocalDateTime.class),anyInt(),anyString());

        mockMvc.perform(post("/addOrder.do")
                        .param("venueName", "Venue1")
                        .param("date", "2024-03-30")
                        .param("startTime", "10:00")
                        .param("hours", "-1")
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testAddOrderWithNoParam() throws Exception {
        doNothing().when(orderService).submit(anyString(),any(LocalDateTime.class),anyInt(),anyString());
        mockMvc.perform(post("/addOrder.do").session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testAddOrderWithFailed() throws Exception {
        doThrow(EntityNotFoundException.class).when(orderService).submit(anyString(),any(LocalDateTime.class),anyInt(),anyString());
        mockMvc.perform(post("/addOrder.do")
                        .param("venueName", "Venue1")
                        .param("date", "2024-03-30")
                        .param("startTime", "10:00")
                        .param("hours", "-1")
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isNotFound());
    }
    @Test
    public void testAddOrderWithoutLogin() throws Exception {
        doNothing().when(orderService).submit(anyString(),any(LocalDateTime.class),anyInt(),anyString());
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/addOrder.do")
                        .param("venueName", "Venue1")
                        .param("date", "2024-03-30")
                        .param("startTime", "10:00")
                        .param("hours", "-1"))
                .andExpect(status().isUnauthorized());
    }


    @Test
    public void testFinishOrderWithValidID() throws Exception {
        int orderID = 127;
        Order mockOrder = new Order();
        mockOrder.setOrderID(orderID);
        when(orderDao.findByOrderID(orderID)).thenReturn(mockOrder);
        doNothing().when(orderDao).updateState(STATE_FINISH,orderID);

        mockMvc.perform(post("/finishOrder.do").param("orderID", String.valueOf(orderID)).session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk());
        verify(orderService, times(1)).finishOrder(orderID);
    }
    @Test
    public void testFinishOrderWithNotFoundID() throws Exception {
        int orderID = 127;
        doThrow(RuntimeException.class).when(orderService).finishOrder(orderID);
        mockMvc.perform(post("/finishOrder.do").param("orderID", String.valueOf(orderID)).session((MockHttpSession) request.getSession()))
                .andExpect(status().isNotFound());
    }
    @Test
    public void testFinishOrderWithInvalidOrderID() throws Exception {
        mockMvc.perform(post("/finishOrder.do").param("orderID","-1").session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testFinishOrderWithStringID() throws Exception {
        mockMvc.perform(post("/finishOrder.do").param("orderID", "nct127").session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testFinishOrderWithEmptyParam() throws Exception {
        mockMvc.perform(post("/finishOrder.do").session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testFinishOrderWithoutLogin() throws Exception {
        mockMvc.perform(post("/finishOrder.do").param("orderID", "12"))
                .andExpect(status().isUnauthorized());
    }


    @Test
    public void testModifyOrderDoWithValidID() throws Exception {
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
        mockMvc.perform(get("/modifyOrder.do").param("orderID",String.valueOf(validId)).session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("order_edit"))
                .andExpect(model().attribute("order",mockOrder))
                .andExpect(model().attribute("venue",mockVenue));
    }
    @Test
    public void testModifyOrderDoWithNotFoundID() throws Exception {
        int notFoundID = 127;
        when(orderService.findById(notFoundID)).thenThrow(EntityNotFoundException.class);
        mockMvc.perform(get("/modifyOrder.do").param("orderID",String.valueOf(notFoundID)).session((MockHttpSession) request.getSession()))
                .andExpect(status().isNotFound())
                .andExpect(view().name("order_edit"));
    }
    @Test
    public void testModifyOrderDoWithInvalidID() throws Exception {
        // bug here
        // 程序中没有处理非法order id情况，可能导致null调用后续的函数
        mockMvc.perform(get("/modifyOrder.do").param("orderID","-1").session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testModifyOrderDoWithStringID() throws Exception {
        mockMvc.perform(get("/modifyOrder.do").param("orderID", "nct127").session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testModifyOrderDoWithEmptyID() throws Exception {
        mockMvc.perform(get("/modifyOrder.do").session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testModifyOrderDoWithoutLogin() throws Exception {
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
                .andExpect(status().isUnauthorized());

    }


    @Test
    public void testModifyOrderWithSuccess() throws Exception {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime ldt = LocalDateTime.parse("2024-03-30 10:00:00",df);
        doNothing().when(orderService).updateOrder(anyInt(),anyString(),any(LocalDateTime.class),anyInt(),anyString());

        User user = new User();
        user.setUserID("1");

        mockMvc.perform(post("/modifyOrder")
                        .param("venueName", "Venue1")
                        .param("date", "2024-03-30")
                        .param("startTime", "10:00")
                        .param("hours", "2")
                        .param("orderID","2")
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("order_manage"))
                .andExpect(content().string("true"));
        verify(orderService).updateOrder(2,"Venue1",ldt,2,"1");
    }
    @Test
    public void testModifyOrderWithErrorDateTime() throws Exception {
        // 都是自带解析 格式和内容错误检查一种即可
        User user = new User();
        user.setUserID("1");
        mockMvc.perform(post("/modifyOrder")
                        .param("venueName", "Venue1")
                        .param("date", "2024-33-33")
                        .param("startTime", "10:00")
                        .param("hours", "2")
                        .param("orderID","2")
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testModifyOrderWithErrorFloatHours() throws Exception {
        User user = new User();
        user.setUserID("1");
        mockMvc.perform(post("/modifyOrder")
                        .param("venueName", "Venue1")
                        .param("date", "2024-03-30")
                        .param("startTime", "10:00")
                        .param("hours", "1.5")
                        .param("orderID","2")
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testModifyOrderWithErrorNegativeHours() throws Exception {
        doNothing().when(orderService).updateOrder(anyInt(),anyString(),any(LocalDateTime.class),anyInt(),anyString());
        User user = new User();
        user.setUserID("1");
        mockMvc.perform(post("/modifyOrder")
                        .param("venueName", "Venue1")
                        .param("date", "2024-03-30")
                        .param("startTime", "10:00")
                        .param("hours", "-1")
                        .param("orderID","2")
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testModifyOrderWithErrorFloatOrderID() throws Exception {
        User user = new User();
        user.setUserID("1");
        mockMvc.perform(post("/modifyOrder")
                        .param("venueName", "Venue1")
                        .param("date", "2024-03-30")
                        .param("startTime", "10:00")
                        .param("hours", "2")
                        .param("orderID","2.5")
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testModifyOrderWithErrorNegativeOrderID() throws Exception {
        doNothing().when(orderService).updateOrder(anyInt(),anyString(),any(LocalDateTime.class),anyInt(),anyString());
        User user = new User();
        user.setUserID("1");
        mockMvc.perform(post("/modifyOrder")
                        .param("venueName", "Venue1")
                        .param("date", "2024-03-30")
                        .param("startTime", "10:00")
                        .param("hours", "2")
                        .param("orderID","-1")
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testModifyOrderWithNoParam() throws Exception {
        doNothing().when(orderService).updateOrder(anyInt(),anyString(),any(LocalDateTime.class),anyInt(),anyString());
        mockMvc.perform(post("/modifyOrder").session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testModifyOrderWithFailed() throws Exception {
        doThrow(EntityNotFoundException.class).when(orderService).updateOrder(anyInt(),anyString(),any(LocalDateTime.class),anyInt(),anyString());
        User user = new User();
        user.setUserID("1");
        mockMvc.perform(post("/modifyOrder")
                        .param("venueName", "Venue1")
                        .param("date", "2024-03-30")
                        .param("startTime", "10:00")
                        .param("hours", "1")
                        .param("orderID","2")
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("false"));
    }
    @Test
    public void testModifyOrderWithoutLogin() throws Exception {
        doNothing().when(orderService).updateOrder(anyInt(),anyString(),any(LocalDateTime.class),anyInt(),anyString());
        mockMvc.perform(post("/modifyOrder")
                        .param("venueName", "Venue1")
                        .param("date", "2024-03-30")
                        .param("startTime", "10:00")
                        .param("hours", "-1"))
                .andExpect(status().is4xxClientError());
    }


    @Test
    public void testDelOrderWithValidID() throws Exception {
        int orderID = 1;
        doNothing().when(orderService).delOrder(orderID);
        mockMvc.perform(post("/delOrder.do").param("orderID", String.valueOf(orderID)).session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
        verify(orderService).delOrder(orderID);
    }
    @Test
    public void testDelOrderWithNotFoundID() throws Exception {
        int orderID = 127;
        doThrow(EmptyResultDataAccessException.class).when(orderService).delOrder(orderID);
        mockMvc.perform(post("/delOrder.do").param("orderID", String.valueOf(orderID)).session((MockHttpSession) request.getSession()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("false"));
        verify(orderService).delOrder(orderID);
    }
    @Test
    public void testDelOrderWithInvalidID() throws Exception {
        int orderID = -1;
        doNothing().when(orderDao).deleteById(orderID);
        mockMvc.perform(post("/delOrder.do").param("orderID", String.valueOf(orderID)).session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testDelOrderWithStringID() throws Exception {
        mockMvc.perform(post("/delOrder.do").param("orderID", "nct127").session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testDelOrderWithEmptyID() throws Exception {
        mockMvc.perform(post("/delOrder.do").session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testDelOrderWithoutLogin() throws Exception {
        int orderID = 1;
        doNothing().when(orderService).delOrder(orderID);
        mockMvc.perform(post("/delOrder.do").param("orderID", String.valueOf(orderID)))
                .andExpect(status().isUnauthorized());
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

        mockMvc.perform(get("/order/getOrderList.do").param("venueName","nct127").param("date","2023-03-31")
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.venue").value(mockVenue))
                .andExpect(jsonPath("$.orders").value(mockOrders));
    }
    @Test
    public void testOrderGetOrderListWithInvalidDate() throws Exception {
        // bug here
        // 没有检测date格式
        String venueName = "nct127";

        // mock venue
        Venue mockVenue = new Venue();
        mockVenue.setVenueID(1);
        when(venueService.findByVenueName(venueName)).thenReturn(mockVenue);

        mockMvc.perform(get("/order/getOrderList.do").param("venueName", venueName).param("date","11:11")
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testOrderGetOrderListWithNotFoundVenueName() throws Exception {
        String venueName = "nct127-false";
        when(venueService.findByVenueName(venueName)).thenThrow(EntityNotFoundException.class);

        mockMvc.perform(get("/order/getOrderList.do").param("venueName", venueName).param("date","2023-01-27")
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isNotFound());
    }
    @Test
    public void testOrderGetOrderListWithEmptyOrders() throws Exception {
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

        mockMvc.perform(get("/order/getOrderList.do").param("venueName","nct127").param("date","2023-03-31")
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.venue").value(mockVenue))
                .andExpect(jsonPath("$.orders",hasSize(0)));
    }
    @Test
    public void testOrderGetOrderListWithNoParam() throws Exception {

        mockMvc.perform(get("/order/getOrderList.do")
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testOrderGetOrderListWithoutLogin() throws Exception {
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
                .andExpect(status().isUnauthorized());
    }
}
