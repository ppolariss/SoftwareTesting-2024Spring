package com.demo.venue;

import com.demo.controller.admin.AdminVenueController;
import com.demo.entity.Venue;
import com.demo.service.VenueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminVenueController.class)
public class AdminVenueControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VenueService venueService; // Mocked service

    @Test
    public void venueManagePageTest() throws Exception {
        // Mock data and behavior of venueService
        when(venueService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList())); // You need to adjust this according to your actual service method

        // Perform GET request
        mockMvc.perform(get("/venue_manage"))
                .andExpect(status().isOk()) // Expect HTTP 200 status
                .andExpect(view().name("admin/venue_manage")) // Expect the returned view name
                .andExpect(model().attributeExists("total")); // Expect that the "total" attribute is set in the model
    }

    @Test
    public void editVenuePageTest() throws Exception {
        when(venueService.findByVenueID(1))
                .thenReturn(new Venue(1, "venue_name", "description", 1, "picture", "address", "open_time", "close_time"));
        when(venueService.findByVenueID(2))
                .thenReturn(null);

        mockMvc.perform(get("/venue_edit?venueID=1"))
                .andExpect(status().isOk())
                .andExpect(view().name("/admin/venue_edit"))
                .andExpect(model().attributeExists("venue"));

        mockMvc.perform(get("/venue_edit?venueID=2"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/venue_edit"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/venue_edit?venueID=-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void addVenuePageTest() throws Exception {
        mockMvc.perform(get("/venue_add"))
                .andExpect(status().isOk())
                .andExpect(view().name("/admin/venue_add"));
    }

    @Test
    public void getVenueListTest() throws Exception {
        Page<Venue> page = new PageImpl<>(Collections.singletonList(
                new Venue(1, "venue_name", "description", 1, "picture", "address", "open_time", "close_time")
        ));

        when(venueService.findAll(PageRequest.of(0, 10, Sort.by("venueID").ascending())))
                .thenReturn(page);
        mockMvc.perform(get("/venueList.do?page=1"))
                .andExpect(status().isOk())
                .andExpect(content().json("[{\"venueID\":1,\"venueName\":\"venue_name\",\"description\":\"description\",\"price\":1,\"picture\":\"picture\",\"address\":\"address\",\"open_time\":\"open_time\",\"close_time\":\"close_time\"}]"));
        mockMvc.perform(get("/venueList.do"))
                .andExpect(status().isOk())
                .andExpect(content().json("[{\"venueID\":1,\"venueName\":\"venue_name\",\"description\":\"description\",\"price\":1,\"picture\":\"picture\",\"address\":\"address\",\"open_time\":\"open_time\",\"close_time\":\"close_time\"}]"));


        when(venueService.findAll(PageRequest.of(1, 10, Sort.by("venueID").ascending())))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        mockMvc.perform(get("/venueList.do?page=2"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        mockMvc.perform(get("/venueList.do?page=-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void addVenueTest() throws Exception {
        mockMvc.perform(get("/addVenue.do"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(post("/addVenue.do"))
                .andExpect(status().isBadRequest());

        when(venueService.create(any(Venue.class)))
                .thenReturn(0);

        mockMvc.perform(post("/addVenue.do")
                        .param("venueName", "venue_name")
                        .param("address", "address")
                        .param("description", "description")
                        .param("price", "1")
                        .param("open_time", "open_time")
                        .param("close_time", "close_time"))
                .andExpect(model().attribute("message", "添加失败"))
                .andExpect(redirectedUrl("venue_add"));
//                .andExpect(status().isBadRequest())


        when(venueService.create(any(Venue.class)))
                .thenReturn(1);
        mockMvc.perform(post("/addVenue.do")
                        .param("venueName", "venue_name")
                        .param("address", "address")
                        .param("description", "description")
                        .param("price", "1")
                        .param("open_time", "open_time")
                        .param("close_time", "close_time"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("venue_manage"));


        mockMvc.perform(post("/addVenue.do")
                        .param("venueName", "venue_name")
                        .param("address", "address")
                        .param("description", "description")
                        .param("price", "1.5")
                        .param("open_time", "open_time")
                        .param("close_time", "close_time"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/addVenue.do")
                        .param("venueName", "venue_name")
                        .param("address", "address")
                        .param("description", "description")
                        .param("price", "-1")
                        .param("open_time", "open_time")
                        .param("close_time", "close_time"))
                .andExpect(status().isBadRequest());

        Path imagePath = Paths.get("./resources/test.jpg");
        byte[] imageBytes = Files.readAllBytes(imagePath);
        MockMultipartFile imageFile = new MockMultipartFile("picture", "test.jpg", "image/jpg", imageBytes);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/addVenue.do")
                        .file(imageFile)
                        .param("venueName", "venue_name")
                        .param("address", "address")
                        .param("description", "description")
                        .param("price", "1")
                        .param("open_time", "open_time")
                        .param("close_time", "close_time"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("venue_manage"));

        mockMvc.perform(MockMvcRequestBuilders.multipart("/addVenue.do")
                        .file(new MockMultipartFile("picture", "", "image/jpg", new byte[0]))
                        .param("venueName", "venue_name")
                        .param("address", "address")
                        .param("description", "description")
                        .param("price", "1")
                        .param("open_time", "open_time")
                        .param("close_time", "close_time"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("venue_manage"));

    }

    @Test
    public void modifyVenueTest() throws Exception {
        mockMvc.perform(get("/modifyVenue.do"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(post("/modifyVenue.do"))
                .andExpect(status().isBadRequest());

        when(venueService.findByVenueID(1))
                .thenReturn(new Venue(1, "venue_name", "description", 1, "picture", "address", "open_time", "close_time"));

        mockMvc.perform(post("/modifyVenue.do")
                        .param("venueID", "1")
                        .param("venueName", "venue_name")
                        .param("address", "address")
                        .param("description", "description")
                        .param("price", "1")
                        .param("open_time", "open_time")
                        .param("close_time", "close_time"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("venue_manage"));

        mockMvc.perform(post("/modifyVenue.do")
                        .param("venueID", "1")
                        .param("venueName", "venue_name")
                        .param("address", "address")
                        .param("description", "description")
                        .param("price", "1.5")
                        .param("open_time", "open_time")
                        .param("close_time", "close_time"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/modifyVenue.do")
                        .param("venueID", "1")
                        .param("venueName", "venue_name")
                        .param("address", "address")
                        .param("description", "description")
                        .param("price", "-1")
                        .param("open_time", "open_time")
                        .param("close_time", "close_time"))
                .andExpect(status().isBadRequest());

        Path imagePath = Paths.get("./resources/test.jpg");
        byte[] imageBytes = Files.readAllBytes(imagePath);
        MockMultipartFile imageFile = new MockMultipartFile("picture", "test.jpg", "image/jpg", imageBytes);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/modifyVenue.do")
                        .file(imageFile)
                        .param("venueID", "1")
                        .param("venueName", "venue_name")
                        .param("address", "address")
                        .param("description", "description")
                        .param("price", "1")
                        .param("open_time", "open_time")
                        .param("close_time", "close_time"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("venue_manage"));

        mockMvc.perform(MockMvcRequestBuilders.multipart("/modifyVenue.do")
                        .file(new MockMultipartFile("picture", "", "image/jpg", new byte[0]))
                        .param("venueID", "1")
                        .param("venueName", "venue_name")
                        .param("address", "address")
                        .param("description", "description")
                        .param("price", "1")
                        .param("open_time", "open_time")
                        .param("close_time", "close_time"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("venue_manage"));
    }

    @Test
    public void delVenueTest() throws Exception {
        mockMvc.perform(get("/delVenue.do"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(post("/delVenue.do"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/delVenue.do")
                        .param("venueID", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    public void checkVenueNameTest() throws Exception {
        mockMvc.perform(get("/checkVenueName.do"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(post("/checkVenueName.do"))
                .andExpect(status().isBadRequest());

        when(venueService.countVenueName("venue_name"))
                .thenReturn(0);
        mockMvc.perform(post("/checkVenueName.do")
                        .param("venueName", "venue_name"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        when(venueService.countVenueName("venue_name"))
                .thenReturn(1);
        mockMvc.perform(post("/checkVenueName.do")
                        .param("venueName", "venue_name"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }
}
