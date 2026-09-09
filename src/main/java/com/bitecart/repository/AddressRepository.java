package com.bitecart.repository;

import com.bitecart.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Long> {

    Optional<Address> findByIdAndUserEmailIgnoreCase(Long id, String email);

    List<Address> findByUserEmailIgnoreCaseOrderByDefaultAddressDescCreatedAtDesc(String email);
}
