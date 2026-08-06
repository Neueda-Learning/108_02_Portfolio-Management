package com.example.controller;

import com.example.dto.BuyAssetRequest;
import com.example.dto.PortfolioItemDTO;
import com.example.dto.SellAssetRequest;
import com.example.service.PortfolioItemServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}/portfolios/{portfolioId}/trades")
@Tag(name = "Trades", description = "APIs for buying and selling assets")
@CrossOrigin(origins = "*")
public class TradeController {

    private final PortfolioItemServiceInterface portfolioItemService;

    public TradeController(PortfolioItemServiceInterface portfolioItemService) {
        this.portfolioItemService = portfolioItemService;
    }

    @PostMapping("/buy")
    @Operation(summary = "Buy an asset", description = "Buy an asset using user id first, then portfolio id")
    public ResponseEntity<PortfolioItemDTO> buyAsset(
            @PathVariable Long userId,
            @PathVariable Long portfolioId,
            @Valid @RequestBody BuyAssetRequest request) {
        PortfolioItemDTO updated = portfolioItemService.buyAsset(userId, portfolioId, request);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/sell")
    @Operation(summary = "Sell an asset", description = "Sell an asset using user id first, then portfolio id")
    public ResponseEntity<PortfolioItemDTO> sellAsset(
            @PathVariable Long userId,
            @PathVariable Long portfolioId,
            @Valid @RequestBody SellAssetRequest request) {
        PortfolioItemDTO updated = portfolioItemService.sellAsset(userId, portfolioId, request);
        return ResponseEntity.ok(updated);
    }
}

