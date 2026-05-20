using ClimbBetter.Domain.Interfaces;
using ClimbBetter.Domain.Training;
using ClimbBetter.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;

namespace ClimbBetter.Infrastructure.Repositories
{
    public class ClimbRepository : IClimbRepository
    {

        private readonly ClimbBetterDbContext _dbContext;
        public ClimbRepository(ClimbBetterDbContext dbcontext)
        {
            _dbContext = dbcontext;
        }

        public async Task<List<Climb>> GetAllAsync(CancellationToken cancellationToken)
        {
            return await _dbContext.Climbs
            .OrderBy(c => c.Name)
            .ToListAsync(cancellationToken);
        }

        public async Task<Climb?> GetByIdAsync(
    Guid id,
    CancellationToken cancellationToken)
{
    return await _dbContext.Climbs
        .FirstOrDefaultAsync(c => c.Id == id, cancellationToken);
}
    }
}