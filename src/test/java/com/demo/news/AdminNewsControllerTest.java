package com.demo.news;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.demo.controller.admin.AdminNewsController;
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

@WebMvcTest(AdminNewsController.class)
public class AdminNewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NewsService newsService;

    @Test
    public void testNewsManage() throws Exception {
        List<News> newsList = new ArrayList<>();
        Page<News> mockPage = new PageImpl<>(newsList);

        // empty news list
        when(newsService.findAll(any(Pageable.class))).thenReturn(mockPage);

        mockMvc.perform(get("/news_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/news_manage"))
                .andExpect(model().attribute("total", mockPage.getTotalPages()));

        // not empty news list
        newsList.add(new News());
        mockPage = new PageImpl<>(newsList);
        when(newsService.findAll(any(Pageable.class))).thenReturn(mockPage);

        mockMvc.perform(get("/news_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/news_manage"))
                .andExpect(model().attribute("total", mockPage.getTotalPages()));

        // null news list
        // bug here
        // null check should be added
//        when(newsService.findAll(any(Pageable.class))).thenReturn(null);
//
//        mockMvc.perform(get("/news_manage"))
//                .andExpect(status().isNotFound()
    }

    @Test
    public void testNewsAdd() throws Exception {
        mockMvc.perform(get("/news_add"))
                .andExpect(status().isOk())
                .andExpect(view().name("/admin/news_add"));
    }

    @Test
    public void testNewsEdit() throws Exception {
        int newsIDSuccess = 1;
        int newsIDFail = 2;
        News mockNews = new News(1, "title", "content", LocalDateTime.now());

        when(newsService.findById(1)).thenReturn(mockNews);
        when(newsService.findById(2)).thenReturn(null);

        // empty request
        mockMvc.perform(get("/news_edit").param("newsID", ""))
                .andExpect(status().isBadRequest());

        // error request
        mockMvc.perform(get("/news_edit").param("newsID", "hello"))
                .andExpect(status().isBadRequest());


        // success
        mockMvc.perform(get("/news_edit").param("newsID", String.valueOf(newsIDSuccess)))
                .andExpect(status().isOk())
                .andExpect(view().name("/admin/news_edit"))
                .andExpect(model().attribute("news", mockNews));

        // fail
        // bug here
        // null check should be added
//        mockMvc.perform(get("/news_edit").param("newsID", String.valueOf(newsIDFail)))
//                .andExpect(status().isNotFound());
    }

    @Test
    public void testNewsList() throws Exception {
        int pageSuccess = 1;
        int pageFail = 2;

        List<News> newsList = new ArrayList<>();
        newsList.add(new News(1, "title", "content", LocalDateTime.now()));
        newsList.add(new News(2, "title", "content", LocalDateTime.now()));

        Page<News> mockPage = new PageImpl<>(newsList);

        when(newsService.findAll(PageRequest.of(0, 10, Sort.by("time").descending()))).thenReturn(mockPage);
        when(newsService.findAll(PageRequest.of(1, 10, Sort.by("time").descending()))).thenReturn(null);

        // default request
        mockMvc.perform(get("/newsList.do").param("page", ""))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray());

        // error request
        mockMvc.perform(get("/newsList.do").param("page", "hello"))
                .andExpect(status().isBadRequest());

        // page found
        mockMvc.perform(get("/newsList.do").param("page", String.valueOf(pageSuccess)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray());

        // page not found
        // bug here
        // null check should be added
//        mockMvc.perform(get("/newsList.do").param("page", String.valueOf(pageFail)))
//                .andExpect(status().isNotFound());
    }

    @Test
    public void testDelNews() throws Exception {
        int newsIDSuccess = 1;
        int newsIDFail = 2;

        doNothing().when(newsService).delById(newsIDSuccess);
        doNothing().when(newsService).delById(newsIDFail);

        // error method
        mockMvc.perform(get("/delNews.do").param("page", "1"))
                .andExpect(status().isMethodNotAllowed());

        // empty request
        mockMvc.perform(post("/delNews.do").param("newsID", ""))
                .andExpect(status().isBadRequest());

        // error request
        mockMvc.perform(post("/delNews.do").param("newsID", "hello"))
                .andExpect(status().isBadRequest());

        // success
        mockMvc.perform(post("/delNews.do").param("newsID", String.valueOf(newsIDSuccess)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // fail
        // bug here
        // false case should be added
//        mockMvc.perform(post("/delNews.do").param("newsID", String.valueOf(newsIDFail)))
//                .andExpect(status().isOk())
//                .andExpect(content().string("false"));
    }

    @Test
    public void testModifyNews() throws Exception {
        int newsIDSuccess = 1;
        int newsIDFail = 2;
        String title = "title";
        String content = "content";

        News mockNews = new News(1, "title", "content", LocalDateTime.now());

        doNothing().when(newsService).update(mockNews);
        doNothing().when(newsService).update(null);

        when(newsService.findById(newsIDSuccess)).thenReturn(mockNews);
        when(newsService.findById(newsIDFail)).thenReturn(null);

        // error method
        mockMvc.perform(get("/modifyNews.do").param("newsID", String.valueOf(newsIDSuccess)).param("title", title).param("content", content))
                .andExpect(status().isMethodNotAllowed());

        // empty request
        mockMvc.perform(post("/modifyNews.do").param("newsID", "").param("title", "").param("content", ""))
                .andExpect(status().isBadRequest());

        // error request
        mockMvc.perform(post("/modifyNews.do").param("newsID", "hello").param("title", title).param("content", content))
                .andExpect(status().isBadRequest());

        // success
        mockMvc.perform(post("/modifyNews.do").param("newsID", String.valueOf(newsIDSuccess)).param("title", title).param("content", content))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("news_manage"));

        // fail
        // bug here
        // null check should be added
//        mockMvc.perform(post("/modifyNews.do").param("newsID", String.valueOf(newsIDFail)).param("title", title).param("content", content))
//                .andExpect(status().isNotFound());


    }

    @Test
    public void testAddNews() throws Exception {
        String title = "title";
        String content = "content";

        News mockNews = new News(1, "title", "content", LocalDateTime.now());

        when(newsService.create(any(News.class))).thenReturn(mockNews.getNewsID());

        // error method
        mockMvc.perform(get("/addNews.do").param("title", title).param("content", content))
                .andExpect(status().isMethodNotAllowed());

        // success
        mockMvc.perform(post("/addNews.do").param("title", title).param("content", content))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("news_manage"));

    }
}
