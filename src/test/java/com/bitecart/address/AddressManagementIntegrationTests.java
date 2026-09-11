package com.bitecart.address;

import com.bitecart.entity.Role;
import com.bitecart.entity.User;
import com.bitecart.repository.AddressRepository;
import com.bitecart.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AddressManagementIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private AddressRepository addressRepository;
    @Autowired private UserRepository userRepository;

    private String ownerEmail;
    private String otherEmail;

    @BeforeEach
    void setUp() {
        ownerEmail = "address-owner-" + UUID.randomUUID() + "@example.com";
        otherEmail = "address-other-" + UUID.randomUUID() + "@example.com";
        userRepository.save(newUser(ownerEmail));
        userRepository.save(newUser(otherEmail));
    }

    @Test
    void addressManagementRequiresAuthenticationAndValidatesInput() throws Exception {
        mockMvc.perform(get("/addresses")).andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/addresses").with(user(ownerEmail).roles("USER")).with(csrf())
                        .param("addressLine1", "").param("city", "").param("state", "Tamil Nadu")
                        .param("postalCode", "123").param("country", "India").param("phone", "123"))
                .andExpect(status().isOk()).andExpect(view().name("address-form"));
    }

    @Test
    void customerCanAddSetDefaultAndUseCheckoutReturnPath() throws Exception {
        mockMvc.perform(post("/addresses").with(user(ownerEmail).roles("USER")).with(csrf())
                        .param("addressLine1", "12 Market Street").param("city", "Chennai")
                        .param("state", "Tamil Nadu").param("postalCode", "600001").param("country", "India")
                        .param("phone", "9876543210").param("returnToCheckout", "true"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/checkout"));

        var first = addressRepository.findByUserEmailIgnoreCaseOrderByDefaultAddressDescCreatedAtDesc(ownerEmail).get(0);
        assertThat(first.isDefaultAddress()).isTrue();

        mockMvc.perform(post("/addresses").with(user(ownerEmail).roles("USER")).with(csrf())
                        .param("addressLine1", "45 Beach Road").param("city", "Chennai")
                        .param("state", "Tamil Nadu").param("postalCode", "600028").param("country", "India")
                        .param("phone", "9876543211"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/addresses"));
        var second = addressRepository.findByUserEmailIgnoreCaseOrderByDefaultAddressDescCreatedAtDesc(ownerEmail).stream()
                .filter(address -> !address.getId().equals(first.getId())).findFirst().orElseThrow();

        mockMvc.perform(post("/addresses/{id}/default", second.getId()).with(user(ownerEmail).roles("USER")).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/addresses"));
        assertThat(addressRepository.findById(second.getId()).orElseThrow().isDefaultAddress()).isTrue();
    }

    @Test
    void customerCannotChangeAnotherUsersAddress() throws Exception {
        mockMvc.perform(post("/addresses").with(user(ownerEmail).roles("USER")).with(csrf())
                        .param("addressLine1", "12 Market Street").param("city", "Chennai")
                        .param("state", "Tamil Nadu").param("postalCode", "600001").param("country", "India")
                        .param("phone", "9876543210"))
                .andExpect(status().is3xxRedirection());
        Long addressId = addressRepository.findByUserEmailIgnoreCaseOrderByDefaultAddressDescCreatedAtDesc(ownerEmail).get(0).getId();

        mockMvc.perform(post("/addresses/{id}/delete", addressId).with(user(otherEmail).roles("USER")).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("addressError"));
        assertThat(addressRepository.findById(addressId)).isPresent();
    }

    private User newUser(String email) {
        User user = new User();
        user.setFirstName("Address");
        user.setLastName("Customer");
        user.setEmail(email);
        user.setPhone("9876543210");
        user.setPassword("encoded-password");
        user.setRole(Role.USER);
        return user;
    }
}
