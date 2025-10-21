package com.auction.system.repository;

import com.auction.system.model.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BidRepository extends JpaRepository<Bid, UUID> {
    
    List<Bid> findByAuctionIdOrderByTimestampDesc(UUID auctionId);
    
    List<Bid> findByBidderId(UUID bidderId);
    
    List<Bid> findByAuctionIdAndBidderId(UUID auctionId, UUID bidderId);
    
    @Query("SELECT b FROM Bid b WHERE b.auctionId = :auctionId ORDER BY b.amount DESC")
    List<Bid> findHighestBidsForAuction(@Param("auctionId") UUID auctionId);
    
    @Query("SELECT MAX(b.amount) FROM Bid b WHERE b.auctionId = :auctionId")
    Double findHighestBidAmount(@Param("auctionId") UUID auctionId);
    
    @Query("SELECT COUNT(b) FROM Bid b WHERE b.auctionId = :auctionId")
    long countBidsForAuction(@Param("auctionId") UUID auctionId);
    
    @Query("SELECT b FROM Bid b WHERE b.auctionId = :auctionId ORDER BY b.logicalTimestamp DESC")
    List<Bid> findByAuctionIdOrderByLogicalTimestampDesc(@Param("auctionId") UUID auctionId);
    
    @Query("SELECT b FROM Bid b WHERE b.serverId = :serverId ORDER BY b.timestamp DESC")
    List<Bid> findByServerId(@Param("serverId") Integer serverId);
    
    List<Bid> findByAuctionIdOrderByAmountDesc(UUID auctionId);
}