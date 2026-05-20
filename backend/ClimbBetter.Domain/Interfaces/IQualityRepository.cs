using ClimbBetter.Domain.Training;

namespace ClimbBetter.Domain.Interfaces;

public interface IQualityRepository
{
    Task<List<Quality>> GetAllAsync(CancellationToken cancellationToken);
}