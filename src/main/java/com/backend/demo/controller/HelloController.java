package com.backend.demo.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.demo.model.Book;

@RestController
@RequestMapping("/api/v1")
public class HelloController {

    List<Book> books = new ArrayList<>();

    @GetMapping
    public String hello(){
        return "Hello World";
    }

    @PostMapping
    public String post(){
        return "post mapping";
    }

    @PutMapping
    public String put(){
        return "put mapping";
    }

    @DeleteMapping
    public String delete(){
        return "delete mapping";
    }
}
