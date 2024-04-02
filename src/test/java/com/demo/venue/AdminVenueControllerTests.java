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

import javax.persistence.EntityNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNull;
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
//    @Autowired
    private VenueService venueService;

//    @Test
//    public void preparation() throws Exception {
////        Venue venue = venueService.findByVenueID(999);
////        assertNull(venue);
//        Page<Venue> pages = venueService.findAll(PageRequest.of(999, 10, Sort.by("venueID").ascending()));
//        assertNull(pages);
////        Page<Venue> page = new PageImpl<>(Collections.singletonList(
////                new Venue(1, "venue_name", "description", 1, "picture", "address", "open_time", "close_time")
////        ));
//
//        System.out.println(venueService.findAll(PageRequest.of(0, 10, Sort.by("venueID").ascending())).getTotalPages());
//    }


    //    venue_manage
    @Test
    public void testVenueManageWithSuccess() throws Exception {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("venueID").ascending());
        when(venueService.findAll(pageable))
                .thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 1));
//        Page<Venue> vs = new PageImpl<>(Collections.emptyList(), pageable, 1);

        mockMvc.perform(get("/venue_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/venue_manage"))
                // Expect the returned view name
                .andExpect(model().attribute("total", 1));
        // Expect that the "total" attribute is set in the model
    }


    //    editVenue
    @Test
    public void testEditVenueWithSuccess() throws Exception {
        Venue venue = new Venue(1, "venue_name", "description", 1, "picture", "address", "open_time", "close_time");
        when(venueService.findByVenueID(1))
                .thenReturn(venue);

        mockMvc.perform(get("/venue_edit?venueID=1"))
                .andExpect(status().isOk())
                .andExpect(view().name("/admin/venue_edit"))
                .andExpect(model().attribute("venue", venue));
    }

    @Test
    public void testEditVenueWithNull() throws Exception {
        when(venueService.findByVenueID(2))
                .thenThrow(EntityNotFoundException.class);
//        try {
        mockMvc.perform(get("/venue_edit?venueID=2"))
                .andExpect(status().isNotFound());
//        } catch ()
    }

    @Test
    public void testEditVenueWithNoParam() throws Exception {
//        only need to mock when guess the database
//        when(venueService.findAll(any(Pageable.class))).
//                thenThrow(IllegalStateException.class);
        mockMvc.perform(get("/venue_edit"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testEditVenueWithNegativeParam() throws Exception {
        mockMvc.perform(get("/venue_edit?venueID=-1"))
                .andExpect(status().isBadRequest());
    }


    //    venue_add
    @Test
    public void testVenueAddWithSuccess() throws Exception {
        mockMvc.perform(get("/venue_add"))
                .andExpect(status().isOk())
                .andExpect(view().name("/admin/venue_add"));
    }


    //    getVenueList
    @Test
    public void testGetVenueListWithSuccess() throws Exception {
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
    }

    @Test
    public void testGetVenueListWithEmpty() throws Exception {
//        curl -i http://localhost:8888/venueList.do?page=999
        when(venueService.findAll(PageRequest.of(1, 10, Sort.by("venueID").ascending())))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        mockMvc.perform(get("/venueList.do?page=2"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    public void testGetVenueListWithNegativeParam() throws Exception {
        mockMvc.perform(get("/venueList.do?page=-1"))
                .andExpect(status().isBadRequest());
    }


    //    addVenue
    @Test
    public void testAddVenueWithSuccess() throws Exception {
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
    }

    //    TODO EmptyPic
    @Test
    public void testAddVenueWithNoPicSuccess() throws Exception {
        when(venueService.create(any(Venue.class)))
                .thenReturn(1);
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
    public void testAddVenueWithCreationFailed() throws Exception {
        when(venueService.create(any(Venue.class)))
                .thenReturn(0);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/addVenue.do")
                        .file(new MockMultipartFile("picture", "", "image/jpg", new byte[0]))
                        .param("venueName", "venue_name")
                        .param("address", "address")
                        .param("description", "description")
                        .param("price", "1")
                        .param("open_time", "open_time")
                        .param("close_time", "close_time"))
//                .andExpect(model().attribute("message", "添加失败"))  //request
                .andExpect(redirectedUrl("venue_add"));
//               picture shouldn't be null
    }

    @Test
    public void testAddVenueWithNoParam() throws Exception {
        mockMvc.perform(post("/addVenue.do"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testAddVenueWithGetMethod() throws Exception {
        mockMvc.perform(get("/addVenue.do"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void testAddVenueWithFloatPrice() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.multipart("/addVenue.do")
                        .file(new MockMultipartFile("picture", "", "image/jpg", new byte[0]))
                        .param("venueName", "venue_name")
                        .param("address", "address")
                        .param("description", "description")
                        .param("price", "1.5")
                        .param("open_time", "open_time")
                        .param("close_time", "close_time"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testAddVenueWithNegPrice() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.multipart("/addVenue.do")
                        .file(new MockMultipartFile("picture", "", "image/jpg", new byte[0]))
                        .param("venueName", "venue_name")
                        .param("address", "address")
                        .param("description", "description")
                        .param("price", "-1")
                        .param("open_time", "open_time")
                        .param("close_time", "close_time"))
                .andExpect(status().isBadRequest());
    }
//    open_time?


    //    modifyVenue
    @Test
    public void testModifyVenueWithSuccess() throws Exception {
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
    }

    @Test
    public void testModifyVenueWithEmptyPicSuccess() throws Exception {
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
    public void testModifyVenueWithNoPicSuccess() throws Exception {
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
    }

    @Test
    public void testModifyVenueWithFloatPrice() throws Exception {
        mockMvc.perform(post("/modifyVenue.do")
                        .param("venueID", "1")
                        .param("venueName", "venue_name")
                        .param("address", "address")
                        .param("description", "description")
                        .param("price", "1.5")
                        .param("open_time", "open_time")
                        .param("close_time", "close_time"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testModifyVenueWithNegPrice() throws Exception {
        mockMvc.perform(post("/modifyVenue.do")
                        .param("venueID", "1")
                        .param("venueName", "venue_name")
                        .param("address", "address")
                        .param("description", "description")
                        .param("price", "-1")
                        .param("open_time", "open_time")
                        .param("close_time", "close_time"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testModifyVenueWithNoParam() throws Exception {
        mockMvc.perform(post("/modifyVenue.do"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testModifyVenueWithGetMethod() throws Exception {
        mockMvc.perform(get("/modifyVenue.do"))
                .andExpect(status().isMethodNotAllowed());
    }


    //    delVenue
    @Test
    public void testDelVenueWithSuccess() throws Exception {
        mockMvc.perform(post("/delVenue.do")
                        .param("venueID", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    public void testDelVenueWithNoParam() throws Exception {
        mockMvc.perform(post("/delVenue.do"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testDelVenueWithGetMethod() throws Exception {
        mockMvc.perform(get("/delVenue.do"))
                .andExpect(status().isMethodNotAllowed());
    }

    //    checkVenueName
    @Test
    public void testCheckVenueNameWithTrueSuccess() throws Exception {
        when(venueService.countVenueName("venue_name"))
                .thenReturn(0);
        mockMvc.perform(post("/checkVenueName.do")
                        .param("venueName", "venue_name"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    public void testCheckVenueNameWithFalseSuccess() throws Exception {
        when(venueService.countVenueName("venue_name"))
                .thenReturn(1);
        mockMvc.perform(post("/checkVenueName.do")
                        .param("venueName", "venue_name"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    public void testCheckVenueNameWithNoParam() throws Exception {
        mockMvc.perform(post("/checkVenueName.do"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testCheckVenueNameWithGetMethod() throws Exception {
        mockMvc.perform(get("/checkVenueName.do"))
                .andExpect(status().isMethodNotAllowed());
    }
}
