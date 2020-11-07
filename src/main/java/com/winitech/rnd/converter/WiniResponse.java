package com.winitech.rnd.converter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Getter
public class WiniResponse {
    private final String id;
    private final Map params;
    private final int fetchSize;
    private final List keys;
    private final String sqlCmd;
}
