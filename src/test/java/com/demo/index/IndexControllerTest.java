package com.demo.index;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.demo.controller.IndexController;
import com.demo.entity.Message;
import com.demo.entity.News;
import com.demo.entity.Venue;
import com.demo.entity.vo.MessageVo;
import com.demo.service.MessageService;
import com.demo.service.MessageVoService;
import com.demo.service.NewsService;
import com.demo.service.VenueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Test
    public void testNull() {
        Pageable venue_pageable= PageRequest.of(999999999,5, Sort.by("venueID").ascending());
        Pageable news_pageable= PageRequest.of(999999999,5, Sort.by("time").descending());
        Pageable message_pageable= PageRequest.of(999999999,5, Sort.by("time").descending());

        Page<Venue> temp1 = venueService.findAll(venue_pageable);
        assertNull(temp1);
        Page<News> temp2 = newsService.findAll(news_pageable);
        assertNull(temp2);
        Page<Message> temp3 = messageService.findPassState(message_pageable);
        assertNull(temp3);
    }

    @Test
    public void testIndexWithEmptyData() throws Exception {
        List<News> newsList = new ArrayList<>();
        List<Venue> venueList = new ArrayList<>();
        List<MessageVo> messageVoList = new ArrayList<>();
        List<Message> messageList = new ArrayList<>();
        Page<Message> messagePage = new PageImpl<>(messageList);

        // empty data
        when(newsService.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(newsList));
        when(venueService.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(venueList));
        when(messageService.findPassState(any(Pageable.class))).thenReturn(messagePage);
        when(messageVoService.returnVo(anyList())).thenReturn(messageVoList);

        mockMvc.perform(get("/index"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("user", (String) null))
                .andExpect(model().attribute("news_list", newsList))
                .andExpect(model().attribute("venue_list", venueList))
                .andExpect(model().attribute("message_list", messageVoList));
    }

    @Test
    public void testIndexWithNotEmptyData() throws Exception {
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
        messagePage = new PageImpl<>(messageList);

        when(newsService.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(newsList));
        when(venueService.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(venueList));
        when(messageService.findPassState(any(Pageable.class))).thenReturn(messagePage);
        when(messageVoService.returnVo(anyList())).thenReturn(messageVoList);

        mockMvc.perform(get("/index"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("user", (String) null))
                .andExpect(model().attribute("news_list", newsList))
                .andExpect(model().attribute("venue_list", venueList))
                .andExpect(model().attribute("message_list", messageVoList));
    }

    @Test
    public void testIndexWithNullData() throws Exception {

        // null data
        // bug here
        // null check should be added
        when(newsService.findAll(any(Pageable.class))).thenReturn(null);
        when(venueService.findAll(any(Pageable.class))).thenReturn(null);
        when(messageService.findPassState(any(Pageable.class))).thenReturn(null);
        when(messageVoService.returnVo(anyList())).thenReturn(null);

        mockMvc.perform(get("/index"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testAdminIndex() throws Exception {
        mockMvc.perform(get("/admin_index"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/admin_index"));
    }
}
