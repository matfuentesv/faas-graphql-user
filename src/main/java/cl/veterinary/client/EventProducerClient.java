package cl.veterinary.client;

import cl.veterinary.model.UserEvent;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "eventProducerClient", url = "https://event-producer-function.azurewebsites.net")
public interface EventProducerClient {

    @GetMapping("/api/UserCrudFunction")
    String eventGet(
            @RequestParam("code") String code,
            @RequestParam("operation") String operation,
            @RequestParam("id") Long id
    );


    @PostMapping(value = "/api/UserCrudFunction", consumes = "application/json")
    void eventPost(
            @RequestParam("code") String code,
            @RequestParam("operation") String operation,
            @RequestBody UserEvent dto
    );

    @PutMapping(value = "/api/UserCrudFunction", consumes = "application/json")
    void eventPut(
            @RequestParam("code") String code,
            @RequestParam("operation") String operation,
            @RequestBody UserEvent dto
    );

    @DeleteMapping(value = "/api/UserCrudFunction", consumes = "application/json")
    void eventDelete(
            @RequestParam("code") String code,
            @RequestParam("operation") String operation,
            @RequestBody UserEvent dto
    );



}
