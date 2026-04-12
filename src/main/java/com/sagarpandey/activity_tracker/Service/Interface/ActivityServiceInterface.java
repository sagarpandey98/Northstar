package com.sagarpandey.activity_tracker.Service.Interface;

import com.sagarpandey.activity_tracker.dtos.ActivityResponse;
import com.sagarpandey.activity_tracker.models.Activity;

import java.util.HashMap;
import java.util.List;

public interface ActivityServiceInterface {
    Activity create(HashMap<String, String> ActivityInfo);
    Activity read(Long id);
    List<ActivityResponse> readAll();
    void update(HashMap<String, String> ActivityInfo);
    void delete(Long id);
}