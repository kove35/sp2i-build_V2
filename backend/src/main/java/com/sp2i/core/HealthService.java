package com.sp2i.core;

import org.springframework.stereotype.Service;

@Service
public class HealthService {

    public String getStatus() {
        return "SP2I backend is running";
    }
}
