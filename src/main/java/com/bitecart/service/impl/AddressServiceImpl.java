package com.bitecart.service.impl;

import com.bitecart.dto.AddressDTO;
import com.bitecart.dto.AddressForm;
import com.bitecart.entity.Address;
import com.bitecart.entity.User;
import com.bitecart.exception.ResourceNotFoundException;
import com.bitecart.repository.AddressRepository;
import com.bitecart.repository.OrderRepository;
import com.bitecart.repository.UserRepository;
import com.bitecart.service.AddressService;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public AddressServiceImpl(AddressRepository addressRepository, UserRepository userRepository,
                              OrderRepository orderRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public List<AddressDTO> getCurrentUsersAddresses() {
        return addressRepository.findByUserEmailIgnoreCaseOrderByDefaultAddressDescCreatedAtDesc(currentEmail()).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public AddressDTO getCurrentUsersAddress(Long addressId) {
        return toDto(ownedAddress(addressId));
    }

    @Override
    @Transactional
    public AddressDTO createAddress(AddressForm form) {
        List<Address> addresses = addressesForCurrentUser();
        User user = userRepository.findByEmailIgnoreCase(currentEmail())
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Authenticated user was not found."));
        Address address = new Address();
        address.setUser(user);
        copy(form, address);
        if (form.isDefaultAddress() || addresses.isEmpty()) {
            clearDefault(addresses);
            address.setDefaultAddress(true);
        }
        return toDto(addressRepository.save(address));
    }

    @Override
    @Transactional
    public AddressDTO updateAddress(Long addressId, AddressForm form) {
        Address address = ownedAddress(addressId);
        copy(form, address);
        if (form.isDefaultAddress()) {
            clearDefault(addressesForCurrentUser());
            address.setDefaultAddress(true);
        }
        return toDto(address);
    }

    @Override
    @Transactional
    public void deleteAddress(Long addressId) {
        Address address = ownedAddress(addressId);
        if (orderRepository.existsByAddressId(addressId)) {
            throw new IllegalStateException("This address is part of an existing order and cannot be deleted.");
        }
        boolean wasDefault = address.isDefaultAddress();
        addressRepository.delete(address);
        if (wasDefault) {
            addressesForCurrentUser().stream().findFirst().ifPresent(nextAddress -> nextAddress.setDefaultAddress(true));
        }
    }

    @Override
    @Transactional
    public void setDefaultAddress(Long addressId) {
        Address address = ownedAddress(addressId);
        clearDefault(addressesForCurrentUser());
        address.setDefaultAddress(true);
    }

    private Address ownedAddress(Long addressId) {
        return addressRepository.findByIdAndUserEmailIgnoreCase(addressId, currentEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));
    }

    private List<Address> addressesForCurrentUser() {
        return addressRepository.findByUserEmailIgnoreCaseOrderByDefaultAddressDescCreatedAtDesc(currentEmail());
    }

    private void clearDefault(List<Address> addresses) {
        addresses.forEach(address -> address.setDefaultAddress(false));
    }

    private void copy(AddressForm form, Address address) {
        address.setAddressLine1(form.getAddressLine1().trim());
        address.setAddressLine2(blankToNull(form.getAddressLine2()));
        address.setCity(form.getCity().trim());
        address.setState(form.getState().trim());
        address.setPostalCode(form.getPostalCode().trim());
        address.setCountry(form.getCountry().trim());
        address.setPhone(form.getPhone().trim());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String currentEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new AuthenticationCredentialsNotFoundException("Authentication is required to access addresses.");
        }
        return authentication.getName();
    }

    private AddressDTO toDto(Address address) {
        return new AddressDTO(address.getId(), address.getAddressLine1(), address.getAddressLine2(), address.getCity(),
                address.getState(), address.getPostalCode(), address.getCountry(), address.getPhone(), address.isDefaultAddress());
    }
}
