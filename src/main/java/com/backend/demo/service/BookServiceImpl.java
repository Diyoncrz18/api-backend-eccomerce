package com.backend.demo.service;

import com.backend.demo.dto.BookDto;
import com.backend.demo.model.Book;
import com.backend.demo.repository.BookRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService{

    private final BookRepository bookRepository;


    @Override
    public List<Book> allBooks() {
        return bookRepository.findAll();
    }

    @Override
    public Book createBook(BookDto bookDto) {
        Book book = new Book();
        book.setIsbn(bookDto.getIsbn());
        book.setAuthor(bookDto.getAuthor());
        book.setTitle(bookDto.getTitle());
        return bookRepository.save(book);
    }

    @Override
    public Book updateBook(Long id, BookDto bookDto) {
        Optional<Book> book = bookRepository.findById(id);
        if(book.isPresent()){
            book.get().setTitle(bookDto.getTitle());
            book.get().setIsbn(bookDto.getIsbn());
            book.get().setAuthor(bookDto.getAuthor());
            bookRepository.save(book.get());
        }
        return book.get();
    }

    @Override
    public Book getDetail(Long id) {
        Optional<Book> book = bookRepository.findById(id);
        return book.get();
    }

    @Override
    public void delete(Long id) {
        Optional<Book> book = bookRepository.findById(id);
        bookRepository.existsById(id);
        bookRepository.delete(book.get());
    }
}
