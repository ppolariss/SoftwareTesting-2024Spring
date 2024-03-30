package com.demo.order;

import static org.mockito.Mockito.when;
import static org.springframework.test.util.AssertionErrors.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.demo.controller.admin.AdminOrderController;
import com.demo.entity.Order;
import com.demo.entity.vo.OrderVo;
import com.demo.service.OrderService;
import com.demo.service.OrderVoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

@WebMvcTest(AdminOrderController.class)
public class AdminOrderControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private OrderVoService orderVoService; // 使用 @MockBean 注解模拟 OrderVoService 类

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



}
