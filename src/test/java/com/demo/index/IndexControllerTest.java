package com.demo.index;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.demo.controller.IndexController;
import com.demo.entity.Message;
import com.demo.entity.News;
import com.demo.entity.User;
import com.demo.entity.Venue;
import com.demo.entity.vo.MessageVo;
import com.demo.service.MessageService;
import com.demo.service.MessageVoService;
import com.demo.service.NewsService;
import com.demo.service.VenueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@WebMvcTest(IndexController.class)
public class IndexControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NewsService newsService;

    @MockBean
    private VenueService venueService;

    @MockBean
    private MessageVoService messageVoService;

    @MockBean
    private MessageService messageService;

    private MockHttpServletRequest request;

    @BeforeEach
    public void setUp() {
        // 倒数第二个is admin字段标识身份，0-用户，1-管理员
        User admin = new User(1, "adminID", "adminName", "adminPassword", "admin@example.com", "15649851625", 1, "adminPic");
        request = new MockHttpServletRequest();
        Objects.requireNonNull(request.getSession()).setAttribute("admin", admin);
    }


    @Test
    public void testIndexWithEmptyData() {
        try {
            Pageable venue_pageable= PageRequest.of(0,5, Sort.by("venueID").ascending());
            Pageable news_pageable= PageRequest.of(0,5, Sort.by("time").descending());
            Pageable message_pageable= PageRequest.of(0,5, Sort.by("time").descending());

            // empty data
            when(newsService.findAll(news_pageable))
                    .thenReturn(new PageImpl<>(Collections.emptyList(), news_pageable, 0));
            when(venueService.findAll(venue_pageable))
                    .thenReturn(new PageImpl<>(Collections.emptyList(), venue_pageable, 0));
            when(messageService.findPassState(message_pageable))
                    .thenReturn(new PageImpl<>(Collections.emptyList(), message_pageable, 0));
            when(messageVoService.returnVo(anyList()))
                    .thenReturn(new ArrayList<>());

            mockMvc.perform(get("/index"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("index"))
                    .andExpect(model().attribute("user", (String) null))
                    .andExpect(model().attribute("news_list", new ArrayList<>()))
                    .andExpect(model().attribute("venue_list", new ArrayList<>()))
                    .andExpect(model().attribute("message_list", new ArrayList<>()));
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testIndexWithNotEmptyData() {
        try {
            Pageable venue_pageable= PageRequest.of(0,5, Sort.by("venueID").ascending());
            Pageable news_pageable= PageRequest.of(0,5, Sort.by("time").descending());
            Pageable message_pageable= PageRequest.of(0,5, Sort.by("time").descending());

            List<News> newsList = new ArrayList<>();
            List<Venue> venueList = new ArrayList<>();
            List<MessageVo> messageVoList = new ArrayList<>();
            List<Message> messageList = new ArrayList<>();
            Page<Message> messagePage;

            // not empty data
            newsList.add(new News(1, "title", "content", LocalDateTime.now()));
            newsList.add(new News(2, "title", "content", LocalDateTime.now()));
            venueList.add(new Venue(1, "venue_name", "description", 1, "picture", "address", "open_time", "close_time"));
            messageVoList.add(new MessageVo(1,"title","content",LocalDateTime.now(),"username","email",1));
            messageList.add(new Message(1,"userID","content",LocalDateTime.now(),1));

            when(newsService.findAll(news_pageable)).thenReturn(new PageImpl<>(newsList, news_pageable, 1));
            when(venueService.findAll(venue_pageable)).thenReturn(new PageImpl<>(venueList, venue_pageable, 2));
            when(messageService.findPassState(message_pageable)).thenReturn(new PageImpl<>(messageList, message_pageable, 10));
            when(messageVoService.returnVo(anyList())).thenReturn(messageVoList);

            mockMvc.perform(get("/index"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("index"))
                    .andExpect(model().attribute("user", (String) null))
                    .andExpect(model().attribute("news_list", newsList))
                    .andExpect(model().attribute("venue_list", venueList))
                    .andExpect(model().attribute("message_list", messageVoList));
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testAdminIndexWithInvalidRole() {
        try {
            User user = new User(1, "userID", "userName", "userPassword", "user@example.com", "14695846221", 0, "userPic");
            Objects.requireNonNull(request.getSession()).setAttribute("admin", user);

            mockMvc.perform(get("/admin_index").session((MockHttpSession) request.getSession()))
                    .andExpect(status().isUnauthorized());  // expect 401 Unauthorized
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testAdminIndex() throws Exception {
        try {
            mockMvc.perform(get("/admin_index"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/admin_index"));
        } catch (Exception e) {
            fail();
        }
    }

}
