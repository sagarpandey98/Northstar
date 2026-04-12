package com.sagarpandey.activity_tracker.Service.Interface;

import com.sagarpandey.activity_tracker.models.Specifics;
import java.util.List;

public interface SpecificsServicesInterface {
    void create(Specifics specific);
    Specifics read(Long id);
    List<Specifics> readAll();
    void update(Specifics specific);
    void delete(Long id);
}