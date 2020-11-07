package com.winitech.rnd.service;

import com.linecorp.armeria.server.annotation.*;

import com.winitech.rnd.converter.WiniResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

@Slf4j
public class Test2 {
    @Get("/greet3") // `:name` style is also available
    public WiniResponse greet(@Param(value = "name") String nm) {
        HashMap map = new HashMap<>();
        map.put("stnid", "108");
        map.put("tmseq", "98");

        String id = "winitech.selectT3";

        return new WiniResponse(id, map, 0, Collections.emptyList(), "SELECT");
    }

    @Delete("/greet3") // `:name` style is also available
    public WiniResponse dgreet(@Param(value = "name") String nm) {
        HashMap map = new HashMap<>();
        map.put("stnid", "108");
        map.put("tmseq", "98");

        String id = "winitech.selectT3";

        return new WiniResponse(id, map, 0, Collections.emptyList(), "SELECT");
    }

    @Post("/greet3") // `:name` style is also available
    public WiniResponse pgreet(@Param(value = "name") Optional<String> nm) {
        HashMap map = new HashMap<>();
        map.put("stnid", "108");
        map.put("tmseq", "98");

        String id = "winitech.selectT3";

        return new WiniResponse(id, map, 0, Collections.emptyList(), "SELECT");
    }

    @Put("/greet3") // `:name` style is also available
    public WiniResponse hgreet(@Param(value = "name") String nm) {
        HashMap map = new HashMap<>();
        map.put("stnid", "108");
        map.put("tmseq", "98");

        String id = "winitech.selectT3";

        return new WiniResponse(id, map, 0, Collections.emptyList(), "SELECT");
    }
}

