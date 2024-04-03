package com.demo.venue;

import com.demo.controller.admin.AdminVenueController;
import com.demo.entity.Venue;
import com.demo.service.VenueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.persistence.EntityNotFoundException;
import javax.persistence.NonUniqueResultException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminVenueController.class)
public class AdminVenueControllerTests {
    //    "2006-01-02 15:04:05"
    final String CORRECT_OPEN_TIME = "15:04";
    final String CORRECT_CLOSE_TIME = "23:20";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VenueService venueService;


    //    venue_manage

    @Test
    public void testVenueManageWithSuccess() throws Exception {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("venueID").ascending());
        when(venueService.findAll(pageable))
                .thenReturn(new PageImpl<>(Collections.singletonList(
                        new Venue(1, "venue_name", "description", 1, "", "address", CORRECT_OPEN_TIME, CORRECT_CLOSE_TIME)
                ), pageable, 1));

        mockMvc.perform(get("/venue_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/venue_manage"))
                .andExpect(model().attribute("total", 1));
        verify(venueService).findAll(pageable);
    }

    @Test
    public void testVenueManageWithEmptySuccess() throws Exception {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("venueID").ascending());
        when(venueService.findAll(pageable))
                .thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));
//        Page<Venue> vs = new PageImpl<>(Collections.emptyList(), pageable, 1);
//        System.out.println(new PageImpl<>(Collections.emptyList(), pageable, 0).getTotalElements());

        mockMvc.perform(get("/venue_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/venue_manage"))
                .andExpect(model().attribute("total", 0));
        verify(venueService).findAll(pageable);
    }

    @Test
    public void testVenueManageWithOverflowSuccess() {
        List<Venue> venues = IntStream.range(0, 15)
                .mapToObj(i -> new Venue(i + 1, "venue_name", "description", 1, "", "address", CORRECT_OPEN_TIME, CORRECT_CLOSE_TIME))
                .collect(Collectors.toList());
        Pageable pageable = PageRequest.of(0, 10, Sort.by("venueID").ascending());
        Page<Venue> page = new PageImpl<>(venues.subList(0, 10), pageable, 15);
        when(venueService.findAll(pageable))
                .thenReturn(page);
        try {
            mockMvc.perform(get("/venue_manage"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("total", 2));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }


    //    editVenue
    @Test
    public void testEditVenueWithSuccess() throws Exception {
        Venue venue = new Venue(1, "venue_name", "description", 1, "picture", "address", CORRECT_OPEN_TIME, CORRECT_CLOSE_TIME);
        when(venueService.findByVenueID(1))
                .thenReturn(venue);

        mockMvc.perform(get("/venue_edit?venueID=1"))
                .andExpect(status().isOk())
                .andExpect(view().name("/admin/venue_edit"))
                .andExpect(model().attribute("venue", venue));
        verify(venueService).findByVenueID(1);
    }

    @Test
    public void testEditVenueWithNotFound() {
        when(venueService.findByVenueID(2))
                .thenThrow(EntityNotFoundException.class);
        try {
            mockMvc.perform(get("/venue_edit?venueID=2"))
                    .andExpect(status().isNotFound());
        } catch (Exception e) {
            fail();
        }
        verify(venueService).findByVenueID(2);
    }

    //        only need to mock when guess the database
//        when(venueService.findAll(any(Pageable.class))).
//                thenThrow(IllegalStateException.class);
    @Test
    public void testEditVenueWithNoParam() {
        try {
            mockMvc.perform(get("/venue_edit"))
                    .andExpect(status().isBadRequest());
        } catch (Exception exception) {
            fail();
        }
    }

    @Test
    public void testEditVenueWithNegativeParam() {
        try {
            mockMvc.perform(get("/venue_edit?venueID=-1"))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testEditVenueWithFloatParam() {
        try {
            mockMvc.perform(get("/venue_edit?venueID=1.5"))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            fail();
        }
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
        Pageable pageable = PageRequest.of(0, 10, Sort.by("venueID").ascending());
        Page<Venue> page = new PageImpl<>(Collections.singletonList(
                new Venue(1, "venue_name", "description", 1, "picture", "address", CORRECT_OPEN_TIME, CORRECT_CLOSE_TIME)
        ), pageable, 1);

        when(venueService.findAll(pageable))
                .thenReturn(page);
        mockMvc.perform(get("/venueList.do?page=1"))
                .andExpect(status().isOk())
                .andExpect(content().json("[{\"venueID\":1,\"venueName\":\"venue_name\",\"description\":\"description\",\"price\":1,\"picture\":\"picture\",\"address\":\"address\",\"open_time\":\"" + CORRECT_OPEN_TIME + "\",\"close_time\":\"" + CORRECT_CLOSE_TIME + "\"}]"));
        verify(venueService).findAll(pageable);
    }

    @Test
    public void testGetVenueListWithNoParamSuccess() throws Exception {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("venueID").ascending());
        Page<Venue> page = new PageImpl<>(Collections.singletonList(
                new Venue(1, "venue_name", "description", 1, "picture", "address", CORRECT_OPEN_TIME, CORRECT_CLOSE_TIME)
        ), pageable, 1);

        when(venueService.findAll(pageable))
                .thenReturn(page);
        mockMvc.perform(get("/venueList.do"))
                .andExpect(status().isOk())
                .andExpect(content().json("[{\"venueID\":1,\"venueName\":\"venue_name\",\"description\":\"description\",\"price\":1,\"picture\":\"picture\",\"address\":\"address\",\"open_time\":\"" + CORRECT_OPEN_TIME + "\",\"close_time\":\"" + CORRECT_CLOSE_TIME + "\"}]"));
        verify(venueService).findAll(pageable);
    }

    @Test
    public void testGetVenueListWithEmpty() throws Exception {
//        curl -i http://localhost:8888/venueList.do?page=999
        Pageable pageable = PageRequest.of(1, 10, Sort.by("venueID").ascending());
        when(venueService.findAll(pageable))
                .thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));
        mockMvc.perform(get("/venueList.do?page=2"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(venueService).findAll(pageable);
    }

    @Test
    public void testGetVenueListWithNegativeParam() {
        try {
            mockMvc.perform(get("/venueList.do?page=-1"))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testGetVenueListWithFloatParam() {
        try {
            mockMvc.perform(get("/venueList.do?page=1.5"))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            fail();
        }
    }


    //    addVenue
    @Test
    public void testAddVenueWithSuccess() throws Exception {
//        ./resources/test.jpg
        Path imagePath = Paths.get("./src/main/resources/static/venue.jpg");
        byte[] imageBytes = Files.readAllBytes(imagePath);
        MockMultipartFile imageFile = new MockMultipartFile("picture", "test.jpg", "image/jpg", imageBytes);
        when(venueService.create(any(Venue.class)))
                .thenReturn(1);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/addVenue.do")
                        .file(imageFile)
                        .param("venueName", "venue_name")
                        .param("address", "address")
                        .param("description", "description")
                        .param("price", "1")
                        .param("open_time", "2006-01-02 15:04:05")
                        .param("close_time", "2024-04-02 23:20:05"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("venue_manage"));
        verify(venueService).create(any(Venue.class));
    }

    @Test
    public void testAddVenueWithNoPicSuccess() throws Exception {
        Venue venue = new Venue(0, "venue_name", "description", 1, "", "address", "2006-01-02 15:04:05", "2024-04-02 23:20:05");
        when(venueService.create(venue))
                .thenReturn(1);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/addVenue.do")
                        .file(new MockMultipartFile("picture", "", "image/jpg", new byte[0]))
                        .param("venueName", "venue_name")
                        .param("address", "address")
                        .param("description", "description")
                        .param("price", "1")
                        .param("open_time", "2006-01-02 15:04:05")
                        .param("close_time", "2024-04-02 23:20:05"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("venue_manage"));
        verify(venueService).create(venue);
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
                        .param("open_time", "2006-01-02 15:04:05")
                        .param("close_time", "2024-04-02 23:20:05"))
                .andExpect(request().attribute("message", "添加失败"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("venue_add"));
        verify(venueService).create(any(Venue.class));
    }

    @Test
    public void testAddVenueWithNoParam() {
        try {
            mockMvc.perform(post("/addVenue.do"))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testAddVenueWithFloatPrice() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.multipart("/addVenue.do")
                        .file(new MockMultipartFile("picture", "", "image/jpg", new byte[0]))
                        .param("venueName", "venue_name")
                        .param("address", "address")
                        .param("description", "description")
                        .param("price", "1.5")
                        .param("open_time", "2006-01-02 15:04:05")
                        .param("close_time", "2024-04-02 23:20:05"))
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
                        .param("open_time", "2006-01-02 15:04:05")
                        .param("close_time", "2024-04-02 23:20:05"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testAddVenueWithBadTime() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.multipart("/addVenue.do")
                        .file(new MockMultipartFile("picture", "", "image/jpg", new byte[0]))
                        .param("venueName", "venue_name")
                        .param("address", "address")
                        .param("description", "description")
                        .param("price", "1")
                        .param("open_time", "open_time")
                        .param("close_time", "close_time"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testAddVenueWithConflict() throws Exception {
        when(venueService.countVenueName("conflict"))
                .thenReturn(1);
        when(venueService.findByVenueName("conflict"))
                .thenThrow(NonUniqueResultException.class);

        when(venueService.create(any(Venue.class)))
                .thenReturn(1);

        Path imagePath = Paths.get("./src/main/resources/static/venue.jpg");
        byte[] imageBytes = Files.readAllBytes(imagePath);
        MockMultipartFile imageFile = new MockMultipartFile("picture", "test.jpg", "image/jpg", imageBytes);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.multipart("/addVenue.do")
                .file(imageFile)
                .param("venueName", UUID.randomUUID().toString())
                .param("address", "address")
                .param("description", "description")
                .param("price", "1")
                .param("open_time", "2006-01-02 15:04:05")
                .param("close_time", "2024-04-02 23:20:05");

        mockMvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("venue_add"))
                .andExpect(model().attribute("message", "添加失败！"));
    }

    //    modifyVenue
    @Test
    public void testModifyVenueWithSuccess() throws Exception {
        Path imagePath = Paths.get("./src/main/resources/static/venue.jpg");
        byte[] imageBytes = Files.readAllBytes(imagePath);
        MockMultipartFile imageFile = new MockMultipartFile("picture", "test.jpg", "image/jpg", imageBytes);

        when(venueService.findByVenueID(1))
                .thenReturn(new Venue(1, "venue_names", "descriptions", 1, "", "addresses", "open_time", "close_time"));

        mockMvc.perform(MockMvcRequestBuilders.multipart("/modifyVenue.do")
                        .file(imageFile)
                        .param("venueID", "1")
                        .param("venueName", "venue_name")
                        .param("address", "address")
                        .param("description", "description")
                        .param("price", "1")
                        .param("open_time", CORRECT_OPEN_TIME)
                        .param("close_time", CORRECT_CLOSE_TIME))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("venue_manage"));
        verify(venueService).findByVenueID(1);
        verify(venueService).update(any(Venue.class));
    }


    @Test
    public void testModifyVenueWithEmptyPicSuccess() throws Exception {
        when(venueService.findByVenueID(1))
                .thenReturn(new Venue(1, "venue_name", "description", 1, "pic1", "address", "open_time", "close_time"));
        mockMvc.perform(MockMvcRequestBuilders.multipart("/modifyVenue.do")
                        .file(new MockMultipartFile("picture", "", "image/jpg", new byte[0]))
                        .param("venueID", "1")
                        .param("venueName", "venue_name")
                        .param("address", "address")
                        .param("description", "description")
                        .param("price", "1")
                        .param("open_time", CORRECT_OPEN_TIME)
                        .param("close_time", CORRECT_CLOSE_TIME))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("venue_manage"));
        verify(venueService).findByVenueID(1);
        verify(venueService).update(new Venue(1, "venue_name", "description", 1, "pic1", "address", CORRECT_OPEN_TIME, CORRECT_CLOSE_TIME));
    }

    @Test
    public void testModifyVenueWithNoPic() {
        when(venueService.findByVenueID(1))
                .thenReturn(new Venue(1, "venue_name", "description", 1, "pic", "address", CORRECT_OPEN_TIME, CORRECT_CLOSE_TIME));

        try {
            mockMvc.perform(post("/modifyVenue.do")
                            .param("venueID", "1")
                            .param("venueName", "venue_name")
                            .param("address", "address")
                            .param("description", "description")
                            .param("price", "1")
                            .param("open_time", CORRECT_OPEN_TIME)
                            .param("close_time", CORRECT_CLOSE_TIME))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
//                .andExpect(status().is3xxRedirection())
//                .andExpect(redirectedUrl("venue_manage"));
//        verify(venueService).findByVenueID(1);
//        verify(venueService).update(new Venue(1, "venue_name", "description", 1, "pic", "address", CORRECT_OPEN_TIME, CORRECT_CLOSE_TIME));
    }

    @Test
    public void testModifyVenueWithNotFound() throws Exception {
        Path imagePath = Paths.get("./src/main/resources/static/venue.jpg");
        byte[] imageBytes = Files.readAllBytes(imagePath);
        MockMultipartFile imageFile = new MockMultipartFile("picture", "test.jpg", "image/jpg", imageBytes);

        when(venueService.findByVenueID(1))
                .thenThrow(EntityNotFoundException.class);

        try {
            mockMvc.perform(MockMvcRequestBuilders.multipart("/modifyVenue.do")
                            .file(imageFile)
                            .param("venueID", "1")
                            .param("venueName", "venue_name")
                            .param("address", "address")
                            .param("description", "description")
                            .param("price", "1")
                            .param("open_time", CORRECT_OPEN_TIME)
                            .param("close_time", CORRECT_CLOSE_TIME))
                    .andExpect(status().isNotFound());
            verify(venueService).findByVenueID(1);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testModifyVenueWithFloatPrice() {
        try {
            mockMvc.perform(post("/modifyVenue.do")
                            .param("venueID", "1.5")
                            .param("venueName", "venue_name")
                            .param("address", "address")
                            .param("description", "description")
                            .param("price", "1.5")
                            .param("open_time", CORRECT_OPEN_TIME)
                            .param("close_time", CORRECT_CLOSE_TIME))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            fail();
        }

    }

    @Test
    public void testModifyVenueWithNegPrice() {
//        when(venueService.findByVenueID(1))
//                .thenReturn(new Venue(1, "venue_name", "description", 1, "pic", "address", "open_time", "close_time"));
        try {
            mockMvc.perform(post("/modifyVenue.do")
                            .param("venueID", "-1")
                            .param("venueName", "venue_name")
                            .param("address", "address")
                            .param("description", "description")
                            .param("price", "-1")
                            .param("open_time", CORRECT_OPEN_TIME)
                            .param("close_time", CORRECT_CLOSE_TIME))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testModifyVenueWithBadTime() {
        try {
            mockMvc.perform(post("/modifyVenue.do")
                            .param("venueID", "1")
                            .param("venueName", "venue_name")
                            .param("address", "address")
                            .param("description", "description")
                            .param("price", "1")
                            .param("open_time", "open_time")
                            .param("close_time", "close_time"))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testModifyVenueWithConflictName() throws Exception {
        when(venueService.findByVenueName("conflict"))
                .thenThrow(NonUniqueResultException.class);
        when(venueService.countVenueName("conflict"))
                .thenReturn(1);
        when(venueService.findByVenueID(1))
                .thenReturn(new Venue(1, "venue_name", "description",
                        1, "pic", "address", CORRECT_OPEN_TIME, CORRECT_CLOSE_TIME));

        Path imagePath = Paths.get("./src/main/resources/static/venue.jpg");
        byte[] imageBytes = Files.readAllBytes(imagePath);
        MockMultipartFile imageFile = new MockMultipartFile("picture", "test.jpg", "image/jpg", imageBytes);

        try {
            MockHttpServletRequestBuilder request = MockMvcRequestBuilders.multipart("/modifyVenue.do")
                    .file(imageFile)
                    .param("venueID", "1")
                    .param("venueName", "conflict")
                    .param("address", "address")
                    .param("description", "description")
                    .param("price", "1")
                    .param("open_time", CORRECT_OPEN_TIME)
                    .param("close_time", CORRECT_CLOSE_TIME);

            mockMvc.perform(request)
                    .andExpect(status().is4xxClientError());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testModifyVenueWithNoParam() {
        try {
            mockMvc.perform(post("/modifyVenue.do"))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            fail();
        }
    }


    //    delVenue
    @Test
    public void testDelVenueWithSuccess() throws Exception {
        doNothing()
                .when(venueService).delById(1);
        mockMvc.perform(post("/delVenue.do")
                        .param("venueID", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    public void testDelVenueWithNotFound() throws Exception {
        doThrow(EmptyResultDataAccessException.class)
                .when(venueService).delById(1);
        try {
            mockMvc.perform(post("/delVenue.do")
                            .param("venueID", "1"))
                    .andExpect(status().isNotFound())
                    .andExpect(content().string("false"));
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testDelVenueWithFloatParam() throws Exception {
        try {
            mockMvc.perform(post("/delVenue.do")
                            .param("venueID", "1.5"))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testDelVenueWithNegParam() throws Exception {
        doThrow(new EmptyResultDataAccessException(-1))
                .when(venueService).delById(-1);
        try {
            mockMvc.perform(post("/delVenue.do")
                            .param("venueID", "-1"))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testDelVenueWithNoParam() throws Exception {
        try {
            mockMvc.perform(post("/delVenue.do"))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            fail();
        }
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

}
