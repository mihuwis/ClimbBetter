using ClimbBetter.Domain.Training;

namespace ClimbBetter.Domain.Interfaces;

public interface IDifficultyRepository
{
    Task<List<Difficulty>> GetAllAsync(CancellationToken cancellationToken);

        Task<Difficulty?> GetByIdAsync(
        int id,
        CancellationToken cancellationToken);
}