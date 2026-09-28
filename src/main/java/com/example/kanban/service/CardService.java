package com.example.kanban.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.kanban.dto.CardResponse;
import com.example.kanban.dto.CreateCardRequest;
import com.example.kanban.dto.UpdateCardRequest;
import com.example.kanban.exception.CardNotFoundException;
import com.example.kanban.exception.ForbiddenException;
import com.example.kanban.exception.ListNotFoundException;
import com.example.kanban.model.BoardList;
import com.example.kanban.model.Card;
import com.example.kanban.model.User;
import com.example.kanban.repository.BoardListRepository;
import com.example.kanban.repository.CardRepository;

@Service
public class CardService {

    private final CardRepository cardRepository;
    private final BoardListRepository listRepository;
    private final ListService listService;

    public CardService(
            CardRepository cardRepository,
            BoardListRepository listRepository,
            ListService listService) {
        this.cardRepository = cardRepository;
        this.listRepository = listRepository;
        this.listService = listService;
    }

    @Transactional(readOnly = true)
    public List<CardResponse> findByList(Long listId, User currentUser) {
        BoardList list = listService.requireOwned(listId, currentUser);
        return cardRepository.findByList_IdOrderByPositionAscIdAsc(list.getId()).stream()
                .map(CardResponse::from)
                .toList();
    }

    @Transactional
    public CardResponse create(Long listId, CreateCardRequest request, User currentUser) {
        BoardList list = listService.requireOwned(listId, currentUser);
        Card card = new Card();
        card.setTitle(request.title().trim());
        card.setDescription(request.description());
        card.setPosition(request.position() == null ? 0 : request.position());
        card.setList(list);
        return CardResponse.from(cardRepository.save(card));
    }

    @Transactional(readOnly = true)
    public CardResponse findById(Long id, User currentUser) {
        return CardResponse.from(requireOwned(id, currentUser));
    }

    @Transactional
    public CardResponse update(Long id, UpdateCardRequest request, User currentUser) {
        Card card = requireOwned(id, currentUser);
        if (request.title() != null) {
            card.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            card.setDescription(request.description());
        }
        if (request.position() != null) {
            card.setPosition(request.position());
        }
        if (request.listId() != null) {
            card.setList(requireOwnedTargetList(Long.valueOf(request.listId()), currentUser));
        }
        return CardResponse.from(cardRepository.save(card));
    }

    @Transactional
    public void delete(Long id, User currentUser) {
        cardRepository.delete(requireOwned(id, currentUser));
    }

    private Card requireOwned(Long id, User currentUser) {
        Card card = cardRepository.findById(id).orElseThrow(CardNotFoundException::new);
        if (!card.getList().getOwner().getId().equals(currentUser.getId())) {
            throw new ForbiddenException();
        }
        return card;
    }

    private BoardList requireOwnedTargetList(Long listId, User currentUser) {
        BoardList list = listRepository.findById(listId).orElseThrow(ListNotFoundException::new);
        if (!list.getOwner().getId().equals(currentUser.getId())) {
            throw new ForbiddenException();
        }
        return list;
    }
}
