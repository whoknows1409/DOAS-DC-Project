package com.auction.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class DistributedAuctionSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistributedAuctionSystemApplication.class, args);
    }
}