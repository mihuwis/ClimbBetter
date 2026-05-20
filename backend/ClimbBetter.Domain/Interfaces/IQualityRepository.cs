using ClimbBetter.Domain.Training;

namespace ClimbBetter.Domain.Interfaces;

public interface IQualityRepository
{
    Task<List<Quality>> GetAllAsync(CancellationToken cancellationToken);

        Task<Quality?> GetByIdAsync(
        int id,
        CancellationToken cancellationToken);
}