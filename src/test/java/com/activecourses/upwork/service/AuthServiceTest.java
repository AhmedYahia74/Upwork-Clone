package com.activecourses.upwork.service;

import com.activecourses.upwork.dto.authentication.registration.RegistrationRequestDto;
import com.activecourses.upwork.mapper.Mapper;
import com.activecourses.upwork.model.User;
import com.activecourses.upwork.model.Role;
import com.activecourses.upwork.repository.user.UserRepository;
import com.activecourses.upwork.repository.role.RoleRepository;
import com.activecourses.upwork.service.authentication.AuthServiceImpl;
import com.activecourses.upwork.service.role.RoleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Mapper<User, RegistrationRequestDto> userMapper;

    @InjectMocks
    private AuthServiceImpl userService;

    @InjectMocks
    private RoleServiceImpl roleService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void registerUser() {
        RegistrationRequestDto registrationRequestDto = new RegistrationRequestDto();
        registrationRequestDto.setFirstName("Ahmed");
        registrationRequestDto.setLastName("Muhammed");
        registrationRequestDto.setEmail("am0103738@gmail.com");
        registrationRequestDto.setPassword("password123");

        User user = new User();
        user.setFirstName("Ahmed");
        user.setLastName("Muhammed");
        user.setEmail("am0103738@gmail.com");
        user.setPassword("encodedPassword");

        when(userMapper.mapFrom(any(RegistrationRequestDto.class))).thenReturn(user);
        when(passwordEncoder.encode(any(String.class))).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userRepository.findByEmail("am0103738@gmail.com")).thenReturn(Optional.of(user));

        userService.registerUser(registrationRequestDto);

        verify(userRepository, times(1)).save(user);
        assertNotNull(userRepository.findByEmail("am0103738@gmail.com"));
        assertEquals("encodedPassword", user.getPassword());
    }

    @Test
    void assignRolesToUser() {
        int userId = 1;
        Map<String, Object> roles = new HashMap<>();
        roles.put("ROLE_ADMIN", null);
        roles.put("ROLE_USER", null);

        User user = new User();
        user.setId(userId);

        Role adminRole = new Role();
        adminRole.setName("ROLE_ADMIN");

        Role userRole = new Role();
        userRole.setName("ROLE_USER");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));

        boolean success = roleService.assignRolesToUser(userId, roles);

        assertTrue(success);
        assertEquals(2, user.getRoles().size());
        assertTrue(user.getRoles().contains(adminRole));
        assertTrue(user.getRoles().contains(userRole));
    }

    @Test
    void deactivateUser() {
        int userId = 1;

        User user = new User();
        user.setId(userId);
        user.setAccountEnabled(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        boolean success = userService.deactivateUser(userId);

        assertTrue(success);
        assertFalse(user.isAccountEnabled());
    }

    @Test
    void reactivateUser() {
        int userId = 1;

        User user = new User();
        user.setId(userId);
        user.setAccountEnabled(false);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        boolean success = userService.reactivateUser(userId);

        assertTrue(success);
        assertTrue(user.isAccountEnabled());
    }

    @Test
    void addRole() {
        Role role = new Role();
        role.setName("ROLE_TEST");

        when(roleRepository.save(any(Role.class))).thenReturn(role);

        Role createdRole = roleService.addRole(role);

        assertNotNull(createdRole);
        assertEquals("ROLE_TEST", createdRole.getName());
    }

    @Test
    void removeRole() {
        int roleId = 1;

        Role role = new Role();
        role.setId(roleId);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        boolean success = roleService.removeRole(roleId);

        assertTrue(success);
        verify(roleRepository, times(1)).delete(role);
    }

    @Test
    void updateRole() {
        int roleId = 1;

        Role role = new Role();
        role.setId(roleId);
        role.setName("ROLE_OLD");

        Role updatedRole = new Role();
        updatedRole.setName("ROLE_NEW");

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(roleRepository.save(any(Role.class))).thenReturn(updatedRole);

        Role result = roleService.updateRole(roleId, updatedRole);

        assertNotNull(result);
        assertEquals("ROLE_NEW", result.getName());
    }

    @Test
    void getAllRoles() {
        Role role1 = new Role();
        role1.setName("ROLE_1");

        Role role2 = new Role();
        role2.setName("ROLE_2");

        List<Role> roles = List.of(role1, role2);

        when(roleRepository.findAll()).thenReturn(roles);

        List<Role> result = roleService.getAllRoles();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(role1));
        assertTrue(result.contains(role2));
    }
}
