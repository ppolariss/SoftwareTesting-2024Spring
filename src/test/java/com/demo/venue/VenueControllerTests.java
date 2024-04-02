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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VenueController.class)
public class VenueControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VenueService venueService;

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

        mockMvc.perform(get("/venue?venueID=3"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testToGymPageWithNoParam() throws Exception {
        mockMvc.perform(get("/venue"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testToGymPageWithNegParam() throws Exception {
        mockMvc.perform(get("/venue?venueID=-1"))
                .andExpect(status().isBadRequest());
    }


    // /venuelist/getVenueList
    @Test
    public void testVenue_listWithSuccess() throws Exception {
        Pageable venue_pageable = PageRequest.of(0, 5, Sort.by("venueID").ascending());
        Page<Venue> page = new PageImpl<>(Collections.singletonList(
                new Venue(1, "venue_name", "description", 1, "picture", "address", "open_time", "close_time")
        ), venue_pageable, 1);

        when(venueService.findAll(venue_pageable))
                .thenReturn(page);

        mockMvc.perform(post("/venuelist/getVenueList")
                        .param("page", "1")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.content[0].venueID").value(1))
                .andExpect(jsonPath("$.content[0].venueName").value("venue_name"))
                .andExpect(jsonPath("$.content[0].description").value("description"))
                .andExpect(jsonPath("$.content[0].price").value(1))
                .andExpect(jsonPath("$.content[0].picture").value("picture"))
                .andExpect(jsonPath("$.content[0].address").value("address"))
                .andExpect(jsonPath("$.content[0].open_time").value("open_time"))
                .andExpect(jsonPath("$.content[0].close_time").value("close_time"));
    }

    @Test
    public void testVenue_listWithNoParamSuccess() throws Exception {
        Pageable venue_pageable = PageRequest.of(0, 5, Sort.by("venueID").ascending());
        Page<Venue> page = new PageImpl<>(Collections.singletonList(
                new Venue(1, "venue_name", "description", 1, "picture", "address", "open_time", "close_time")
        ), venue_pageable, 1);

        when(venueService.findAll(venue_pageable))
                .thenReturn(page);
        mockMvc.perform(post("/venuelist/getVenueList"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.content[0].venueID").value(1))
                .andExpect(jsonPath("$.content[0].venueName").value("venue_name"))
                .andExpect(jsonPath("$.content[0].description").value("description"))
                .andExpect(jsonPath("$.content[0].price").value(1))
                .andExpect(jsonPath("$.content[0].picture").value("picture"))
                .andExpect(jsonPath("$.content[0].address").value("address"))
                .andExpect(jsonPath("$.content[0].open_time").value("open_time"))
                .andExpect(jsonPath("$.content[0].close_time").value("close_time"));
    }

    @Test
    public void testVenue_listWithNegParam() throws Exception {
        mockMvc.perform(post("/venuelist/getVenueList")
                        .param("page", "-1")
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testVenue_listWithNotFound() throws Exception {
//        NOT certain
        Pageable pageable = PageRequest.of(1, 5, Sort.by("venueID").ascending());

        when(venueService.findAll(pageable)).
                thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 1));

        mockMvc.perform(post("/venuelist/getVenueList")
                        .param("page", "2")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.content").isEmpty());
    }

    //    /venue_list
    @Test
    public void testVenueListWithSuccess() throws Exception {
//TODO
        when(venueService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(
                        new Venue(1, "venue_name", "description", 1, "picture", "address", "open_time", "close_time")
                )));
        mockMvc.perform(get("/venue_list"))
                .andExpect(status().isOk())
                .andExpect(view().name("venue_list"))
                .andExpect(model().attribute("venue_list", new PageImpl<>(Collections.singletonList(
                        new Venue(1, "venue_name", "description", 1, "picture", "address", "open_time", "close_time")
                )).getContent()))
                .andExpect(model().attribute("total", 1));
    }

    @Test
    public void testVenueListWithEmptySuccess() throws Exception {
//        TODO
        when(venueService.findAll(any(Pageable.class)))
                .thenReturn(Page.empty());
        mockMvc.perform(get("/venue_list"))
                .andExpect(status().isOk())
                .andExpect(view().name("venue_list"))
                .andExpect(model().attribute("venue_list", Page.empty().getContent()))
                .andExpect(model().attribute("total", 0));
    }
}