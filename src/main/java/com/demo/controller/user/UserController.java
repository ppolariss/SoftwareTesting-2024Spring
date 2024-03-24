package com.demo.controller.user;

import com.demo.entity.User;
import com.demo.service.UserService;
import com.demo.utils.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

@Controller
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping("/signup")
    public String signUp(){
        return "signup";
    }

    @GetMapping("/login")
    public String login(){
        return "login";
    }




    @PostMapping("/loginCheck.do")
    @ResponseBody
    public String login(String userID,String password, HttpServletRequest request) throws IOException {
        User user=userService.checkLogin(userID,password);
        if(user!=null){
            if(user.getIsadmin()==0){
                request.getSession().setAttribute("user",user);
                System.out.println("user login!");
                return "/index";
            }
            else if(user.getIsadmin()==1){
                request.getSession().setAttribute("admin",user);
                System.out.println("admin login!");
                return "/admin_index";
            }
        }
        return "false";

    }

    @PostMapping("/register.do")
    public void register(String userID,String userName, String password, String email, String phone,
                         HttpServletResponse response) throws IOException{
        User user=new User();
        user.setUserID(userID);
        user.setUserName(userName);
        user.setPassword(password);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPicture("");
        userService.create(user);
        response.sendRedirect("login");
    }

    @GetMapping("/logout.do")
    public void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.getSession().removeAttribute("user");
        System.out.println("log out success!");
        response.sendRedirect("/index");
    }
    @GetMapping("/quit.do")
    public void quit(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.getSession().removeAttribute("admin");
        System.out.println("log out success!");
        response.sendRedirect("/index");
    }



    @PostMapping("/updateUser.do")
    public void updateUser(String userName, String userID, String passwordNew,String email, String phone, MultipartFile picture,HttpServletRequest request, HttpServletResponse response) throws Exception {
        User user=userService.findByUserID(userID);
        user.setUserName(userName);
        if(passwordNew!=null&& !"".equals(passwordNew)){
            user.setPassword(passwordNew);
        }
        user.setEmail(email);
        user.setPhone(phone);
        if(!Objects.equals(picture.getOriginalFilename(), "")){
            user.setPicture(FileUtil.saveUserFile(picture));
        }

        userService.updateUser(user);
        request.getSession().removeAttribute("user");
        request.getSession().setAttribute("user",user);
        response.sendRedirect("user_info");
    }


    @GetMapping("/checkPassword.do")
    @ResponseBody
    public boolean checkPassword(String userID,String password)
    {
        User user=userService.findByUserID(userID);
        return user.getPassword().equals(password);
    }

    @GetMapping("/user_info")
    public String user_info(Model model){
        return "user_info";
    }
}
