package com.backend.demo.service;

import com.backend.demo.model.Author;

import java.util.List;

public interface AuthorService {
    Author create(Author author);
    List<Author> getListAuthor();
}
