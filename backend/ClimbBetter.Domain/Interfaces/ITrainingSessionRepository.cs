using ClimbBetter.Domain.Training;

namespace ClimbBetter.Domain.Interfaces;

public interface ITrainingSessionRepository
{
    Task AddAsync(TrainingSession trainingSession, CancellationToken cancellationToken);
    Task<List<TrainingSession>> GetAllAsync(CancellationToken cancellationToken);

    Task<bool> ExistsAsync(
        Guid id,
        CancellationToken cancellationToken);

    Task AddEntryAsync(
        TrainingEntry trainingEntry,
        CancellationToken cancellationToken);
}