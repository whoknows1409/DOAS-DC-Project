package com.auction.system.repository;

import com.auction.system.model.Auction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, UUID> {
    
    List<Auction> findByStatusAndEndTimeAfterOrderByEndTimeAsc(String status, LocalDateTime endTime);
    
    List<Auction> findBySellerId(UUID sellerId);
    
    List<Auction> findByStatus(String status);
    
    @Query("SELECT a FROM Auction a WHERE a.status = 'ACTIVE' AND a.endTime > :now ORDER BY a.endTime ASC")
    List<Auction> findActiveAuctions(@Param("now") LocalDateTime now);
    
    @Query("SELECT a FROM Auction a WHERE a.endTime < :now AND a.status = 'ACTIVE'")
    List<Auction> findExpiredAuctions(@Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(a) FROM Auction a WHERE a.status = 'ACTIVE'")
    long countActiveAuctions();
    
    @Query("SELECT COUNT(a) FROM Auction a WHERE a.sellerId = :sellerId")
    long countBySellerId(@Param("sellerId") UUID sellerId);
    
    List<Auction> findByWinnerId(UUID winnerId);
}