package com.example.kanban.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.kanban.model.BoardList;

public interface BoardListRepository extends JpaRepository<BoardList, Long> {

    List<BoardList> findByOwner_IdOrderByPositionAscIdAsc(Long ownerId);
}
