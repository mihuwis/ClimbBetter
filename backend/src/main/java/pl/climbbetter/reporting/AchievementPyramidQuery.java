package pl.climbbetter.reporting;

public interface AchievementPyramidQuery {
    AchievementPyramid findPyramid(PyramidRequest request);

    PyramidClimbsPage findClimbs(PyramidRequest request, String bucketKey, int offset, int limit);
}
