package com.demo.order;

import static com.demo.service.OrderService.*;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;
import static org.springframework.test.util.AssertionErrors.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.demo.controller.admin.AdminOrderController;
import com.demo.dao.OrderDao;
import com.demo.entity.Order;
import com.demo.entity.User;
import com.demo.entity.vo.OrderVo;
import com.demo.service.OrderService;
import com.demo.service.OrderVoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@WebMvcTest(AdminOrderController.class)
public class AdminOrderControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private OrderVoService orderVoService; // 使用 @MockBean 注解模拟 OrderVoService 类

    @MockBean
    private OrderDao orderDao;
    private MockHttpServletRequest request;

    @BeforeEach
    public void setUp() {
        // 倒数第二个isadmin字段标识身份，0-用户，1-管理员
        User admin = new User(1, "adminID", "adminName", "adminPassword", "admin@example.com", "15649851625", 1, "adminPic");

        request = new MockHttpServletRequest();
        Objects.requireNonNull(request.getSession()).setAttribute("admin", admin);
    }
    @Test
    public void testReservationManageWithBothOrdersPaged() throws Exception {
        // mock data
        List<Order> mockOrders = IntStream.range(0,15)
                .mapToObj(i -> new Order())
                .collect(Collectors.toList());
        List<OrderVo> mockOrderVos = IntStream.range(0,15)
                .mapToObj(i -> new OrderVo())
                .collect(Collectors.toList());
        Pageable order_pageable= PageRequest.of(0,10, Sort.by("orderTime").descending());
        Page<Order> mockPage = new PageImpl<>(mockOrders,order_pageable,mockOrders.size());

        when(orderService.findAuditOrder()).thenReturn(mockOrders);
        when(orderVoService.returnVo(mockOrders)).thenReturn(mockOrderVos);
        when(orderService.findNoAuditOrder(order_pageable)).thenReturn(mockPage);

        mockMvc.perform(get("/reservation_manage").session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reservation_manage"))
                .andExpect(model().attribute("order_list",mockOrderVos))
                .andExpect(model().attribute("total",2));
    }
    @Test
    public void testReservationManageWithoutAuditOrders() throws Exception {
        List<Order> mockOrders = new ArrayList<>();
        Pageable order_pageable= PageRequest.of(0,10, Sort.by("orderTime").descending());
        Page<Order> mockPage = new PageImpl<>(mockOrders);
        List<OrderVo> mockOrderVos = new ArrayList<>();

        when(orderService.findAuditOrder()).thenReturn(new ArrayList<>());
        when(orderService.findNoAuditOrder(order_pageable)).thenReturn(mockPage);

        mockMvc.perform(get("/reservation_manage").session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reservation_manage"))
                .andExpect(model().attribute("order_list",hasSize(0)))
                .andExpect(model().attribute("total",mockPage.getTotalPages()));
    }
    @Test
    public void testReservationManageWithoutNoAuditOrders() throws Exception {
        List<Order> mockOrders = new ArrayList<>();
        Pageable order_pageable= PageRequest.of(0,10, Sort.by("orderTime").descending());
        Page<Order> mockEmptyPage = new PageImpl<>(mockOrders);
        List<OrderVo> mockOrderVos = new ArrayList<>();

        when(orderService.findAuditOrder()).thenReturn(mockOrders);
        when(orderVoService.returnVo(mockOrders)).thenReturn(mockOrderVos);
        when(orderService.findNoAuditOrder(order_pageable)).thenReturn(new PageImpl<>(Collections.emptyList(),order_pageable,0));

        mockMvc.perform(get("/reservation_manage").session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reservation_manage"))
                .andExpect(model().attribute("order_list",mockOrderVos))
                .andExpect(model().attribute("total",0));
    }

    @Test
    public void testReservationManageWithInvalidRole() throws Exception {
        // 倒数第二个isadmin字段设置为0，代表为用户
        User user = new User(1, "userID", "userName", "userPassword", "user@example.com", "14695846221", 0, "userPic");
        Objects.requireNonNull(request.getSession()).setAttribute("admin", user);

        mockMvc.perform(get("/reservation_manage").session((MockHttpSession) request.getSession()))
                .andExpect(status().isUnauthorized());  // expect 401 Unauthorized
    }


    @Test
    public void testAdminGetOrderListWithValidPage() throws Exception{
        // mock orders
        List<Order> mockOrderList = new ArrayList<>();
        mockOrderList.add(new Order());
        List<OrderVo> mockVo = new ArrayList<>();

        // mock page
        Page<Order> pageOfManyOrders = new PageImpl<>(mockOrderList);

        //mock service
        int pageNormal = 0;
        Pageable orderPageableNormal = PageRequest.of(pageNormal,10, Sort.by("orderTime").descending());
        when(orderService.findNoAuditOrder(orderPageableNormal)).thenReturn(pageOfManyOrders);
        when(orderVoService.returnVo(mockOrderList)).thenReturn(mockVo);

        // test
        mockMvc.perform(get("/admin/getOrderList.do").session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
    @Test
    public void testGetOrderListWithPageNotPositive() throws Exception {
        mockMvc.perform(get("/admin/getOrderList.do").param("page", "-1").session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testGetOrderListWithEmptyParam() throws Exception {
        mockMvc.perform(get("/admin/getOrderList.do").session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testGetOrderListWithEmptyPage() throws Exception {
        Pageable order_pageable = PageRequest.of(5-1,5, Sort.by("orderTime").descending());
        when(orderService.findNoAuditOrder(any()))
                .thenReturn(new PageImpl<>(Collections.emptyList(),order_pageable,0));

        mockMvc.perform(get("/admin/getOrderList.do").param("page","5").session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }
    @Test
    public void testGetOrderListWithStringPage() throws Exception {
        mockMvc.perform(get("/admin/getOrderList.do").param("page","jw").session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testGetOrderListWithInvalidRole() throws Exception {
        // 倒数第二个isadmin字段设置为0，代表为用户
        User user = new User(1, "userID", "userName", "userPassword", "user@example.com", "14695846221", 0, "userPic");
        Objects.requireNonNull(request.getSession()).setAttribute("admin", user);

        mockMvc.perform(get("/admin/getOrderList.do").session((MockHttpSession) request.getSession()))
                .andExpect(status().isUnauthorized());  // expect 401 Unauthorized
    }


    @Test
    public void testPassOrderWithValidID() throws Exception {
        int orderID = 127;
        Order mockOrder = new Order();
        mockOrder.setOrderID(orderID);
        when(orderDao.findByOrderID(orderID)).thenReturn(mockOrder);
        doNothing().when(orderDao).updateState(STATE_WAIT,orderID);

        mockMvc.perform(post("/passOrder.do").param("orderID", String.valueOf(orderID)).session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
        verify(orderService, times(1)).confirmOrder(orderID);
    }
    @Test
    public void testPassOrderWithNotFoundID() throws Exception {
        int orderID = 1;
        doThrow(EmptyResultDataAccessException.class).when(orderService).confirmOrder(orderID);

        mockMvc.perform(post("/passOrder.do").param("orderID", String.valueOf(orderID)).session((MockHttpSession) request.getSession()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("false"));
        verify(orderService, times(1)).confirmOrder(orderID);
    }
    @Test
    public void testPassOrderWithNegativeID() throws Exception {
        int orderID = -1;
        mockMvc.perform(post("/passOrder.do").param("orderID", String.valueOf(orderID)).session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testPassOrderWithStringID() throws Exception {
        mockMvc.perform(post("/passOrder.do").param("orderID", "nct127").session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testPassOrderWithEmptyParam() throws Exception {
        mockMvc.perform(post("/passOrder.do").session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testPassOrderListWithInvalidRole() throws Exception {
        // 倒数第二个isadmin字段设置为0，代表为用户
        User user = new User(1, "userID", "userName", "userPassword", "user@example.com", "14695846221", 0, "userPic");
        Objects.requireNonNull(request.getSession()).setAttribute("admin", user);

        mockMvc.perform(post("/passOrder.do").param("orderID", "127").session((MockHttpSession) request.getSession()))
                .andExpect(status().isUnauthorized());  // expect 401 Unauthorized
    }


    @Test
    public void testRejectOrderWithValidID() throws Exception {
        int orderID = 127;
        Order mockOrder = new Order();
        mockOrder.setOrderID(orderID);
        when(orderDao.findByOrderID(orderID)).thenReturn(mockOrder);
        doNothing().when(orderDao).updateState(STATE_REJECT,orderID);

        mockMvc.perform(post("/rejectOrder.do").param("orderID", String.valueOf(orderID)).session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));;
        verify(orderService, times(1)).rejectOrder(orderID);
    }
    @Test
    public void testRejectOrderWithNotFoundID() throws Exception {
        int orderID = 1;
        doThrow(EmptyResultDataAccessException.class).when(orderService).rejectOrder(orderID);

        mockMvc.perform(post("/rejectOrder.do").param("orderID", String.valueOf(orderID)).session((MockHttpSession) request.getSession()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("false"));
        verify(orderService, times(1)).rejectOrder(orderID);
    }
    @Test
    public void testRejectOrderWithNegativeID() throws Exception {
        int orderID = -1;
        mockMvc.perform(post("/rejectOrder.do").param("orderID", String.valueOf(orderID)).session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testRejectOrderWithStringID() throws Exception {
        mockMvc.perform(post("/rejectOrder.do").param("orderID", "nct127").session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testRejectOrderWithEmptyParam() throws Exception {
        mockMvc.perform(post("/rejectOrder.do").session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testRejectOrderListWithInvalidRole() throws Exception {
        // 倒数第二个isadmin字段设置为0，代表为用户
        User user = new User(1, "userID", "userName", "userPassword", "user@example.com", "14695846221", 0, "userPic");
        Objects.requireNonNull(request.getSession()).setAttribute("admin", user);

        mockMvc.perform(post("/rejectOrder.do").param("orderID", "127").session((MockHttpSession) request.getSession()))
                .andExpect(status().isUnauthorized());  // expect 401 Unauthorized
    }

}
