using ClimbBetter.Domain.Interfaces;
using ClimbBetter.Domain.Training;
using ClimbBetter.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;

namespace ClimbBetter.Infrastructure.Repositories
{
    public class ClimbRepository : IClimbRepository
    {

        private readonly ClimbBetterDbContext _dbcontext;
        public ClimbRepository(ClimbBetterDbContext dbcontext)
        {
            _dbcontext = dbcontext;
        }

        public async Task<List<Climb>> GetAllAsync(CancellationToken cancellationToken)
        {
            return await _dbcontext.Climbs
            .OrderBy(c => c.Name)
            .ToListAsync(cancellationToken);
        }
    }
}