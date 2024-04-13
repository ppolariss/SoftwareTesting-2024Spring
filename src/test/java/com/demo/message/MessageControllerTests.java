package com.demo.message;
import com.demo.controller.admin.AdminMessageController;
import com.demo.controller.user.MessageController;
import com.demo.entity.Message;
import com.demo.entity.User;
import com.demo.entity.vo.MessageVo;
import com.demo.service.MessageService;
import com.demo.service.MessageVoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.data.domain.*;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(MessageController.class)
public class MessageControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MessageService messageService;

    @MockBean
    private MessageVoService messageVoService;


    @Test
    public void testMessageListWithUserHasNotLoggedIn() throws Exception {
        Pageable message_pageable= PageRequest.of(0,5, Sort.by("time").descending());

        //实际返回空对象
        when(messageService.findPassState(any()))
                .thenReturn(new PageImpl<>(Collections.emptyList(),message_pageable,0));

        MockHttpSession session = new MockHttpSession();
        //bug here
        mockMvc.perform(get("/message_list").session(session))
                .andExpect(status().is4xxClientError());

    }

    @Test
    public void testMessageListWithUserLogInButHasNoMessages() throws Exception{
        Pageable message_pageable= PageRequest.of(0,5, Sort.by("time").descending());

        MockHttpSession session = new MockHttpSession();
        User user = new User();
        user.setUserID("user");
        session.setAttribute("user", user);

        //实际返回空对象
        when(messageService.findPassState(any()))
                .thenReturn(new PageImpl<>(Collections.emptyList(),message_pageable,0));
        when(messageService.findByUser(eq("user"), any(Pageable.class))).thenReturn(new PageImpl<>(Collections.emptyList(), message_pageable, 0));

        mockMvc.perform(get("/message_list").session(session))
                .andExpect(model().attribute("total", 0)) // 验证total属性值为0
                .andExpect(model().attribute("user_total", 0)) // 验证user_total属性值为0
                .andExpect(view().name("message_list"));
    }

    @Test
    public void testMessageListWithUserLogInAndHasMoreThanOnePage() throws Exception{
        List<Message> messages = IntStream.range(0,10)
                .mapToObj(i -> new Message(1,"user","test_message",LocalDateTime.now(),1))
                .collect(Collectors.toList());
        Page<Message> mockPage =
                new PageImpl<>(messages, PageRequest.of(0, 5, Sort.by("time").descending()), messages.size());

        when(messageService.findPassState(any())).thenReturn(mockPage);
        MockHttpSession session = new MockHttpSession();
        User user = new User();
        user.setUserID("user");
        session.setAttribute("user", user);
        when(messageService.findByUser(eq("user"), any(Pageable.class))).thenReturn(mockPage);

        mockMvc.perform(get("/message_list").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attribute("total", 2)) // 验证total属性值为1
                .andExpect(model().attribute("user_total", 2)) // 验证user_total属性值为1
                .andExpect(view().name("message_list"));
    }

    @Test
    public void testMessageListWithValidPage() throws Exception{
        List<Message> nonEmptyMessageList = new ArrayList<>();
        nonEmptyMessageList.add(new Message(1,"user","test_message", LocalDateTime.now(),2));
        Page<Message> mockPage = new PageImpl<>(nonEmptyMessageList);
        when(messageService.findPassState(PageRequest.of(0,5,Sort.by("time").descending()))).thenReturn(mockPage);
        mockMvc.perform(get("/message/getMessageList").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
    @Test
    public void testMessageListWithEmptyParam() throws Exception{
        List<Message> nonEmptyMessageList = new ArrayList<>();
        nonEmptyMessageList.add(new Message(1,"user","test_message", LocalDateTime.now(),2));
        Page<Message> mockPage = new PageImpl<>(nonEmptyMessageList);
        when(messageService.findPassState(PageRequest.of(0,5,Sort.by("time").descending()))).thenReturn(mockPage);
        mockMvc.perform(get("/message/getMessageList"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    public void testMessageListWithEmptyPage() throws Exception{
        Pageable message_pageable= PageRequest.of(0,5, Sort.by("time").descending());
        when(messageService.findPassState(any()))
                .thenReturn(new PageImpl<>(Collections.emptyList(),message_pageable,0));

        mockMvc.perform(get("/message/getMessageList").param("page", "1"))
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
        when(messageService.findPassState(any())).thenReturn(mockPage);
        mockMvc.perform(get("/message/getMessageList").param("page", "4"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

    }

    @Test
    public void testMessageListWithPageNotPositive() throws Exception{
        mockMvc.perform(get("/message/getMessageList").param("page","0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testMessageListWithPageIsNotNum() throws Exception{
        mockMvc.perform(get("/message/getMessageList").param("page","hello"))
                .andExpect(status().isBadRequest());
    }


    @Test
    public void testUserMessageListWithUserHasNotLoggedIn() throws Exception{
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(get("/message/findUserList").session(session))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void testUserMessageListWithUserLogInButHasNoMessages() throws Exception{
        Pageable message_pageable= PageRequest.of(0,5, Sort.by("time").descending());

        MockHttpSession session = new MockHttpSession();
        User user = new User();
        user.setUserID("user");
        session.setAttribute("user", user);

        //实际返回空对象
        when(messageService.findByUser(eq("user"), any(Pageable.class))).thenReturn(new PageImpl<>(Collections.emptyList(), message_pageable, 0));

        mockMvc.perform(get("/message/findUserList").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

    }

    @Test
    public void testUserMessageListWithUserLogInAndValidPageParam() throws Exception{
        List<Message> nonEmptyMessageList = new ArrayList<>();
        nonEmptyMessageList.add(new Message(1,"user","test_message", LocalDateTime.now(),2));
        List<MessageVo> messageVos = new ArrayList<>();
        messageVos.add(new MessageVo(1,"user","test_message", LocalDateTime.now(),"user","test.jpg",2));
        Page<Message> mockPage = new PageImpl<>(nonEmptyMessageList,PageRequest.of(0, 10, Sort.by("time").descending()) ,nonEmptyMessageList.size());


        MockHttpSession session = new MockHttpSession();
        User user = new User();
        user.setUserID("user");
        session.setAttribute("user", user);
        when(messageService.findByUser(eq("user"), any(Pageable.class))).thenReturn(mockPage);
        when(messageVoService.returnVo(nonEmptyMessageList)).thenReturn(messageVos);


        mockMvc.perform(get("/message/findUserList").param("page","1").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));;

    }

    @Test
    public void testUserMessageListWithUserLogInButEmptyPageParam() throws Exception{
        List<Message> nonEmptyMessageList = new ArrayList<>();
        nonEmptyMessageList.add(new Message(1,"user","test_message", LocalDateTime.now(),2));
        List<MessageVo> messageVos = new ArrayList<>();
        messageVos.add(new MessageVo(1,"user","test_message", LocalDateTime.now(),"user","test.jpg",2));
        Page<Message> mockPage = new PageImpl<>(nonEmptyMessageList,PageRequest.of(0, 10, Sort.by("time").descending()) ,nonEmptyMessageList.size());


        MockHttpSession session = new MockHttpSession();
        User user = new User();
        user.setUserID("user");
        session.setAttribute("user", user);
        when(messageService.findByUser(eq("user"), any(Pageable.class))).thenReturn(mockPage);
        when(messageVoService.returnVo(nonEmptyMessageList)).thenReturn(messageVos);


        mockMvc.perform(get("/message/findUserList").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));;

    }
    @Test
    public void testUserMessageListWithUserLogInButNegativePageParam() throws Exception{
        MockHttpSession session = new MockHttpSession();
        User user = new User();
        user.setUserID("user");
        session.setAttribute("user", user);

        mockMvc.perform(get("/message/findUserList").param("page","-1").session(session))
                .andExpect(status().isBadRequest());

    }
    @Test
    public void testUserMessageListWithUserLogInButStringPageParam() throws Exception{

        MockHttpSession session = new MockHttpSession();
        User user = new User();
        user.setUserID("user");
        session.setAttribute("user", user);

        mockMvc.perform(get("/message/findUserList").param("page","hello").session(session))
                .andExpect(status().isBadRequest());

    }


    //添加用户鉴权
    @Test
    public void testSendMessageWithInvalidRole() throws Exception{
        // 构建请求，包含发送消息所需的参数，但不设置认证信息
        MockHttpServletRequestBuilder request = post("/sendMessage")
                .param("userID", "1") // 假设有效的用户ID
                .param("content", "Test message"); // 消息内容

        // 执行请求并验证响应状态码
        mockMvc.perform(request)
                .andExpect(status().isUnauthorized()); // 期望返回401未授权状态

    }

    @Test
    public void testSendMessageWithSuccess() throws Exception{
        String userID = "user";
        String content = "Test message";

        // 创建一个带有用户属性的会话
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", new User()); // 假设User对象代表认证的用户

        when(messageService.create(any(Message.class)))
                .thenReturn(1);

        // 构建请求并添加必要的参数
        ResultActions actions = mockMvc.perform(post("/sendMessage")
                        .param("userID", userID)
                        .param("content", content)
                        .session(session)) // 使用带有用户属性的会话
                .andExpect(status().is3xxRedirection()) // 期望重定向
                .andExpect(redirectedUrl("/message_list")); // 验证重定向到预期的URL
    }

    @Test
    public void testSendMessageWithEmptyParam() {
        try {
            mockMvc.perform(post("/sendMessage"))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            fail();
        }
    }






    @Test
    public void testModifyMessageWithEmptyParam() throws Exception{
        mockMvc.perform(post("/modifyMessage.do"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testModifyMessageWithIdNotIntParam() throws Exception{
        mockMvc.perform(post("/modifyMessage.do")
                .param("messageID","hello")
                .param("content","content"))
                .andExpect(status().isBadRequest());

    }
    @Test
    public void testModifyMessageWithIdNegativeIntParam() throws Exception{
        mockMvc.perform(post("/modifyMessage.do")
                        .param("messageID","-1")
                        .param("content","content"))
                .andExpect(status().isBadRequest());

    }
    @Test
    public void testModifyMessageWithSuccess() throws Exception{
        int messageID = 1;
        String content = "New content";

        Message message = new Message(messageID, content, "test_message", LocalDateTime.now(), 2);
        doNothing().when(messageService).update(message);

        when(messageService.findById(messageID)).thenReturn(message);

        // 创建一个带有用户属性的会话
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", new User()); // 假设User对象代表认证的用户

        // 构建请求并添加必要的参数和会话
        ResultActions actions = mockMvc.perform(post("/modifyMessage.do")
                        .param("messageID", String.valueOf(messageID))
                        .param("content", content)
                        .session(session)) // 使用带有用户属性的会话
                .andExpect(status().isOk()) // 期望请求成功
                .andExpect(jsonPath("$").value(true)); // 验证响应体中的值

        // 清理Mock对象
        verify(messageService, times(1)).update(message);
        reset(messageService);

    }

    @Test
    public void testModifyMessageWithNotFound() throws Exception{
        int messageID = 2;
        String content = "content";

        when(messageService.findById(messageID)).thenReturn(null);

        mockMvc.perform(post("/modifyMessage.do")
                        .param("messageID",String.valueOf(messageID))
                        .param("content",content))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));

    }


    @Test
    public void testDelMessageWithValidId() throws Exception{
        // 模拟messageService的行为
        doNothing().when(messageService).delById(1);

        // 构建请求并添加必要的参数
        mockMvc.perform(post("/delMessage.do")
                        .param("messageID", "1"))
                .andExpect(status().isOk()) // 期望请求成功
                .andExpect(jsonPath("$").value(true)); // 验证响应体中的值

        // 验证messageService的行为是否被调用
        verify(messageService).delById(1);


    }

    @Test
    public void testDelMessageWithEmptyParam() throws Exception{
        mockMvc.perform(post("/delMessage.do"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testDelMessageWithNotExistingId() throws Exception{
        int notExistingId = 999;

        doNothing()
                .when(messageService).delById(notExistingId);

        mockMvc.perform(post("/delMessage.do").param("messageID",String.valueOf(notExistingId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));
    }

    @Test
    public void testDelMessageWithNegativeId() throws Exception{
        mockMvc.perform(post("/delMessage.do").param("messageID",String.valueOf("-1")))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testDelMessageWithStringId() throws Exception{
        mockMvc.perform(post("/delMessage.do").param("messageID",String.valueOf("hello")))
                .andExpect(status().isBadRequest());
    }


}
