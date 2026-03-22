package com.sagarpandey.activity_tracker.Service.V1;

import com.sagarpandey.activity_tracker.Exceptions.GoalNotFoundException;
import com.sagarpandey.activity_tracker.Exceptions.ValidationException;
import com.sagarpandey.activity_tracker.Mapper.GoalMapper;
import com.sagarpandey.activity_tracker.Repository.GoalRepository;
import com.sagarpandey.activity_tracker.dtos.GoalRequest;
import com.sagarpandey.activity_tracker.dtos.GoalResponse;
import com.sagarpandey.activity_tracker.dtos.GoalStatsResponse;
import com.sagarpandey.activity_tracker.models.Goal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalServiceV1Test {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private GoalMapper goalMapper;

    @InjectMocks
    private GoalServiceV1 goalService;

    private GoalRequest goalRequest;
    private Goal goal;
    private GoalResponse goalResponse;
    private String userId;

    @BeforeEach
    void setUp() {
        userId = "test-user-123";
        
        goalRequest = new GoalRequest();
        goalRequest.setTitle("Test Goal");
        goalRequest.setDescription("Test Description");
        goalRequest.setPriority(Goal.Priority.HIGH);
        goalRequest.setMetric(Goal.Metric.COUNT);
        goalRequest.setTargetOperator(Goal.TargetOperator.GREATER_THAN);
        goalRequest.setTargetValue(100.0);
        goalRequest.setCurrentValue(0.0);

        goal = new Goal();
        goal.setId(1L);
        goal.setUuid("test-uuid-123");
        goal.setUserId(userId);
        goal.setTitle("Test Goal");
        goal.setDescription("Test Description");
        goal.setPriority(Goal.Priority.HIGH);
        goal.setStatus(Goal.Status.NOT_STARTED);
        goal.setMetric(Goal.Metric.COUNT);
        goal.setTargetOperator(Goal.TargetOperator.GREATER_THAN);
        goal.setTargetValue(100.0);
        goal.setCurrentValue(0.0);
        goal.setProgressPercentage(0.0);
        goal.setCreatedAt(LocalDateTime.now());
        goal.setLastUpdatedAt(LocalDateTime.now());
        goal.setIsDeleted(false);

        goalResponse = new GoalResponse();
        goalResponse.setId(1L);
        goalResponse.setUuid("test-uuid-123");
        goalResponse.setUserId(userId);
        goalResponse.setTitle("Test Goal");
        goalResponse.setDescription("Test Description");
        goalResponse.setPriority(Goal.Priority.HIGH);
        goalResponse.setStatus(Goal.Status.NOT_STARTED);
        goalResponse.setMetric(Goal.Metric.COUNT);
        goalResponse.setTargetOperator(Goal.TargetOperator.GREATER_THAN);
        goalResponse.setTargetValue(100.0);
        goalResponse.setCurrentValue(0.0);
        goalResponse.setProgressPercentage(0.0);
    }

    @Test
    void createGoal_Success() {
        // Given
        when(goalMapper.toEntity(goalRequest, userId)).thenReturn(goal);
        when(goalRepository.save(goal)).thenReturn(goal);
        when(goalMapper.toResponse(goal)).thenReturn(goalResponse);

        // When
        GoalResponse result = goalService.createGoal(goalRequest, userId);

        // Then
        assertNotNull(result);
        assertEquals("Test Goal", result.getTitle());
        assertEquals(userId, result.getUserId());
        verify(goalRepository).save(goal);
        verify(goalMapper).toEntity(goalRequest, userId);
        verify(goalMapper).toResponse(goal);
    }

    @Test
    void createGoal_ValidationError_NullTitle() {
        // Given
        goalRequest.setTitle(null);

        // When & Then
        assertThrows(ValidationException.class, () -> goalService.createGoal(goalRequest, userId));
        verify(goalRepository, never()).save(any());
    }

    @Test
    void createGoal_ValidationError_EmptyTitle() {
        // Given
        goalRequest.setTitle("");

        // When & Then
        assertThrows(ValidationException.class, () -> goalService.createGoal(goalRequest, userId));
        verify(goalRepository, never()).save(any());
    }

    @Test
    void createGoal_ValidationError_NullPriority() {
        // Given
        goalRequest.setPriority(null);

        // When & Then
        assertThrows(ValidationException.class, () -> goalService.createGoal(goalRequest, userId));
        verify(goalRepository, never()).save(any());
    }

    @Test
    void createGoal_ValidationError_NegativeTargetValue() {
        // Given
        goalRequest.setTargetValue(-10.0);

        // When & Then
        assertThrows(ValidationException.class, () -> goalService.createGoal(goalRequest, userId));
        verify(goalRepository, never()).save(any());
    }

    @Test
    void createGoal_WithParentGoal_Success() {
        // Given
        goalRequest.setParentGoalId("parent-uuid-123");
        Goal parentGoal = new Goal();
        parentGoal.setUuid("parent-uuid-123");
        
        when(goalRepository.findByUuidAndUserIdAndIsDeletedFalse("parent-uuid-123", userId))
                .thenReturn(Optional.of(parentGoal));
        when(goalMapper.toEntity(goalRequest, userId)).thenReturn(goal);
        when(goalRepository.save(goal)).thenReturn(goal);
        when(goalMapper.toResponse(goal)).thenReturn(goalResponse);

        // When
        GoalResponse result = goalService.createGoal(goalRequest, userId);

        // Then
        assertNotNull(result);
        verify(goalRepository).findByUuidAndUserIdAndIsDeletedFalse("parent-uuid-123", userId);
        verify(goalRepository).save(goal);
    }

    @Test
    void createGoal_WithInvalidParentGoal_ThrowsException() {
        // Given
        goalRequest.setParentGoalId("invalid-parent-uuid");
        when(goalRepository.findByUuidAndUserIdAndIsDeletedFalse("invalid-parent-uuid", userId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(ValidationException.class, () -> goalService.createGoal(goalRequest, userId));
        verify(goalRepository, never()).save(any());
    }

    @Test
    void getGoalById_Success() {
        // Given
        Long goalId = 1L;
        when(goalRepository.findByIdAndUserIdAndIsDeletedFalse(goalId, userId))
                .thenReturn(Optional.of(goal));
        when(goalMapper.toResponse(goal)).thenReturn(goalResponse);

        // When
        GoalResponse result = goalService.getGoalById(goalId, userId);

        // Then
        assertNotNull(result);
        assertEquals(goalId, result.getId());
        assertEquals(userId, result.getUserId());
        verify(goalRepository).findByIdAndUserIdAndIsDeletedFalse(goalId, userId);
        verify(goalMapper).toResponse(goal);
    }

    @Test
    void getGoalById_NotFound_ThrowsException() {
        // Given
        Long goalId = 999L;
        when(goalRepository.findByIdAndUserIdAndIsDeletedFalse(goalId, userId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(GoalNotFoundException.class, () -> goalService.getGoalById(goalId, userId));
        verify(goalMapper, never()).toResponse(any());
    }

    @Test
    void getAllGoalsByUser_Success() {
        // Given
        List<Goal> goals = Arrays.asList(goal);
        List<GoalResponse> goalResponses = Arrays.asList(goalResponse);
        
        when(goalRepository.findByUserIdAndIsDeletedFalse(userId)).thenReturn(goals);
        when(goalMapper.toResponseList(goals)).thenReturn(goalResponses);

        // When
        List<GoalResponse> result = goalService.getAllGoalsByUser(userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Goal", result.get(0).getTitle());
        verify(goalRepository).findByUserIdAndIsDeletedFalse(userId);
        verify(goalMapper).toResponseList(goals);
    }

    @Test
    void updateGoal_Success() {
        // Given
        Long goalId = 1L;
        goalRequest.setTitle("Updated Goal");
        
        when(goalRepository.findByIdAndUserIdAndIsDeletedFalse(goalId, userId))
                .thenReturn(Optional.of(goal));
        when(goalRepository.save(goal)).thenReturn(goal);
        when(goalMapper.toResponse(goal)).thenReturn(goalResponse);

        // When
        GoalResponse result = goalService.updateGoal(goalId, goalRequest, userId);

        // Then
        assertNotNull(result);
        verify(goalRepository).findByIdAndUserIdAndIsDeletedFalse(goalId, userId);
        verify(goalMapper).updateEntity(goal, goalRequest);
        verify(goalRepository).save(goal);
        verify(goalMapper).toResponse(goal);
    }

    @Test
    void updateGoal_NotFound_ThrowsException() {
        // Given
        Long goalId = 999L;
        when(goalRepository.findByIdAndUserIdAndIsDeletedFalse(goalId, userId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(GoalNotFoundException.class, () -> goalService.updateGoal(goalId, goalRequest, userId));
        verify(goalRepository, never()).save(any());
    }

    @Test
    void deleteGoal_Success() {
        // Given
        Long goalId = 1L;
        when(goalRepository.findByIdAndUserIdAndIsDeletedFalse(goalId, userId))
                .thenReturn(Optional.of(goal));
        when(goalRepository.findByParentGoalIdAndUserIdAndIsDeletedFalse(goal.getUuid(), userId))
                .thenReturn(Collections.emptyList());

        // When
        goalService.deleteGoal(goalId, userId);

        // Then
        assertTrue(goal.getIsDeleted());
        verify(goalRepository).findByIdAndUserIdAndIsDeletedFalse(goalId, userId);
        verify(goalRepository).save(goal);
        verify(goalRepository).findByParentGoalIdAndUserIdAndIsDeletedFalse(goal.getUuid(), userId);
    }

    @Test
    void deleteGoal_NotFound_ThrowsException() {
        // Given
        Long goalId = 999L;
        when(goalRepository.findByIdAndUserIdAndIsDeletedFalse(goalId, userId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(GoalNotFoundException.class, () -> goalService.deleteGoal(goalId, userId));
        verify(goalRepository, never()).save(any());
    }

    @Test
    void getGoalStatistics_Success() {
        // Given
        List<Goal> goals = Arrays.asList(
            createGoalWithStatus(Goal.Status.COMPLETED),
            createGoalWithStatus(Goal.Status.IN_PROGRESS),
            createGoalWithStatus(Goal.Status.NOT_STARTED),
            createGoalWithStatus(Goal.Status.OVERDUE)
        );
        
        when(goalRepository.findByUserIdAndIsDeletedFalse(userId)).thenReturn(goals);

        // When
        GoalStatsResponse result = goalService.getGoalStatistics(userId);

        // Then
        assertNotNull(result);
        assertEquals(4, result.getTotalGoals());
        assertEquals(1, result.getCompletedGoals());
        assertEquals(1, result.getInProgressGoals());
        assertEquals(1, result.getNotStartedGoals());
        assertEquals(1, result.getOverdueGoals());
        assertEquals(25.0, result.getOverallCompletionPercentage(), 0.01);
        assertEquals(25.0, result.getOverduePercentage(), 0.01);
        verify(goalRepository).findByUserIdAndIsDeletedFalse(userId);
    }

    @Test
    void updateProgress_Success() {
        // Given
        Long goalId = 1L;
        Double newCurrentValue = 50.0;
        
        when(goalRepository.findByIdAndUserIdAndIsDeletedFalse(goalId, userId))
                .thenReturn(Optional.of(goal));
        when(goalMapper.calculateProgressPercentage(goal)).thenReturn(50.0);
        when(goalRepository.save(goal)).thenReturn(goal);
        when(goalMapper.toResponse(goal)).thenReturn(goalResponse);

        // When
        GoalResponse result = goalService.updateProgress(goalId, newCurrentValue, userId);

        // Then
        assertNotNull(result);
        assertEquals(newCurrentValue, goal.getCurrentValue());
        verify(goalRepository).findByIdAndUserIdAndIsDeletedFalse(goalId, userId);
        verify(goalMapper).calculateProgressPercentage(goal);
        verify(goalMapper).updateStatusBasedOnProgress(goal);
        verify(goalRepository).save(goal);
        verify(goalMapper).toResponse(goal);
    }

    @Test
    void searchGoals_Success() {
        // Given
        String searchQuery = "test";
        List<Goal> goals = Arrays.asList(goal);
        List<GoalResponse> goalResponses = Arrays.asList(goalResponse);
        
        when(goalRepository.searchGoals(userId, searchQuery)).thenReturn(goals);
        when(goalMapper.toResponseList(goals)).thenReturn(goalResponses);

        // When
        List<GoalResponse> result = goalService.searchGoals(searchQuery, userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(goalRepository).searchGoals(userId, searchQuery);
        verify(goalMapper).toResponseList(goals);
    }

    @Test
    void getOverdueGoals_Success() {
        // Given
        List<Goal> overdueGoals = Arrays.asList(goal);
        List<GoalResponse> goalResponses = Arrays.asList(goalResponse);
        
        when(goalRepository.findOverdueGoals(eq(userId), any(LocalDateTime.class)))
                .thenReturn(overdueGoals);
        when(goalMapper.toResponseList(overdueGoals)).thenReturn(goalResponses);

        // When
        List<GoalResponse> result = goalService.getOverdueGoals(userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(goalRepository).findOverdueGoals(eq(userId), any(LocalDateTime.class));
        verify(goalMapper).toResponseList(overdueGoals);
    }

    @Test
    void getDueSoonGoals_Success() {
        // Given
        List<Goal> dueSoonGoals = Arrays.asList(goal);
        List<GoalResponse> goalResponses = Arrays.asList(goalResponse);
        
        when(goalRepository.findDueSoonGoals(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(dueSoonGoals);
        when(goalMapper.toResponseList(dueSoonGoals)).thenReturn(goalResponses);

        // When
        List<GoalResponse> result = goalService.getDueSoonGoals(userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(goalRepository).findDueSoonGoals(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(goalMapper).toResponseList(dueSoonGoals);
    }

    private Goal createGoalWithStatus(Goal.Status status) {
        Goal goal = new Goal();
        goal.setStatus(status);
        goal.setPriority(Goal.Priority.MEDIUM);
        goal.setIsMilestone(false);
        return goal;
    }
}
