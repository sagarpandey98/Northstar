package com.sagarpandey.activity_tracker.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sagarpandey.activity_tracker.Service.Interface.GoalHealthService;
import com.sagarpandey.activity_tracker.Service.Interface.GoalPeriodService;
import com.sagarpandey.activity_tracker.Service.Interface.GoalService;
import com.sagarpandey.activity_tracker.dtos.GoalRequest;
import com.sagarpandey.activity_tracker.dtos.GoalResponse;
import com.sagarpandey.activity_tracker.dtos.GoalStatsResponse;
import com.sagarpandey.activity_tracker.models.Goal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GoalController.class)
class GoalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private GoalService goalService;

    @MockBean
    private GoalHealthService goalHealthService;

    @MockBean
    private GoalPeriodService goalPeriodService;

    @Autowired
    private ObjectMapper objectMapper;

    private GoalRequest goalRequest;
    private GoalResponse goalResponse;
    private String userId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        userId = "test-user-123";

        goalRequest = new GoalRequest();
        goalRequest.setTitle("Test Goal");
        goalRequest.setDescription("Test Description");
        goalRequest.setPriority(Goal.Priority.HIGH);
        goalRequest.setMetric(Goal.Metric.COUNT);
        goalRequest.setTargetOperator(Goal.TargetOperator.GREATER_THAN);
        goalRequest.setTargetValue(100.0);
        goalRequest.setCurrentValue(0.0);

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
        goalResponse.setCreatedAt(LocalDateTime.now());
        goalResponse.setLastUpdatedAt(LocalDateTime.now());
    }

    @Test
    @WithMockUser
    void createGoal_Success() throws Exception {
        // Given
        when(goalService.createGoal(any(GoalRequest.class), eq(userId)))
                .thenReturn(goalResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/goals")
                        .with(jwt().jwt(jwt -> jwt.claim("id", userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(goalRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Goal created successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Test Goal"));

        verify(goalService).createGoal(any(GoalRequest.class), eq(userId));
    }

    @Test
    @WithMockUser
    void createGoal_ValidationError() throws Exception {
        // Given
        goalRequest.setTitle(""); // Invalid title

        // When & Then
        mockMvc.perform(post("/api/v1/goals")
                        .with(jwt().jwt(jwt -> jwt.claim("id", userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(goalRequest)))
                .andExpect(status().isBadRequest());

        verify(goalService, never()).createGoal(any(), any());
    }

    @Test
    @WithMockUser
    void getAllGoals_Success() throws Exception {
        // Given
        List<GoalResponse> goals = Arrays.asList(goalResponse);
        when(goalService.getAllGoalsByUser(userId)).thenReturn(goals);

        // When & Then
        mockMvc.perform(get("/api/v1/goals")
                        .with(jwt().jwt(jwt -> jwt.claim("id", userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Goals retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Test Goal"));

        verify(goalService).getAllGoalsByUser(userId);
    }

    @Test
    @WithMockUser
    void getGoalById_Success() throws Exception {
        // Given
        Long goalId = 1L;
        when(goalService.getGoalById(goalId, userId)).thenReturn(goalResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/goals/{id}", goalId)
                        .with(jwt().jwt(jwt -> jwt.claim("id", userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Goal retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Test Goal"));

        verify(goalService).getGoalById(goalId, userId);
    }

    @Test
    @WithMockUser
    void updateGoal_Success() throws Exception {
        // Given
        Long goalId = 1L;
        goalRequest.setTitle("Updated Goal");
        goalResponse.setTitle("Updated Goal");
        
        when(goalService.updateGoal(eq(goalId), any(GoalRequest.class), eq(userId)))
                .thenReturn(goalResponse);

        // When & Then
        mockMvc.perform(put("/api/v1/goals/{id}", goalId)
                        .with(jwt().jwt(jwt -> jwt.claim("id", userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(goalRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Goal updated successfully"))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(goalService).updateGoal(eq(goalId), any(GoalRequest.class), eq(userId));
    }

    @Test
    @WithMockUser
    void deleteGoal_Success() throws Exception {
        // Given
        Long goalId = 1L;
        doNothing().when(goalService).deleteGoal(goalId, userId);

        // When & Then
        mockMvc.perform(delete("/api/v1/goals/{id}", goalId)
                        .with(jwt().jwt(jwt -> jwt.claim("id", userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Goal deleted successfully"))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(goalService).deleteGoal(goalId, userId);
    }

    @Test
    @WithMockUser
    void getGoalTree_Success() throws Exception {
        // Given
        List<GoalResponse> goalTree = Arrays.asList(goalResponse);
        when(goalService.getGoalTree(userId)).thenReturn(goalTree);

        // When & Then
        mockMvc.perform(get("/api/v1/goals/tree")
                        .with(jwt().jwt(jwt -> jwt.claim("id", userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Goal tree retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray());

        verify(goalService).getGoalTree(userId);
    }

    @Test
    @WithMockUser
    void updateProgressBulk_Success() throws Exception {
        // Given
        Map<Long, Double> progressUpdates = new HashMap<>();
        progressUpdates.put(1L, 50.0);
        
        List<GoalResponse> updatedGoals = Arrays.asList(goalResponse);
        when(goalService.updateProgressBulk(progressUpdates, userId)).thenReturn(updatedGoals);

        // When & Then
        mockMvc.perform(patch("/api/v1/goals/progress/bulk")
                        .with(jwt().jwt(jwt -> jwt.claim("id", userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(progressUpdates)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Progress updated successfully"))
                .andExpect(jsonPath("$.data").isArray());

        verify(goalService).updateProgressBulk(progressUpdates, userId);
    }

    @Test
    @WithMockUser
    void updateStatusBulk_Success() throws Exception {
        // Given
        Map<Long, Goal.Status> statusUpdates = new HashMap<>();
        statusUpdates.put(1L, Goal.Status.COMPLETED);
        
        List<GoalResponse> updatedGoals = Arrays.asList(goalResponse);
        when(goalService.updateStatusBulk(statusUpdates, userId)).thenReturn(updatedGoals);

        // When & Then
        mockMvc.perform(patch("/api/v1/goals/status/bulk")
                        .with(jwt().jwt(jwt -> jwt.claim("id", userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdates)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Status updated successfully"))
                .andExpect(jsonPath("$.data").isArray());

        verify(goalService).updateStatusBulk(statusUpdates, userId);
    }

    @Test
    @WithMockUser
    void getGoalStatistics_Success() throws Exception {
        // Given
        GoalStatsResponse stats = new GoalStatsResponse();
        stats.setTotalGoals(10);
        stats.setCompletedGoals(5);
        stats.setOverallCompletionPercentage(50.0);
        
        when(goalService.getGoalStatistics(userId)).thenReturn(stats);

        // When & Then
        mockMvc.perform(get("/api/v1/goals/statistics")
                        .with(jwt().jwt(jwt -> jwt.claim("id", userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Statistics retrieved successfully"))
                .andExpect(jsonPath("$.data.totalGoals").value(10))
                .andExpect(jsonPath("$.data.completedGoals").value(5))
                .andExpect(jsonPath("$.data.overallCompletionPercentage").value(50.0));

        verify(goalService).getGoalStatistics(userId);
    }

    @Test
    @WithMockUser
    void getOverdueGoals_Success() throws Exception {
        // Given
        List<GoalResponse> overdueGoals = Arrays.asList(goalResponse);
        when(goalService.getOverdueGoals(userId)).thenReturn(overdueGoals);

        // When & Then
        mockMvc.perform(get("/api/v1/goals/overdue")
                        .with(jwt().jwt(jwt -> jwt.claim("id", userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Overdue goals retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray());

        verify(goalService).getOverdueGoals(userId);
    }

    @Test
    @WithMockUser
    void getDueSoonGoals_Success() throws Exception {
        // Given
        List<GoalResponse> dueSoonGoals = Arrays.asList(goalResponse);
        when(goalService.getDueSoonGoals(userId)).thenReturn(dueSoonGoals);

        // When & Then
        mockMvc.perform(get("/api/v1/goals/due-soon")
                        .with(jwt().jwt(jwt -> jwt.claim("id", userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Due soon goals retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray());

        verify(goalService).getDueSoonGoals(userId);
    }

    @Test
    @WithMockUser
    void searchGoals_Success() throws Exception {
        // Given
        String searchQuery = "test";
        List<GoalResponse> searchResults = Arrays.asList(goalResponse);
        when(goalService.searchGoals(searchQuery, userId)).thenReturn(searchResults);

        // When & Then
        mockMvc.perform(get("/api/v1/goals/search")
                        .param("query", searchQuery)
                        .with(jwt().jwt(jwt -> jwt.claim("id", userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Search completed successfully"))
                .andExpect(jsonPath("$.data").isArray());

        verify(goalService).searchGoals(searchQuery, userId);
    }

    @Test
    @WithMockUser
    void getMilestones_Success() throws Exception {
        // Given
        List<GoalResponse> milestones = Arrays.asList(goalResponse);
        when(goalService.getMilestones(userId)).thenReturn(milestones);

        // When & Then
        mockMvc.perform(get("/api/v1/goals/milestones")
                        .with(jwt().jwt(jwt -> jwt.claim("id", userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Milestones retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray());

        verify(goalService).getMilestones(userId);
    }

    @Test
    @WithMockUser
    void updateGoalProgress_Success() throws Exception {
        // Given
        Long goalId = 1L;
        Map<String, Double> progressData = new HashMap<>();
        progressData.put("currentValue", 75.0);
        
        when(goalService.updateProgress(goalId, 75.0, userId)).thenReturn(goalResponse);

        // When & Then
        mockMvc.perform(patch("/api/v1/goals/{id}/progress", goalId)
                        .with(jwt().jwt(jwt -> jwt.claim("id", userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(progressData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Goal progress updated successfully"))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(goalService).updateProgress(goalId, 75.0, userId);
    }

    @Test
    @WithMockUser
    void recalculateAllProgress_Success() throws Exception {
        // Given
        doNothing().when(goalService).recalculateAllProgress(userId);

        // When & Then
        mockMvc.perform(post("/api/v1/goals/recalculate")
                        .with(jwt().jwt(jwt -> jwt.claim("id", userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Progress recalculated successfully"))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(goalService).recalculateAllProgress(userId);
    }
}
