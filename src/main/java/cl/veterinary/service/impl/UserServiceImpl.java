package cl.veterinary.service.impl;

import cl.veterinary.client.EventProducerClient;
import cl.veterinary.model.User;
import cl.veterinary.model.UserEvent;
import cl.veterinary.repository.UserRepository;
import cl.veterinary.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {


    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventProducerClient eventProducerClient;

    private static final String FUNCTION_CODE = "cceJI-uILrnyuNrtxxFStFY_50DXXxx2DvDOgTeI5X00AzFua6HdAA==";


    @Override
    public List<User> findAll() {
        List<User> usuarios = userRepository.findAll();
        eventProducerClient.eventGet(FUNCTION_CODE, "GET", 1L);
        return usuarios;
    }

    @Override
    public Optional<User> findUserById(Long id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            eventProducerClient.eventGet(FUNCTION_CODE, "GET", user.get().getId());
        } else {
            log.info("Usuario no encontrado para ID: {}", id);
        }
        return user;
    }

    @Override
    public User saveUser(User user) {
        User savedUser = userRepository.save(user);
        UserEvent evento = new UserEvent(user.getId(), user.getNombre(), user.getApellidoPaterno(),
                user.getApellidoMaterno(), user.getRut());
        eventProducerClient.eventPost(FUNCTION_CODE, "CREATE", evento);
        return savedUser;
    }

    @Override
    public User updateUser(User user) {
        User updatedUser = userRepository.save(user);
        UserEvent evento = new UserEvent(user.getId(), user.getNombre(), user.getApellidoPaterno(),
                user.getApellidoMaterno(), user.getRut());
        eventProducerClient.eventPut(FUNCTION_CODE, "UPDATE", evento);
        return updatedUser;
    }

    @Override
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
        UserEvent evento = new UserEvent(id);
        eventProducerClient.eventDelete(FUNCTION_CODE, "DELETE", evento);
    }


}
