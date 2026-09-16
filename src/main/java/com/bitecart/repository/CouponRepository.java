package com.bitecart.repository;

import com.bitecart.entity.Coupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCodeIgnoreCase(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Coupon> findWithLockByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<Coupon> findByActiveTrueAndExpiryDateAfterOrderByExpiryDateAsc(LocalDateTime expiryDate);
}
