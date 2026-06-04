package com.sweetshop.orderingservices.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sweetshop.orderingservices.model.Food;

import jakarta.persistence.LockModeType;

public interface FoodRepository extends JpaRepository<Food , Long>{

    List<Food> findByAvailableGreaterThan(long amount);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM Food f WHERE f.foodID = :id")
    Optional<Food> findByIdForUpdate(@Param("id")Long id);
}
