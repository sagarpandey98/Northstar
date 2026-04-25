package com.sagarpandey.activity_tracker.Service.V1;

import com.sagarpandey.activity_tracker.Repository.GoalPeriodRepository;
import com.sagarpandey.activity_tracker.Service.Interface.GoalPeriodService;
import com.sagarpandey.activity_tracker.enums.HealthStatus;
import com.sagarpandey.activity_tracker.models.Goal;
import com.sagarpandey.activity_tracker.models.GoalPeriod;
import com.sagarpandey.activity_tracker.models.ScheduleSpec;
import com.sagarpandey.activity_tracker.utils.ScheduleSpecEvaluator;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GoalPeriodServiceV1 implements GoalPeriodService {

    private final GoalPeriodRepository goalPeriodRepository;

    @Autowired
    public GoalPeriodServiceV1(GoalPeriodRepository goalPeriodRepository) {
        this.goalPeriodRepository = goalPeriodRepository;
    }

    @Override
    public GoalPeriod createPeriodForGoal(Goal goal) {
        PeriodRange range = calculateFirstPeriodRange(goal);
        return createPeriodForGoal(goal, range.getPeriodStart(), range.getPeriodEnd());
    }

    @Override
    public GoalPeriod createPeriodForGoal(Goal goal, LocalDate periodStart, LocalDate periodEnd) {
        GoalPeriod period = new GoalPeriod();

        period.setUuid(UUID.randomUUID().toString());
        period.setCreatedAt(LocalDateTime.now());
        period.setLastUpdatedAt(LocalDateTime.now());
        period.setParentGoalUuid(goal.getUuid());

        period.setPeriodStart(periodStart);
        period.setPeriodEnd(periodEnd);
        period.setScheduleSpec(goal.getScheduleSpec());
        period.setMinimumSessionDaily(calculateMinimumSessionDaily(goal, periodStart, periodEnd));
        period.setMinimumSessionPeriod(goal.getMinimumSessionPeriod());
        period.setMaximumSessionPeriod(goal.getMaximumSessionPeriod());
        period.setAllowDoubleLogging(goal.getAllowDoubleLogging());
        period.setMetric(goal.getMetric());
        period.setTargetOperator(goal.getTargetOperator());
        period.setMissesAllowedPerPeriod(goal.getMissesAllowedPerPeriod());
        period.setLongestStreak(goal.getLongestStreak());
        period.setConsistencyWeight(goal.getConsistencyWeight());
        period.setMomentumWeight(goal.getMomentumWeight());
        period.setProgressWeight(goal.getProgressWeight());

        period.setCurrentValue(0.0);
        period.setProgressPercentage(0.0);
        period.setHealthScore(0.0);
        period.setConsistencyScore(0.0);
        period.setMomentumScore(0.0);
        period.setProgressScore(0.0);
        period.setCurrentStreak(0);
        period.setCompletedDate(null);
        period.setHealthStatus(Boolean.TRUE.equals(goal.getIsMilestone()) ? HealthStatus.UNTRACKED : HealthStatus.ON_TRACK);

        return goalPeriodRepository.save(period);
    }

    @Override
    public List<GoalPeriod> getPeriodsForGoal(String goalUuid) {
        List<GoalPeriod> periods = goalPeriodRepository.findByParentGoalUuid(goalUuid);
        periods.sort((p1, p2) -> p1.getPeriodStart().compareTo(p2.getPeriodStart()));
        return periods;
    }

    @Override
    public Optional<GoalPeriod> getActivePeriodForGoal(String goalUuid, LocalDate date) {
        return goalPeriodRepository.findActivePeriodForGoal(goalUuid, date);
    }

    @Override
    public GoalPeriod updatePeriod(GoalPeriod period) {
        period.setLastUpdatedAt(LocalDateTime.now());
        return goalPeriodRepository.save(period);
    }

    @Override
    public void deletePeriod(String periodUuid) {
        goalPeriodRepository.findByUuid(periodUuid).ifPresent(period -> {
            period.setLastUpdatedAt(LocalDateTime.now());
            goalPeriodRepository.save(period);
        });
    }

    @Override
    public Optional<GoalPeriod> createNextPeriod(String goalUuid) {
        return Optional.empty();
    }

    @Override
    public PeriodRange calculatePeriodRange(Goal goal, LocalDate referenceDate) {
        ScheduleSpec spec = goal != null ? goal.getScheduleSpec() : null;
        ScheduleSpec.ScheduleType scheduleType = spec != null ? spec.getScheduleType() : null;

        if (scheduleType == null) {
            return calculateMonthlyPeriod(referenceDate);
        }

        return switch (scheduleType) {
            case DAILY -> calculateDailyPeriod(referenceDate);
            case WEEKLY -> calculateWeeklyPeriod(referenceDate, resolveWeekStartsOn(spec));
            case MONTHLY -> calculateMonthlyPeriod(referenceDate);
            case QUARTERLY -> calculateQuarterlyPeriod(referenceDate);
            case YEARLY -> calculateYearlyPeriod(referenceDate);
        };
    }

    @Override
    public PeriodRange calculateFirstPeriodRange(Goal goal) {
        LocalDate startDate = goal.getStartDate() != null
            ? goal.getStartDate().toLocalDate()
            : LocalDate.now();
        return calculatePeriodRange(goal, startDate);
    }

    private Double calculateMinimumSessionDaily(Goal goal, LocalDate periodStart, LocalDate periodEnd) {
        if (goal == null || goal.getMinimumSessionPeriod() == null || goal.getMinimumSessionPeriod() <= 0) {
            return 0.0;
        }

        int actionableDays = ScheduleSpecEvaluator.countActionableDays(periodStart, periodEnd, goal.getScheduleSpec());
        if (actionableDays <= 0) {
            return 0.0;
        }

        double dailyMinimum = goal.getMinimumSessionPeriod() / (double) actionableDays;
        return Math.round(dailyMinimum * 100.0) / 100.0;
    }

    private PeriodRange calculateDailyPeriod(LocalDate referenceDate) {
        return new PeriodRange(referenceDate, referenceDate);
    }

    private PeriodRange calculateWeeklyPeriod(LocalDate referenceDate, DayOfWeek weekStartsOn) {
        LocalDate start = referenceDate.with(TemporalAdjusters.previousOrSame(weekStartsOn));
        return new PeriodRange(start, start.plusDays(6));
    }

    private PeriodRange calculateMonthlyPeriod(LocalDate referenceDate) {
        LocalDate start = referenceDate.withDayOfMonth(1);
        return new PeriodRange(start, start.withDayOfMonth(start.lengthOfMonth()));
    }

    private PeriodRange calculateQuarterlyPeriod(LocalDate referenceDate) {
        int quarter = (referenceDate.getMonthValue() - 1) / 3;
        int startMonth = quarter * 3 + 1;
        LocalDate start = referenceDate.withMonth(startMonth).withDayOfMonth(1);
        LocalDate endMonth = start.plusMonths(2);
        return new PeriodRange(start, endMonth.withDayOfMonth(endMonth.lengthOfMonth()));
    }

    private PeriodRange calculateYearlyPeriod(LocalDate referenceDate) {
        return new PeriodRange(
            referenceDate.withDayOfYear(1),
            referenceDate.withDayOfYear(referenceDate.lengthOfYear())
        );
    }

    private DayOfWeek resolveWeekStartsOn(ScheduleSpec spec) {
        if (spec == null || spec.getWeekStartsOn() == null || spec.getWeekStartsOn().isBlank()) {
            return DayOfWeek.MONDAY;
        }
        try {
            return DayOfWeek.valueOf(spec.getWeekStartsOn().toUpperCase());
        } catch (Exception e) {
            return DayOfWeek.MONDAY;
        }
    }
}
