package com.demo.order;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.demo.controller.user.OrderController;
import com.demo.entity.Order;
import com.demo.entity.User;
import com.demo.entity.Venue;
import com.demo.exception.LoginException;
import com.demo.service.OrderService;
import com.demo.service.OrderVoService;
import com.demo.service.VenueService;
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
    public void testOrderPlace() throws  Exception{
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
}
