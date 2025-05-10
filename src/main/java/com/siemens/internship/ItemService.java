package com.siemens.internship;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ItemService {

    @Autowired
    private ItemRepository itemRepository;
    private static final Logger logger = LoggerFactory.getLogger(ItemService.class);
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);

    // Used CopyOnWriteArrayList to make the list thread-safe
    private List<Item> processedItems = new CopyOnWriteArrayList<>();
    private AtomicInteger processedCount = new AtomicInteger(0);


    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    public Item save(Item item) {
        return itemRepository.save(item);
    }

    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }

    @Async
    public CompletableFuture<List<Item>> processItemsAsync() {

        // Clear the previous state before starting a new async processing cycle
        // Important to avoid mixing results from previous executions
        processedItems.clear();
        processedCount.set(0);

        // Get all item IDs from the database instead of loading entire objects
        // This reduces memory usage and speeds up initial loading
        List<Long> itemIds = itemRepository.findAllIds();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Long id : itemIds) {
            // Each item is processed in a separate thread for better performance and scalability
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    // Fetch item by ID inside the thread to avoid sharing non-thread-safe objects
                    Item item = itemRepository.findById(id).orElse(null);
                    if (item != null) {
                        item.setStatus("PROCESSED");
                        itemRepository.save(item);

                        // Safe to update shared structures due to thread-safe types (CopyOnWriteArrayList, AtomicInteger)
                        processedItems.add(item);
                        processedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Log errors instead of printing them — better for debugging and monitoring in real applications
                    logger.error("Saving failed for item ID {}: {}", id, e.getMessage());
                }
            }, executor);

            futures.add(future);
        }

        // Wait for all threads to finish to ensure consistent and complete results
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Return a fresh copy to prevent accidental external modification of internal list
        return CompletableFuture.completedFuture(new CopyOnWriteArrayList<>(processedItems));
    }

}
