package com.demo.news;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
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

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@WebMvcTest(NewsController.class)
public class NewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NewsService newsService;

    @Test
    public void testNewsWithEmptyParam() throws Exception {
        try {
            // empty param
            mockMvc.perform(get("/news"))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            fail();
        }
    }


    @Test
    public void testNewsWithNotIntParam() throws Exception {
        try {
            // not int param
            mockMvc.perform(get("/news").param("newsID", "hello"))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testNewsWithNegativeIntParam() throws Exception {
        try {
            // negative int param
            mockMvc.perform(get("/news").param("newsID", "-10"))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testNewsWithSuccess() throws Exception {
        try {
            int newsIDSuccess = 1;
            int newsIDFail = 2;
            News mockNews1 = new News(1, "title", "content", LocalDateTime.now());

            when(newsService.findById(1)).thenReturn(mockNews1);
            when(newsService.findById(2)).thenThrow(EntityNotFoundException.class);

            // positive int param
            // news found
            mockMvc.perform(get("/news").param("newsID", String.valueOf(newsIDSuccess)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("news"))
                    .andExpect(model().attribute("news", mockNews1));
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testNewsWithNotFound() throws Exception {
        try {
            int newsIDSuccess = 1;
            int newsIDFail = 2;
            News mockNews1 = new News(1, "title", "content", LocalDateTime.now());

            when(newsService.findById(1)).thenReturn(mockNews1);
            when(newsService.findById(2)).thenThrow(EntityNotFoundException.class);

            // positive int param
            // news not found
            mockMvc.perform(get("/news").param("newsID", String.valueOf(newsIDFail)))
                    .andExpect(status().isNotFound());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testNewsListWithEmptyParam() throws Exception {
        try {
            List<News> newsList = new ArrayList<>();
            LocalDateTime time = LocalDateTime.of(2021, 1, 1, 0, 0);
            newsList.add(new News(1, "title", "content", time));

            Page<News> mockPage = new PageImpl<>(newsList, PageRequest.of(0, 5, Sort.by("time").descending()), 5);
            Page<News> mockEmptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 5, Sort.by("time").descending()), 0);

            when(newsService.findAll(PageRequest.of(0, 5, Sort.by("time").descending()))).thenReturn(mockPage);
            when(newsService.findAll(PageRequest.of(1, 5, Sort.by("time").descending()))).thenReturn(mockEmptyPage);

            // empty param
            mockMvc.perform(get("/news/getNewsList"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(content().json("{\"content\":[{\"newsID\":1,\"title\":\"title\",\"content\":\"content\",\"time\":\"2021-01-01 00:00:00\"}],\"pageable\":{\"sort\":{\"sorted\":true,\"unsorted\":false,\"empty\":false},\"offset\":0,\"pageNumber\":0,\"pageSize\":5,\"paged\":true,\"unpaged\":false},\"totalPages\":1,\"totalElements\":5,\"last\":true,\"size\":5,\"number\":0,\"numberOfElements\":1,\"first\":true,\"empty\":false}"))
                    .andExpect(jsonPath("$.content").isArray());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testNewsListWithNotIntParam() throws Exception {
        try {
            // not int param
            mockMvc.perform(get("/news/getNewsList").param("page", "hello"))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testNewsListWithNegativeIntParam() throws Exception {
        try {
            // negative int param
            mockMvc.perform(get("/news/getNewsList").param("page", "-10"))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testNewsListWithSuccess() throws Exception {
        try {
            int pageSuccess = 1;
            int pageFail = 2;

            List<News> newsList = new ArrayList<>();
            LocalDateTime time = LocalDateTime.of(2021, 1, 1, 0, 0);
            newsList.add(new News(1, "title", "content", time));

            Page<News> mockPage = new PageImpl<>(newsList, PageRequest.of(0, 5, Sort.by("time").descending()), 5);
            Page<News> mockEmptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 5, Sort.by("time").descending()), 0);

            when(newsService.findAll(PageRequest.of(0, 5, Sort.by("time").descending()))).thenReturn(mockPage);
            when(newsService.findAll(PageRequest.of(1, 5, Sort.by("time").descending()))).thenReturn(mockEmptyPage);

            // positive int param
            // page found
            mockMvc.perform(get("/news/getNewsList").param("page", String.valueOf(pageSuccess)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(content().json("{\"content\":[{\"newsID\":1,\"title\":\"title\",\"content\":\"content\",\"time\":\"2021-01-01 00:00:00\"}],\"pageable\":{\"sort\":{\"sorted\":true,\"unsorted\":false,\"empty\":false},\"offset\":0,\"pageNumber\":0,\"pageSize\":5,\"paged\":true,\"unpaged\":false},\"totalPages\":1,\"totalElements\":5,\"last\":true,\"size\":5,\"number\":0,\"numberOfElements\":1,\"first\":true,\"empty\":false}"))
                    .andExpect(jsonPath("$.content").isArray());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testNewsListWithNotFound() throws Exception {
        try {
            int pageSuccess = 1;
            int pageFail = 2;

            List<News> newsList = new ArrayList<>();
            LocalDateTime time = LocalDateTime.of(2021, 1, 1, 0, 0);
            newsList.add(new News(1, "title", "content", time));

            Page<News> mockPage = new PageImpl<>(newsList, PageRequest.of(0, 5, Sort.by("time").descending()), 5);
            Page<News> mockEmptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 5, Sort.by("time").descending()), 0);

            when(newsService.findAll(PageRequest.of(0, 5, Sort.by("time").descending()))).thenReturn(mockPage);
            when(newsService.findAll(PageRequest.of(1, 5, Sort.by("time").descending()))).thenReturn(mockEmptyPage);


            // positive int param
            // page not found
            mockMvc.perform(get("/news/getNewsList").param("page", String.valueOf(pageFail)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(content().json("{\"content\":[],\"pageable\":{\"sort\":{\"sorted\":true,\"unsorted\":false,\"empty\":false},\"offset\":0,\"pageNumber\":0,\"pageSize\":5,\"paged\":true,\"unpaged\":false},\"totalPages\":0,\"totalElements\":0,\"last\":true,\"size\":5,\"number\":0,\"numberOfElements\":0,\"first\":true,\"empty\":true}"));
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testNewsListPageWithEmptyData() throws Exception {
        try {
            Page<News> mockEmptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 5, Sort.by("time").descending()), 0);

            // empty page
            when(newsService.findAll(PageRequest.of(0, 5, Sort.by("time").descending()))).thenReturn(mockEmptyPage);

            mockMvc.perform(get("/news_list"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("news_list"))
                    .andExpect(model().attribute("news_list", new ArrayList<>()))
                    .andExpect(model().attribute("total", mockEmptyPage.getTotalPages()));
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testNewsListPageWithNotEmptyData() throws Exception {
        try {
            List<News> newsList = new ArrayList<>();

            newsList.add(new News(1, "title", "content", LocalDateTime.now()));
            newsList.add(new News(2, "title", "content", LocalDateTime.now()));

            Page<News> mockNotEmptyPage = new PageImpl<>(newsList, PageRequest.of(0, 5, Sort.by("time").descending()), 2);

            // not empty page
            when(newsService.findAll(PageRequest.of(0, 5, Sort.by("time").descending()))).thenReturn(mockNotEmptyPage);

            mockMvc.perform(get("/news_list"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("news_list"))
                    .andExpect(model().attribute("news_list", newsList))
                    .andExpect(model().attribute("total", mockNotEmptyPage.getTotalPages()));
        } catch (Exception e) {
            fail();
        }
    }


}
