package com.siemens.internship;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ItemServiceTests {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemService itemService;

    public ItemServiceTests() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testFindAll() {
        List<Item> items = List.of(new Item(1L, "item1", "desc", "NEW", "email@example.com"));
        when(itemRepository.findAll()).thenReturn(items);

        List<Item> result = itemService.findAll();
        assertEquals(1, result.size());
    }

    @Test
    void testFindById_found() {
        Item item = new Item(1L, "item1", "desc", "NEW", "email@example.com");
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        Optional<Item> result = itemService.findById(1L);
        assertTrue(result.isPresent());
    }

    @Test
    void testSave() {
        Item item = new Item(null, "item1", "desc", "NEW", "email@example.com");
        when(itemRepository.save(item)).thenReturn(new Item(1L, "item1", "desc", "NEW", "email@example.com"));

        Item result = itemService.save(item);
        assertNotNull(result.getId());
    }

    @Test
    void testProcessItemsAsync() {
        Long itemId = 1L;
        Item item = new Item(itemId, "item1", "desc", "NEW", "email@example.com");

        when(itemRepository.findAllIds()).thenReturn(List.of(itemId));
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(itemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CompletableFuture<List<Item>> future = itemService.processItemsAsync();
        List<Item> processed = future.join();

        assertEquals(1, processed.size());
        assertEquals("PROCESSED", processed.get(0).getStatus());
    }
}
