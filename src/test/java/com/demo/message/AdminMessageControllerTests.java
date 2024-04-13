package com.demo.message;

import com.demo.controller.admin.AdminMessageController;
import com.demo.entity.Message;
import com.demo.entity.User;
import com.demo.service.MessageService;
import com.demo.service.MessageVoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.data.domain.*;

@WebMvcTest(AdminMessageController.class)
public class AdminMessageControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MessageService messageService;

    @MockBean
    private MessageVoService messageVoService;

    private MockHttpServletRequest request;

    @BeforeEach
    public void setUp() {
        // 倒数第二个isadmin字段标识身份，0-用户，1-管理员
        User admin = new User(1, "adminID", "adminName", "adminPassword", "admin@example.com", "15649851625", 1, "adminPic");

        request = new MockHttpServletRequest();
        Objects.requireNonNull(request.getSession()).setAttribute("admin", admin);
    }

    @Test
    public void testMessageManageWithInvalidRole() throws Exception {
        // 倒数第二个isadmin字段设置为0，代表为用户
        User user = new User(1, "userID", "userName", "userPassword", "user@example.com", "14695846221", 0, "userPic");
        Objects.requireNonNull(request.getSession()).setAttribute("admin", user);

        mockMvc.perform(get("/message_manage").session((MockHttpSession) request.getSession()))
                .andExpect(status().isUnauthorized());  // expect 401 Unauthorized
    }
    @Test
    public void testMessageManageWithEmptyPage() throws Exception {

        Pageable message_pageable= PageRequest.of(0,10, Sort.by("time").descending());

        //实际返回空对象
        when(messageService.findWaitState(any()))
                .thenReturn(new PageImpl<>(Collections.emptyList(),message_pageable,0));

        mockMvc.perform(get("/message_manage").session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("total", 0))
                .andExpect(view().name("admin/message_manage"));

    }

    @Test
    public void testMessageManageWithMessagesMoreThanOnePage() throws Exception {

        List<Message> messages = IntStream.range(0,15)
                .mapToObj(i -> new Message(1,"user","test_message", LocalDateTime.now(),1))
                .collect(Collectors.toList());

        Page<Message> pageWithMessages =
                new PageImpl<>(messages, PageRequest.of(0, 10, Sort.by("time").descending()), messages.size());

        when(messageService.findWaitState(any(Pageable.class))).thenReturn(pageWithMessages);

        // Expect total pages to be 2 because there are more messages (15) than can fit on one page (10), necessitating a second page.
        mockMvc.perform(get("/message_manage").session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("total", 2))
                .andExpect(view().name("admin/message_manage"));
    }


    @Test
    public void testMessageListWithInvalidRole() throws Exception {
        // 倒数第二个isadmin字段设置为0，代表为用户
        User user = new User(1, "userID", "userName", "userPassword", "user@example.com", "14695846221", 0, "userPic");
        Objects.requireNonNull(request.getSession()).setAttribute("admin", user);

        mockMvc.perform(get("/messageList.do").session((MockHttpSession) request.getSession()))
                .andExpect(status().isUnauthorized());  // expect 401 Unauthorized
    }

    @Test
    public void testMessageListWithValidPage() throws Exception{
        List<Message> nonEmptyMessageList = new ArrayList<>();
        nonEmptyMessageList.add(new Message(1,"user","test_message", LocalDateTime.now(),1));
        Page<Message> mockPage = new PageImpl<>(nonEmptyMessageList);
        when(messageService.findWaitState(PageRequest.of(0,10,Sort.by("time").descending()))).thenReturn(mockPage);
        mockMvc.perform(get("/messageList.do").param("page", "1").session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
    @Test
    public void testMessageListWithEmptyParam() throws Exception{
        List<Message> nonEmptyMessageList = new ArrayList<>();
        nonEmptyMessageList.add(new Message(1,"user","test_message", LocalDateTime.now(),1));
        Page<Message> mockPage = new PageImpl<>(nonEmptyMessageList);
        when(messageService.findWaitState(PageRequest.of(0,10,Sort.by("time").descending()))).thenReturn(mockPage);
        mockMvc.perform(get("/messageList.do").session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    public void testMessageListWithEmptyPage() throws Exception{

        Pageable message_pageable= PageRequest.of(0,10, Sort.by("time").descending());
        when(messageService.findWaitState(any()))
                .thenReturn(new PageImpl<>(Collections.emptyList(),message_pageable,0));

        mockMvc.perform(get("/messageList.do").param("page", "1").session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0))); // 直接检查响应体是否为一个空数组

    }

    @Test
    public void testMessageListWithPageExceedLimit() throws Exception{
        List<Message> messages = IntStream.range(0,10)
                .mapToObj(i -> new Message(1,"user","test_message",LocalDateTime.now(),1))
                .collect(Collectors.toList());
        Page<Message> mockPage =
                new PageImpl<>(messages, PageRequest.of(0, 5, Sort.by("time").descending()), messages.size());
        when(messageService.findWaitState(any())).thenReturn(mockPage);
        mockMvc.perform(get("/messageList.do").param("page", "4").session((MockHttpSession) request.getSession()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void testMessageListWithPageNotPositive() throws Exception{
        mockMvc.perform(get("/messageList.do").param("page","0").session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testMessageListWithPageIsNotNum() throws Exception{
        mockMvc.perform(get("/messageList.do").param("page","hello").session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }


    @Test
    public void testPassMessageWithInvalidRole() throws Exception {
        // 倒数第二个isadmin字段设置为0，代表为用户
        User user = new User(1, "userID", "userName", "userPassword", "user@example.com", "14695846221", 0, "userPic");
        Objects.requireNonNull(request.getSession()).setAttribute("admin", user);

        mockMvc.perform(get("/passMessage.do").session((MockHttpSession) request.getSession()))
                .andExpect(status().isUnauthorized());  // expect 401 Unauthorized
    }

    @Test
    public void testPassMessageWithValidId() throws Exception{
        int validId = 1;

        //mock service
        Message mockMessage = new Message();
        mockMessage.setMessageID(1);
        doNothing().when(messageService).confirmMessage(1);

        mockMvc.perform(post("/passMessage.do").param("messageID",String.valueOf(validId)).session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    public void testPassMessageWithEmptyParam() throws Exception{
        mockMvc.perform(post("/passMessage.do").session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testPassMessageWithNotExistingId() throws Exception{

        int emptyId = 2;
        doThrow(new RuntimeException()).when(messageService).confirmMessage(2);

        mockMvc.perform(post("/passMessage.do").param("messageID",String.valueOf(emptyId)).session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));
    }

    @Test
    public void testPassMessageWithNegativeId() throws Exception{
        mockMvc.perform(post("/passMessage.do").param("messageID",String.valueOf("-1")).session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testPassMessageWithStringId() throws Exception{
        mockMvc.perform(post("/passMessage.do").param("messageID",String.valueOf("hello")).session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testRejectMessageWithInvalidRole() throws Exception {
        // 倒数第二个isadmin字段设置为0，代表为用户
        User user = new User(1, "userID", "userName", "userPassword", "user@example.com", "14695846221", 0, "userPic");
        Objects.requireNonNull(request.getSession()).setAttribute("admin", user);

        mockMvc.perform(get("/rejectMessage.do").session((MockHttpSession) request.getSession()))
                .andExpect(status().isUnauthorized());  // expect 401 Unauthorized
    }

    @Test
    public void testRejectMessageWithValidId() throws Exception{
        int validId = 1;

        //mock service
        Message mockMessage = new Message();
        mockMessage.setMessageID(1);
        doNothing().when(messageService).rejectMessage(1);

        mockMvc.perform(post("/rejectMessage.do").param("messageID",String.valueOf(validId)).session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    public void testRejectMessageWithEmptyParam() throws Exception{
        mockMvc.perform(post("/rejectMessage.do").session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testRejectMessageWithNotExistingId() throws Exception{
        int emptyId = 2;
        doThrow(new RuntimeException()).when(messageService).rejectMessage(2);

        mockMvc.perform(post("/rejectMessage.do").param("messageID",String.valueOf(emptyId)).session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    public void testRejectMessageWithNegativeId() throws Exception{
        mockMvc.perform(post("/rejectMessage.do").param("messageID",String.valueOf("-1")).session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testRejectMessageWithStringId() throws Exception{
        mockMvc.perform(post("/passMessage.do").param("messageID",String.valueOf("hello")).session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }


    @Test
    public void testDelMessageWithInvalidRole() throws Exception {
        // 倒数第二个isadmin字段设置为0，代表为用户
        User user = new User(1, "userID", "userName", "userPassword", "user@example.com", "14695846221", 0, "userPic");
        Objects.requireNonNull(request.getSession()).setAttribute("admin", user);

        mockMvc.perform(get("/delMessage.do").session((MockHttpSession) request.getSession()))
                .andExpect(status().isUnauthorized());  // expect 401 Unauthorized
    }
    @Test
    public void testDelMessageWithValidId() throws Exception{
        int validId = 1;
        Message mockMessage = new Message();
        mockMessage.setMessageID(1);
        doNothing().when(messageService).delById(1);

        mockMvc.perform(post("/delMessage.do").param("messageID",String.valueOf(validId)).session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));


    }

    @Test
    public void testDelMessageWithEmptyParam() throws Exception{
        mockMvc.perform(post("/delMessage.do").session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testDelMessageWithNotExistingId() throws Exception{
        int notExistingId = 999;

        doNothing().when(messageService).delById(notExistingId);

        mockMvc.perform(post("/delMessage.do").param("messageID",String.valueOf(notExistingId)).session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));
    }

    @Test
    public void testDelMessageWithNegativeId() throws Exception{
        mockMvc.perform(post("/delMessage.do").param("messageID",String.valueOf("-1")).session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testDelMessageWithStringId() throws Exception{
        mockMvc.perform(post("/delMessage.do").param("messageID",String.valueOf("hello")).session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }










}
