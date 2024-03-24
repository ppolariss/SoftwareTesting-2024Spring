package com.demo.controller;

import com.demo.entity.Message;
import com.demo.entity.News;
import com.demo.entity.Venue;
import com.demo.entity.vo.MessageVo;
import com.demo.service.MessageService;
import com.demo.service.MessageVoService;
import com.demo.service.NewsService;
import com.demo.service.VenueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class IndexController {
    @Autowired
    private NewsService newsService;
    @Autowired
    private VenueService venueService;
    @Autowired
    private MessageVoService messageVoService;
    @Autowired
    private MessageService messageService;

    @GetMapping("/index")
    public String index(Model model){
        Pageable venue_pageable= PageRequest.of(0,5, Sort.by("venueID").ascending());
        Pageable news_pageable= PageRequest.of(0,5, Sort.by("time").descending());
        Pageable message_pageable= PageRequest.of(0,5, Sort.by("time").descending());

        List<Venue> venue_list=venueService.findAll(venue_pageable).getContent();
        List<News> news_list= newsService.findAll(news_pageable).getContent();
        Page<Message> messages=messageService.findPassState(message_pageable);
        List<MessageVo> message_list=messageVoService.returnVo(messages.getContent());

        model.addAttribute("user", null);
        model.addAttribute("news_list",news_list);
        model.addAttribute("venue_list",venue_list);
        model.addAttribute("message_list",message_list);
        return "index";
    }


    @GetMapping("/admin_index")
    public String admin_index(Model model){
        return "admin/admin_index";
    }

}
