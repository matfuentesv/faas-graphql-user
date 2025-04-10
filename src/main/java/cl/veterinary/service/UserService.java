package cl.veterinary.service;

import cl.veterinary.model.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    List<User> findAll();
    Optional<User> findUserById(Long id);



}
