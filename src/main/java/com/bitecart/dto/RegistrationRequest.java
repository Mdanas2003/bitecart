package com.bitecart.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegistrationRequest {

    @NotBlank(message = "First name is required.")
    @Size(max = 100, message = "First name must not exceed 100 characters.")
    private String firstName;

    @NotBlank(message = "Last name is required.")
    @Size(max = 100, message = "Last name must not exceed 100 characters.")
    private String lastName;

    @NotBlank(message = "Email is required.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 255, message = "Email must not exceed 255 characters.")
    private String email;

    @NotBlank(message = "Password is required.")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters.")
    private String password;

    @NotBlank(message = "Please confirm your password.")
    private String confirmPassword;

    @NotBlank(message = "Phone number is required.")
    @Size(max = 30, message = "Phone number must not exceed 30 characters.")
    private String phone;

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
