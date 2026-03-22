package com.sagarpandey.activity_tracker.Repository;

import com.sagarpandey.activity_tracker.models.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {
    
    // Find by userId
    List<Goal> findByUserIdAndIsDeletedFalse(String userId);
    
    // Find by specific goal ID and userId for security
    Optional<Goal> findByIdAndUserIdAndIsDeletedFalse(Long id, String userId);
    
    // Find by UUID and userId for security
    Optional<Goal> findByUuidAndUserIdAndIsDeletedFalse(String uuid, String userId);
    
    // Find by parentGoalId
    List<Goal> findByParentGoalIdAndUserIdAndIsDeletedFalse(String parentGoalId, String userId);
    
    // Find root goals (no parent)
    List<Goal> findByParentGoalIdIsNullAndUserIdAndIsDeletedFalse(String userId);
    
    // Find overdue goals (targetDate < now and not completed)
    @Query("SELECT g FROM Goal g WHERE g.userId = :userId " +
           "AND g.isDeleted = false " +
           "AND g.targetDate < :now " +
           "AND g.status != 'COMPLETED'")
    List<Goal> findOverdueGoals(@Param("userId") String userId, @Param("now") LocalDateTime now);
    
    // Find due soon goals (targetDate between now and now+7days)
    @Query("SELECT g FROM Goal g WHERE g.userId = :userId " +
           "AND g.isDeleted = false " +
           "AND g.targetDate BETWEEN :now AND :sevenDaysFromNow " +
           "AND g.status != 'COMPLETED'")
    List<Goal> findDueSoonGoals(@Param("userId") String userId, 
                                @Param("now") LocalDateTime now, 
                                @Param("sevenDaysFromNow") LocalDateTime sevenDaysFromNow);
    
    // Text search in title/description
    @Query("SELECT g FROM Goal g WHERE g.userId = :userId " +
           "AND g.isDeleted = false " +
           "AND (LOWER(g.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(g.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Goal> searchGoals(@Param("userId") String userId, @Param("query") String query);
    
    // Count goals by status for statistics
    @Query("SELECT g.status, COUNT(g) FROM Goal g WHERE g.userId = :userId " +
           "AND g.isDeleted = false GROUP BY g.status")
    List<Object[]> countGoalsByStatus(@Param("userId") String userId);
    
    // Count goals by priority for statistics
    @Query("SELECT g.priority, COUNT(g) FROM Goal g WHERE g.userId = :userId " +
           "AND g.isDeleted = false GROUP BY g.priority")
    List<Object[]> countGoalsByPriority(@Param("userId") String userId);
    
    // Find milestones
    List<Goal> findByUserIdAndIsMilestoneTrueAndIsDeletedFalse(String userId);

    // Find all active goals (not deleted and not completed)
    @Query("SELECT g FROM Goal g WHERE g.isDeleted = false " +
           "AND g.status != com.sagarpandey.activity_tracker." +
           "models.Goal.Status.COMPLETED")
    List<Goal> findAllActiveGoals();

    boolean existsByIdAndUserId(Long id, String userId);
}
