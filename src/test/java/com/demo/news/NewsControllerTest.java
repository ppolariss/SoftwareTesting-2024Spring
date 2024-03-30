package com.demo.news;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.demo.controller.user.NewsController;
import com.demo.entity.News;
import com.demo.service.NewsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@WebMvcTest(NewsController.class)
public class NewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NewsService newsService;

    @Test
    public void testNull() {
        Pageable news_pageable = PageRequest.of(999999999, 10, Sort.by("time").ascending());
        Page<News> temp1 = newsService.findAll(news_pageable);
        assertNull(temp1);

        int newsID = -1;
        News temp2 = newsService.findById(newsID);
        assertNull(temp2);
    }

    @Test
    public void testNewsWithEmptyParam() throws Exception {
        // empty param
        mockMvc.perform(get("/news").param("newsID", ""))
                .andExpect(status().isBadRequest());

    }

    @Test
    public void testNewsWithErrorParam() throws Exception {
        // error param
        mockMvc.perform(get("/news").param("newsID", "hello"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testNewsWithSuccess() throws Exception {
        int newsIDSuccess = 1;
        int newsIDFail = 2;
        News mockNews1 = new News(1, "title", "content", LocalDateTime.now());

        when(newsService.findById(1)).thenReturn(mockNews1);
        when(newsService.findById(2)).thenReturn(null);

        // news found
        mockMvc.perform(get("/news").param("newsID", String.valueOf(newsIDSuccess)))
                .andExpect(status().isOk())
                .andExpect(view().name("news"))
                .andExpect(model().attribute("news", mockNews1));
    }

    @Test
    public void testNewsWithNotFound() throws Exception {
        int newsIDSuccess = 1;
        int newsIDFail = 2;
        News mockNews1 = new News(1, "title", "content", LocalDateTime.now());

        when(newsService.findById(1)).thenReturn(mockNews1);
        when(newsService.findById(2)).thenReturn(null);

        // news not found
        // bug here
        // null check should be added
        mockMvc.perform(get("/news").param("newsID", String.valueOf(newsIDFail)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testNewsListWithEmptyParam() throws Exception {

        List<News> newsList = new ArrayList<>();
        newsList.add(new News(1, "title", "content", LocalDateTime.now()));
        newsList.add(new News(2, "title", "content", LocalDateTime.now()));

        Page<News> mockPage = new PageImpl<>(newsList);

        when(newsService.findAll(PageRequest.of(0, 5, Sort.by("time").descending()))).thenReturn(mockPage);
        when(newsService.findAll(PageRequest.of(1, 5, Sort.by("time").descending()))).thenReturn(null);


        // empty param
        mockMvc.perform(get("/news/getNewsList").param("page", ""))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    public void testNewsListWithErrorParam() throws Exception {
        // error param
        mockMvc.perform(get("/news/getNewsList").param("page", "hello"))
                .andExpect(status().isBadRequest());

    }

    @Test
    public void testNewsListWithSuccess() throws Exception {
        int pageSuccess = 1;
        int pageFail = 2;

        List<News> newsList = new ArrayList<>();
        newsList.add(new News(1, "title", "content", LocalDateTime.now()));
        newsList.add(new News(2, "title", "content", LocalDateTime.now()));

        Page<News> mockPage = new PageImpl<>(newsList);

        when(newsService.findAll(PageRequest.of(0, 5, Sort.by("time").descending()))).thenReturn(mockPage);
        when(newsService.findAll(PageRequest.of(1, 5, Sort.by("time").descending()))).thenReturn(null);

        // page found
        mockMvc.perform(get("/news/getNewsList").param("page", String.valueOf(pageSuccess)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    public void testNewsListWithNotFound() throws Exception {
        int pageSuccess = 1;
        int pageFail = 2;

        List<News> newsList = new ArrayList<>();
        newsList.add(new News(1, "title", "content", LocalDateTime.now()));
        newsList.add(new News(2, "title", "content", LocalDateTime.now()));

        Page<News> mockPage = new PageImpl<>(newsList);

        when(newsService.findAll(PageRequest.of(0, 5, Sort.by("time").descending()))).thenReturn(mockPage);
        when(newsService.findAll(PageRequest.of(1, 5, Sort.by("time").descending()))).thenReturn(null);

        // page not found
        mockMvc.perform(get("/news/getNewsList").param("page", String.valueOf(pageFail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").doesNotExist());
    }

    @Test
    public void testNewsListPageWithEmptyData() throws Exception {
        List<News> newsList = new ArrayList<>();

        Page<News> mockEmptyPage = new PageImpl<>(newsList);

        // empty page
        when(newsService.findAll(PageRequest.of(0, 5, Sort.by("time").descending()))).thenReturn(mockEmptyPage);

        mockMvc.perform(get("/news_list"))
                .andExpect(status().isOk())
                .andExpect(view().name("news_list"))
                .andExpect(model().attribute("news_list", new ArrayList<>()))
                .andExpect(model().attribute("total", mockEmptyPage.getTotalPages()));
    }

    @Test
    public void testNewsListPageWithNotEmptyData() throws Exception {
        List<News> newsList = new ArrayList<>();

        newsList.add(new News(1, "title", "content", LocalDateTime.now()));
        newsList.add(new News(2, "title", "content", LocalDateTime.now()));

        Page<News> mockNotEmptyPage = new PageImpl<>(newsList);

        // not empty page
        when(newsService.findAll(PageRequest.of(0, 5, Sort.by("time").descending()))).thenReturn(mockNotEmptyPage);

        mockMvc.perform(get("/news_list"))
                .andExpect(status().isOk())
                .andExpect(view().name("news_list"))
                .andExpect(model().attribute("news_list", newsList))
                .andExpect(model().attribute("total", mockNotEmptyPage.getTotalPages()));
    }

    @Test
    public void testNewsListPageWithNullData() throws Exception {
        // null page
        // bug here
        // null check should be added
        when(newsService.findAll(PageRequest.of(1, 5, Sort.by("time").descending()))).thenReturn(null);

        mockMvc.perform(get("/news_list"))
                .andExpect(status().isNotFound());
    }
}
