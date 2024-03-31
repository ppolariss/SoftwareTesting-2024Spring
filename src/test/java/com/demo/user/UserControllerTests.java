package com.demo.user;

import static org.hamcrest.Matchers.containsString;
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.multipart.MultipartFile;

@WebMvcTest(UserController.class)
//@RunWith(PowerMockRunner.class)
//@PrepareForTest(FileUtil.class)
public class UserControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;


//    @InjectMocks
//    private FileUtil fileUtil;


    /**
     * 注册新用户
     */

    @Test
    public void testSignUpView() throws Exception {
        mockMvc.perform(get("/signup"))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"));
    }

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

    // BUG: 没有检查必要字段为空的情况，如 UserID —— 但是这个其实会在 DAO层插入数据库时报错
    // 不合法的情况还有其他必要字段为空、字段格式不匹配，是否需一一列出？
    @Test
    public void testRegisterWithIncompleteUserInfo() throws Exception {

        String userID = null;  // userID cannot be null
        String userName = "test";
        String password = "password123";
        String email = "test@example.com";
        String phone = "13495684256";

//        when(userService.create(any(User.class))).thenReturn(0);
        doThrow(new RuntimeException("UserID cannot be empty.")).when(userService).create(any(User.class));
        // 我们应该如何假设service层出现异常的行为？假定抛出异常还是设置返回值？但是返回值在controller函数中根本不起作用
        // 如果是设定抛出异常的话，这个测试函数会报错（标红报错，不是标黄的未通过）

        mockMvc.perform(post("/register.do")
                        .param("userID", userID)
                        .param("userName", userName)
                        .param("password", password)
                        .param("email", email)
                        .param("phone", phone))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("UserID cannot be empty.")));

        verify(userService).create(any(User.class));
    }

    // BUG: 没有检查用户已存在的情况
    @Test
    public void testRegisterWithExistingUser() throws Exception {
        String userID = "1";
        String userName = "Existing User";
        String password = "password123";
        String email = "existing@example.com";
        String phone = "1234567890";

        when(userService.create(any(User.class))).thenReturn(0);
//        doThrow(new RuntimeException("User already exists.")).when(userService).create(any(User.class));

        mockMvc.perform(post("/register.do")
                        .param("userID", userID)
                        .param("userName", userName)
                        .param("password", password)
                        .param("email", email)
                        .param("phone", phone))
                .andExpect(status().isConflict());  // 409 conflict
//                .andExpect(content().string(containsString("User already exists.")));

        verify(userService).create(any(User.class));
    }


    /**
     * 账号登录
     */

    @Test
    public void testLoginView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    public void testLoginWithValidUser() throws Exception {

        String userID = "1";
        String password = "validPassword";

        User user = new User();
        user.setUserID(userID);
        user.setPassword(password);
        user.setIsadmin(0);

        when(userService.checkLogin(anyString(), anyString())).thenReturn(user);

        mockMvc.perform(post("/loginCheck.do")
                        .param("userID", userID)
                        .param("password", password))
                .andExpect(status().isOk())
                .andExpect(content().string("/index")) // redirect
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
        user.setIsadmin(1);

        when(userService.checkLogin(anyString(), anyString())).thenReturn(user);

        mockMvc.perform(post("/loginCheck.do")
                        .param("userID", userID)
                        .param("password", password))
                .andExpect(status().isOk())
                .andExpect(content().string("/admin_index")) // redirect
                .andExpect(request().sessionAttribute("admin", user));

        verify(userService).checkLogin(userID, password);
    }

    // 讨论：下面这两种情况是否该合并

    @Test
    public void testLoginWithNotExistingUser() throws Exception {

        String userID = "1";
        String password = "notExistingPassword";

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


    /*
     * 账号登出
     * */

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    public void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    public void testLogout() throws Exception {
        request.getSession().setAttribute("user", new User());

        mockMvc.perform(get("/logout.do").session((MockHttpSession) request.getSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/index"));

        assertNull(request.getSession().getAttribute("user")); // user in session has been removed correctly
    }

    @Test
    public void testQuit() throws Exception {
        request.getSession().setAttribute("admin", new User());

        mockMvc.perform(get("/quit.do").session((MockHttpSession) request.getSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/index"));

        assertNull(request.getSession().getAttribute("admin"));
    }

    // TODO: 是否需要考虑在没有登录的情况下，就登出的情况？无效等价类应该如何设置？

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

        User userAfterUpdate = new User(0, userID, userName, password, email, phone, 0, "image.jpg");

        // TODO: 模拟静态方法行为

        // Mock Static method
//        PowerMockito.mockStatic(FileUtil.class);
//        when(FileUtil.saveUserFile(picture)).thenReturn(picture.getOriginalFilename());

        when(userService.findByUserID(anyString())).thenReturn(userBeforeUpdate);
//        when(fileUtil.saveUserFile(any(MultipartFile.class))).thenReturn("image.jpg");

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

        assertEquals(userAfterUpdate, request.getSession().getAttribute("user"));

        verify(userService).findByUserID(userID);
        verify(userService).updateUser(userAfterUpdate);
    }

    @Test
    public void testUpdateUserWithIncompleteInfo() throws Exception {

        String userID = null; // userID cannot be null
        String userName = "test";
        String password = "password123";
        String email = "test@example.com";
        String phone = "1234567890";
        MockMultipartFile picture = new MockMultipartFile("picture", "image.jpg", "image/jpeg", "Some image content here".getBytes());

        when(userService.findByUserID(anyString())).thenReturn(null);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/updateUser.do")
                        .file(picture)
                        .param("userName", userName)
                        .param("userID", userID)
                        .param("passwordNew", password)
                        .param("email", email)
                        .param("phone", phone)
                        .session((MockHttpSession) request.getSession())
                )
                .andExpect(status().isBadRequest());

        verify(userService).findByUserID(userID);
    }

    @Test
    public void testUpdateUserWithNotExistingUser() throws Exception {

        String userID = "1";
        String userName = "test";
        String password = "password123";
        String email = "test@example.com";
        String phone = "1234567890";
        MockMultipartFile picture = new MockMultipartFile("picture", "image.jpg", "image/jpeg", "Some image content here".getBytes());

        when(userService.findByUserID(anyString())).thenReturn(null);

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

        String userID = "1";
        String password = "password";

        when(userService.findByUserID(anyString())).thenReturn(null);

        mockMvc.perform(get("/checkPassword.do")
                        .param("userID", userID)
                        .param("password", password))
                .andExpect(status().isNotFound());

        verify(userService).findByUserID(userID);
    }

}