package com.demo.venue;


import com.demo.controller.user.VenueController;
import com.demo.entity.Venue;
import com.demo.service.VenueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;


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
    private VenueService venueService; // Mocked service

    @Test
    public void toGymPageTest() throws Exception {
        // Mock data and behavior of venueService
        when(venueService.findByVenueID(2))
                .thenReturn(new Venue(2, "venue_name", "description", 1, "picture", "address", "open_time", "close_time"));
//        when(venueService.findByVenueID(5555))
//                .thenReturn(null);
//
//        mockMvc.perform(get("/venue?venueID=5555"))
//                .andExpect(status().isNotFound());

        mockMvc.perform(get("/venue?venueID=2"))
                .andExpect(status().isOk()) // Expect HTTP 200 status
                .andExpect(view().name("venue")) // Expect the returned view name
                .andExpect(model().attributeExists("venue"));

        when(venueService.findByVenueID(3))
                .thenReturn(null);

        mockMvc.perform(get("/venue?venueID=3"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/venue"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/venue?venueID=-1"))
                .andExpect(status().isBadRequest());

    }

    @Test
    public void venue_listTest() throws Exception {

        mockMvc.perform(post("/venuelist/getVenueList")
                        .param("page", "1")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.content").isArray());

    }

    @Test
    public void venueListTest() throws Exception {
        when(venueService.findAll(any(Pageable.class)))
                .thenReturn(Page.empty());
        mockMvc.perform(get("/venue_list"))
                .andExpect(status().isOk())
                .andExpect(view().name("venue_list"))
                .andExpect(model().attributeExists("venue_list"))
                .andExpect(model().attributeExists("total"));
    }
}