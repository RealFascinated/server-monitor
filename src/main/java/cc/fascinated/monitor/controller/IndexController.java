package cc.fascinated.monitor.controller;

import cc.fascinated.monitor.model.dto.response.IndexResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/")
public class IndexController {

    @GetMapping(value = "/")
    public IndexResponse index() {
        return new IndexResponse("Monitor API!");
    }
}
