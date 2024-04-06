package com.demo.user;

import com.demo.entity.User;
import com.demo.entity.Venue;
import com.demo.service.UserService;
import com.demo.service.VenueService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest
@ExtendWith(SpringExtension.class) // 使用 @ExtendWith 替代 @RunWith
public class NotFoundTests {

    @Autowired
    private UserService userService;

    @Autowired
    private VenueService venueService;

    @Test
    public void testUserNotFound() {
        try {
            User userExists = userService.checkLogin("test", "test");
            User userNotExists = userService.checkLogin("not exist", "wrong pwd");
            System.out.println(userExists);
            System.out.println(userNotExists);
        } catch (Exception e) {
            System.out.println("Exception here");
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void testVenueNotFound() {
        try {
            Venue venue = venueService.findByVenueID(1);
            System.out.println(venue);
        } catch (Exception e) {
            System.out.println("Exception here");
            System.out.println(e.getMessage());
        }
    }

}
