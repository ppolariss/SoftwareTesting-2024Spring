package com.demo.venue;


import com.demo.controller.user.VenueController;
import com.demo.entity.Venue;
import com.demo.service.VenueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.test.web.servlet.MockMvc;


import javax.persistence.EntityNotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VenueController.class)
public class VenueControllerTests {
    final String CORRECT_OPEN_TIME = "2006-01-02 15:04:05";
    final String CORRECT_CLOSE_TIME = "2024-04-02 23:20:05";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VenueService venueService;

    //    toGymPage
    @Test
    public void testToGymPageWithSuccess() throws Exception {
        Venue venue = new Venue(2, "venue_name", "description", 1, "picture", "address", "open_time", "close_time");
        when(venueService.findByVenueID(2))
                .thenReturn(venue);

        mockMvc.perform(get("/venue?venueID=2"))
                .andExpect(status().isOk())
                .andExpect(view().name("venue"))
                .andExpect(model().attribute("venue", venue));
    }

    @Test
    public void testToGymPageWithNotFound() throws Exception {
        when(venueService.findByVenueID(3))
                .thenThrow(EntityNotFoundException.class);
        try {
            mockMvc.perform(get("/venue?venueID=3"))
                    .andExpect(status().isNotFound());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testToGymPageWithNoParam() throws Exception {
        try {
            mockMvc.perform(get("/venue"))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testToGymPageWithNegParam() throws Exception {
//        org.springframework.web.util.NestedServletException: Request processing failed; nested exception is org.thymeleaf.exceptions.TemplateInputException: An error happened during template parsing (template: "class path resource [templates/venue.html]")
        try {
            mockMvc.perform(get("/venue?venueID=-1"))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testToGymPageWithFloatParam() throws Exception {
        mockMvc.perform(get("/venue?venueID=1.5"))
                .andExpect(status().isBadRequest());
    }


    // venue_list /venuelist/getVenueList
    @Test
    public void testVenue_listWithSuccess() throws Exception {
        Pageable venue_pageable = PageRequest.of(0, 5, Sort.by("venueID").ascending());
        Page<Venue> page = new PageImpl<>(Collections.singletonList(
                new Venue(1, "venue_name", "description", 1, "picture", "address", CORRECT_OPEN_TIME, CORRECT_CLOSE_TIME)
        ), venue_pageable, 1);

        when(venueService.findAll(venue_pageable))
                .thenReturn(page);

        mockMvc.perform(get("/venuelist/getVenueList?page=1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.content[0].venueID").value(1))
                .andExpect(jsonPath("$.content[0].venueName").value("venue_name"))
                .andExpect(jsonPath("$.content[0].description").value("description"))
                .andExpect(jsonPath("$.content[0].price").value(1))
                .andExpect(jsonPath("$.content[0].picture").value("picture"))
                .andExpect(jsonPath("$.content[0].address").value("address"))
                .andExpect(jsonPath("$.content[0].open_time").value(CORRECT_OPEN_TIME))
                .andExpect(jsonPath("$.content[0].close_time").value(CORRECT_CLOSE_TIME));
    }

    @Test
    public void testVenue_listWithNoParamSuccess() throws Exception {
        Pageable venue_pageable = PageRequest.of(0, 5, Sort.by("venueID").ascending());
        Page<Venue> page = new PageImpl<>(Collections.singletonList(
                new Venue(1, "venue_name", "description", 1, "picture", "address", CORRECT_OPEN_TIME, CORRECT_CLOSE_TIME)
        ), venue_pageable, 1);

        when(venueService.findAll(venue_pageable))
                .thenReturn(page);
        mockMvc.perform(get("/venuelist/getVenueList"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.content[0].venueID").value(1))
                .andExpect(jsonPath("$.content[0].venueName").value("venue_name"))
                .andExpect(jsonPath("$.content[0].description").value("description"))
                .andExpect(jsonPath("$.content[0].price").value(1))
                .andExpect(jsonPath("$.content[0].picture").value("picture"))
                .andExpect(jsonPath("$.content[0].address").value("address"))
                .andExpect(jsonPath("$.content[0].open_time").value(CORRECT_OPEN_TIME))
                .andExpect(jsonPath("$.content[0].close_time").value(CORRECT_CLOSE_TIME));
    }

    @Test
    public void testVenue_listWithNegParam() throws Exception {
        try {
            mockMvc.perform(get("/venuelist/getVenueList?page=-1"))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
//            NestedServletException
            fail();
        }
    }

    @Test
    public void testVenue_listWithNotFound() throws Exception {
//        NOT certain
        Pageable pageable = PageRequest.of(1, 5, Sort.by("venueID").ascending());

        when(venueService.findAll(pageable)).
                thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 1));

        mockMvc.perform(get("/venuelist/getVenueList?page=2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    public void testVenue_listWithFloatParam() throws Exception {
        mockMvc.perform(get("/venuelist/getVenueList?page=1.5"))
                .andExpect(status().isBadRequest());
    }

    //  venue_list  /venue_list
    @Test
    public void testVenueListWithSuccess() throws Exception {
        Pageable pageable = PageRequest.of(0, 5, Sort.by("venueID").ascending());
        Venue venue = new Venue(1, "venue_name", "description", 1, "picture", "address", CORRECT_OPEN_TIME, CORRECT_CLOSE_TIME);
        when(venueService.findAll(pageable))
                .thenReturn(new PageImpl<>(Collections.singletonList(venue), pageable, 1));
        mockMvc.perform(get("/venue_list"))
                .andExpect(status().isOk())
                .andExpect(view().name("venue_list"))
                .andExpect(model().attribute("venue_list",
                        new PageImpl<>(Collections.singletonList(venue)).getContent()))
                .andExpect(model().attribute("total", 1));
    }

    @Test
    public void testVenueListWithEmptySuccess() throws Exception {
        Pageable pageable = PageRequest.of(0, 5, Sort.by("venueID").ascending());
        when(venueService.findAll(pageable))
                .thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));

        mockMvc.perform(get("/venue_list"))
                .andExpect(status().isOk())
                .andExpect(view().name("venue_list"))
                .andExpect(model().attribute("venue_list", Collections.emptyList()))
                .andExpect(model().attribute("total", 0));
    }

    @Test
    public void testVenueListWithOverflowVenues() throws Exception {
        List<Venue> venues = IntStream.range(0, 15)
                .mapToObj(i -> new Venue(i + 1, "venue_name", "description", 1, "", "address", CORRECT_OPEN_TIME, CORRECT_CLOSE_TIME))
                .collect(Collectors.toList());
        Pageable pageable = PageRequest.of(0, 5, Sort.by("venueID").ascending());
        Page<Venue> page = new PageImpl<>(venues.subList(0, 5), pageable, 15);
        when(venueService.findAll(pageable))
                .thenReturn(page);
        try {
            mockMvc.perform(get("/venue_list"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("venue_list"))
                    .andExpect(model().attribute("total", 3))
                    .andExpect(model().attribute("venue_list", venues.subList(0, 5)));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }

//                    .andExpect(content().json("[{\"venueID\":1,\"venueName\":\"venue_name\",\"description\":\"description\",\"price\":1,\"picture\":\"\",\"address\":\"address\",\"open_time\":\"2006-01-02 15:04:05\",\"close_time\":\"2024-04-02 23:20:05\"}," +
//                            "{\"venueID\":2,\"venueName\":\"venue_name\",\"description\":\"description\",\"price\":1,\"picture\":\"\",\"address\":\"address\",\"open_time\":\"2006-01-02 15:04:05\",\"close_time\":\"2024-04-02 23:20:05\"}," +
//                            "{\"venueID\":3,\"venueName\":\"venue_name\",\"description\":\"description\",\"price\":1,\"picture\":\"\",\"address\":\"address\",\"open_time\":\"2006-01-02 15:04:05\",\"close_time\":\"2024-04-02 23:20:05\"}," +
//                            "{\"venueID\":4,\"venueName\":\"venue_name\",\"description\":\"description\",\"price\":1,\"picture\":\"\",\"address\":\"address\",\"open_time\":\"2006-01-02 15:04:05\",\"close_time\":\"2024-04-02 23:20:05\"}," +
//                            "{\"venueID\":5,\"venueName\":\"venue_name\",\"description\":\"description\",\"price\":1,\"picture\":\"\",\"address\":\"address\",\"open_time\":\"2006-01-02 15:04:05\",\"close_time\":\"2024-04-02 23:20:05\"}," +
//                            "{\"venueID\":6,\"venueName\":\"venue_name\",\"description\":\"description\",\"price\":1,\"picture\":\"\",\"address\":\"address\",\"open_time\":\"2006-01-02 15:04:05\",\"close_time\":\"2024-04-02 23:20:05\"}," +
//                            "{\"venueID\":7,\"venueName\":\"venue_name\",\"description\":\"description\",\"price\":1,\"picture\":\"\",\"address\":\"address\",\"open_time\":\"2006-01-02 15:04:05\",\"close_time\":\"2024-04-02 23:20:05\"}," +
//                            "{\"venueID\":8,\"venueName\":\"venue_name\",\"description\":\"description\",\"price\":1,\"picture\":\"\",\"address\":\"address\",\"open_time\":\"2006-01-02 15:04:05\",\"close_time\":\"2024-04-02 23:20:05\"}," +
//                            "{\"venueID\":9,\"venueName\":\"venue_name\",\"description\":\"description\",\"price\":1,\"picture\":\"\",\"address\":\"address\",\"open_time\":\"2006-01-02 15:04:05\",\"close_time\":\"2024-04-02 23:20:05\"}," +
//                            "{\"venueID\":10,\"venueName\":\"venue_name\",\"description\":\"description\",\"price\":1,\"picture\":\"\",\"address\":\"address\",\"open_time\":\"2006-01-02 15:04:05\",\"close_time\":\"2024-04-02 23:20:05\"}]"));
    }
}