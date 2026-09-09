package com.bitecart.repository;

import com.bitecart.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    Optional<Address> findByIdAndUserEmailIgnoreCase(Long id, String email);
}
