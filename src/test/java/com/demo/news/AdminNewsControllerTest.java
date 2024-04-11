package com.demo.news;

import static org.junit.jupiter.api.Assertions.assertNull;
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

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@WebMvcTest(AdminNewsController.class)
public class AdminNewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NewsService newsService;

    @Test
    public void testNewsManageWithEmptyData() throws Exception {
        List<News> newsList = new ArrayList<>();
        Page<News> mockPage = new PageImpl<>(newsList, PageRequest.of(0, 10), 0);

        // empty data
        when(newsService.findAll(any(Pageable.class))).thenReturn(mockPage);

        mockMvc.perform(get("/news_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/news_manage"))
                .andExpect(model().attribute("total", mockPage.getTotalPages()));
    }

    @Test
    public void testNewsManageWithNotEmptyData() throws Exception {
        List<News> newsList = new ArrayList<>();
        newsList.add(new News(1,"title", "content", LocalDateTime.now()));

        // not empty data
        Page<News> mockPage = new PageImpl<>(newsList, PageRequest.of(0, 10), 1);
        when(newsService.findAll(any(Pageable.class))).thenReturn(mockPage);

        mockMvc.perform(get("/news_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/news_manage"))
                .andExpect(model().attribute("total", mockPage.getTotalPages()));
    }


    @Test
    public void testNewsAdd() throws Exception {
        mockMvc.perform(get("/news_add"))
                .andExpect(status().isOk())
                .andExpect(view().name("/admin/news_add"));
    }

    @Test
    public void testNewsEditWithEmptyParam() throws Exception {
        // empty param
        mockMvc.perform(get("/news_edit"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testNewsEditWithNotIntParam() throws Exception {
        // error param
        mockMvc.perform(get("/news_edit").param("newsID", "hello"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testNewsEditWithNegativeIntParam() throws Exception {
        // error param
        mockMvc.perform(get("/news_edit").param("newsID", "-10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testNewsEditWithSuccess() throws Exception {
        int newsIDSuccess = 1;
        int newsIDFail = 2;

        News mockNews = new News(1, "title", "content", LocalDateTime.now());

        when(newsService.findById(1)).thenReturn(mockNews);
        when(newsService.findById(2)).thenThrow(EntityNotFoundException.class);

        // positive int param
        // success
        mockMvc.perform(get("/news_edit").param("newsID", String.valueOf(newsIDSuccess)))
                .andExpect(status().isOk())
                .andExpect(view().name("/admin/news_edit"))
                .andExpect(model().attribute("news", mockNews));
    }

    @Test
    public void testNewsEditWithNotFound() throws Exception {
        int newsIDSuccess = 1;
        int newsIDFail = 2;

        News mockNews = new News(1, "title", "content", LocalDateTime.now());

        when(newsService.findById(1)).thenReturn(mockNews);
        when(newsService.findById(2)).thenThrow(EntityNotFoundException.class);

        // positive int param
        // not found
        mockMvc.perform(get("/news_edit").param("newsID", String.valueOf(newsIDFail)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testNewsListWithEmptyParam() throws Exception {

        List<News> newsList = new ArrayList<>();
        newsList.add(new News(1, "title", "content", LocalDateTime.now()));
        newsList.add(new News(2, "title", "content", LocalDateTime.now()));

        Page<News> mockPage = new PageImpl<>(newsList);

        when(newsService.findAll(PageRequest.of(0, 10, Sort.by("time").descending()))).thenReturn(mockPage);
        when(newsService.findAll(PageRequest.of(1, 10, Sort.by("time").descending()))).thenReturn(null);

        // default request
        mockMvc.perform(get("/newsList.do"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    public void testNewsListWithNotIntParam() throws Exception {
        // error param
        mockMvc.perform(get("/newsList.do").param("page", "hello"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testNewsListWithNegativeIntParam() throws Exception {
        // error param
        mockMvc.perform(get("/newsList.do").param("page", "-10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testNewsListWithSuccess() throws Exception {
        int pageSuccess = 1;
        int pageFail = 2;

        List<News> newsList = new ArrayList<>();
        LocalDateTime time = LocalDateTime.of(2021, 1, 1, 0, 0);
        newsList.add(new News(1, "title", "content", time));

        Page<News> mockPage = new PageImpl<>(newsList, PageRequest.of(0, 10, Sort.by("time").descending()), 1);
        Page<News> mockEmptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10, Sort.by("time").descending()), 0);

        when(newsService.findAll(PageRequest.of(0, 10, Sort.by("time").descending()))).thenReturn(mockPage);
        when(newsService.findAll(PageRequest.of(1, 10, Sort.by("time").descending()))).thenReturn(mockEmptyPage);

        // page found
        mockMvc.perform(get("/newsList.do").param("page", String.valueOf(pageSuccess)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(content().json("[{\"newsID\":1,\"title\":\"title\",\"content\":\"content\",\"time\":\"2021-01-01 00:00:00\"}]"))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    public void testNewsListWithNotFound() throws Exception {
        int pageSuccess = 1;
        int pageFail = 2;

        List<News> newsList = new ArrayList<>();
        LocalDateTime time = LocalDateTime.of(2021, 1, 1, 0, 0);
        newsList.add(new News(1, "title", "content", time));

        Page<News> mockPage = new PageImpl<>(newsList, PageRequest.of(0, 10, Sort.by("time").descending()), 1);
        Page<News> mockEmptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10, Sort.by("time").descending()), 0);

        when(newsService.findAll(PageRequest.of(0, 10, Sort.by("time").descending()))).thenReturn(mockPage);
        when(newsService.findAll(PageRequest.of(1, 10, Sort.by("time").descending()))).thenReturn(mockEmptyPage);

        // page not found
        mockMvc.perform(get("/newsList.do").param("page", String.valueOf(pageFail)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(content().json("[]"))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    public void testDelNewsWithEmptyParam() throws Exception {
        // empty param
        mockMvc.perform(post("/delNews.do"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testDelNewsWithNotIntParam() throws Exception {
        // error param
        mockMvc.perform(post("/delNews.do").param("newsID", "hello"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testDelNewsWithNegativeIntParam() throws Exception {
        // error param
        mockMvc.perform(post("/delNews.do").param("newsID", "-10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testDelNewsWithSuccess() throws Exception {
        int newsIDSuccess = 1;
        int newsIDFail = 2;

        doNothing().when(newsService).delById(newsIDSuccess);
        doNothing().when(newsService).delById(newsIDFail);

        // success
        mockMvc.perform(post("/delNews.do").param("newsID", String.valueOf(newsIDSuccess)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(newsService).delById(newsIDSuccess);
    }

    @Test
    public void testDelNewsWithNotFound() throws Exception {
        int newsIDSuccess = 1;
        int newsIDFail = 2;

        doNothing().when(newsService).delById(newsIDSuccess);
        doNothing().when(newsService).delById(newsIDFail);

        // not found
        // false case should be added
        mockMvc.perform(post("/delNews.do").param("newsID", String.valueOf(newsIDFail)))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        verify(newsService).delById(newsIDFail);
    }

    @Test
    public void testModifyNewsWithEmptyParam() throws Exception {
        // empty param
        mockMvc.perform(post("/modifyNews.do"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testModifyNewsWithIdNotIntParam() throws Exception {
        // error param
        mockMvc.perform(post("/modifyNews.do")
                        .param("newsID", "hello")
                        .param("title", "abc")
                        .param("content", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testModifyNewsWithIdNegativeIntParam() throws Exception {
        // error param
        mockMvc.perform(post("/modifyNews.do")
                        .param("newsID", "-10")
                        .param("title", "abc")
                        .param("content", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testModifyNewsWithSuccess() throws Exception {
        int newsIDSuccess = 1;
        int newsIDFail = 2;
        String title = "title";
        String content = "content";

        News mockNews = new News(1, title, content, LocalDateTime.now());

        doNothing().when(newsService).update(mockNews);

        when(newsService.findById(newsIDSuccess)).thenReturn(mockNews);
        when(newsService.findById(newsIDFail)).thenThrow(EntityNotFoundException.class);

        // success
        mockMvc.perform(post("/modifyNews.do")
                        .param("newsID", String.valueOf(newsIDSuccess))
                        .param("title", title)
                        .param("content", content))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("news_manage"));

        verify(newsService).update(mockNews);
    }

    @Test
    public void testModifyNewsWithNotFound() throws Exception {
        int newsIDSuccess = 1;
        int newsIDFail = 2;
        String title = "title";
        String content = "content";

        News mockNews = new News(1, title, content, LocalDateTime.now());

        doNothing().when(newsService).update(mockNews);

        when(newsService.findById(newsIDSuccess)).thenReturn(mockNews);
        when(newsService.findById(newsIDFail)).thenThrow(EntityNotFoundException.class);

        // not found
        mockMvc.perform(post("/modifyNews.do")
                        .param("newsID", String.valueOf(newsIDFail))
                        .param("title", title)
                        .param("content", content))
                .andExpect(status().isNotFound());

    }


    @Test
    public void testAddNewsWithEmptyParam() throws Exception {
        // empty param
        mockMvc.perform(post("/addNews.do"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testAddNewsWithSuccess() throws Exception {
        String title = "title";
        String content = "content";

        News mockNews = new News(1, "title", "content", LocalDateTime.now());

        when(newsService.create(mockNews)).thenReturn(mockNews.getNewsID());

        // success
        mockMvc.perform(post("/addNews.do").param("title", title).param("content", content))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("news_manage"));
    }

    @Test
    public void testAddNewsWithFail() throws Exception {
        String title = "title";
        String content = "content";

        News mockNews = new News(1, "title", "content", LocalDateTime.now());

        when(newsService.create(mockNews)).thenReturn(0);

        // fail
        mockMvc.perform(post("/addNews.do").param("title", title).param("content", content))
                .andExpect(status().is3xxRedirection());
    }
}
