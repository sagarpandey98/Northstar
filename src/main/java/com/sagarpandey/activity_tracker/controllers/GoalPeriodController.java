package com.sagarpandey.activity_tracker.controllers;

import com.sagarpandey.activity_tracker.Exceptions.ValidationException;
import com.sagarpandey.activity_tracker.Mapper.GoalPeriodMapper;
import com.sagarpandey.activity_tracker.Repository.GoalRepository;
import com.sagarpandey.activity_tracker.Service.Interface.GoalPeriodService;
import com.sagarpandey.activity_tracker.dtos.GoalPeriodBulkCreateRequest;
import com.sagarpandey.activity_tracker.dtos.GoalPeriodCreateRequest;
import com.sagarpandey.activity_tracker.dtos.GoalPeriodResponse;
import com.sagarpandey.activity_tracker.dtos.GoalPeriodUpdateRequest;
import com.sagarpandey.activity_tracker.dtos.ResponseWrapper;
import com.sagarpandey.activity_tracker.models.Goal;
import com.sagarpandey.activity_tracker.models.GoalPeriod;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/goals/{goalUuid}/periods")
@CrossOrigin(origins = "*")
public class GoalPeriodController {

    private final GoalPeriodService goalPeriodService;
    private final GoalRepository goalRepository;
    private final GoalPeriodMapper goalPeriodMapper;

    public GoalPeriodController(
            GoalPeriodService goalPeriodService,
            GoalRepository goalRepository,
            GoalPeriodMapper goalPeriodMapper) {
        this.goalPeriodService = goalPeriodService;
        this.goalRepository = goalRepository;
        this.goalPeriodMapper = goalPeriodMapper;
    }

    private String extractUserIdFromJwt(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaimAsString("id");
        }
        throw new ValidationException("User not authenticated");
    }

    private Goal resolveGoal(String goalIdentifier, String userId) {
        return goalRepository.findByUuidAndUserIdAndIsDeletedFalse(goalIdentifier, userId)
            .or(() -> parseGoalId(goalIdentifier)
                .flatMap(goalId -> goalRepository.findByIdAndUserIdAndIsDeletedFalse(goalId, userId)))
            .orElseThrow(() -> new ValidationException("Goal not found"));
    }

    private java.util.Optional<Long> parseGoalId(String goalIdentifier) {
        if (goalIdentifier == null || goalIdentifier.isBlank()) {
            return java.util.Optional.empty();
        }
        try {
            return java.util.Optional.of(Long.parseLong(goalIdentifier));
        } catch (NumberFormatException ex) {
            return java.util.Optional.empty();
        }
    }

    @GetMapping
    public ResponseEntity<ResponseWrapper> getGoalPeriods(
            @PathVariable String goalUuid,
            Authentication authentication) {
        String userId = extractUserIdFromJwt(authentication);
        Goal goal = resolveGoal(goalUuid, userId);
        List<GoalPeriodResponse> periods = goalPeriodMapper.toResponseList(
            goalPeriodService.getPeriodsForGoal(goal.getUuid())
        );
        return ResponseEntity.ok(new ResponseWrapper("Goal periods retrieved successfully", "success", periods));
    }

    @GetMapping("/active")
    public ResponseEntity<ResponseWrapper> getActivePeriod(
            @PathVariable String goalUuid,
            @RequestParam(required = false) LocalDate date,
            Authentication authentication) {
        String userId = extractUserIdFromJwt(authentication);
        Goal goal = resolveGoal(goalUuid, userId);
        GoalPeriod active = goalPeriodService.getOrCreateActivePeriod(goal, date != null ? date : LocalDate.now());
        return ResponseEntity.ok(
            new ResponseWrapper("Active goal period retrieved successfully", "success", goalPeriodMapper.toResponse(active))
        );
    }

    @GetMapping("/{periodUuid}")
    public ResponseEntity<ResponseWrapper> getGoalPeriod(
            @PathVariable String goalUuid,
            @PathVariable String periodUuid,
            Authentication authentication) {
        String userId = extractUserIdFromJwt(authentication);
        Goal goal = resolveGoal(goalUuid, userId);
        GoalPeriod period = goalPeriodService.getPeriodForGoal(goal.getUuid(), periodUuid)
            .orElseThrow(() -> new ValidationException("Goal period not found"));
        return ResponseEntity.ok(
            new ResponseWrapper("Goal period retrieved successfully", "success", goalPeriodMapper.toResponse(period))
        );
    }

    @PostMapping
    public ResponseEntity<ResponseWrapper> createGoalPeriod(
            @PathVariable String goalUuid,
            @Valid @RequestBody(required = false) GoalPeriodCreateRequest request,
            Authentication authentication) {
        String userId = extractUserIdFromJwt(authentication);
        Goal goal = resolveGoal(goalUuid, userId);

        GoalPeriod period;
        if (request != null && request.getPeriodStart() != null && request.getPeriodEnd() != null) {
            period = goalPeriodService.createPeriodForGoal(goal, request.getPeriodStart(), request.getPeriodEnd());
            if (request.getCurrentValue() != null) {
                period.setCurrentValue(request.getCurrentValue());
            }
            if (request.getScheduleSpec() != null) {
                period.setScheduleSpec(request.getScheduleSpec());
            }
            period = goalPeriodService.updatePeriod(period);
        } else {
            period = goalPeriodService.createNextPeriod(goal.getUuid())
                .orElseThrow(() -> new ValidationException("No further goal periods can be created for this goal"));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(
            new ResponseWrapper("Goal period created successfully", "success", goalPeriodMapper.toResponse(period))
        );
    }

    @PostMapping("/bulk")
    public ResponseEntity<ResponseWrapper> bulkCreateGoalPeriods(
            @PathVariable String goalUuid,
            @RequestBody(required = false) GoalPeriodBulkCreateRequest request,
            Authentication authentication) {
        String userId = extractUserIdFromJwt(authentication);
        Goal goal = resolveGoal(goalUuid, userId);

        GoalPeriodBulkCreateRequest effectiveRequest = request != null ? request : new GoalPeriodBulkCreateRequest();
        List<GoalPeriodResponse> periods = goalPeriodMapper.toResponseList(
            goalPeriodService.bulkCreatePeriods(
                goal,
                effectiveRequest.getStartDate(),
                effectiveRequest.getThroughDate() != null ? effectiveRequest.getThroughDate() : LocalDate.now(),
                effectiveRequest.getMaxPeriods(),
                Boolean.TRUE.equals(effectiveRequest.getFillGaps())
            )
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(
            new ResponseWrapper("Goal periods bulk created successfully", "success", periods)
        );
    }

    @PostMapping("/reconcile")
    public ResponseEntity<ResponseWrapper> reconcileGoalPeriods(
            @PathVariable String goalUuid,
            @RequestParam(required = false) LocalDate throughDate,
            Authentication authentication) {
        String userId = extractUserIdFromJwt(authentication);
        Goal goal = resolveGoal(goalUuid, userId);
        List<GoalPeriodResponse> periods = goalPeriodMapper.toResponseList(
            goalPeriodService.ensurePeriodsThroughDate(goal, throughDate != null ? throughDate : LocalDate.now())
        );
        return ResponseEntity.ok(
            new ResponseWrapper("Goal periods reconciled successfully", "success", periods)
        );
    }

    @PutMapping("/{periodUuid}")
    public ResponseEntity<ResponseWrapper> updateGoalPeriod(
            @PathVariable String goalUuid,
            @PathVariable String periodUuid,
            @Valid @RequestBody GoalPeriodUpdateRequest request,
            Authentication authentication) {
        String userId = extractUserIdFromJwt(authentication);
        Goal goal = resolveGoal(goalUuid, userId);
        GoalPeriod period = goalPeriodService.getPeriodForGoal(goal.getUuid(), periodUuid)
            .orElseThrow(() -> new ValidationException("Goal period not found"));

        if (request.getPeriodStart() != null) {
            period.setPeriodStart(request.getPeriodStart());
        }
        if (request.getPeriodEnd() != null) {
            period.setPeriodEnd(request.getPeriodEnd());
        }
        if (request.getCurrentValue() != null) {
            period.setCurrentValue(request.getCurrentValue());
        }
        if (request.getScheduleSpec() != null) {
            period.setScheduleSpec(request.getScheduleSpec());
        }

        GoalPeriod updated = goalPeriodService.updatePeriod(period);
        return ResponseEntity.ok(
            new ResponseWrapper("Goal period updated successfully", "success", goalPeriodMapper.toResponse(updated))
        );
    }

    @DeleteMapping("/{periodUuid}")
    public ResponseEntity<ResponseWrapper> deleteGoalPeriod(
            @PathVariable String goalUuid,
            @PathVariable String periodUuid,
            Authentication authentication) {
        String userId = extractUserIdFromJwt(authentication);
        Goal goal = resolveGoal(goalUuid, userId);
        GoalPeriod period = goalPeriodService.getPeriodForGoal(goal.getUuid(), periodUuid)
            .orElseThrow(() -> new ValidationException("Goal period not found"));
        goalPeriodService.deletePeriod(period.getUuid());
        return ResponseEntity.ok(new ResponseWrapper("Goal period deleted successfully", "success", null));
    }
}
