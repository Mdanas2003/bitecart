package com.bitecart.service;

import com.bitecart.dto.AddressDTO;
import com.bitecart.dto.AddressForm;

import java.util.List;

public interface AddressService {

    List<AddressDTO> getCurrentUsersAddresses();

    AddressDTO getCurrentUsersAddress(Long addressId);

    AddressDTO createAddress(AddressForm form);

    AddressDTO updateAddress(Long addressId, AddressForm form);

    void deleteAddress(Long addressId);

    void setDefaultAddress(Long addressId);
}
