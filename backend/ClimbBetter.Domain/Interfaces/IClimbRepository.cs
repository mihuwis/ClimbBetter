using ClimbBetter.Domain.Training;

namespace ClimbBetter.Domain.Interfaces
{
    public interface IClimbRepository
    {
        Task<List<Climb>> GetAllAsync(CancellationToken cancellationToken);
    }
}