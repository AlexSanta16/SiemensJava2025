package com.siemens.internship;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ItemRepositoryTests {

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void testSaveAndFind() {
        Item item = new Item(null, "item1", "desc", "NEW", "email@example.com");
        Item saved = itemRepository.save(item);

        assertNotNull(saved.getId());

        Optional<Item> fetched = itemRepository.findById(saved.getId());
        assertTrue(fetched.isPresent());
    }

    @Test
    void testFindAllIds() {
        Item item1 = new Item(null, "item1", "desc", "NEW", "a@a.com");
        Item item2 = new Item(null, "item2", "desc", "NEW", "b@b.com");

        itemRepository.saveAll(List.of(item1, item2));

        List<Long> ids = itemRepository.findAllIds();
        assertEquals(2, ids.size());
    }
}
