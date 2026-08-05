package com.example.controller;

import com.example.dto.CreatePortfolioItemRequest;
import com.example.dto.PortfolioItemDTO;
import com.example.model.AssetType;
import com.example.service.PortfolioItemServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/portfolios/{portfolioId}/items")
@Tag(name = "Portfolio Items", description = "APIs for managing portfolio items")
@CrossOrigin(origins = "*")
public class PortfolioItemController {
    
    private final PortfolioItemServiceInterface portfolioItemService;
    
    public PortfolioItemController(PortfolioItemServiceInterface portfolioItemService) {
        this.portfolioItemService = portfolioItemService;
    }
    
    @GetMapping
    @Operation(summary = "Get all items in portfolio", description = "Retrieve all items in a specific portfolio")
    public ResponseEntity<List<PortfolioItemDTO>> getPortfolioItems(@PathVariable Long portfolioId) {
        List<PortfolioItemDTO> items = portfolioItemService.getPortfolioItems(portfolioId);
        return ResponseEntity.ok(items);
    }
    
    @GetMapping("/by-type/{assetType}")
    @Operation(summary = "Get items by asset type", description = "Retrieve items filtered by asset type")
    public ResponseEntity<List<PortfolioItemDTO>> getItemsByAssetType(
            @PathVariable Long portfolioId,
            @PathVariable AssetType assetType) {
        List<PortfolioItemDTO> items = portfolioItemService.getItemsByAssetType(portfolioId, assetType);
        return ResponseEntity.ok(items);
    }
    
    @PostMapping
    @Operation(summary = "Add item to portfolio", description = "Add a new item to the portfolio")
    public ResponseEntity<PortfolioItemDTO> addItemToPortfolio(
            @PathVariable Long portfolioId,
            @Valid @RequestBody CreatePortfolioItemRequest request) {
        PortfolioItemDTO created = portfolioItemService.addItemToPortfolio(portfolioId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{itemId}")
    @Operation(summary = "Get item by ID", description = "Retrieve a specific portfolio item")
    public ResponseEntity<PortfolioItemDTO> getItemById(@PathVariable Long itemId) {
        PortfolioItemDTO item = portfolioItemService.getItemById(itemId);
        return ResponseEntity.ok(item);
    }
    
    @PutMapping("/{itemId}")
    @Operation(summary = "Update portfolio item", description = "Update an existing portfolio item")
    public ResponseEntity<PortfolioItemDTO> updateItem(
            @PathVariable Long itemId,
            @Valid @RequestBody CreatePortfolioItemRequest request) {
        PortfolioItemDTO updated = portfolioItemService.updateItem(itemId, request);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/{itemId}")
    @Operation(summary = "Delete portfolio item", description = "Remove an item from the portfolio")
    public ResponseEntity<Void> deleteItem(@PathVariable Long itemId) {
        portfolioItemService.deleteItem(itemId);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{itemId}/refresh-price")
    @Operation(summary = "Refresh item price", description = "Update current market price for a specific item")
    public ResponseEntity<PortfolioItemDTO> refreshItemPrice(@PathVariable Long itemId) {
        PortfolioItemDTO updated = portfolioItemService.refreshItemPrice(itemId);
        return ResponseEntity.ok(updated);
    }
}

