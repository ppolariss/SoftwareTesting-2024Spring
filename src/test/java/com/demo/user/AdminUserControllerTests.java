package com.demo.user;

import com.demo.controller.admin.AdminUserController;
import com.demo.entity.News;
import com.demo.entity.User;
import com.demo.service.UserService;
import com.mysql.cj.x.protobuf.MysqlxDatatypes;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.springframework.test.web.servlet.result.JsonPathResultMatchers.*;

import static org.hamcrest.Matchers.any;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@WebMvcTest(AdminUserController.class)
public class AdminUserControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    public void testNull() {
        Pageable user_pageable = PageRequest.of(999999999, 10, Sort.by("id").ascending());

        Page<User> temp1 = userService.findByUserID(user_pageable);
        assertNull(temp1);

        int newsID = -1;
        User temp2 = userService.findByUserID("-1");
        assertNull(temp2);
    }

    /*
     * 展示管理用户界面
     * */

    @Test
    public void testUserManageWithValidData() throws Exception {
        List<User> userList = new ArrayList<>();
        userList.add(new User(0, "1", "test", "testPassword", "test@example", "16592354982", 0, "image.jpg"));
        Page<User> mockPage = new PageImpl<>(userList);
        Pageable user_pageable = PageRequest.of(0, 10, Sort.by("id").ascending());

        when(userService.findByUserID(user_pageable)).thenReturn(mockPage);

        mockMvc.perform(get("/user_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user_manage"))
                .andExpect(model().attribute("total", mockPage.getTotalPages()));

        verify(userService).findByUserID(user_pageable);
    }

    // BUG: 没有检查用户不存在的情况
    @Test
    public void testUserManageWithNullData() throws Exception {
        Pageable user_pageable = PageRequest.of(0, 10, Sort.by("id").ascending());

        when(userService.findByUserID(user_pageable)).thenReturn(null);

        mockMvc.perform(get("/user_manage"))
                .andExpect(status().isNotFound());

        verify(userService).findByUserID(user_pageable);
    }


    /*
     * 展示指定页号用户数据
     * */

    @Test
    public void testUserListWithDefaultPage() throws Exception {
        List<User> userList = new ArrayList<>();
        userList.add(new User(0, "1", "test", "testPassword", "test@example", "16592354982", 0, "image.jpg"));
        Page<User> mockPage = new PageImpl<>(userList);

        int pageDefault = 1;
        int pageSize = 10;
        Pageable user_pageable = PageRequest.of(pageDefault - 1, pageSize, Sort.by("id").ascending());

        when(userService.findByUserID(user_pageable)).thenReturn(mockPage);

        mockMvc.perform(get("/userList.do"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray());

        verify(userService).findByUserID(user_pageable);
    }

    @Test
    public void testUserListWithValidPage() throws Exception {
        List<User> userList = new ArrayList<>();
        userList.add(new User(0, "1", "test", "testPassword", "test@example", "16592354982", 0, "image.jpg"));
        Page<User> mockPage = new PageImpl<>(userList);

        int pageValid = 2;
        int pageSize = 10;
        Pageable user_pageable = PageRequest.of(pageValid - 1, pageSize, Sort.by("id").ascending());

        when(userService.findByUserID(user_pageable)).thenReturn(mockPage);

        mockMvc.perform(get("/userList.do").param("page", String.valueOf(pageValid)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray());

        verify(userService).findByUserID(user_pageable);
    }

    // BUG: 没有检查参数页数小于 1的情况
    @Test
    public void testUserListWithNotPositivePage() throws Exception {
        int pageInvalid = 0;

        mockMvc.perform(get("/userList.do").param("page", String.valueOf(pageInvalid)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).findByUserID(anyString());
    }

    // BUG: 没有检查参数页数超出限制的情况
    @Test
    public void testUserListWithExceedingPageLimit() throws Exception {
        int pageExceedingLimit = 99999;

        Pageable user_pageable = PageRequest.of(pageExceedingLimit - 1, 10, Sort.by("id").ascending());

        when(userService.findByUserID(user_pageable)).thenReturn(null);

        mockMvc.perform(get("/userList.do").param("page", String.valueOf(pageExceedingLimit)))
                .andExpect(status().isNotFound());

        verify(userService).findByUserID(user_pageable);
    }


    /*
     * 展示添加用户界面
     * */

    @Test
    public void testUserAdd() throws Exception {
        mockMvc.perform(get("/user_add"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user_add"));
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
                        .param("phone", phone))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("user_manage"));

        verify(userService).create(ArgumentMatchers.any(User.class));
    }

    // BUG: 没有检查 userID 为空的情况
    @Test
    public void testAddUserWithIncompleteInfo() throws Exception {

        String userID = null;  // userID cannot be null
        String userName = "test";
        String password = "password123";
        String email = "test@example.com";
        String phone = "13495684256";

        User user = new User();
        user.setUserID(userID);
        user.setUserName(userName);
        user.setPassword(password);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPicture("");

        doThrow(new RuntimeException("UserID cannot be null.")).when(userService).create(user);

        mockMvc.perform(post("/addUser.do")
                        .param("userID", userID)
                        .param("userName", userName)
                        .param("password", password)
                        .param("email", email)
                        .param("phone", phone))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("UserID cannot be null.")));

        verify(userService).create(user);
    }

    // BUG: 没有检查用户已存在的情况（如userID重复）
    @Test
    public void testAddUserWithExistingUser() throws Exception {
        String userID = "1";
        String userName = "Existing User";
        String password = "password123";
        String email = "existing@example.com";
        String phone = "1234567890";

        when(userService.create(ArgumentMatchers.any(User.class))).thenReturn(0);
//        doThrow(new RuntimeException("User already exists.")).when(userService).create(any(User.class));

        mockMvc.perform(post("/addUser.do")
                        .param("userID", userID)
                        .param("userName", userName)
                        .param("password", password)
                        .param("email", email)
                        .param("phone", phone))
                .andExpect(status().isConflict());  // 409 conflict
//                .andExpect(content().string(containsString("User already exists.")));

        verify(userService).create(ArgumentMatchers.any(User.class));
    }


    /*
     * 展示编辑用户界面
     * */

    @Test
    public void testUserEditWithValidId() throws Exception {
        int idValid = 1;

        User user = new User();
        user.setId(idValid);

        when(userService.findById(anyInt())).thenReturn(user);

        mockMvc.perform(get("/user_edit").param("id", String.valueOf(idValid)))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user_edit"))
                .andExpect(model().attribute("user", user));

        verify(userService).findById(idValid);
    }

    // BUG: 没有检查 id不合法的情况，导致 user可能为 null
    @Test
    public void testUserEditWithInvalidId() throws Exception {
        int idInvalid = -1;

        User user = new User();
        user.setId(idInvalid);

        when(userService.findById(anyInt())).thenReturn(null);

        mockMvc.perform(get("/user_edit").param("id", String.valueOf(idInvalid)))
                .andExpect(status().isBadRequest());

        verify(userService).findById(idInvalid);
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
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("user_manage"));

        verify(userService).findByUserID(oldUserID);
        verify(userService).updateUser(userAfterUpdate);
    }

    // BUG: 没有检查 oldUserID 为 null 的情况
    @Test
    public void testModifyUserWithIncompleteInfo() throws Exception {

        String userID = "2";
        String oldUserID = null;  // oldUserID cannot be null
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

        when(userService.findByUserID(anyString())).thenReturn(null);

        mockMvc.perform(post("/modifyUser.do")
                        .param("userID", userID)
                        .param("oldUserID", oldUserID)
                        .param("userName", userName)
                        .param("password", password)
                        .param("email", email)
                        .param("phone", phone)
                )
                .andExpect(status().isBadRequest());

        verify(userService).findByUserID(oldUserID);
        verify(userService, never()).updateUser(userAfterUpdate);
    }

    // BUG: 没有检查用户不存在的情况
    @Test
    public void testModifyUserWithNotExistingUser() throws Exception {

        String userID = "2";
        String oldUserID = "999"; // not exists
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

        when(userService.findByUserID(anyString())).thenReturn(null);

        mockMvc.perform(post("/modifyUser.do")
                        .param("userID", userID)
                        .param("oldUserID", oldUserID)
                        .param("userName", userName)
                        .param("password", password)
                        .param("email", email)
                        .param("phone", phone)
                )
                .andExpect(status().isNotFound());

        verify(userService).findByUserID(oldUserID);
        verify(userService, never()).updateUser(userAfterUpdate);
    }


    /*
     * 删除用户
     * */

    @Test
    public void testDelUserWithValidId() throws Exception {
        int existingUserId = 1;

        doNothing().when(userService).delByID(existingUserId);

        mockMvc.perform(post("/delUser.do").param("id", String.valueOf(existingUserId)))
                .andExpect(status().isOk()) // 验证响应状态码为 200 OK
                .andExpect(content().string("true")); // 验证响应内容为 "true"

        verify(userService).delByID(existingUserId);
    }

    // BUG: 没有检查 id不合法的情况，导致 user可能为 null
    @Test
    public void testDelUserWithInvalidId() throws Exception {
        int nonExistingUserId = -1;

        // TODO: 所以是要 doNothing 还是 抛异常？
//        doNothing().when(userService).delByID(nonExistingUserId);
        doThrow(new RuntimeException("User doesn't exists.")).when(userService).delByID(nonExistingUserId);

        mockMvc.perform(post("/delUser.do").param("id", String.valueOf(nonExistingUserId)))
                .andExpect(status().isBadRequest());

        verify(userService).delByID(nonExistingUserId);
    }


    /*
    * 检查 userID
    * */

    @Test
    public void testCheckUserIDWithExistingUserID() throws Exception {
        String existingUserID = "1";

        when(userService.countUserID(anyString())).thenReturn(1);

        mockMvc.perform(post("/checkUserID.do").param("userID", existingUserID))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        verify(userService).countUserID(existingUserID);
    }

    @Test
    public void testCheckUserIDWithNotExistingUserID() throws Exception {
        String notExistingUserID = "999";

        when(userService.countUserID(anyString())).thenReturn(0);

        mockMvc.perform(post("/checkUserID.do").param("userID", notExistingUserID))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(userService).countUserID(notExistingUserID);
    }

    // BUG: 没有检查 userID 为 null
    @Test
    public void testCheckUserIDWithInvalidUserID() throws Exception {
        String invalidUserID = null;  // cannot be null

        doThrow(new RuntimeException("UserID cannot be null.")).when(userService).countUserID(invalidUserID);
//        when(userService.countUserID(anyString())).thenReturn(0);

        mockMvc.perform(post("/checkUserID.do").param("userID", invalidUserID))
                .andExpect(status().isBadRequest());

        verify(userService).countUserID(invalidUserID);
    }

}
