package projects.smart_grocery.pantry;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import projects.smart_grocery.pantry.dto.CreatePantryItemRequest;
import projects.smart_grocery.pantry.dto.UpdatePantryItemRequest;

import java.util.List;

@RestController
@RequestMapping("/api/pantry")
@RequiredArgsConstructor
public class PantryController {
    private final PantryService pantryService;
    @GetMapping
    public List<PantryItem> getByHousehold(@RequestParam Long householdId) {
        return pantryService.getByHousehold(householdId);
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PantryItem create(@Valid @RequestBody CreatePantryItemRequest request) {
        return pantryService.create(request);
    }
    @PatchMapping("/{id}")
    public PantryItem update(@PathVariable Long id, @RequestBody UpdatePantryItemRequest request) {
        return pantryService.update(id, request);
    }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        pantryService.delete(id);
    }
}
