package com.backend.demo.repository;

import com.backend.demo.model.Book;
import org.hibernate.query.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import java.awt.print.Pageable;

public interface BookRepository extends JpaRepository<Book, Long> {

//    Page findAll(Pageable pageable);
}
