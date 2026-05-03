package com.sagarpandey.activity_tracker.Service.V1;

import com.sagarpandey.activity_tracker.Exceptions.ValidationException;
import com.sagarpandey.activity_tracker.Repository.GoalPeriodRepository;
import com.sagarpandey.activity_tracker.Repository.GoalRepository;
import com.sagarpandey.activity_tracker.Service.Interface.GoalPeriodService;
import com.sagarpandey.activity_tracker.models.Goal;
import com.sagarpandey.activity_tracker.models.GoalPeriod;
import com.sagarpandey.activity_tracker.models.ScheduleSpec;
import com.sagarpandey.activity_tracker.utils.ScheduleSpecEvaluator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GoalPeriodServiceV1 implements GoalPeriodService {

    private static final Logger log = LoggerFactory.getLogger(GoalPeriodServiceV1.class);

    private final GoalPeriodRepository goalPeriodRepository;
    private final GoalRepository goalRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public GoalPeriodServiceV1(
            GoalPeriodRepository goalPeriodRepository,
            GoalRepository goalRepository) {
        this.goalPeriodRepository = goalPeriodRepository;
        this.goalRepository = goalRepository;
    }

    @Override
    public GoalPeriod createPeriodForGoal(Goal goal) {
        PeriodRange range = calculateFirstPeriodRange(goal);
        return createPeriodForGoal(goal, range.getPeriodStart(), range.getPeriodEnd());
    }

    @Override
    public GoalPeriod createPeriodForGoal(Goal goal, LocalDate periodStart, LocalDate periodEnd) {
        validateGoal(goal);
        PeriodRange normalized = normalizeRange(goal, periodStart, periodEnd);
        assertNoOverlappingPeriod(goal.getUuid(), normalized.getPeriodStart(), normalized.getPeriodEnd(), null);

        Optional<GoalPeriod> existing = goalPeriodRepository.findByParentGoalUuid(goal.getUuid()).stream()
            .filter(period -> normalized.getPeriodStart().equals(period.getPeriodStart()))
            .filter(period -> normalized.getPeriodEnd().equals(period.getPeriodEnd()))
            .findFirst();
        if (existing.isPresent()) {
            return existing.get();
        }

        GoalPeriod period = new GoalPeriod();
        period.setUuid(UUID.randomUUID().toString());
        period.setCreatedAt(LocalDateTime.now());
        period.setLastUpdatedAt(LocalDateTime.now());
        period.setParentGoalUuid(goal.getUuid());
        period.setGoal(goal);
        period.setPeriodStart(normalized.getPeriodStart());
        period.setPeriodEnd(normalized.getPeriodEnd());
        period.setScheduleSpec(goal.getScheduleSpec());
        period.setMinimumSessionDaily(calculateMinimumSessionDaily(goal, normalized.getPeriodStart(), normalized.getPeriodEnd()));
        period.setCurrentValue(0.0);
        period.setCurrentStreak(goal.getCurrentStreak() != null ? goal.getCurrentStreak() : 0);
        period.setLongestStreak(goal.getLongestStreak() != null ? goal.getLongestStreak() : 0);

        // Copy inherited fields from parent Goal (needed until database migration removes these columns)
        period.setMetric(goal.getMetric());
        period.setTargetOperator(goal.getTargetOperator());
        period.setTargetValue(goal.getTargetValue());
        period.setAllowDoubleLogging(goal.getAllowDoubleLogging());
        period.setMissesAllowedPerPeriod(goal.getMissesAllowedPerPeriod());
        period.setMinimumSessionPeriod(goal.getMinimumSessionPeriod());
        period.setMaximumSessionPeriod(goal.getMaximumSessionPeriod());
        period.setConsistencyWeight(goal.getConsistencyWeight());
        period.setMomentumWeight(goal.getMomentumWeight());
        period.setProgressWeight(goal.getProgressWeight());

        initializeHealthSnapshot(period);

        syncGoalPeriodPrimaryKeySequence();
        GoalPeriod saved = goalPeriodRepository.save(period);
        log.info(
            "Created goal period {} for goal {} from {} to {}",
            saved.getUuid(),
            goal.getUuid(),
            saved.getPeriodStart(),
            saved.getPeriodEnd()
        );
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoalPeriod> getPeriodsForGoal(String goalUuid) {
        return goalPeriodRepository.findByParentGoalUuid(goalUuid).stream()
            .sorted(Comparator.comparing(GoalPeriod::getPeriodStart))
            .toList();
    }

    @Override
    public Optional<GoalPeriod> getActivePeriodForGoal(String goalUuid, LocalDate date) {
        if (goalUuid == null || date == null) {
            return Optional.empty();
        }
        Optional<GoalPeriod> active = goalPeriodRepository.findActivePeriodForGoal(goalUuid, date);
        if (active.isPresent()) {
            return active;
        }
        return goalRepository.findByUuidAndIsDeletedFalse(goalUuid)
            .map(goal -> getOrCreateActivePeriod(goal, date));
    }

    @Override
    public GoalPeriod getOrCreateActivePeriod(Goal goal, LocalDate date) {
        validateGoal(goal);
        LocalDate targetDate = date != null ? date : LocalDate.now();
        ensurePeriodsThroughDate(goal, targetDate);
        return goalPeriodRepository.findActivePeriodForGoal(goal.getUuid(), targetDate)
            .orElseGet(() -> {
                PeriodRange range = calculatePeriodRange(goal, targetDate);
                return createPeriodForGoal(goal, range.getPeriodStart(), range.getPeriodEnd());
            });
    }

    @Override
    public List<GoalPeriod> ensurePeriodsThroughDate(Goal goal, LocalDate throughDate) {
        validateGoal(goal);
        LocalDate effectiveThroughDate = clampToGoalDateBounds(goal, throughDate != null ? throughDate : LocalDate.now());
        if (effectiveThroughDate == null) {
            return getPeriodsForGoal(goal.getUuid());
        }

        LocalDate startCursor = calculateFirstPeriodRange(goal).getPeriodStart();
        createExpectedPeriods(goal, startCursor, effectiveThroughDate, null, true);
        return getPeriodsForGoal(goal.getUuid());
    }

    @Override
    public List<GoalPeriod> bulkCreatePeriods(
            Goal goal,
            LocalDate startDate,
            LocalDate throughDate,
            Integer maxPeriods,
            boolean fillGaps) {
        validateGoal(goal);

        LocalDate effectiveStart = startDate != null
            ? clampToGoalStart(goal, startDate)
            : determineBulkStart(goal, fillGaps);
        LocalDate effectiveThroughDate = clampToGoalDateBounds(goal, throughDate);

        if (effectiveStart == null || effectiveThroughDate == null || effectiveThroughDate.isBefore(effectiveStart)) {
            return List.of();
        }

        return createExpectedPeriods(goal, effectiveStart, effectiveThroughDate, maxPeriods, fillGaps);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GoalPeriod> getPeriodForGoal(String goalUuid, String periodUuid) {
        if (goalUuid == null || periodUuid == null) {
            return Optional.empty();
        }
        return goalPeriodRepository.findByParentGoalUuidAndUuid(goalUuid, periodUuid);
    }

    @Override
    public GoalPeriod updatePeriod(GoalPeriod period) {
        if (period == null || period.getUuid() == null || period.getParentGoalUuid() == null) {
            throw new ValidationException("Goal period and identifiers are required");
        }

        Goal goal = resolveGoal(period.getParentGoalUuid());
        PeriodRange normalized = normalizeRange(goal, period.getPeriodStart(), period.getPeriodEnd());
        assertNoOverlappingPeriod(goal.getUuid(), normalized.getPeriodStart(), normalized.getPeriodEnd(), period.getUuid());

        period.setGoal(goal);
        period.setPeriodStart(normalized.getPeriodStart());
        period.setPeriodEnd(normalized.getPeriodEnd());
        period.setMinimumSessionDaily(calculateMinimumSessionDaily(goal, normalized.getPeriodStart(), normalized.getPeriodEnd()));
        if (period.getScheduleSpec() == null) {
            period.setScheduleSpec(goal.getScheduleSpec());
        }
        initializeHealthSnapshot(period);
        period.setLastUpdatedAt(LocalDateTime.now());
        return goalPeriodRepository.save(period);
    }

    @Override
    public void deletePeriod(String periodUuid) {
        goalPeriodRepository.findByUuid(periodUuid).ifPresent(goalPeriodRepository::delete);
    }

    @Override
    public Optional<GoalPeriod> createNextPeriod(String goalUuid) {
        if (goalUuid == null || goalUuid.isBlank()) {
            return Optional.empty();
        }
        Goal goal = resolveGoal(goalUuid);
        Optional<GoalPeriod> lastPeriod = goalPeriodRepository.findTopByParentGoalUuidOrderByPeriodEndDesc(goalUuid);
        if (lastPeriod.isEmpty()) {
            return Optional.of(createPeriodForGoal(goal));
        }

        LocalDate nextStart = lastPeriod.get().getPeriodEnd().plusDays(1);
        LocalDate clampedStart = clampToGoalDateBounds(goal, nextStart);
        if (clampedStart == null) {
            return Optional.empty();
        }

        PeriodRange nextRange = calculatePeriodRange(goal, clampedStart);
        LocalDate goalTargetDate = goal.getTargetDate() != null ? goal.getTargetDate().toLocalDate() : null;
        if (goalTargetDate != null && nextRange.getPeriodStart().isAfter(goalTargetDate)) {
            return Optional.empty();
        }
        return Optional.of(createPeriodForGoal(goal, nextRange.getPeriodStart(), nextRange.getPeriodEnd()));
    }

    @Override
    public PeriodRange calculatePeriodRange(Goal goal, LocalDate referenceDate) {
        validateGoal(goal);
        LocalDate effectiveReference = referenceDate != null ? referenceDate : LocalDate.now();
        ScheduleSpec spec = goal.getScheduleSpec();
        ScheduleSpec.ScheduleType scheduleType = spec != null ? spec.getScheduleType() : null;

        if (scheduleType == null) {
            return calculateMonthlyPeriod(effectiveReference);
        }

        return switch (scheduleType) {
            case DAILY -> calculateDailyPeriod(effectiveReference);
            case WEEKLY -> calculateWeeklyPeriod(effectiveReference, resolveWeekStartsOn(spec));
            case MONTHLY -> calculateMonthlyPeriod(effectiveReference);
            case QUARTERLY -> calculateQuarterlyPeriod(effectiveReference);
            case YEARLY -> calculateYearlyPeriod(effectiveReference);
        };
    }

    @Override
    public PeriodRange calculateFirstPeriodRange(Goal goal) {
        validateGoal(goal);
        LocalDate startDate = goal.getStartDate() != null
            ? goal.getStartDate().toLocalDate()
            : LocalDate.now();
        return calculatePeriodRange(goal, startDate);
    }

    private List<GoalPeriod> createExpectedPeriods(
            Goal goal,
            LocalDate startDate,
            LocalDate throughDate,
            Integer maxPeriods,
            boolean fillGaps) {
        List<GoalPeriod> created = new ArrayList<>();
        LocalDate cursor = startDate;
        int createdCount = 0;
        while (cursor != null && !cursor.isAfter(throughDate)) {
            if (maxPeriods != null && maxPeriods > 0 && createdCount >= maxPeriods) {
                break;
            }

            PeriodRange expected = normalizeRange(goal, calculatePeriodRange(goal, cursor).getPeriodStart(), calculatePeriodRange(goal, cursor).getPeriodEnd());
            boolean exists = goalPeriodRepository.existsByParentGoalUuidAndPeriodStartAndPeriodEnd(
                goal.getUuid(),
                expected.getPeriodStart(),
                expected.getPeriodEnd()
            );

            if (!exists) {
                created.add(createPeriodForGoal(goal, expected.getPeriodStart(), expected.getPeriodEnd()));
                createdCount++;
            } else if (!fillGaps) {
                // For forward-only proactive creation, stop once we hit an already materialized period.
                // The caller can request fillGaps=true for full reconciliation.
                createdCount++;
            }

            cursor = expected.getPeriodEnd().plusDays(1);
        }
        return created;
    }

    private Goal resolveGoal(String goalUuid) {
        return goalRepository.findByUuidAndIsDeletedFalse(goalUuid)
            .orElseThrow(() -> new ValidationException("Goal not found for goal period operation"));
    }

    private void validateGoal(Goal goal) {
        if (goal == null || goal.getUuid() == null || goal.getUuid().isBlank()) {
            throw new ValidationException("Valid goal is required for period operation");
        }
    }

    private PeriodRange normalizeRange(Goal goal, LocalDate periodStart, LocalDate periodEnd) {
        if (periodStart == null || periodEnd == null) {
            throw new ValidationException("periodStart and periodEnd are required");
        }
        LocalDate normalizedStart = clampToGoalStart(goal, periodStart);
        LocalDate normalizedEnd = clampToGoalDateBounds(goal, periodEnd);
        if (normalizedEnd == null) {
            normalizedEnd = periodEnd;
        }
        if (normalizedEnd.isBefore(normalizedStart)) {
            throw new ValidationException("Goal period end cannot be before goal period start");
        }
        return new PeriodRange(normalizedStart, normalizedEnd);
    }

    private LocalDate clampToGoalStart(Goal goal, LocalDate date) {
        if (date == null) {
            return null;
        }
        if (goal != null && goal.getStartDate() != null && date.isBefore(goal.getStartDate().toLocalDate())) {
            return goal.getStartDate().toLocalDate();
        }
        return date;
    }

    private LocalDate clampToGoalDateBounds(Goal goal, LocalDate date) {
        LocalDate clamped = clampToGoalStart(goal, date);
        if (clamped == null) {
            return null;
        }
        if (goal != null && goal.getTargetDate() != null) {
            LocalDate targetDate = goal.getTargetDate().toLocalDate();
            if (clamped.isAfter(targetDate)) {
                return null;
            }
            return clamped.isAfter(targetDate) ? targetDate : clamped;
        }
        return clamped;
    }

    private LocalDate determineBulkStart(Goal goal, boolean fillGaps) {
        if (fillGaps) {
            return calculateFirstPeriodRange(goal).getPeriodStart();
        }
        return goalPeriodRepository.findTopByParentGoalUuidOrderByPeriodEndDesc(goal.getUuid())
            .map(period -> period.getPeriodEnd().plusDays(1))
            .orElseGet(() -> calculateFirstPeriodRange(goal).getPeriodStart());
    }

    private void assertNoOverlappingPeriod(
            String goalUuid,
            LocalDate periodStart,
            LocalDate periodEnd,
            String ignorePeriodUuid) {
        boolean overlaps = goalPeriodRepository.findByParentGoalUuid(goalUuid).stream()
            .filter(existing -> ignorePeriodUuid == null || !ignorePeriodUuid.equals(existing.getUuid()))
            .anyMatch(existing ->
                !periodEnd.isBefore(existing.getPeriodStart()) && !periodStart.isAfter(existing.getPeriodEnd())
            );
        if (overlaps) {
            throw new ValidationException("Goal period overlaps an existing period for this goal");
        }
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

    private void initializeHealthSnapshot(GoalPeriod period) {
        period.setProgressPercentage(0.0);
        period.setConsistencyScore(null);
        period.setMomentumScore(null);
        period.setProgressScore(null);
        period.setHealthScore(null);
        period.setHealthStatus(null);
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

    private void syncGoalPeriodPrimaryKeySequence() {
        entityManager.createNativeQuery(
            """
            SELECT setval(
                pg_get_serial_sequence('goal_periods', 'id'),
                COALESCE((SELECT MAX(id) FROM goal_periods), 0) + 1,
                false
            )
            """
        ).getSingleResult();
    }
}
