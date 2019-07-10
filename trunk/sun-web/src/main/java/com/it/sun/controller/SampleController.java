package com.it.sun.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value="/Sample")
public class SampleController {

    @RequestMapping(value="/login", method= RequestMethod.GET)
    public void login()  throws Exception{

    }


}
