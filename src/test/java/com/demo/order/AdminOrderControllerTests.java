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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.NestedServletException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    @Test
    public void testReservationManageWithBothOrders() throws Exception {
        // mock data
        List<Order> mockOrders = new ArrayList<>();
        List<OrderVo> mockOrderVos = new ArrayList<>();
        Pageable order_pageable= PageRequest.of(0,10, Sort.by("orderTime").descending());
        Page<Order> mockPage = new PageImpl<>(mockOrders);

        when(orderService.findAuditOrder()).thenReturn(mockOrders);
        when(orderVoService.returnVo(mockOrders)).thenReturn(mockOrderVos);
        when(orderService.findNoAuditOrder(order_pageable)).thenReturn(mockPage);

        mockMvc.perform(get("/reservation_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reservation_manage"))
                .andExpect(model().attribute("order_list",mockOrderVos))
                .andExpect(model().attribute("total",mockPage.getTotalPages()));
    }
    @Test
    public void testReservationManageWithoutAuditOrders() throws Exception {
        List<Order> mockOrders = new ArrayList<>();
        Pageable order_pageable= PageRequest.of(0,10, Sort.by("orderTime").descending());
        Page<Order> mockPage = new PageImpl<>(mockOrders);
        List<OrderVo> mockOrderVos = new ArrayList<>();

        when(orderService.findAuditOrder()).thenReturn(null);
        when(orderService.findNoAuditOrder(order_pageable)).thenReturn(mockPage);

        mockMvc.perform(get("/reservation_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reservation_manage"))
                .andExpect(model().attribute("order_list",mockOrderVos))
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
        when(orderService.findNoAuditOrder(order_pageable)).thenReturn(null);

        mockMvc.perform(get("/reservation_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reservation_manage"))
                .andExpect(model().attribute("order_list",mockOrderVos))
                .andExpect(model().attribute("total",mockEmptyPage.getTotalPages()));
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
        mockMvc.perform(get("/admin/getOrderList.do"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
    @Test
    public void testGetOrderListWithPageNotPositive() throws Exception {
        mockMvc.perform(get("/admin/getOrderList.do").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testGetOrderListWithEmptyParam() throws Exception {
        mockMvc.perform(get("/admin/getOrderList.do"))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testGetOrderListWithEmptyPage() throws Exception {
        User user = new User();
        user.setUserID("nct127");

        Pageable order_pageable = PageRequest.of(5-1,5, Sort.by("orderTime").descending());
        when(orderService.findUserOrder("1", order_pageable))
                .thenReturn(new PageImpl<>(Collections.emptyList(),order_pageable,0));

        mockMvc.perform(get("/admin/getOrderList.do").param("page","5"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }
    @Test
    public void testGetOrderListWithStringPage() throws Exception {
        User user = new User();
        user.setUserID("nct127");
        mockMvc.perform(get("/admin/getOrderList.do").param("page","jw"))
                .andExpect(status().isBadRequest());
    }


    @Test
    public void testPassOrderWithValidID() throws Exception {
        int orderID = 127;
        Order mockOrder = new Order();
        mockOrder.setOrderID(orderID);
        when(orderDao.findByOrderID(orderID)).thenReturn(mockOrder);
        doNothing().when(orderDao).updateState(STATE_WAIT,orderID);

        mockMvc.perform(post("/passOrder.do").param("orderID", String.valueOf(orderID)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
        verify(orderService, times(1)).confirmOrder(orderID);
    }
    @Test
    public void testPassOrderWithNotFoundID() throws Exception {
        int orderID = 1;
        doThrow(EmptyResultDataAccessException.class).when(orderService).confirmOrder(orderID);

        mockMvc.perform(post("/passOrder.do").param("orderID", String.valueOf(orderID)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("false"));
        verify(orderService, times(1)).confirmOrder(orderID);
    }
    @Test
    public void testPassOrderWithNegativeID() throws Exception {
        int orderID = -1;
        mockMvc.perform(post("/passOrder.do").param("orderID", String.valueOf(orderID)))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testPassOrderWithStringID() throws Exception {
        mockMvc.perform(post("/passOrder.do").param("orderID", "nct127"))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testPassOrderWithEmptyParam() throws Exception {
        mockMvc.perform(post("/passOrder.do"))
                .andExpect(status().isBadRequest());
    }


    @Test
    public void testRejectOrderWithValidID() throws Exception {
        int orderID = 127;
        Order mockOrder = new Order();
        mockOrder.setOrderID(orderID);
        when(orderDao.findByOrderID(orderID)).thenReturn(mockOrder);
        doNothing().when(orderDao).updateState(STATE_REJECT,orderID);

        mockMvc.perform(post("/rejectOrder.do").param("orderID", String.valueOf(orderID)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));;
        verify(orderService, times(1)).rejectOrder(orderID);
    }
    @Test
    public void testRejectOrderWithNotFoundID() throws Exception {
        int orderID = 1;
        doThrow(EmptyResultDataAccessException.class).when(orderService).rejectOrder(orderID);

        mockMvc.perform(post("/rejectOrder.do").param("orderID", String.valueOf(orderID)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("false"));
        verify(orderService, times(1)).rejectOrder(orderID);
    }
    @Test
    public void testRejectOrderWithNegativeID() throws Exception {
        int orderID = -1;
        mockMvc.perform(post("/rejectOrder.do").param("orderID", String.valueOf(orderID)))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testRejectOrderWithStringID() throws Exception {
        mockMvc.perform(post("/rejectOrder.do").param("orderID", "nct127"))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testRejectOrderWithEmptyParam() throws Exception {
        mockMvc.perform(post("/rejectOrder.do"))
                .andExpect(status().isBadRequest());
    }

}
