package com.example.kanban.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.kanban.dto.CreateListRequest;
import com.example.kanban.dto.ListResponse;
import com.example.kanban.dto.UpdateListRequest;
import com.example.kanban.exception.ForbiddenException;
import com.example.kanban.exception.ListNotFoundException;
import com.example.kanban.model.BoardList;
import com.example.kanban.model.User;
import com.example.kanban.repository.BoardListRepository;
import com.example.kanban.repository.UserRepository;

@Service
public class ListService {

    private final BoardListRepository listRepository;
    private final UserRepository userRepository;

    public ListService(BoardListRepository listRepository, UserRepository userRepository) {
        this.listRepository = listRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ListResponse> findAll(User currentUser) {
        return listRepository.findByOwner_IdOrderByPositionAscIdAsc(currentUser.getId()).stream()
                .map(ListResponse::from)
                .toList();
    }

    @Transactional
    public ListResponse create(CreateListRequest request, User currentUser) {
        BoardList list = new BoardList();
        list.setTitle(request.title().trim());
        list.setPosition(request.position() == null ? 0 : request.position());
        list.setOwner(userRepository.getReferenceById(currentUser.getId()));
        return ListResponse.from(listRepository.save(list));
    }

    @Transactional
    public ListResponse update(Long id, UpdateListRequest request, User currentUser) {
        BoardList list = requireOwned(id, currentUser);
        if (request.title() != null) {
            list.setTitle(request.title().trim());
        }
        if (request.position() != null) {
            list.setPosition(request.position());
        }
        return ListResponse.from(listRepository.save(list));
    }

    @Transactional
    public void delete(Long id, User currentUser) {
        BoardList list = requireOwned(id, currentUser);
        list.getCards().clear();
        listRepository.delete(list);
    }

    @Transactional(readOnly = true)
    public BoardList requireOwned(Long id, User currentUser) {
        BoardList list = listRepository.findById(id).orElseThrow(ListNotFoundException::new);
        if (!list.getOwner().getId().equals(currentUser.getId())) {
            throw new ForbiddenException();
        }
        return list;
    }
}
