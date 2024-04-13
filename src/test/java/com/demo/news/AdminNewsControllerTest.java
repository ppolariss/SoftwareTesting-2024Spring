package com.demo.news;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.demo.controller.admin.AdminNewsController;
import com.demo.entity.News;
import com.demo.entity.User;
import com.demo.service.NewsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@WebMvcTest(AdminNewsController.class)
public class AdminNewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NewsService newsService;

    private MockHttpServletRequest request;

    @BeforeEach
    public void setUp() {
        // 倒数第二个is admin字段标识身份，0-用户，1-管理员
        User admin = new User(1, "adminID", "adminName", "adminPassword", "admin@example.com", "15649851625", 1, "adminPic");
        request = new MockHttpServletRequest();
        Objects.requireNonNull(request.getSession()).setAttribute("admin", admin);
    }

    @Test
    public void testNewManageWithInvalidRole() {
        try {
            User user = new User(1, "userID", "userName", "userPassword", "user@example.com", "14695846221", 0, "userPic");
            Objects.requireNonNull(request.getSession()).setAttribute("admin", user);

            mockMvc.perform(get("/news_manage").session((MockHttpSession) request.getSession()))
                    .andExpect(status().isUnauthorized());  // expect 401 Unauthorized
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testNewsManageWithEmptyData() {
        try {
            List<News> newsList = new ArrayList<>();
            Page<News> mockPage = new PageImpl<>(newsList, PageRequest.of(0, 10), 0);

            // empty data
            when(newsService.findAll(any(Pageable.class))).thenReturn(mockPage);

            mockMvc.perform(get("/news_manage").session((MockHttpSession) request.getSession()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/news_manage"))
                    .andExpect(model().attribute("total", mockPage.getTotalPages()));
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testNewsManageWithNotEmptyData() {
        try {
            List<News> newsList = new ArrayList<>();
            newsList.add(new News(1,"title", "content", LocalDateTime.now()));

            // not empty data
            Page<News> mockPage = new PageImpl<>(newsList, PageRequest.of(0, 10), 1);
            when(newsService.findAll(any(Pageable.class))).thenReturn(mockPage);

            mockMvc.perform(get("/news_manage").session((MockHttpSession) request.getSession()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/news_manage"))
                    .andExpect(model().attribute("total", mockPage.getTotalPages()));
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testNewsAddWithInvalidRole() {
        try {
            User user = new User(1, "userID", "userName", "userPassword", "user@example.com", "14695846221", 0, "userPic");
            Objects.requireNonNull(request.getSession()).setAttribute("admin", user);

            mockMvc.perform(get("/news_add").session((MockHttpSession) request.getSession()))
                    .andExpect(status().isUnauthorized());  // expect 401 Unauthorized
        } catch (Exception e) {
            fail();
        }
    }


    @Test
    public void testNewsAdd() {
        try {
            mockMvc.perform(get("/news_add").session((MockHttpSession) request.getSession()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("/admin/news_add"));
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testNewsEditWithInvalidRole() {
        try {
            User user = new User(1, "userID", "userName", "userPassword", "user@example.com", "14695846221", 0, "userPic");
            Objects.requireNonNull(request.getSession()).setAttribute("admin", user);

            mockMvc.perform(get("/news_edit").session((MockHttpSession) request.getSession()))
                    .andExpect(status().isUnauthorized());  // expect 401 Unauthorized
        } catch (Exception e) {
            fail();
        }
    }


    @Test
    public void testNewsEditWithEmptyParam() {
        try {
            // empty param
            mockMvc.perform(get("/news_edit").session((MockHttpSession) request.getSession()))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testNewsEditWithNotIntParam() {
        try {
            // error param
            mockMvc.perform(get("/news_edit").param("newsID", "hello").session((MockHttpSession) request.getSession()))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testNewsEditWithNegativeIntParam() {
        try {
            // error param
            mockMvc.perform(get("/news_edit").param("newsID", "-10").session((MockHttpSession) request.getSession()))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testNewsEditWithSuccess() {
        try {
            int newsIDSuccess = 1;
            int newsIDFail = 2;

            News mockNews = new News(1, "title", "content", LocalDateTime.now());

            when(newsService.findById(1)).thenReturn(mockNews);
            when(newsService.findById(2)).thenThrow(EntityNotFoundException.class);

            // positive int param
            // success
            mockMvc.perform(get("/news_edit").param("newsID", String.valueOf(newsIDSuccess)).session((MockHttpSession) request.getSession()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("/admin/news_edit"))
                    .andExpect(model().attribute("news", mockNews));
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testNewsEditWithNotFound() {
        try {
            int newsIDSuccess = 1;
            int newsIDFail = 2;

            News mockNews = new News(1, "title", "content", LocalDateTime.now());

            when(newsService.findById(1)).thenReturn(mockNews);
            when(newsService.findById(2)).thenThrow(EntityNotFoundException.class);

            // positive int param
            // not found
            mockMvc.perform(get("/news_edit").param("newsID", String.valueOf(newsIDFail)).session((MockHttpSession) request.getSession()))
                    .andExpect(status().isNotFound());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testNewsListWithInvalidRole() {
        try {
            User user = new User(1, "userID", "userName", "userPassword", "user@example.com", "14695846221", 0, "userPic");
            Objects.requireNonNull(request.getSession()).setAttribute("admin", user);

            mockMvc.perform(get("/newsList.do").session((MockHttpSession) request.getSession()))
                    .andExpect(status().isUnauthorized());  // expect 401 Unauthorized
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testNewsListWithEmptyParam() {
        try {
            List<News> newsList = new ArrayList<>();
            newsList.add(new News(1, "title", "content", LocalDateTime.now()));
            newsList.add(new News(2, "title", "content", LocalDateTime.now()));

            Page<News> mockPage = new PageImpl<>(newsList);

            when(newsService.findAll(PageRequest.of(0, 10, Sort.by("time").descending()))).thenReturn(mockPage);
            when(newsService.findAll(PageRequest.of(1, 10, Sort.by("time").descending()))).thenReturn(null);

            // default request
            mockMvc.perform(get("/newsList.do").session((MockHttpSession) request.getSession()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$").isArray());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testNewsListWithNotIntParam() {
        try {
            // error param
            mockMvc.perform(get("/newsList.do").param("page", "hello").session((MockHttpSession) request.getSession()))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testNewsListWithNegativeIntParam() {
        try {
            // error param
            mockMvc.perform(get("/newsList.do").param("page", "-10").session((MockHttpSession) request.getSession()))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testNewsListWithSuccess() {
        try {
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
            mockMvc.perform(get("/newsList.do").param("page", String.valueOf(pageSuccess)).session((MockHttpSession) request.getSession()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(content().json("[{\"newsID\":1,\"title\":\"title\",\"content\":\"content\",\"time\":\"2021-01-01 00:00:00\"}]"))
                    .andExpect(jsonPath("$").isArray());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testNewsListWithNotFound() {
        try {
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
            mockMvc.perform(get("/newsList.do").param("page", String.valueOf(pageFail)).session((MockHttpSession) request.getSession()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(content().json("[]"))
                    .andExpect(jsonPath("$").isArray());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testDelNewsWithInvalidRole() {
        try {
            User user = new User(1, "userID", "userName", "userPassword", "user@example.com", "14695846221", 0, "userPic");
            Objects.requireNonNull(request.getSession()).setAttribute("admin", user);

            mockMvc.perform(get("/delNews.do").session((MockHttpSession) request.getSession()))
                    .andExpect(status().isUnauthorized());  // expect 401 Unauthorized
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testDelNewsWithEmptyParam() {
        try {
            // empty param
            mockMvc.perform(post("/delNews.do").session((MockHttpSession) request.getSession()))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testDelNewsWithNotIntParam() {
        try {
            // error param
            mockMvc.perform(post("/delNews.do").param("newsID", "hello").session((MockHttpSession) request.getSession()))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testDelNewsWithNegativeIntParam() {
        try {
            // error param
            mockMvc.perform(post("/delNews.do").param("newsID", "-10").session((MockHttpSession) request.getSession()))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testDelNewsWithSuccess() {
        try {
            int newsIDSuccess = 1;
            int newsIDFail = 2;

            doNothing().when(newsService).delById(newsIDSuccess);
            doNothing().when(newsService).delById(newsIDFail);

            // success
            mockMvc.perform(post("/delNews.do").param("newsID", String.valueOf(newsIDSuccess)).session((MockHttpSession) request.getSession()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("true"));

            verify(newsService).delById(newsIDSuccess);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testDelNewsWithNotFound() {
        try {
            int newsIDSuccess = 1;
            int newsIDFail = 2;

            doNothing().when(newsService).delById(newsIDSuccess);
            doNothing().when(newsService).delById(newsIDFail);

            // not found
            // false case should be added
            mockMvc.perform(post("/delNews.do").param("newsID", String.valueOf(newsIDFail)).session((MockHttpSession) request.getSession()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("false"));

            verify(newsService).delById(newsIDFail);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testModifyNewsWithInvalidRole() {
        try {
            User user = new User(1, "userID", "userName", "userPassword", "user@example.com", "14695846221", 0, "userPic");
            Objects.requireNonNull(request.getSession()).setAttribute("admin", user);

            mockMvc.perform(get("/modifyNews.do").session((MockHttpSession) request.getSession()))
                    .andExpect(status().isUnauthorized());  // expect 401 Unauthorized
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testModifyNewsWithEmptyParam() {
        try {
            // empty param
            mockMvc.perform(post("/modifyNews.do").session((MockHttpSession) request.getSession()))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testModifyNewsWithIdNotIntParam() {
        try {
            // error param
            mockMvc.perform(post("/modifyNews.do")
                            .param("newsID", "hello")
                            .param("title", "abc")
                            .param("content", "abc")
                            .session((MockHttpSession) request.getSession()))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testModifyNewsWithIdNegativeIntParam() {
        try {
            // error param
            mockMvc.perform(post("/modifyNews.do")
                            .param("newsID", "-10")
                            .param("title", "abc")
                            .param("content", "abc")
                            .session((MockHttpSession) request.getSession()))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testModifyNewsWithSuccess() {
        try {
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
                            .param("content", content)
                            .session((MockHttpSession) request.getSession()))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrl("news_manage"));

            verify(newsService).update(mockNews);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testModifyNewsWithNotFound() {
        try {
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
                            .param("content", content)
                            .session((MockHttpSession) request.getSession()))
                    .andExpect(status().isNotFound());
        } catch (Exception e) {
            fail();
        }

    }

    @Test
    public void testAddNewsWithInvalidRole() {
        try {
            User user = new User(1, "userID", "userName", "userPassword", "user@example.com", "14695846221", 0, "userPic");
            Objects.requireNonNull(request.getSession()).setAttribute("admin", user);

            mockMvc.perform(get("/addNews.do").session((MockHttpSession) request.getSession()))
                    .andExpect(status().isUnauthorized());  // expect 401 Unauthorized
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testAddNewsWithEmptyParam() {
        try {
            // empty param
            mockMvc.perform(post("/addNews.do").session((MockHttpSession) request.getSession()))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testAddNewsWithSuccess() {
        try {
            String title = "title";
            String content = "content";

            News mockNews = new News(1, "title", "content", LocalDateTime.now());

            when(newsService.create(mockNews)).thenReturn(mockNews.getNewsID());

            // success
            mockMvc.perform(post("/addNews.do").param("title", title).param("content", content).session((MockHttpSession) request.getSession()))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrl("news_manage"));
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testAddNewsWithFail() {
        try {
            String title = "title";
            String content = "content";

            News mockNews = new News(1, "title", "content", LocalDateTime.now());

            when(newsService.create(mockNews)).thenReturn(0);

            // fail
            mockMvc.perform(post("/addNews.do").param("title", title).param("content", content).session((MockHttpSession) request.getSession()))
                    .andExpect(status().is3xxRedirection());
        } catch (Exception e) {
            fail();
        }
    }
}
