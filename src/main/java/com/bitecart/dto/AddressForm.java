package com.bitecart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AddressForm {

    @NotBlank(message = "Enter your address.")
    @Size(max = 255)
    private String addressLine1;

    @Size(max = 255)
    private String addressLine2;

    @NotBlank(message = "Enter your city.")
    @Size(max = 100)
    private String city;

    @NotBlank(message = "Enter your state.")
    @Size(max = 100)
    private String state;

    @NotBlank(message = "Enter a valid six-digit pincode.")
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Enter a valid six-digit pincode.")
    private String postalCode;

    @NotBlank(message = "Enter your country.")
    @Size(max = 100)
    private String country;

    @NotBlank(message = "Enter a valid phone number.")
    @Pattern(regexp = "^(?:\\+91[- ]?)?[6-9][0-9]{9}$", message = "Enter a valid Indian mobile number.")
    private String phone;

    private boolean defaultAddress;

    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public boolean isDefaultAddress() { return defaultAddress; }
    public void setDefaultAddress(boolean defaultAddress) { this.defaultAddress = defaultAddress; }
}
