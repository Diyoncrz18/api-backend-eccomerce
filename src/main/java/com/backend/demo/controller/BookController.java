package com.backend.demo.controller;

import java.util.List;

import com.backend.demo.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.backend.demo.dto.BookDto;
import com.backend.demo.model.Book;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/books")
public class BookController {

    private final BookService booksService;

    @GetMapping
    public List<Book> getBooks() {
        return booksService.allBooks();
    }

    @GetMapping("/{id}")
    public Book getDetailBook(@PathVariable("id") Long id){
        return booksService.getDetail(id);
    }

    @PostMapping
    public Book createBook(@RequestBody BookDto bookDto) {
        return booksService.createBook(bookDto);
    }

    @PutMapping("/{id}")
    public Book updateData(@PathVariable("id") Long id, @RequestBody BookDto bookDto){
        return booksService.updateBook(id, bookDto);
    }

    @DeleteMapping("/{id}")
    public void deleteDate(@PathVariable("id") Long id){
        booksService.delete(id);
    }
}