package com.demo.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.demo.controller.user.UserController;
import com.demo.entity.User;
import com.demo.service.UserService;
import com.demo.utils.FileUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(UserController.class)
public class UserControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    public void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    /*
     * 展示注册界面
     * */

    @Test
    public void testSignUpView() throws Exception {
        mockMvc.perform(get("/signup"))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"));
    }


    /**
     * 注册新用户
     */

    @Test
    public void testRegisterWithValidUserInfo() throws Exception {
        // Prepare test data
        String userID = "1";
        String userName = "test";
        String password = "password123";
        String email = "test@example.com";
        String phone = "13495684256";

        // Mock the behavior of UserService.create method
        when(userService.create(any(User.class))).thenReturn(1);

        // Perform the request with parameters
        mockMvc.perform(post("/register.do")
                        .param("userID", userID)
                        .param("userName", userName)
                        .param("password", password)
                        .param("email", email)
                        .param("phone", phone))
                .andExpect(status().is3xxRedirection())        // Check if the response is a redirection
                .andExpect(redirectedUrl("login")); // Check if the redirect URL is "login"

        // Verify UserService.create is called
        verify(userService).create(any(User.class));
    }

    // BUG: 没有检查邮箱格式不合法的情况
    @Test
    public void testRegisterWithInvalidEmailFormat() throws Exception {
        mockMvc.perform(post("/register.do")
                        .param("userID", "1")
                        .param("userName", "test")
                        .param("password", "password123")
                        .param("email", "invalid_email")  // invalid format
                        .param("phone", "13495684256"))
                .andExpect(status().isBadRequest());
    }

    // BUG: 没有检查手机格式不合法的情况
    @Test
    public void testRegisterWithInvalidPhoneFormat() throws Exception {
        mockMvc.perform(post("/register.do")
                        .param("userID", "1")
                        .param("userName", "test")
                        .param("password", "password123")
                        .param("email", "test@example.com")
                        .param("phone", "invalid_phone_number"))  // invalid format
                .andExpect(status().isBadRequest());
    }

    // BUG: 没有检查参数为空的情况
    @Test
    public void testRegisterWithEmptyParam() throws Exception {
        mockMvc.perform(post("/register.do"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).create(ArgumentMatchers.any(User.class));
    }

    // BUG: 没有检查用户已存在的情况（即 userID重复）
    @Test
    public void testRegisterWithExistingUserID() throws Exception {
        String conflictUserID = "conflict";
        String userName = "user";
        String password = "password123";
        String email = "existing@example.com";
        String phone = "1234567890";

        // 期望 controller层主动调用 countUserID函数来检查是否存在重复用户
        // 或者在 service层返回 0时发现新增用户失败
        when(userService.countUserID(conflictUserID)).thenReturn(1);  // 说明此 userID已存在
        when(userService.create(ArgumentMatchers.any(User.class))).thenReturn(0);  // 创建用户失败，返回0

        mockMvc.perform(post("/register.do")
                        .param("userID", conflictUserID)
                        .param("userName", userName)
                        .param("password", password)
                        .param("email", email)
                        .param("phone", phone))
                .andExpect(status().isConflict());  // 409 conflict
    }


    /**
     * 展示登录界面
     */

    @Test
    public void testLoginView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }


    /*
     * 用户登录
     * */

    @Test
    public void testLoginWithValidUser() throws Exception {

        String userID = "1";
        String password = "validPassword";

        User user = new User();
        user.setUserID(userID);
        user.setPassword(password);
        user.setIsadmin(0);  // user

        when(userService.checkLogin(anyString(), anyString())).thenReturn(user);

        mockMvc.perform(post("/loginCheck.do")
                        .param("userID", userID)
                        .param("password", password))
                .andExpect(status().isOk())
                .andExpect(content().string("/index")) // redirect to user index
                .andExpect(request().sessionAttribute("user", user));

        verify(userService).checkLogin(userID, password);
    }

    @Test
    public void testLoginWithValidAdmin() throws Exception {

        String userID = "1";
        String password = "validPassword";

        User user = new User();
        user.setUserID(userID);
        user.setPassword(password);
        user.setIsadmin(1);  // admin

        when(userService.checkLogin(anyString(), anyString())).thenReturn(user);

        mockMvc.perform(post("/loginCheck.do")
                        .param("userID", userID)
                        .param("password", password))
                .andExpect(status().isOk())
                .andExpect(content().string("/admin_index")) // redirect to admin index
                .andExpect(request().sessionAttribute("admin", user));

        verify(userService).checkLogin(userID, password);
    }

    @Test
    public void testLoginWithNotExistingUser() throws Exception {
        String userID = "not existing user";
        String password = "password";

        when(userService.checkLogin(anyString(), anyString())).thenReturn(null);

        mockMvc.perform(post("/loginCheck.do")
                        .param("userID", userID)
                        .param("password", password))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        verify(userService).checkLogin(userID, password);
    }

    @Test
    public void testLoginWithIncorrectPassword() throws Exception {
        String userID = "1";
        String password = "incorrectPassword";

        when(userService.checkLogin(anyString(), anyString())).thenReturn(null);

        mockMvc.perform(post("/loginCheck.do")
                        .param("userID", userID)
                        .param("password", password))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        verify(userService).checkLogin(userID, password);
    }

    // BUG: 没有检查参数为空的情况
    @Test
    public void testLoginWithEmptyParam() throws Exception {
        mockMvc.perform(post("/loginCheck.do"))
                .andExpect(status().isBadRequest());
    }


    /*
     * 账号登出
     * */

    @Test
    public void testLogout() throws Exception {
        request.getSession().setAttribute("user", new User());

        mockMvc.perform(get("/logout.do").session((MockHttpSession) request.getSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/index"));

        assertNull(request.getSession().getAttribute("user")); // user in session has been removed correctly
    }

    @Test
    public void testLogoutWithoutLogin() throws Exception {
        mockMvc.perform(get("/logout.do"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/index"));
    }

    @Test
    public void testQuit() throws Exception {
        request.getSession().setAttribute("admin", new User());

        mockMvc.perform(get("/quit.do").session((MockHttpSession) request.getSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/index"));

        assertNull(request.getSession().getAttribute("admin"));
    }

    @Test
    public void testQuitWithoutLogin() throws Exception {
        mockMvc.perform(get("/quit.do"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/index"));
//                .andExpect(status().isUnauthorized());
    }


    /*
     * 展示用户信息
     * */

    @Test
    public void testUserInfo() throws Exception {
        User user = new User(0, "1", "test", "testPwd", "test@example.com", "13549526153", 0, "image.jpg");

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", user);

        mockMvc.perform(get("/user_info").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("user_info"));
    }


    /*
     * 修改用户信息
     * */

    @Test
    public void testUpdateUserWithValidInfo() throws Exception {

        String userID = "1";
        String userName = "test";
        String password = "password123";
        String email = "test@example.com";
        String phone = "1234567890";
        MockMultipartFile picture = new MockMultipartFile("picture", "image.jpg", "image/jpeg", "Some image content here".getBytes());

        User userBeforeUpdate = new User();
        userBeforeUpdate.setUserID(userID);

        User userAfterUpdate = new User(0, userID, userName, password, email, phone, 0, picture.getOriginalFilename());

        // Mock Static Method
        MockedStatic<FileUtil> mockedStatic = Mockito.mockStatic(FileUtil.class);
        mockedStatic.when(() -> FileUtil.saveUserFile(picture)).thenReturn(picture.getOriginalFilename());

        when(userService.findByUserID(anyString())).thenReturn(userBeforeUpdate);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/updateUser.do")
                        .file(picture)
                        .param("userName", userName)
                        .param("userID", userID)
                        .param("passwordNew", password)
                        .param("email", email)
                        .param("phone", phone)
                        .session((MockHttpSession) request.getSession())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("user_info"));

        // 检查 session中的 user信息是否被成功更新
        assertEquals(userAfterUpdate, request.getSession().getAttribute("user"));

        verify(userService).findByUserID(userID);
        verify(userService).updateUser(userAfterUpdate);
        mockedStatic.verify(() -> FileUtil.saveUserFile(picture));
    }

    @Test
    public void testUpdateUserWithNullNewPassword() throws Exception {

        String userID = "1";
        String userName = "test";
        String password = "password123";  // old password
        String email = "test@example.com";
        String phone = "1234567890";
        MockMultipartFile picture = new MockMultipartFile("picture", "image.jpg", "image/jpeg", "Some image content here".getBytes());

        User userBeforeUpdate = new User();
        userBeforeUpdate.setUserID(userID);
        userBeforeUpdate.setPassword(password);

        User userAfterUpdate = new User(0, userID, userName, password, email, phone, 0, picture.getOriginalFilename());

        // Mock Static Method
        MockedStatic<FileUtil> mockedStatic = Mockito.mockStatic(FileUtil.class);
        mockedStatic.when(() -> FileUtil.saveUserFile(picture)).thenReturn(picture.getOriginalFilename());

        when(userService.findByUserID(anyString())).thenReturn(userBeforeUpdate);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/updateUser.do")
                                .file(picture)
                                .param("userName", userName)
                                .param("userID", userID)
//                        .param("passwordNew", password)  // new password is null so will not be updated
                                .param("email", email)
                                .param("phone", phone)
                                .session((MockHttpSession) request.getSession())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("user_info"));

        // 检查 session中的 user信息是否被成功更新
        assertEquals(userAfterUpdate, request.getSession().getAttribute("user"));

        verify(userService).findByUserID(userID);
        verify(userService).updateUser(userAfterUpdate);
        mockedStatic.verify(() -> FileUtil.saveUserFile(picture));
    }

    @Test
    public void testUpdateUserWithEmptyStringNewPassword() throws Exception {

        String userID = "1";
        String userName = "test";
        String password = "password123";  // old password
        String email = "test@example.com";
        String phone = "1234567890";
        MockMultipartFile picture = new MockMultipartFile("picture", "image.jpg", "image/jpeg", "Some image content here".getBytes());

        User userBeforeUpdate = new User();
        userBeforeUpdate.setUserID(userID);
        userBeforeUpdate.setPassword(password);

        User userAfterUpdate = new User(0, userID, userName, password, email, phone, 0, picture.getOriginalFilename());

        MockedStatic<FileUtil> mockedStatic = Mockito.mockStatic(FileUtil.class);
        mockedStatic.when(() -> FileUtil.saveUserFile(picture)).thenReturn(picture.getOriginalFilename());

        when(userService.findByUserID(anyString())).thenReturn(userBeforeUpdate);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/updateUser.do")
                        .file(picture)
                        .param("userName", userName)
                        .param("userID", userID)
                        .param("passwordNew", "")  // new password is empty string so will not be updated
                        .param("email", email)
                        .param("phone", phone)
                        .session((MockHttpSession) request.getSession())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("user_info"));

        // 检查 session中的 user信息是否被成功更新
        assertEquals(userAfterUpdate, request.getSession().getAttribute("user"));

        verify(userService).findByUserID(userID);
        verify(userService).updateUser(userAfterUpdate);
        mockedStatic.verify(() -> FileUtil.saveUserFile(picture));
    }

    @Test
    public void testUpdateUserWithEmptyStringPicture() throws Exception {

        String userID = "1";
        String userName = "test";
        String password = "password123";
        String email = "test@example.com";
        String phone = "1234567890";
        String originalPictureName = "origin.jpg";
        // new picture's original name is empty string, so will not be updated
        MockMultipartFile picture = new MockMultipartFile("picture", "", "image/jpeg", "Some image content here".getBytes());

        User userBeforeUpdate = new User();
        userBeforeUpdate.setUserID(userID);
        userBeforeUpdate.setPicture(originalPictureName);

        User userAfterUpdate = new User(0, userID, userName, password, email, phone, 0, originalPictureName);

        MockedStatic<FileUtil> mockedStatic = Mockito.mockStatic(FileUtil.class);
        mockedStatic.when(() -> FileUtil.saveUserFile(picture)).thenReturn(picture.getOriginalFilename());

        when(userService.findByUserID(anyString())).thenReturn(userBeforeUpdate);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/updateUser.do")
                        .file(picture)   // new picture's original name is empty string so will not be updated
                        .param("userName", userName)
                        .param("userID", userID)
                        .param("passwordNew", password)
                        .param("email", email)
                        .param("phone", phone)
                        .session((MockHttpSession) request.getSession())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("user_info"));

        // 检查 session中的 user信息是否被成功更新
        assertEquals(userAfterUpdate, request.getSession().getAttribute("user"));

        verify(userService).findByUserID(userID);
        verify(userService).updateUser(userAfterUpdate);
        mockedStatic.verify(never(), () -> FileUtil.saveUserFile(picture));  // method will not be invoked
    }

    // BUG: 没有检查用户不存在的情况
    @Test
    public void testUpdateUserWithWithNotExistingUser() throws Exception {

        String userID = "not exists";  // not exists
        String userName = "test";
        String password = "password123";
        String email = "test@example.com";
        String phone = "1234567890";
        MockMultipartFile picture = new MockMultipartFile("picture", "image.jpg", "image/jpeg", "Some image content here".getBytes());

        when(userService.findByUserID(userID)).thenReturn(null);  // user not exists

        mockMvc.perform(MockMvcRequestBuilders.multipart("/updateUser.do")
                        .file(picture)
                        .param("userName", userName)
                        .param("userID", userID)
                        .param("passwordNew", password)
                        .param("email", email)
                        .param("phone", phone)
                        .session((MockHttpSession) request.getSession())
                )
                .andExpect(status().isNotFound());

        verify(userService).findByUserID(userID);
    }

    // BUG: 没有检查参数为空的情况
    @Test
    public void testUpdateUserWithEmptyParam() throws Exception {
        mockMvc.perform(post("/updateUser.do"))
                .andExpect(status().isBadRequest());
    }


    /*
     * 检查密码
     * */

    @Test
    public void testCheckPasswordWithCorrectPassword() throws Exception {

        String userID = "1";
        String password = "correctPassword";

        User user = new User();
        user.setUserID(userID);
        user.setPassword(password);

        when(userService.findByUserID(anyString())).thenReturn(user);

        mockMvc.perform(get("/checkPassword.do")
                        .param("userID", userID)
                        .param("password", password))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(userService).findByUserID(userID);
    }

    @Test
    public void testCheckPasswordWithWrongPassword() throws Exception {

        String userID = "1";
        String passwordCorrect = "correctPassword";
        String passwordWrong = "wrongPassword";

        User user = new User();
        user.setUserID(userID);
        user.setPassword(passwordCorrect);

        when(userService.findByUserID(anyString())).thenReturn(user);

        mockMvc.perform(get("/checkPassword.do")
                        .param("userID", userID)
                        .param("password", passwordWrong))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        verify(userService).findByUserID(userID);
    }

    // BUG: 没有检查用户不存在的情况
    @Test
    public void testCheckPasswordWithNotExistingUser() throws Exception {

        String userID = "not exists user";
        String password = "password";

        when(userService.findByUserID(anyString())).thenReturn(null);  // user not found

        mockMvc.perform(get("/checkPassword.do")
                        .param("userID", userID)
                        .param("password", password))
                .andExpect(status().isNotFound());

        verify(userService).findByUserID(userID);
    }

    // BUG: 没有检查参数为 null的情况
    @Test
    public void testCheckPasswordWithEmptyParam() throws Exception {
        mockMvc.perform(get("/checkPassword.do"))
                .andExpect(status().isBadRequest());
    }

}