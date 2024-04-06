package com.demo.user;

import com.demo.controller.user.UserController;
import com.demo.entity.User;
import com.demo.service.UserService;
import com.demo.utils.FileUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(PowerMockRunner.class) // 使用 PowerMockito 扩展
@WebMvcTest(UserController.class) // 仅限定测试范围在 MVC 层
@PrepareForTest(FileUtil.class) // 准备对 StaticClass 进行测试
public class StaticMockTests {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private UserService userService; // 假设您需要注入的服务

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    public void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    public void testControllerMethod() throws Exception {
        String userID = "1";
        String userName = "test";
        String password = "password123";
        String email = "test@example.com";
        String phone = "1234567890";
        MockMultipartFile picture = new MockMultipartFile("picture", "image.jpg", "image/jpeg", "Some image content here".getBytes());

        User userBeforeUpdate = new User();
        userBeforeUpdate.setUserID(userID);

        User userAfterUpdate = new User(0, userID, userName, password, email, phone, 0, "image.jpg");

        when(userService.findByUserID(anyString())).thenReturn(userBeforeUpdate);
        when(FileUtil.saveUserFile(picture)).thenReturn(picture.getOriginalFilename());

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
}
