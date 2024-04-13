package com.demo.user;

import com.demo.controller.admin.AdminUserController;
import com.demo.entity.User;
import com.demo.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;

@WebMvcTest(AdminUserController.class)
public class AdminUserControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    private MockHttpServletRequest request;

    @BeforeEach
    public void setUp() {
        reset(userService);

        // isadmin字段标识身份，0-用户，1-管理员
        User admin = new User(1, "adminID", "adminName", "adminPassword", "admin@example.com", "15649851625", 1, "adminPic");

        // 对于每个请求模拟设置session，假设为合法的管理员身份，不合法的情况在测试函数中再单独设置
        request = new MockHttpServletRequest();
        Objects.requireNonNull(request.getSession()).setAttribute("admin", admin);

    }

    /*
     * 展示管理用户界面
     * */

    // 测试了正常显示内容的情况，同时也测试了是否正确分页
    @Test
    public void testUserManageWithValidPage() throws Exception {
        List<User> userList = IntStream.range(0, 15)
                .mapToObj(i -> new User(0, "1", "test", "testPassword", "test@example", "16592354982", 0, "image.jpg"))
                .collect(Collectors.toList());

        Pageable user_pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
        Page<User> mockPage = new PageImpl<>(userList, user_pageable, userList.size());

        when(userService.findByUserID(user_pageable)).thenReturn(mockPage);

        // Expect total pages to be 2 because there are more messages (15) than can fit on one page (10), necessitating a second page.
        mockMvc.perform(get("/user_manage")
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user_manage"))
                .andExpect(model().attribute("total", 2));

        verify(userService).findByUserID(user_pageable);
    }

    @Test
    public void testUserManageWithEmptyPage() throws Exception {
        Pageable user_pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
        Page<User> emptyPage = new PageImpl<>(Collections.emptyList(), user_pageable, 0);

        when(userService.findByUserID(user_pageable)).thenReturn(emptyPage);

        mockMvc.perform(get("/user_manage")
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user_manage"))
                .andExpect(model().attribute("total", 0));

        verify(userService).findByUserID(user_pageable);
    }

    // BUG: 未进行鉴权检查
    @Test
    public void testUserManageWithInvalidRole() throws Exception {
        // 倒数第二个isadmin字段设置为0，代表为用户，为非法权限
        User user = new User(1, "userID", "userName", "userPassword", "user@example.com", "14695846221", 0, "userPic");
        Objects.requireNonNull(request.getSession()).setAttribute("admin", user);

        // 携带session的验证，期望返回 401 Unauthorized
        mockMvc.perform(get("/user_manage")
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isUnauthorized());
    }


    /*
     * 展示指定页号用户数据
     * */

    @Test
    public void testUserListWithValidPage() throws Exception {
        List<User> userList = new ArrayList<>();
        userList.add(new User(0, "1", "test", "testPassword", "test@example", "16592354982", 0, "image.jpg"));
        Page<User> mockPage = new PageImpl<>(userList);

        int pageValid = 1;
        Pageable user_pageable = PageRequest.of(pageValid - 1, 10, Sort.by("id").ascending());

        when(userService.findByUserID(user_pageable)).thenReturn(mockPage);

        mockMvc.perform(get("/userList.do")
                        .param("page", String.valueOf(pageValid))
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(userService).findByUserID(user_pageable);
    }

    @Test
    public void testUserListWithDefaultPage() throws Exception {
        List<User> userList = new ArrayList<>();
        userList.add(new User(0, "1", "test", "testPassword", "test@example", "16592354982", 0, "image.jpg"));
        Page<User> mockPage = new PageImpl<>(userList);

        int pageDefault = 1;
        Pageable user_pageable = PageRequest.of(pageDefault - 1, 10, Sort.by("id").ascending());

        when(userService.findByUserID(user_pageable)).thenReturn(mockPage);

        mockMvc.perform(get("/userList.do")
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(userService).findByUserID(user_pageable);
    }

    @Test
    public void testUserListWithEmptyPage() throws Exception {
        Pageable user_pageable = PageRequest.of(0, 10, Sort.by("id").ascending());

        when(userService.findByUserID(user_pageable)).thenReturn(new PageImpl<>(Collections.emptyList(), user_pageable, 0));

        mockMvc.perform(get("/userList.do")
                        .param("page", String.valueOf(1))
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0))); // 直接检查响应体是否为一个空数组

        verify(userService).findByUserID(user_pageable);
    }

    // BUG: 没有检查参数页数小于 1的情况
    @Test
    public void testUserListWithPageNotPositive() throws Exception {
        int pageInvalid = 0;

        mockMvc.perform(get("/userList.do")
                        .param("page", String.valueOf(pageInvalid))
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());

        verify(userService, never()).findByUserID(anyString());
    }

    // BUG: 没有检查参数页数超出限制的情况
    @Test
    public void testUserListWithPageExceedingLimit() throws Exception {
        int pageExceedingLimit = 99999;

        Pageable user_pageable = PageRequest.of(pageExceedingLimit - 1, 10, Sort.by("id").ascending());

        when(userService.findByUserID(user_pageable)).thenReturn(new PageImpl<>(Collections.emptyList(), user_pageable, 0));

        mockMvc.perform(get("/userList.do")
                        .param("page", String.valueOf(pageExceedingLimit))
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isNotFound());

        verify(userService).findByUserID(user_pageable);
    }

    @Test
    public void testUserListWithPageNotNum() throws Exception {
        String pageNotNum = "hello";

        mockMvc.perform(get("/userList.do")
                        .param("page", pageNotNum)
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());

        verify(userService, never()).findByUserID(anyString());
    }

    // BUG: 未进行鉴权检查
    @Test
    public void testUserListWithInvalidRole() throws Exception {
        // 倒数第二个isadmin字段设置为0，代表为用户，为非法权限
        User user = new User(1, "userID", "userName", "userPassword", "user@example.com", "14695846221", 0, "userPic");
        Objects.requireNonNull(request.getSession()).setAttribute("admin", user);

        // 携带session的验证，期望返回 401 Unauthorized
        mockMvc.perform(get("/userList.do")
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isUnauthorized());
    }


    /*
     * 展示添加用户界面
     * */

    @Test
    public void testUserAddWithValidRole() throws Exception {
        mockMvc.perform(get("/user_add")
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user_add"));
    }

    // BUG: 未进行鉴权检查
    @Test
    public void testUserAddWithInvalidRole() throws Exception {
        // 倒数第二个isadmin字段设置为0，代表为用户，为非法权限
        User user = new User(1, "userID", "userName", "userPassword", "user@example.com", "14695846221", 0, "userPic");
        Objects.requireNonNull(request.getSession()).setAttribute("admin", user);

        // 携带session的验证，期望返回 401 Unauthorized
        mockMvc.perform(get("/user_add")
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isUnauthorized());
    }


    /*
     * 添加用户
     * */

    @Test
    public void testAddUserWithValidInfo() throws Exception {
        String userID = "1";
        String userName = "test";
        String password = "password123";
        String email = "test@example.com";
        String phone = "13495684256";

        when(userService.create(ArgumentMatchers.any(User.class))).thenReturn(1);

        mockMvc.perform(post("/addUser.do")
                        .param("userID", userID)
                        .param("userName", userName)
                        .param("password", password)
                        .param("email", email)
                        .param("phone", phone)
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("user_manage"));

        verify(userService).create(ArgumentMatchers.any(User.class));
    }

    // BUG: 没有检查参数为空的情况
    @Test
    public void testAddUserWithEmptyParam() throws Exception {
        mockMvc.perform(post("/addUser.do")
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());

        verify(userService, never()).create(ArgumentMatchers.any(User.class));
    }

    // BUG: 没有检查邮箱格式不合法的情况
    @Test
    public void testAddUserWithInvalidEmailFormat() throws Exception {
        mockMvc.perform(post("/addUser.do")
                        .param("userID", "1")
                        .param("userName", "test")
                        .param("password", "password123")
                        .param("email", "invalid_email")  // invalid format
                        .param("phone", "13495684256")
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }

    // BUG: 没有检查手机格式不合法的情况
    @Test
    public void testAddUserWithInvalidPhoneFormat() throws Exception {
        mockMvc.perform(post("/addUser.do")
                        .param("userID", "1")
                        .param("userName", "test")
                        .param("password", "password123")
                        .param("email", "test@example.com")
                        .param("phone", "invalid_phone_number") // invalid format
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }

    // BUG: 没有检查用户已存在的情况（即 userID重复）
    @Test
    public void testAddUserWithExistingUserID() throws Exception {
        String conflictUserID = "conflict";
        String userName = "user";
        String password = "password123";
        String email = "existing@example.com";
        String phone = "1234567890";

        when(userService.countUserID(conflictUserID)).thenReturn(1);  // 说明此 userID已存在
        when(userService.create(ArgumentMatchers.any(User.class))).thenReturn(0);

        mockMvc.perform(post("/addUser.do")
                        .param("userID", conflictUserID)
                        .param("userName", userName)
                        .param("password", password)
                        .param("email", email)
                        .param("phone", phone)
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isConflict());  // 409 conflict
    }

    // BUG: 未进行鉴权检查
    @Test
    public void testAddUserWithInvalidRole() throws Exception {
        // 倒数第二个isadmin字段设置为0，代表为用户，为非法权限
        User user = new User(1, "userID", "userName", "userPassword", "user@example.com", "14695846221", 0, "userPic");
        Objects.requireNonNull(request.getSession()).setAttribute("admin", user);

        // 携带session的验证，期望返回 401 Unauthorized
        mockMvc.perform(post("/addUser.do")
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isUnauthorized());
    }


    /*
     * 展示编辑用户界面
     * */

    @Test
    public void testUserEditWithValidId() throws Exception {
        int validId = 1;

        User user = new User();
        user.setId(validId);

        when(userService.findById(validId)).thenReturn(user);

        mockMvc.perform(get("/user_edit")
                        .param("id", String.valueOf(validId))
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user_edit"))
                .andExpect(model().attribute("user", user));

        verify(userService).findById(validId);
    }

    // BUG: 没有处理用户不存在的情况
    @Test
    public void testUserEditWithNotExistingId() throws Exception {
        int notExistingId = 999;

        when(userService.findById(notExistingId)).thenReturn(null);

        mockMvc.perform(get("/user_edit")
                        .param("id", String.valueOf(notExistingId))
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isNotFound())
                .andExpect(view().name("admin/user_edit"));

        verify(userService).findById(notExistingId);
    }

    // BUG: 没有检查 id小于 1 的情况
    @Test
    public void testUserEditWithNotPositiveId() throws Exception {
        int notPositiveId = -1;

        mockMvc.perform(get("/user_edit")
                        .param("id", String.valueOf(notPositiveId))
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }

    // id无法转换为 int型的情况
    @Test
    public void testUserEditWithNotNumId() throws Exception {
        String notNumId = "hello";

        mockMvc.perform(get("/user_edit")
                        .param("id", notNumId)
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }

    // BUG: 没有检查 id为空的情况
    @Test
    public void testUserEditWithEmptyParam() throws Exception {
        mockMvc.perform(get("/user_edit")
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }

    // BUG: 未进行鉴权检查
    @Test
    public void testUserEditWithInvalidRole() throws Exception {
        // 倒数第二个isadmin字段设置为0，代表为用户，为非法权限
        User user = new User(1, "userID", "userName", "userPassword", "user@example.com", "14695846221", 0, "userPic");
        Objects.requireNonNull(request.getSession()).setAttribute("admin", user);

        // 携带session的验证，期望返回 401 Unauthorized
        mockMvc.perform(get("/user_edit")
                        .param("id", "1")
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isUnauthorized());
    }


    /*
     * 修改用户信息
     * */

    @Test
    public void testModifyUserWithValidInfo() throws Exception {

        String userID = "2";
        String oldUserID = "1";
        String userName = "test";
        String password = "password123";
        String email = "test@example.com";
        String phone = "1234567890";

        User userBeforeUpdate = new User();
        userBeforeUpdate.setUserID(oldUserID);

        User userAfterUpdate = new User();
        userAfterUpdate.setUserID(userID);
        userAfterUpdate.setUserName(userName);
        userAfterUpdate.setPassword(password);
        userAfterUpdate.setEmail(email);
        userAfterUpdate.setPhone(phone);

        when(userService.findByUserID(anyString())).thenReturn(userBeforeUpdate);
        doNothing().when(userService).updateUser(userAfterUpdate);

        mockMvc.perform(post("/modifyUser.do")
                        .param("userID", userID)
                        .param("oldUserID", oldUserID)
                        .param("userName", userName)
                        .param("password", password)
                        .param("email", email)
                        .param("phone", phone)
                        .session((MockHttpSession) request.getSession())
                )
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("user_manage"));

        verify(userService).findByUserID(oldUserID);
        verify(userService).updateUser(userAfterUpdate);
    }

    // BUG: 没有检查用户不存在的情况
    @Test
    public void testModifyUserWithNotExistingUser() throws Exception {
        String userID = "2";
        String oldUserID = "not exist"; // not exists
        String userName = "test";
        String password = "password123";
        String email = "test@example.com";
        String phone = "1234567890";

        when(userService.findByUserID(anyString())).thenReturn(null);

        mockMvc.perform(post("/modifyUser.do")
                        .param("userID", userID)
                        .param("oldUserID", oldUserID)
                        .param("userName", userName)
                        .param("password", password)
                        .param("email", email)
                        .param("phone", phone)
                        .session((MockHttpSession) request.getSession())
                )
                .andExpect(status().isNotFound());

        verify(userService).findByUserID(oldUserID);
    }

    // BUG: 没有检查参数为空的情况
    @Test
    public void testModifyUserWithEmptyParam() throws Exception {
        mockMvc.perform(post("/modifyUser.do")
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }

    // 这里是用 string型的 userID来查找用户，所以没有对负数和非数字参数的检查
    // 不用检查 userID冲突的情况，因为函数中并不会修改 userID

    // BUG: 未进行鉴权检查
    @Test
    public void testModifyUserWithInvalidRole() throws Exception {
        // 倒数第二个isadmin字段设置为0，代表为用户，为非法权限
        User user = new User(1, "userID", "userName", "userPassword", "user@example.com", "14695846221", 0, "userPic");
        Objects.requireNonNull(request.getSession()).setAttribute("admin", user);

        // 携带session的验证，期望返回 401 Unauthorized
        mockMvc.perform(post("/modifyUser.do")
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isUnauthorized());
    }


    /*
     * 删除用户
     * */

    @Test
    public void testDelUserWithValidId() throws Exception {
        int validId = 1;

        doNothing().when(userService).delByID(validId);

        mockMvc.perform(post("/delUser.do")
                        .param("id", String.valueOf(validId))
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(userService).delByID(validId);
    }

    // BUG: 没有检查用户不存在的情况
    @Test
    public void testDelUserWithNotExistingUser() throws Exception {
        int notExistingId = 999;

        // 模拟 delByID底层抛出的异常，期望在出现问题时 controller层能捕获这样的异常
        doThrow(new EmptyResultDataAccessException(String.format("No User with id %s exists!", notExistingId), 1))
                .when(userService).delByID(notExistingId);

        mockMvc.perform(post("/delUser.do")
                        .param("id", String.valueOf(notExistingId))
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("false"));

        verify(userService).delByID(notExistingId);
    }

    // BUG: 没有检查 id为负数的情况
    @Test
    public void testDelUserWithInvalidId() throws Exception {
        int invalidId = -1;
        mockMvc.perform(post("/delUser.do")
                        .param("id", String.valueOf(invalidId))
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testDelUserWithStringId() throws Exception {
        String stringId = "errorId";
        mockMvc.perform(post("/delUser.do")
                        .param("id", stringId)
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }

    // BUG: 没有检查参数不存在的情况
    @Test
    public void testDelUserWithEmptyParam() throws Exception {
        mockMvc.perform(post("/delUser.do")
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }

    // BUG: 未进行鉴权检查
    @Test
    public void testDelUserWithInvalidRole() throws Exception {
        // 倒数第二个isadmin字段设置为0，代表为用户，为非法权限
        User user = new User(1, "userID", "userName", "userPassword", "user@example.com", "14695846221", 0, "userPic");
        Objects.requireNonNull(request.getSession()).setAttribute("admin", user);

        // 携带session的验证，期望返回 401 Unauthorized
        mockMvc.perform(post("/delUser.do")
                        .param("id", "1")
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isUnauthorized());
    }


    /*
     * 检查 userID
     * */

    @Test
    public void testCheckUserIDWithExistingUserID() throws Exception {
        String existingUserID = "1";

        when(userService.countUserID(anyString())).thenReturn(1);

        mockMvc.perform(post("/checkUserID.do")
                        .param("userID", existingUserID)
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        verify(userService).countUserID(existingUserID);
    }

    @Test
    public void testCheckUserIDWithNotExistingUserID() throws Exception {
        String notExistingUserID = "999";

        when(userService.countUserID(anyString())).thenReturn(0);

        mockMvc.perform(post("/checkUserID.do")
                        .param("userID", notExistingUserID)
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(userService).countUserID(notExistingUserID);
    }

    // BUG: 没有检查 userID 为 null
    @Test
    public void testCheckUserIDWithInvalidUserID() throws Exception {
        mockMvc.perform(post("/checkUserID.do")
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isBadRequest());
    }

    // BUG: 未进行鉴权检查
    @Test
    public void testCheckUserIDWithInvalidRole() throws Exception {
        // 倒数第二个isadmin字段设置为0，代表为用户，为非法权限
        User user = new User(1, "userID", "userName", "userPassword", "user@example.com", "14695846221", 0, "userPic");
        Objects.requireNonNull(request.getSession()).setAttribute("admin", user);

        // 携带session的验证，期望返回 401 Unauthorized
        mockMvc.perform(post("/checkUserID.do")
                        .session((MockHttpSession) request.getSession()))
                .andExpect(status().isUnauthorized());
    }

}
