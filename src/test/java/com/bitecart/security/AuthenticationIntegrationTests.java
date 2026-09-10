package com.bitecart.security;

import com.bitecart.dto.RegistrationRequest;
import com.bitecart.entity.Role;
import com.bitecart.entity.User;
import com.bitecart.exception.DuplicateEmailException;
import com.bitecart.repository.UserRepository;
import com.bitecart.repository.CartRepository;
import com.bitecart.repository.WishlistRepository;
import com.bitecart.service.RegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationIntegrationTests {

    private static final String TEST_PASSWORD = "a-safe-test-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        wishlistRepository.deleteAll();
        cartRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void userRepositoryFindsEmailWithoutCaseSensitivity() {
        User user = user("Customer@Example.com", Role.USER, true);
        userRepository.save(user);

        assertThat(userRepository.findByEmailIgnoreCase("customer@example.com"))
                .isPresent()
                .get()
                .extracting(User::getEmail)
                .isEqualTo("Customer@Example.com");
    }

    @Test
    void registrationEncodesPasswordAndAssignsEnabledUserRole() {
        User registeredUser = registrationService.register(registrationRequest("new.customer@example.com"));

        assertThat(registeredUser.getRole()).isEqualTo(Role.USER);
        assertThat(registeredUser.isEnabled()).isTrue();
        assertThat(registeredUser.getPassword()).isNotEqualTo(TEST_PASSWORD);
        assertThat(passwordEncoder.matches(TEST_PASSWORD, registeredUser.getPassword())).isTrue();
        assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    void registrationRejectsDuplicateEmail() {
        registrationService.register(registrationRequest("duplicate@example.com"));

        assertThatThrownBy(() -> registrationService.register(registrationRequest("DUPLICATE@example.com")))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void registrationDisplaysValidationErrorsForInvalidInput() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("firstName", "")
                        .param("lastName", "Customer")
                        .param("email", "not-an-email")
                        .param("phone", "")
                        .param("password", "short")
                        .param("confirmPassword", "different"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("registrationRequest", "firstName", "email", "phone", "password", "confirmPassword"));
    }

    @Test
    void registrationRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/register"))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticationPagesArePublicAndRender() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void successfulRegistrationRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("firstName", "New")
                        .param("lastName", "Customer")
                        .param("email", "new.customer@example.com")
                        .param("phone", "5550100")
                        .param("password", TEST_PASSWORD)
                        .param("confirmPassword", TEST_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        User savedUser = userRepository.findByEmailIgnoreCase("new.customer@example.com").orElseThrow();
        assertThat(passwordEncoder.matches(TEST_PASSWORD, savedUser.getPassword())).isTrue();
        assertThat(savedUser.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void invalidLoginRedirectsWithGenericError() throws Exception {
        mockMvc.perform(post("/login").with(csrf())
                        .param("email", "unknown@example.com")
                        .param("password", TEST_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void userLoginRedirectsToStorefrontUsingGrantedRole() throws Exception {
        userRepository.save(user("member@example.com", Role.USER, true));

        mockMvc.perform(post("/login").with(csrf())
                        .param("email", "member@example.com")
                        .param("password", TEST_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void adminLoginRedirectsToAdminUsingGrantedRole() throws Exception {
        userRepository.save(user("admin@example.com", Role.ADMIN, true));

        mockMvc.perform(post("/login").with(csrf())
                        .param("email", "admin@example.com")
                        .param("password", TEST_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void logoutInvalidatesAuthenticatedSessionAndReturnsToLogin() throws Exception {
        userRepository.save(user("logout@example.com", Role.USER, true));
        MvcResult login = mockMvc.perform(post("/login").with(csrf())
                        .param("email", "logout@example.com")
                        .param("password", TEST_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mockMvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    void disabledUserReceivesTheSameGenericLoginError() throws Exception {
        userRepository.save(user("disabled@example.com", Role.USER, false));

        mockMvc.perform(post("/login").with(csrf())
                        .param("email", "disabled@example.com")
                        .param("password", TEST_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void protectedRoutesRedirectAnonymousUsersToLogin() throws Exception {
        mockMvc.perform(get("/cart/current"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void userCannotAccessAdminRoutes() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void authenticatedUserCanPassUserRouteAuthorization() throws Exception {
        mockMvc.perform(get("/profile/current"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanPassAdminRouteAuthorization() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isNotFound());
    }

    private RegistrationRequest registrationRequest(String email) {
        RegistrationRequest request = new RegistrationRequest();
        request.setFirstName("New");
        request.setLastName("Customer");
        request.setEmail(email);
        request.setPhone("5550100");
        request.setPassword(TEST_PASSWORD);
        request.setConfirmPassword(TEST_PASSWORD);
        return request;
    }

    private User user(String email, Role role, boolean enabled) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("Customer");
        user.setEmail(email);
        user.setPhone("5550100");
        user.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        user.setRole(role);
        user.setEnabled(enabled);
        return user;
    }
}
