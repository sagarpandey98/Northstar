package com.sagarpandey.activity_tracker.Service.Interface;

import com.sagarpandey.activity_tracker.models.Domain;
import java.util.List;

public interface DomainServiceInterface {
    Domain create(Domain domain);
    Domain read(Long id);
    List<Domain> readAll();
    boolean update(Domain domain);
    boolean delete(Long id);
}