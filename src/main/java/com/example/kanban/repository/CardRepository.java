package com.example.kanban.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.kanban.model.Card;

public interface CardRepository extends JpaRepository<Card, Long> {

    List<Card> findByList_IdOrderByPositionAscIdAsc(Long listId);
}
