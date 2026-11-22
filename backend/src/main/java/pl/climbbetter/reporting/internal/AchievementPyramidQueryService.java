package pl.climbbetter.reporting.internal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pl.climbbetter.identity.CurrentUserProvider;
import pl.climbbetter.reporting.AchievementPyramid;
import pl.climbbetter.reporting.AchievementPyramid.Layer;
import pl.climbbetter.reporting.AchievementPyramid.Peak;
import pl.climbbetter.reporting.AchievementPyramidQuery;
import pl.climbbetter.reporting.PyramidClimbsPage;
import pl.climbbetter.reporting.PyramidRequest;
import pl.climbbetter.training.PyramidLevelEstimator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
class AchievementPyramidQueryService implements AchievementPyramidQuery {
    private final PyramidRepository repository;
    private final CurrentUserProvider currentUserProvider;

    AchievementPyramidQueryService(PyramidRepository repository, CurrentUserProvider currentUserProvider) {
        this.repository = repository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public AchievementPyramid findPyramid(PyramidRequest request) {
        UUID userId = currentUserProvider.currentUserId();
        List<PyramidRepository.GradeCount> counts = repository.findCounts(userId, request);
        List<Integer> calibrationVersions = repository.findCalibrationVersions(userId, request);
        int size = counts.stream().mapToInt(PyramidRepository.GradeCount::climbCount).reduce(0, Math::addExact);

        if (request.view() == PyramidRequest.View.COMBINED) {
            List<Layer> layers = counts.stream().map(count -> new Layer(
                    "points:" + count.calibrationVersion() + ":" + count.gradePoints().stripTrailingZeros().toPlainString(),
                    null, null, count.gradePoints(), count.calibrationVersion(), count.climbCount()
            )).toList();
            Peak peak = counts.isEmpty() || calibrationVersions.size() != 1 ? null
                    : new Peak(null, null, counts.getFirst().gradePoints(), counts.getFirst().calibrationVersion());
            return new AchievementPyramid(request, request.windowStart(), request.asOf(), size, layers,
                    peak, null, calibrationVersions, "COMBINED_SCALE",
                    calibrationVersions.size() > 1 ? "MIXED_POINT_CALIBRATIONS" : null);
        }

        Map<Integer, String> labels = new TreeMap<>(repository.findGradeLabels(request.scaleCode()));
        Map<Integer, Integer> countsByIndex = new HashMap<>();
        for (PyramidRepository.GradeCount count : counts) {
            labels.put(count.gradeIndex(), count.gradeLabel());
            countsByIndex.put(count.gradeIndex(), count.climbCount());
        }
        List<Layer> layers = new ArrayList<>();
        Peak peak = null;
        if (!counts.isEmpty()) {
            int maxIndex = counts.getFirst().gradeIndex();
            int minIndex = counts.getLast().gradeIndex();
            labels.entrySet().stream().filter(entry -> entry.getKey() >= minIndex && entry.getKey() <= maxIndex)
                    .sorted(Map.Entry.<Integer, String>comparingByKey().reversed())
                    .forEach(entry -> layers.add(new Layer(
                            "grade:" + request.scaleCode() + ":" + entry.getKey(), entry.getValue(), entry.getKey(),
                            null, null, countsByIndex.getOrDefault(entry.getKey(), 0)
                    )));
            peak = new Peak(labels.get(maxIndex), maxIndex, null, null);
        }
        return new AchievementPyramid(request, request.windowStart(), request.asOf(), size, List.copyOf(layers),
                peak, PyramidLevelEstimator.estimate(countsByIndex, labels), calibrationVersions, null, null);
    }

    @Override
    public PyramidClimbsPage findClimbs(PyramidRequest request, String bucketKey, int offset, int limit) {
        if (offset < 0 || limit < 1 || limit > 100 || offset > Integer.MAX_VALUE - limit) {
            throw new IllegalArgumentException("Offset must be nonnegative and limit must be between 1 and 100");
        }
        PyramidRepository.Bucket bucket = PyramidRepository.Bucket.parse(request, bucketKey);
        UUID userId = currentUserProvider.currentUserId();
        long total = repository.countClimbs(userId, request, bucket);
        List<PyramidClimbsPage.Climb> items = repository.findClimbs(userId, request, bucket, offset, limit);
        Integer nextOffset = (long) offset + items.size() < total ? offset + items.size() : null;
        return new PyramidClimbsPage(request, bucketKey, total, offset, limit, nextOffset, items);
    }
}
