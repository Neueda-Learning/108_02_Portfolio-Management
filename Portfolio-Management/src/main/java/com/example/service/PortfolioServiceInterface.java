package com.example.service;

import com.example.dto.CreatePortfolioRequest;
import com.example.dto.PortfolioDTO;
import com.example.dto.PortfolioSummaryDTO;

import java.util.List;

public interface PortfolioServiceInterface {
    List<PortfolioDTO> getAllPortfolios();

    PortfolioDTO getPortfolioById(Long id);

    PortfolioDTO createPortfolio(CreatePortfolioRequest request);

    PortfolioDTO updatePortfolio(Long id, CreatePortfolioRequest request);

    void deletePortfolio(Long id);

    PortfolioSummaryDTO getPortfolioSummary(Long portfolioId);

    void refreshPortfolioPrices(Long portfolioId);
}
