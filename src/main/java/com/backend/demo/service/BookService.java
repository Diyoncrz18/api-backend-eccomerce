package com.backend.demo.service;

import com.backend.demo.dto.BookDto;
import com.backend.demo.model.Book;

import java.util.List;

public interface BookService {
    List<Book> allBooks();
    Book createBook(BookDto bookDto);
    Book updateBook(Long id, BookDto bookDto);
    Book getDetail(Long id);
    void delete(Long id);

}
