package com.siemens.internship;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

 /**
  Problems:
  POST Method:
  Issue: The controller was returning 201 CREATED even when the input data was invalid
  which doesn’t make sense
  It should only return 201 CREATED when the item is actually saved, not when there's an error in the input

  GET by ID Method:
  Issue: When an item wasn’t found, it was returning 204 NO_CONTENT. But this is confusing
  because NO_CONTENT implies that the item exists but doesn’t have any content, which isn’t the case
  It should return 404 NOT_FOUND if the item doesn't exist

  PUT Method:
  Issue: When updating an existing item, it was returning 201 CREATED, but this status is
  only meant for creating new items. It should return 200 OK to indicate that the update was successful

  DELETE Method:
  Issue: The controller was returning 409 CONFLICT even when an item was deleted, which was wrong
  409 is for conflicts
  It should return 204 NO_CONTENT for successful deletions, and 404 NOT_FOUND if the item wasn’t found

  Process Method:
  Issue: The processItems endpoint didn’t wait for asynchronous tasks to finish before responding
  so sometimes it would send incomplete data back. This could cause problems because the client might get a
  response before the processing is actually done


  Solutions:
  POST Method:
  Now, if there are validation errors, it returns 400 BAD_REQUEST instead of 201 CREATED
  It only returns 201 CREATED when the item is actually saved.

  GET by ID Method:
  If the item doesn’t exist, it now returns 404 NOT_FOUND instead of 204 NO_CONTENT

  PUT Method:
  When updating an item, it now returns 200 OK (instead of 201 CREATED), because updates are not considered "new creations"

  DELETE Method:
  If the item is deleted, it returns 204 NO_CONTENT to indicate success
  If the item isn’t found, it now returns 404 NOT_FOUND (instead of 409 CONFLICT)

  Process Method:
  It now waits for all async tasks to finish before sending a response, ensuring that the client gets
  all the processed data at once
 */


@RestController
@RequestMapping("/api/items")
public class ItemController {

    @Autowired
    private ItemService itemService;

    @GetMapping
    public ResponseEntity<List<Item>> getAllItems() {
        return new ResponseEntity<>(itemService.findAll(), HttpStatus.OK);
    }


    @PostMapping
    public ResponseEntity<Item> createItem(@Valid @RequestBody Item item, BindingResult result) {
        // Changed: If the validation fails, we now return 400 BAD_REQUEST
        // Before it was returning 201 CREATED even when validation failed which is incorrect
        if (result.hasErrors()) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        // Changed: Only return 201 CREATED if the item is successfully created
        // This follows the correct REST practice: 201 for successfully created resources
        return new ResponseEntity<>(itemService.save(item), HttpStatus.CREATED);
    }

     @GetMapping("/{id}")
     public ResponseEntity<Item> getItemById(@PathVariable Long id)
     {
         // Changed: If the item doesn't exist we now return 404 NOT_FOUND instead of 204 NO_CONTENT
         // NO_CONTENT would be wrong because it would suggest the item exists but has no content
         return itemService.findById(id)
                 .map(item -> new ResponseEntity<>(item, HttpStatus.OK))
                 .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
     }

     @PutMapping("/{id}")
     public ResponseEntity<Item> updateItem(@PathVariable Long id, @RequestBody Item item) {
         Optional<Item> existingItem = itemService.findById(id);
         if (existingItem.isPresent()) {
             item.setId(id);
             // Changed: Return 200 OK when the item is updated successfully
             // Before 201 CREATED was used for updates but that's meant only for creating new resources
             return new ResponseEntity<>(itemService.save(item), HttpStatus.OK);
         } else {
             // Changed: Return 404 NOT_FOUND when trying to update a non-existing item
             return new ResponseEntity<>(HttpStatus.NOT_FOUND);
         }
     }

     @DeleteMapping("/{id}")
     public ResponseEntity<Void> deleteItem(@PathVariable Long id)
     {
         if (itemService.findById(id).isPresent()) {
             itemService.deleteById(id);
             // Changed: Return 204 NO_CONTENT to indicate successful deletion with no body
             // Before, 409 CONFLICT was used, which was wrong in this case. 204 is standard for successful deletions
             return new ResponseEntity<>(HttpStatus.NO_CONTENT);
         } else {
             // Changed: Return 404 NOT_FOUND if the item to delete does not exist
             // Before, 409 CONFLICT was returned which wasn't correct when trying to delete something that doesn't exist
             return new ResponseEntity<>(HttpStatus.NOT_FOUND);
         }
     }


     @GetMapping("/process")
     public ResponseEntity<List<Item>> processItems() {
         // Changed: We now wait for the async processItemsAsync() method to finish before sending a response
         // In the previous version the server didn't wait for all tasks to finish before responding
         CompletableFuture<List<Item>> processedItemsFuture = itemService.processItemsAsync();

         // Wait for the CompletableFuture to finish and get the result
         // Changed: We use .join() to block execution until the result is ready
         List<Item> processedItems = processedItemsFuture.join();  // blocks until the result is ready

         // Return the processed items once all tasks are completed
         return new ResponseEntity<>(processedItems, HttpStatus.OK);
     }

}
