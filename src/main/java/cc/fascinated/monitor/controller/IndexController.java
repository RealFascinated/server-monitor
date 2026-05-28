package cc.fascinated.monitor.controller;

import cc.fascinated.monitor.model.dto.response.IndexResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/")
@Tag(name = "Index Controller")
public class IndexController {

    @GetMapping(value = "/")
    public IndexResponse index() {
        return new IndexResponse("Monitor API!");
    }
}
