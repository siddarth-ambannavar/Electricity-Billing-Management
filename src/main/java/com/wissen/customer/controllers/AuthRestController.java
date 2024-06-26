package com.wissen.customer.controllers;

import com.wissen.customer.customExceptions.CustomerAlreadyExistsException;
import com.wissen.customer.customExceptions.InValidLoginCredentialsException;
import com.wissen.customer.customExceptions.InValidSignInCredentialsException;
import com.wissen.customer.entities.Customer;
import com.wissen.customer.implementations.CustomerServiceImplementation;
import com.wissen.customer.reqResModels.CustomerDetails;
import com.wissen.customer.reqResModels.JwtRequest;
import com.wissen.customer.reqResModels.JwtResponse;
import com.wissen.customer.reqResModels.RegisterResponse;
import com.wissen.customer.security.JwtHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Slf4j
@CrossOrigin({"http://localhost:4200"})
public class AuthRestController {

    @Autowired
    private CustomerServiceImplementation customerServiceImplementation;
    @Autowired
    private AuthenticationManager manager;
    @Autowired
    private JwtHelper helper;

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@RequestBody JwtRequest request) {
        this.doAuthenticate(request.getPhoneNumber(), request.getPassword());
        Customer userDetails = customerServiceImplementation.loadUserByPhoneNumber(request.getPhoneNumber());
        String token = this.helper.generateToken(userDetails);
        CustomerDetails loggedInCustomerDetails = CustomerDetails.builder()
                .customerId(userDetails.getCustomerId())
                .name(userDetails.getName())
                .phoneNumber(userDetails.getPhoneNumber())
                .address(userDetails.getAddress())
                .build();
        JwtResponse response = JwtResponse.builder()
                .message("Customer Logged In Successfully")
                .status(HttpStatus.OK)
                .jwtToken(token)
                .customer(loggedInCustomerDetails)
                .build();
        log.info("Customer Logged In : {}", loggedInCustomerDetails.getName());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private void doAuthenticate(String phoneNumber, String password) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(phoneNumber, password);
        try {
            manager.authenticate(authentication);
        } catch (InValidLoginCredentialsException e) {
            log.error("Invalid Credentials");
            throw new InValidLoginCredentialsException(" Invalid Username or Password  !!");
        }
    }

    @Transactional
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> createUser(@RequestBody Customer customer) {
        if(customerServiceImplementation.isCustomerPhoneNumberExists(customer.getPhoneNumber()))
            throw new CustomerAlreadyExistsException("Customer with this phone number is already registered");
        if(customer.getPhoneNumber().length() != 10)
            throw new InValidSignInCredentialsException("Please provide valid phone number");
        if(!customer.getPhoneNumber().matches("\\d+"))
            throw new InValidSignInCredentialsException("Please provide valid phone number");
        CustomerDetails customerDetails = customerServiceImplementation.addCustomer(customer);
        RegisterResponse response = RegisterResponse.builder()
                .message("Customer Registered Successfully!")
                .status(HttpStatus.CREATED)
                .customer(customerDetails)
                .build();
        log.info("Customer Registered : {}", customerDetails.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
